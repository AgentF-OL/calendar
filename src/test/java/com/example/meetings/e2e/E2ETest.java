package com.example.meetings.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("staging")
@Tag("e2e-tests")
public class E2ETest {

    @Value("${app.base-url}") String baseUrl;

    @LocalServerPort int port;

    String appUrl;

    WebDriver driver;

    WebDriverWait wait;

    String start = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    String end = LocalDateTime.now().plusDays(1).plusHours(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

    @BeforeEach
    public void setup() {
        appUrl = baseUrl + ":" + port;
        driver = new ChromeDriver(new ChromeOptions());
                //.addArguments("--headless=new");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.navigate().to(appUrl);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void shutdown() {
        driver.close();
    }

    public WebElement write(By locator, String text) {
        WebElement element = driver.findElement(locator);
        element.clear();
        element.sendKeys(text);
        return element;
    }

    public void register(String username, String email, String password) {
        driver.findElement(By.cssSelector("a[href='/register']")).click();
        write(By.id("username"), username);
        write(By.id("email"), email);
        write(By.id("password"), password);
        driver.findElement(By.tagName("button")).click();
        wait.until(ExpectedConditions.urlContains("login"));

        assertDoesNotThrow(() -> driver.findElement(By.className("success")));

        login(username, password);
    }

    public void login(String username, String password) {
        write(By.id("username"), username);
        write(By.id("password"), password);
        driver.findElement(By.tagName("button")).click();
        wait.until(ExpectedConditions.urlContains("calendar"));

        assertThat(driver.getTitle()).isEqualToIgnoringCase("Calendar");
    }

    public void logout() {
        driver.findElement(By.cssSelector("nav div form button")).click();
    }

    public void propose(String title, Map<String, String> credentials) {
        driver.findElement(By.cssSelector("a[href='/meetings/new']")).click();
        wait.until(ExpectedConditions.urlContains("meetings/new"));

        assertThat(driver.getTitle()).isEqualToIgnoringCase("Propose a meeting");

        String description = "description";
        String invitees = String.join(",", credentials.keySet());

        write(By.id("title"), title);
        write(By.id("description"), description);
        write(By.id("start"), start);
        write(By.id("end"), end);
        write(By.id("invitees"), invitees);
        driver.findElement(By.cssSelector(".container button")).click();
        wait.until(ExpectedConditions.urlContains("calendar"));

        assertThat(driver.getTitle()).isEqualToIgnoringCase("Calendar");
        assertDoesNotThrow(() -> driver.findElement(By.className("badge tentative")));
        assertThrows(RuntimeException.class, () -> driver.findElement(By.className("invite")));

        logout();
        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            login(entry.getKey(), entry.getValue());
            WebElement invite = driver.findElement(By.className("invite"));
            assertThat(invite.getText()).contains(title);
            logout();
        }
    }

    @Test
    public void testRegisterAccount() {
        register("AgentF", "emailF@gmail.com", "passwordF");
    }

    @Test
    public void testProposeMeeting() {
        register("AgentP", "emailP@gmail.com", "passwordP");
        logout();
        register("AgentA", "emailA@gmail.com", "passwordA");
        logout();
        login("AgentP", "passwordP");
        propose("title", Map.of("AgentA", "passwordA"));
    }
}
