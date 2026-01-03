package io.github.ashwith;

public abstract class By {

    protected final String selector;

    protected By(String selector) {
        this.selector = selector;
    }

    public String getSelector() {
        return selector;
    }

    public abstract String toCssSelector();

    public abstract boolean isXPath();

    public static By cssSelector(String selector) {
        return new ByCssSelector(selector);
    }

    public static By xpath(String xpath) {
        return new ByXPath(xpath);
    }

    public static By id(String id) {
        return new ById(id);
    }

    public static By className(String className) {
        return new ByClassName(className);
    }

    public static By tagName(String tagName) {
        return new ByTagName(tagName);
    }

    public static By name(String name) {
        return new ByName(name);
    }

    public static By linkText(String linkText) {
        return new ByLinkText(linkText);
    }

    public static By partialLinkText(String linkText) {
        return new ByPartialLinkText(linkText);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + selector;
    }

    private static class ByCssSelector extends By {
        ByCssSelector(String selector) {
            super(selector);
        }

        @Override
        public String toCssSelector() {
            return selector;
        }

        @Override
        public boolean isXPath() {
            return false;
        }
    }

    private static class ByXPath extends By {
        ByXPath(String xpath) {
            super(xpath);
        }

        @Override
        public String toCssSelector() {
            return null;
        }

        @Override
        public boolean isXPath() {
            return true;
        }
    }

    private static class ById extends By {
        ById(String id) {
            super(id);
        }

        @Override
        public String toCssSelector() {
            return "#" + selector;
        }

        @Override
        public boolean isXPath() {
            return false;
        }
    }

    private static class ByClassName extends By {
        ByClassName(String className) {
            super(className);
        }

        @Override
        public String toCssSelector() {
            return "." + selector;
        }

        @Override
        public boolean isXPath() {
            return false;
        }
    }

    private static class ByTagName extends By {
        ByTagName(String tagName) {
            super(tagName);
        }

        @Override
        public String toCssSelector() {
            return selector;
        }

        @Override
        public boolean isXPath() {
            return false;
        }
    }

    private static class ByName extends By {
        ByName(String name) {
            super(name);
        }

        @Override
        public String toCssSelector() {
            return "[name=\"" + selector + "\"]";
        }

        @Override
        public boolean isXPath() {
            return false;
        }
    }

    private static class ByLinkText extends By {
        ByLinkText(String linkText) {
            super(linkText);
        }

        @Override
        public String toCssSelector() {
            return null;
        }

        @Override
        public boolean isXPath() {
            return true;
        }
    }

    private static class ByPartialLinkText extends By {
        ByPartialLinkText(String linkText) {
            super(linkText);
        }

        @Override
        public String toCssSelector() {
            return null;
        }

        @Override
        public boolean isXPath() {
            return true;
        }
    }
}
