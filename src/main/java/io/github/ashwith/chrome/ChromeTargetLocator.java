package io.github.ashwith.chrome;

import io.github.ashwith.WebDriver;
import io.github.ashwith.WebElement;

class ChromeTargetLocator implements WebDriver.TargetLocator {

    private final ChromeDriver driver;

    ChromeTargetLocator(ChromeDriver driver) {
        this.driver = driver;
    }

    @Override
    public WebDriver frame(int index) {
        return driver;
    }

    @Override
    public WebDriver frame(String nameOrId) {
        return driver;
    }

    @Override
    public WebDriver frame(WebElement frameElement) {
        return driver;
    }

    @Override
    public WebDriver parentFrame() {
        return driver;
    }

    @Override
    public WebDriver window(String nameOrHandle) {
        return driver;
    }

    @Override
    public WebDriver defaultContent() {
        return driver;
    }

    @Override
    public WebElement activeElement() {
        return null;
    }
}
