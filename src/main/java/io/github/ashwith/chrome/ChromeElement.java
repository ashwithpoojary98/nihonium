package io.github.ashwith.chrome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ashwith.By;
import io.github.ashwith.Dimension;
import io.github.ashwith.Point;
import io.github.ashwith.Rectangle;
import io.github.ashwith.WebElement;
import io.github.ashwith.cdp.domain.CSSDomain;
import io.github.ashwith.cdp.domain.DOMDomain;
import io.github.ashwith.cdp.domain.InputDomain;
import io.github.ashwith.cdp.domain.RuntimeDomain;
import io.github.ashwith.network.NetworkMonitor;
import io.github.ashwith.wait.AutoWaitEngine;
import io.github.ashwith.wait.ElementWaitConditions;
import io.github.ashwith.wait.WaitConfig;

import java.util.ArrayList;
import java.util.List;

public class ChromeElement implements WebElement {

    private final By locator;
    private final DOMDomain domDomain;
    private final RuntimeDomain runtimeDomain;
    private final InputDomain inputDomain;
    private final CSSDomain cssDomain;
    private final AutoWaitEngine autoWaitEngine;
    private final WaitConfig waitConfig;
    private final NetworkMonitor networkMonitor;

    public ChromeElement(By locator, DOMDomain domDomain, RuntimeDomain runtimeDomain,
                         InputDomain inputDomain, CSSDomain cssDomain) {
        this(locator, domDomain, runtimeDomain, inputDomain, cssDomain, WaitConfig.defaultConfig(), null);
    }

    public ChromeElement(By locator, DOMDomain domDomain, RuntimeDomain runtimeDomain,
                         InputDomain inputDomain, CSSDomain cssDomain, WaitConfig waitConfig) {
        this(locator, domDomain, runtimeDomain, inputDomain, cssDomain, waitConfig, null);
    }

    public ChromeElement(By locator, DOMDomain domDomain, RuntimeDomain runtimeDomain,
                         InputDomain inputDomain, CSSDomain cssDomain, WaitConfig waitConfig, NetworkMonitor networkMonitor) {
        this.locator = locator;
        this.domDomain = domDomain;
        this.runtimeDomain = runtimeDomain;
        this.inputDomain = inputDomain;
        this.cssDomain = cssDomain;
        this.waitConfig = waitConfig;
        this.networkMonitor = networkMonitor;
        ElementWaitConditions conditions = new ElementWaitConditions(domDomain, cssDomain, runtimeDomain);
        this.autoWaitEngine = new AutoWaitEngine(conditions, waitConfig, networkMonitor);
    }

    private int getDocumentNodeId() {
        try {
            JsonObject docResult = domDomain.getDocument().join();
            JsonObject root = docResult.getAsJsonObject("root");
            return root.get("nodeId").getAsInt();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get document node", e);
        }
    }

