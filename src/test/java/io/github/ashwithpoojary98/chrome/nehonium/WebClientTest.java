package io.github.ashwithpoojary98.chrome.nehonium;

import io.github.ashwithpoojary98.By;
import io.github.ashwithpoojary98.Dimension;
import io.github.ashwithpoojary98.WebDriver;
import io.github.ashwithpoojary98.WebElement;
import io.github.ashwithpoojary98.chrome.ChromeDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebClientTest {
    WebDriver webDriver;

    @BeforeEach
    void setUp() {
        webDriver = new ChromeDriver();
    }

    @Test
    void testWebClientTest() {
        webDriver.get("https://asynccodinghub.in/");
        String title = webDriver.getTitle();
        Assertions.assertTrue(title != null && !title.isEmpty(), "Title should not be null or empty");
        webDriver.quit();
    }

    @Test
    void testNavigation() {

        webDriver.get("https://asynccodinghub.in/");
        String initialTitle = webDriver.getTitle();

        webDriver.navigate().to("https://www.google.com");
        String googleTitle = webDriver.getTitle();
        Assertions.assertNotEquals(initialTitle, googleTitle, "Titles should be different after navigation");

        webDriver.navigate().back();
        Assertions.assertEquals(initialTitle, webDriver.getTitle(), "Should navigate back to initial page");

        webDriver.navigate().forward();
        Assertions.assertEquals(googleTitle, webDriver.getTitle(), "Should navigate forward to Google");

        webDriver.quit();
    }

    @Test
    void testWindowManagement() {
        webDriver.get("https://asynccodinghub.in/");

        Dimension initialSize = webDriver.manage().window().getSize();
        webDriver.manage().window().maximize();
        Dimension maximizedSize = webDriver.manage().window().getSize();

        Assertions.assertTrue(maximizedSize.getWidth() >= initialSize.getWidth(), "Width should be greater or equal after maximizing");
        Assertions.assertTrue(maximizedSize.getHeight() >= initialSize.getHeight(), "Height should be greater or equal after maximizing");

        webDriver.quit();
    }

    @Test
    void testFindElement() {
        webDriver.get("https://asynccodinghub.in/");

        WebElement heading = webDriver.findElement(By.tagName("h1"));
        Assertions.assertNotNull(heading, "Heading element should not be null");
        Assertions.assertFalse(heading.getText().isEmpty(), "Heading text should not be empty");

        webDriver.quit();
    }

    @Test
    void testSendKeys() {
        webDriver.get("https://www.google.com");

        WebElement searchBox = webDriver.findElement(By.name("q"));
        searchBox.sendKeys("Neutronium browser automation");

        String value = searchBox.getAttribute("value");
        Assertions.assertEquals("Neutronium browser automation", value, "Search box value should match the input");

        webDriver.quit();
    }

    @Test
    void testClickElement() {
        webDriver.get("https://asynccodinghub.in/");

        WebElement aboutLink = webDriver.findElement(By.xpath("//a[text()='Roadmap']"));
        aboutLink.click();

        String currentUrl = webDriver.getCurrentUrl();
        Assertions.assertEquals(currentUrl, "https://asynccodinghub.in/roadmap.html", "URL should contain 'about' after clicking the About link");

        webDriver.quit();
    }

    @AfterEach
    void tearDown() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }
}
