package io.github.ashwithpoojary98.chrome;

import io.github.ashwithpoojary98.Dimension;
import io.github.ashwithpoojary98.Point;
import io.github.ashwithpoojary98.WebDriver;

class ChromeWindow implements WebDriver.Window {

    private final ChromeDriver driver;

    ChromeWindow(ChromeDriver driver) {
        this.driver = driver;
    }

    @Override
    public Dimension getSize() {
        return new Dimension(1280, 720);
    }

    @Override
    public void setSize(Dimension targetSize) {
    }

    @Override
    public Point getPosition() {
        return new Point(0, 0);
    }

    @Override
    public void setPosition(Point targetPosition) {
    }

    @Override
    public void maximize() {
        try {
            driver.getBrowserDomain().maximizeCurrentWindow().join();
        } catch (Exception e) {
            System.err.println("Failed to maximize window: " + e.getMessage());
        }
    }

    @Override
    public void minimize() {
        try {
            driver.getBrowserDomain().minimizeCurrentWindow().join();
        } catch (Exception e) {
            System.err.println("Failed to minimize window: " + e.getMessage());
        }
    }

    @Override
    public void fullscreen() {
        try {
            driver.getBrowserDomain().fullscreenCurrentWindow().join();
        } catch (Exception e) {
            System.err.println("Failed to fullscreen window: " + e.getMessage());
        }
    }
}
