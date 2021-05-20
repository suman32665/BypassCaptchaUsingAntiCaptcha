import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class AccountCreator {

	private static final String RUNESCAPE_URL = "https://secure.runescape.com/m=account-creation/create_account";
	private static final String RANDGEN_URL = "https://randomuser.me/api/?nat=gb";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT x.y; rv:10.0) Gecko/20100101 Firefox/10.0";
	private static String CAPTCHA_SOLVER = "anticaptcha";
	private static String token = null;
	private static File accountFile = new File("accounts.txt");
			//System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "accounts.txt");
	private static JsonObject account;

	public static void main(String[] args) throws Exception {
		System.out.println("Enter number of accounts to make:");
		Scanner sc = new Scanner(System.in);
		int accountsToMake = sc.nextInt();

		for (int x = 1; x <= accountsToMake; ++x) {
			System.out.println("Creating Account #" + x);

			account = getAccountDetails();

//			int attempts = 0;
//
//			while (token == null) {
//				if (attempts < 5) {
//					switch (CAPTCHA_SOLVER) {
//					case "anticaptcha":
//						AntiCaptcha antiCaptcha = new AntiCaptcha();
//						token = antiCaptcha.solveCaptcha(RUNESCAPE_URL);
//						break;
//					case "twocaptcha":
//						// TODO: 2Captcha Support
//						break;
//					}
//					attempts++;
//				} else {
//					System.out.println("Captcha Solver Failed 5 Times - Stopping");
//					System.gc();
//					System.exit(0);
//				}
//			}

			postForm(token, account);
			writeFile(account.get("email").getAsString() + ":" + account.get("password").getAsString() + ":"
					+ account.get("displayname").getAsString());

			if (accountsToMake > 1) {
				System.out.println("Waiting 10 Seconds Before Next Account...");
				TimeUnit.SECONDS.sleep((long) 10);
			}
		}

		System.out.println("Created All Accounts - Stopping");
		System.gc();
		System.exit(0);
	}

	private static void waitForLoad(WebDriver driver) {
		ExpectedCondition<Boolean> pageLoadCondition = new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
			}
		};
		WebDriverWait wait = new WebDriverWait(driver, 30);
		wait.until(pageLoadCondition);
	}

	private static void postForm(String gresponse, JsonObject account)
			throws UnsupportedEncodingException, InterruptedException {
//		System.setProperty("webdriver.chrome.driver", "chromedriver");

		WebDriver driver = new ChromeDriver();
		driver.manage().window().maximize();

		boolean created = false;
		boolean captchaFailed = false;

		while (!created && !captchaFailed) {
			driver.get(RUNESCAPE_URL);
			waitForLoad(driver);

			WebElement dobDay = driver.findElement(By.name("day"));
			WebElement dobMonth = driver.findElement(By.name("month"));
			WebElement dobYear = driver.findElement(By.name("year"));
			WebElement email = driver.findElement(By.name("email1"));
			WebElement password = driver.findElement(By.name("password1"));
//			WebElement displayname = driver.findElement(By.name("displayname"));
			WebElement textarea = null;
			if (driver.findElements(By.id("g-recaptcha-response")).size() != 0) {
				textarea = driver.findElement(By.id("g-recaptcha-response"));;
			}
			WebElement submit = driver.findElement(By.id("create-submit"));

			dobDay.sendKeys("01");
			dobMonth.sendKeys("01");
			dobYear.sendKeys("1990");
			email.sendKeys(account.get("email").getAsString());
			password.sendKeys(account.get("password").getAsString());
//			displayname.sendKeys(account.get("displayname").getAsString());
			

			String sitekey="";
			String src = driver.findElement(By.xpath("//*[@title='reCAPTCHA']")).getAttribute("src");
			Pattern pattern = Pattern.compile(".*&k=(.*)&co.*");
			Matcher matcher = pattern.matcher(src);
			if (matcher.find()) {
				sitekey=matcher.group(1);
			}
			AntiCaptcha antiCaptcha = new AntiCaptcha();
			try {
				token = antiCaptcha.solveCaptcha(RUNESCAPE_URL, sitekey);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			JavascriptExecutor jse = null;
			
			if (textarea != null) {
				jse = (JavascriptExecutor) driver;
				jse.executeScript("arguments[0].style.display = 'block';", textarea);
				textarea.sendKeys(token);
			}
			driver.switchTo().defaultContent();
			if (jse != null)
				jse.executeScript("window.scrollBy(0,250)", "");
			TimeUnit.SECONDS.sleep(3);
			submit.sendKeys(Keys.ENTER);
			TimeUnit.SECONDS.sleep(3);
			waitForLoad(driver);

			if (driver.findElements(By.className("m-character-name-alts__name")).size() != 0) {
				System.out.println("Username In Use - Trying another");
				WebElement newUsername = driver.findElement(By.className("m-character-name-alts__name"));
				account.remove("displayname");
				account.addProperty("displayname", newUsername.getText());
				newUsername.click();
				waitForLoad(driver);
				submit.sendKeys(Keys.ENTER);
				TimeUnit.SECONDS.sleep(3);
			} else if (driver.findElements(By.className("google-recaptcha-error")).size() != 0) {
				captchaFailed = true;
			}

			waitForLoad(driver);

			if (driver.findElements(By.id("p-account-created")).size() != 0) {
				created = true;
				System.out.println("Account Created");
			} else {
				System.out.println("Failed To Create Account - Retrying");
			}

			token = null;
		}

		driver.quit();
	}

	private static JsonObject getAccountDetails() throws Exception {
		String json = readUrl(RANDGEN_URL);
		JsonParser jsonParser = new JsonParser();
		JsonObject firstNameObject = jsonParser.parse(json).getAsJsonObject().getAsJsonArray("results").get(0)
				.getAsJsonObject().getAsJsonObject("name");
		String firstNameString = firstNameObject.get("first").getAsString();
		JsonObject lastNameObject = jsonParser.parse(json).getAsJsonObject().getAsJsonArray("results").get(0)
				.getAsJsonObject().getAsJsonObject("name");
		String lastNameString = lastNameObject.get("last").getAsString();
		Random randMail = new Random();
		int setMail = randMail.nextInt(90) + 10;
		String mail = firstNameString + "." + lastNameString + setMail + "@gmail.com";
		Random rendpass = new Random();
		int setpass = rendpass.nextInt(70) + 10;
		String PASS_STRING = firstNameString.concat(String.valueOf(setpass));
		JsonObject usernames = jsonParser.parse(json).getAsJsonObject().getAsJsonArray("results").get(0)
				.getAsJsonObject().getAsJsonObject("login");
		String user = usernames.get("username").getAsString();
		if (user.length() > 14) {
			Random randNum = new Random();
			int setNum = randNum.nextInt(90) + 10;
			user = user.substring(0, Math.min(user.length(), 10)) + setNum;
		}
		if (PASS_STRING.length() > 10) {
			Random randNum = new Random();
			int setNum = randNum.nextInt(90) + 10;
			PASS_STRING = PASS_STRING.substring(0, Math.min(user.length(), 10)) + setNum;
		}
		JsonObject account = new JsonObject();
		account.addProperty("email", mail);
		account.addProperty("displayname", user);
		account.addProperty("password", PASS_STRING);

		return account;
	}

	private static void writeFile(String account) {
		BufferedWriter writer = null;
		if (accountFile.exists()) {
			try (FileWriter fw = new FileWriter(accountFile, true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter out = new PrintWriter(bw)) {
				out.println(account);
			} catch (IOException e) {
			}
		} else {
			try {
				File logFile = new File("accounts.txt");
//						System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "accounts.txt");
				writer = new BufferedWriter(new FileWriter(logFile));
				writer.write(account + "\r\n");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					writer.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private static String readUrl(String urlString) throws Exception {
		 // configure the SSLContext with a TrustManager
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

		BufferedReader reader = null;

		try {
			URL url = new URL(urlString);
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuilder buffer = new StringBuilder();
			char[] chars = new char[1024];

			int read;
			while ((read = reader.read(chars)) != -1) {
				buffer.append(chars, 0, read);
			}

			String var7 = buffer.toString();
			return var7;
		} finally {
			if (reader != null) {
				reader.close();
			}

		}
	}
	 private static class DefaultTrustManager implements X509TrustManager {

	        @Override
	        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

	        @Override
	        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

	        @Override
	        public X509Certificate[] getAcceptedIssuers() {
	            return null;
	        }
	    }
}