    private int getNodeId() {
        try {
            int documentNodeId = getDocumentNodeId();
            String cssSelector = locator.toCssSelector();

            if (cssSelector != null) {
                JsonObject result = domDomain.querySelector(documentNodeId, cssSelector).join();
                int nodeId = result.get("nodeId").getAsInt();
                if (nodeId == 0) {
                    throw new RuntimeException("Element not found: " + locator);
                }
                return nodeId;
            } else if (locator.isXPath()) {
                return getNodeIdByXPath(locator.getSelector());
            } else {
                throw new UnsupportedOperationException("Unsupported locator type");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to locate element: " + locator, e);
        }
    }

    private int getNodeIdByXPath(String xpath) {
        try {
            String escapedXPath = xpath.replace("'", "\\'");
            String script = String.format(
                    "document.evaluate('%s', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue",
                    escapedXPath
            );

            JsonObject result = runtimeDomain.evaluate(script, false).join();
            JsonObject resultObj = result.getAsJsonObject("result");

            if (resultObj.get("type").getAsString().equals("object") && resultObj.has("objectId")) {
                String objectId = resultObj.get("objectId").getAsString();
                JsonObject nodeResult = domDomain.requestNode(objectId).join();
                int nodeId = nodeResult.get("nodeId").getAsInt();
                runtimeDomain.releaseObject(objectId).join();

                if (nodeId == 0) {
                    throw new RuntimeException("Element not found: " + locator);
                }
                return nodeId;
            } else {
                throw new RuntimeException("Element not found: " + locator);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to locate element with XPath: " + xpath, e);
        }
    }

    @Override
    public void click() {
        autoWaitEngine.waitForElementClickable(locator);
        try {
            int nodeId = getNodeId();
            domDomain.scrollIntoViewIfNeeded(nodeId).join();

            waitForScrollStability(nodeId, 200);

            JsonObject boxModel = domDomain.getBoxModel(nodeId).join();
            JsonObject model = boxModel.getAsJsonObject("model");
            JsonArray content = model.getAsJsonArray("content");

            double x1 = content.get(0).getAsDouble();
            double y1 = content.get(1).getAsDouble();
            double x2 = content.get(4).getAsDouble();
            double y2 = content.get(5).getAsDouble();

            double centerX = (x1 + x2) / 2;
            double centerY = (y1 + y2) / 2;

            inputDomain.click(centerX, centerY).join();
        } catch (Exception e) {
            throw new RuntimeException("Failed to click element: " + locator, e);
        }
    }

    private void waitForScrollStability(int nodeId, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        Point previousPosition = null;
        int stableCount = 0;
        final int requiredStableChecks = 3;

        while (System.currentTimeMillis() < deadline) {
            Point currentPosition = getStablePosition(nodeId);

            if (previousPosition != null &&
                previousPosition.getX() == currentPosition.getX() &&
                previousPosition.getY() == currentPosition.getY()) {
                stableCount++;
                if (stableCount >= requiredStableChecks) {
                    return;
                }
            } else {
                stableCount = 0;
            }

            previousPosition = currentPosition;

            long remaining = deadline - System.currentTimeMillis();
            if (remaining > 0) {
                long waitTime = Math.min(20, remaining);
                long waitDeadline = System.currentTimeMillis() + waitTime;
                while (System.currentTimeMillis() < waitDeadline) {
                    if (Thread.interrupted()) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private Point getStablePosition(int nodeId) {
        try {
            JsonObject boxModel = domDomain.getBoxModel(nodeId).join();
            JsonObject model = boxModel.getAsJsonObject("model");
            JsonArray content = model.getAsJsonArray("content");

            int x = content.get(0).getAsInt();
            int y = content.get(1).getAsInt();

            return new Point(x, y);
        } catch (Exception e) {
            return new Point(0, 0);
        }
    }

    @Override
    public void submit() {
        try {
            int nodeId = getNodeId();
            JsonObject resolved = domDomain.resolveNode(nodeId).join();
            String objectId = resolved.getAsJsonObject("object").get("objectId").getAsString();

            String script = "this.form ? this.form.submit() : this.submit()";
            runtimeDomain.callFunctionOn(objectId, "function() { " + script + "; }", null).join();
            runtimeDomain.releaseObject(objectId).join();
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit: " + locator, e);
        }
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        autoWaitEngine.waitForElementInteractable(locator);
        try {
            int nodeId = getNodeId();
            domDomain.focus(nodeId).join();

            StringBuilder fullText = new StringBuilder();
            for (CharSequence seq : keysToSend) {
                fullText.append(seq);
            }

            inputDomain.insertText(fullText.toString()).join();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send keys to element: " + locator, e);
        }
    }

    @Override
    public void clear() {
        autoWaitEngine.waitForElementInteractable(locator);
        try {
            int nodeId = getNodeId();
            domDomain.focus(nodeId).join();

            inputDomain.dispatchKeyEvent("keyDown", "a", "KeyA", 2).join();
            inputDomain.dispatchKeyEvent("keyUp", "a", "KeyA", 2).join();
            inputDomain.dispatchKeyEvent("keyDown", "Backspace", "Backspace", 0).join();
            inputDomain.dispatchKeyEvent("keyUp", "Backspace", "Backspace", 0).join();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear element: " + locator, e);
        }
    }

    @Override
    public String getTagName() {
        try {
            int nodeId = getNodeId();
            JsonObject result = domDomain.describeNode(nodeId, 0).join();
            JsonObject node = result.getAsJsonObject("node");
            return node.get("nodeName").getAsString().toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get tag name from element: " + locator, e);
        }
    }

    @Override
    public String getAttribute(String name) {
        try {
            int nodeId = getNodeId();
            JsonObject result = domDomain.getAttributes(nodeId).join();
            JsonArray attributes = result.getAsJsonArray("attributes");

            for (int i = 0; i < attributes.size(); i += 2) {
                String attrName = attributes.get(i).getAsString();
                if (attrName.equals(name)) {
                    return attributes.get(i + 1).getAsString();
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get attribute from element: " + locator, e);
        }
    }

    @Override
    public boolean isSelected() {
        try {
            String checked = getAttribute("checked");
            return checked != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        try {
            String disabled = getAttribute("disabled");
            return disabled == null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getText() {
        autoWaitEngine.waitForElementVisible(locator);
        try {
            int nodeId = getNodeId();
            JsonObject resolved = domDomain.resolveNode(nodeId).join();
            String objectId = resolved.getAsJsonObject("object").get("objectId").getAsString();

            String script = "this.textContent";
            JsonObject result = runtimeDomain.callFunctionOn(objectId,
                    "function() { return " + script + "; }", null).join();

            runtimeDomain.releaseObject(objectId).join();

            JsonObject resultObj = result.getAsJsonObject("result");
            if (resultObj.has("value")) {
                return resultObj.get("value").getAsString();
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException("Failed to get text from element: " + locator, e);
        }
    }

    @Override
    public List<WebElement> findElements(By by) {
        List<WebElement> elements = new ArrayList<>();
        elements.add(new ChromeElement(by, domDomain, runtimeDomain, inputDomain, cssDomain, waitConfig, networkMonitor));
        return elements;
    }

    @Override
    public WebElement findElement(By by) {
        return new ChromeElement(by, domDomain, runtimeDomain, inputDomain, cssDomain, waitConfig, networkMonitor);
    }

    @Override
    public boolean isDisplayed() {
        try {
            String display = getCssValue("display");
            String visibility = getCssValue("visibility");
            String opacity = getCssValue("opacity");

            if ("none".equals(display)) return false;
            if ("hidden".equals(visibility)) return false;
            if ("0".equals(opacity)) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Point getLocation() {
        try {
            int nodeId = getNodeId();
            JsonObject boxModel = domDomain.getBoxModel(nodeId).join();
            JsonObject model = boxModel.getAsJsonObject("model");
            JsonArray content = model.getAsJsonArray("content");

            int x = content.get(0).getAsInt();
            int y = content.get(1).getAsInt();

            return new Point(x, y);
        } catch (Exception e) {
            return new Point(0, 0);
        }
    }

    @Override
    public Dimension getSize() {
        try {
            int nodeId = getNodeId();
            JsonObject boxModel = domDomain.getBoxModel(nodeId).join();
            JsonObject model = boxModel.getAsJsonObject("model");
            JsonArray content = model.getAsJsonArray("content");

            int x1 = content.get(0).getAsInt();
            int x2 = content.get(4).getAsInt();
            int y1 = content.get(1).getAsInt();
            int y2 = content.get(5).getAsInt();

            return new Dimension(x2 - x1, y2 - y1);
        } catch (Exception e) {
            return new Dimension(0, 0);
        }
    }

    @Override
    public Rectangle getRect() {
        Point location = getLocation();
        Dimension size = getSize();
        return new Rectangle(location, size);
    }

    @Override
    public String getCssValue(String propertyName) {
        try {
            int nodeId = getNodeId();
            JsonObject result = cssDomain.getComputedStyleForNode(nodeId).join();
            JsonArray computedStyle = result.getAsJsonArray("computedStyle");

            for (JsonElement element : computedStyle) {
                JsonObject prop = element.getAsJsonObject();
                if (prop.get("name").getAsString().equals(propertyName)) {
                    return prop.get("value").getAsString();
                }
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException("Failed to get CSS value from element: " + locator, e);
        }
    }

    @Override
    public String toString() {
        return "ChromeElement[" + locator + "]";
    }
}
