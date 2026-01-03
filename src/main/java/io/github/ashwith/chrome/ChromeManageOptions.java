package io.github.ashwith.chrome;

import io.github.ashwith.Cookie;
import io.github.ashwith.WebDriver;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class ChromeManageOptions implements WebDriver.Options {

    private final ChromeDriver driver;
    private long implicitWaitMillis = 0;
    private long scriptTimeoutMillis = 30000;
    private long pageLoadTimeoutMillis = 30000;

    ChromeManageOptions(ChromeDriver driver) {
        this.driver = driver;
    }

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public void deleteCookieNamed(String name) {
    }

    @Override
    public void deleteCookie(Cookie cookie) {
    }

    @Override
    public void deleteAllCookies() {
    }

    @Override
    public Set<Cookie> getCookies() {
        return new HashSet<>();
    }

    @Override
    public Cookie getCookieNamed(String name) {
        return null;
    }

    @Override
    public WebDriver.Timeouts timeouts() {
        return new ChromeTimeouts(this);
    }

    @Override
    public WebDriver.Window window() {
        return new ChromeWindow(driver);
    }

    long getImplicitWaitMillis() {
        return implicitWaitMillis;
    }

    void setImplicitWaitMillis(long millis) {
        this.implicitWaitMillis = millis;
    }

    private static class ChromeTimeouts implements WebDriver.Timeouts {
        private final ChromeManageOptions options;

        ChromeTimeouts(ChromeManageOptions options) {
            this.options = options;
        }

        @Override
        public WebDriver.Timeouts implicitlyWait(long time, TimeUnit unit) {
            options.setImplicitWaitMillis(unit.toMillis(time));
            return this;
        }

        @Override
        public WebDriver.Timeouts setScriptTimeout(long time, TimeUnit unit) {
            return this;
        }

        @Override
        public WebDriver.Timeouts pageLoadTimeout(long time, TimeUnit unit) {
            return this;
        }
    }
}
