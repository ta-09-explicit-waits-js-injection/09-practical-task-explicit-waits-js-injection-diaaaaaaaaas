package com.softserve.academy;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreenCityNegativeRegistrationTest {
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;

    @BeforeAll
    static void setUp() {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--lang=en-GB");
        options.setExperimentalOption("prefs", java.util.Map.of("intl.accept_languages", "en-GB,en"));

        if (System.getenv("GITHUB_ACTIONS") != null) {
            options.addArguments(
                    "--headless=new",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--window-size=1920,1080");
        }

        driver = WebDriverManager.chromedriver().capabilities(options).create();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        js = (JavascriptExecutor) driver;

        if (System.getenv("GITHUB_ACTIONS") == null) {
            driver.manage().window().maximize();
        }
    }

    @BeforeEach
    void openRegistrationForm() {
        driver.manage().deleteAllCookies();
        driver.navigate().to("https://www.greencity.cx.ua/#/greenCity");
        WebElement signUp = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector(".header_sign-up-btn > span")));
        js.executeScript("arguments[0].click();", signUp);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("email")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidEmailValues")
    @DisplayName("Invalid email values -> email error")
    void shouldShowErrorForInvalidEmail(String scenario, String email) {
        typeEmail(email);
        blur();
        assertEmailErrorVisible();
        assertSignUpButtonDisabled();
    }

    @Test
    @DisplayName("All fields empty → required errors shown")
    void shouldShowErrorsForAllEmptyFields() {
        driver.findElement(By.id("email")).click();
        driver.findElement(By.id("firstName")).click();
        driver.findElement(By.id("password")).click();
        driver.findElement(By.id("repeatPassword")).click();
        blur();

        assertEmailErrorVisible();
        assertUsernameErrorVisible();
        assertSignUpButtonDisabled();
    }

    @Test
    @DisplayName("Empty username → username required")
    void shouldShowErrorForEmptyUsername() {
        typeEmail("valid@email.com");
        typePassword("ValidPass123!");
        typeConfirm("ValidPass123!");
        driver.findElement(By.id("firstName")).click();
        blur();

        assertUsernameErrorVisible();
        assertSignUpButtonDisabled();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPasswords")
    @DisplayName("Invalid password values -> password rule error")
    void shouldShowErrorForInvalidPassword(String scenario, String password) {
        fillValidRegistrationDataWithoutConfirm();
        typePassword(password);
        blur();

        assertPasswordErrorVisible();
        assertSignUpButtonDisabled();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConfirmPasswordActions")
    @DisplayName("Invalid confirm password scenarios -> confirm error")
    void shouldShowErrorForInvalidConfirmPassword(
            String scenario,
            boolean shouldTypeConfirmPassword,
            String confirmPasswordValue,
            String expectedMessagePart
    ) {
        fillValidRegistrationDataWithoutConfirm();
        typePassword("ValidPass123!");
        applyConfirmPasswordState(shouldTypeConfirmPassword, confirmPasswordValue);
        blur();

        assertConfirmPasswordErrorVisible();
        assertConfirmPasswordErrorContains(expectedMessagePart);
        assertSignUpButtonDisabled();
    }

    private static Stream<Arguments> invalidEmailValues() {
        return Stream.of(
                Arguments.of("Email without @", "invalid-email"),
                Arguments.of("Email without domain", "test@"),
                Arguments.of("Email without username", "@gmail.com"),
                Arguments.of("Double @", "test@@gmail.com"),
                Arguments.of("Space in email", "test @gmail.com"),
                Arguments.of("Empty email", " "),
                Arguments.of("Only text", "test"),
                Arguments.of("Single character email", "a")
        );
    }

    private static Stream<Arguments> invalidPasswords() {
        return Stream.of(
                Arguments.of("Password with space", "Pass 123!"),
                Arguments.of("Only letters", "Password"),
                Arguments.of("Only numbers", "12345678"),
                Arguments.of("Only special chars", "!!!!!!!!"),
                Arguments.of("No uppercase", "password123!"),
                Arguments.of("Too short (1 char)", "A"),
                Arguments.of("Minimum weak boundary (7 chars)", "Pass12"),
                Arguments.of("Empty password", "")
        );
    }

    private static Stream<Arguments> invalidConfirmPasswordActions() {
        return Stream.of(
                Arguments.of("Confirm password mismatch", true, "DifferentPass123!", ""),
                Arguments.of("Empty confirm password", false, "", "required"),
                Arguments.of("Whitespace only", true, "   ", ""),
                Arguments.of("Extra character", true, "ValidPass123!!", ""),
                Arguments.of("Single char confirm", true, "A", ""),
                Arguments.of("Long mismatched confirm", true, "VeryLongDifferentPassword123456!", "")
        );
    }

    private void typeEmail(String value) {
        WebElement field = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        field.click();
        field.clear();
        field.sendKeys(value);
    }

    private void typeUsername(String value) {
        WebElement field = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("firstName")));
        field.click();
        field.clear();
        field.sendKeys(value);
    }

    private void typePassword(String value) {
        WebElement field = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        field.click();
        js.executeScript(
                "arguments[0].value = '';" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                field);
        field.sendKeys(value);
    }


    private void typeConfirm(String value) {
        WebElement field = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("repeatPassword")));
        field.click();
        field.clear();
        field.sendKeys(value);
    }

    private void fillValidRegistrationDataWithoutConfirm() {
        typeEmail("valid@email.com");
        typeUsername("ValidUsername");
        typePassword("ValidPass123!");
    }

    private void applyConfirmPasswordState(boolean shouldTypeConfirmPassword, String confirmPasswordValue) {
        WebElement confirm = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("repeatPassword")));
        if (shouldTypeConfirmPassword) {
            typeConfirm(confirmPasswordValue);
        } else {
            confirm.click();
            js.executeScript("arguments[0].blur();", confirm);
        }
    }

    private void blur() {
        js.executeScript("document.activeElement.blur();");
    }

    private void assertEmailErrorVisible() {
        WebElement error = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email-err-msg")));
        assertTrue(error.isDisplayed(), "Email error message should be visible");
    }

    private void assertUsernameErrorVisible() {
        WebElement error = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@id='firstName']/following-sibling::div")));
        assertTrue(error.isDisplayed(), "Username error message should be visible");
    }

    private void assertPasswordErrorVisible() {
        WebElement passwordField = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("password")));
        wait.until(driver -> passwordField.getAttribute("class").contains("ng-invalid"));
        assertTrue(passwordField.getAttribute("class").contains("ng-invalid"),
                "Password field should be marked invalid");
    }

    private void assertConfirmPasswordErrorVisible() {
        WebElement error = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("confirm-err-msg")));
        assertTrue(error.isDisplayed(), "Confirm password error message should be visible");
    }

    private void assertConfirmPasswordErrorContains(String expectedMessagePart) {
        WebElement error = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("confirm-err-msg")));
        String actualMessage = error.getText().toLowerCase();
        assertTrue(
                actualMessage.contains(expectedMessagePart.toLowerCase()),
                "Confirm password error '" + actualMessage + "' should contain '" + expectedMessagePart + "'");
    }

    private void assertSignUpButtonDisabled() {
        WebElement btn = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("button[type='submit'].greenStyle")));
        assertFalse(btn.isEnabled(), "The 'Sign Up' button should be disabled");
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
