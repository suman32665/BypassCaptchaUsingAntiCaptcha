import com.anti_captcha.Api.NoCaptchaProxyless;
import com.anti_captcha.Api.RecaptchaV2EnterpriseProxyless;

import java.net.MalformedURLException;
import java.net.URL;

public class AntiCaptcha {

	private static String ANTICAPTCHA_KEY = "904dd69ed7576220f792f1cc99b9ec64";
	private String token = null;

	public String solveCaptcha(String RUNESCAPE_URL, String sitekey) throws MalformedURLException, InterruptedException {
		//		NoCaptchaProxyless api = new NoCaptchaProxyless();
		RecaptchaV2EnterpriseProxyless api = new RecaptchaV2EnterpriseProxyless();
		api.setClientKey(ANTICAPTCHA_KEY);
		api.setWebsiteUrl(new URL(RUNESCAPE_URL));
		api.setWebsiteKey(sitekey); // 6LccFA0TAAAAAHEwUJx_c1TfTBWMTAOIphwTtd1b
		System.out.println("Sending Task To AntiCaptcha");

		if (!api.createTask()) {
			System.out.println(api.getErrorMessage());
		} else if (!api.waitForResult()) {
			System.out.println("Failed To Solve Captcha");
		} else {
			System.out.println("AntiCaptcha Task Complete");
			token = api.getTaskSolution().getGRecaptchaResponse();
		}

		return token;
	}

}