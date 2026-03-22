package io.github.ashwithpoojary98;

/**
 * Mechanism used to locate elements within a document.
 *
 * <p>Use the static factory methods to create locators:
 * <pre>{@code
 * driver.findElement(By.id("submit-button"));
 * driver.findElement(By.cssSelector(".nav > a"));
 * driver.findElement(By.xpath("//input[@type='email']"));
 * driver.findElement(By.linkText("Sign in"));
 * }</pre>
 *
 * <p>Internally every locator converts to either a CSS selector string
 * (via {@link #toCssSelector()}) or an XPath expression (via {@link #getSelector()}
 * when {@link #isXPath()} is {@code true}).
 *
 * <p>Note: {@code ByLinkText} and {@code ByPartialLinkText} are represented as
 * XPath expressions internally so they can be dispatched through the same
 * code path as {@code ByXPath}.
 */
public abstract class By {

    /** The underlying selector string (CSS selector or XPath expression). */
    protected final String selector;

    protected By(String selector) {
        this.selector = selector;
    }

    /**
     * Returns the selector string used for CDP queries.
     * <p>For CSS-based locators this is the CSS selector.
     * For XPath-based locators (including link text) this is the XPath expression.
     *
     * @return selector string
     */
    public String getSelector() {
        return selector;
    }

    /**
     * Returns the CSS selector equivalent of this locator, or {@code null} if
     * this locator must be evaluated as XPath.
     *
     * @return CSS selector string, or {@code null}
     */
    public abstract String toCssSelector();

    /**
     * Returns {@code true} if this locator must be dispatched via XPath evaluation.
     *
     * @return {@code true} for XPath and link-text locators
     */
    public abstract boolean isXPath();

    // ── Factory methods ───────────────────────────────────────────────────────

    /** Locates elements by CSS selector. */
    public static By cssSelector(String selector) {
        return new ByCssSelector(selector);
    }

    /** Locates elements by XPath expression. */
    public static By xpath(String xpath) {
        return new ByXPath(xpath);
    }

    /** Locates elements by their {@code id} attribute (equivalent to {@code #id}). */
    public static By id(String id) {
        return new ById(id);
    }

    /** Locates elements by one of their CSS class names. */
    public static By className(String className) {
        return new ByClassName(className);
    }

    /** Locates elements by tag name. */
    public static By tagName(String tagName) {
        return new ByTagName(tagName);
    }

    /** Locates elements by their {@code name} attribute. */
    public static By name(String name) {
        return new ByName(name);
    }

    /**
     * Locates anchor ({@code <a>}) elements whose visible text exactly matches
     * {@code linkText}.
     *
     * <p>Internally converted to an XPath expression:
     * {@code //a[normalize-space(.)='linkText']}.
     */
    public static By linkText(String linkText) {
        return new ByLinkText(linkText);
    }

    /**
     * Locates anchor ({@code <a>}) elements whose visible text contains
     * {@code linkText} as a substring.
     *
     * <p>Internally converted to an XPath expression:
     * {@code //a[contains(normalize-space(.),'linkText')]}.
     */
    public static By partialLinkText(String linkText) {
        return new ByPartialLinkText(linkText);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + selector;
    }

    // ── Implementations ───────────────────────────────────────────────────────

    private static final class ByCssSelector extends By {
        ByCssSelector(String selector) { super(selector); }

        @Override public String  toCssSelector() { return selector; }
        @Override public boolean isXPath()        { return false; }
    }

    private static final class ByXPath extends By {
        ByXPath(String xpath) { super(xpath); }

        @Override public String  toCssSelector() { return null; }
        @Override public boolean isXPath()        { return true; }
    }

    private static final class ById extends By {
        ById(String id) { super(id); }

        @Override public String  toCssSelector() { return "#" + selector; }
        @Override public boolean isXPath()        { return false; }
    }

    private static final class ByClassName extends By {
        ByClassName(String className) { super(className); }

        @Override public String  toCssSelector() { return "." + selector; }
        @Override public boolean isXPath()        { return false; }
    }

    private static final class ByTagName extends By {
        ByTagName(String tagName) { super(tagName); }

        @Override public String  toCssSelector() { return selector; }
        @Override public boolean isXPath()        { return false; }
    }

    private static final class ByName extends By {
        ByName(String name) { super(name); }

        @Override public String  toCssSelector() { return "[name=\"" + selector + "\"]"; }
        @Override public boolean isXPath()        { return false; }
    }

    /**
     * Matches {@code <a>} elements whose normalised text equals the given link text.
     *
     * <p>The raw link text is stored in {@link #selector} for human-readable
     * {@link #toString()} output. {@link #getSelector()} returns the generated XPath.
     */
    private static final class ByLinkText extends By {

        private final String linkText;

        ByLinkText(String linkText) {
            // selector field holds the XPath so it flows through the XPath dispatch path
            super("//a[normalize-space(.)=" + xpathStringLiteral(linkText) + "]");
            this.linkText = linkText;
        }

        @Override public String  toCssSelector() { return null; }
        @Override public boolean isXPath()        { return true; }

        @Override
        public String toString() {
            return "ByLinkText: " + linkText;
        }
    }

    /**
     * Matches {@code <a>} elements whose normalised text contains the given substring.
     *
     * <p>The raw substring is stored for {@link #toString()} output.
     * {@link #getSelector()} returns the generated XPath.
     */
    private static final class ByPartialLinkText extends By {

        private final String partialText;

        ByPartialLinkText(String partialText) {
            super("//a[contains(normalize-space(.)," + xpathStringLiteral(partialText) + ")]");
            this.partialText = partialText;
        }

        @Override public String  toCssSelector() { return null; }
        @Override public boolean isXPath()        { return true; }

        @Override
        public String toString() {
            return "ByPartialLinkText: " + partialText;
        }
    }

    // ── XPath helpers ─────────────────────────────────────────────────────────

    /**
     * Produces a safe XPath string literal for the given value, handling
     * single quotes by using the XPath {@code concat()} function.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "hello"}   → {@code 'hello'}</li>
     *   <li>{@code "it's"}    → {@code concat('it',"'",'s')}</li>
     * </ul>
     *
     * @param value raw string value
     * @return XPath string literal safe for embedding in an XPath expression
     */
    static String xpathStringLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        // Split on single quotes and concat the parts
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = value.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(",\"'\",");
            }
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }
}
