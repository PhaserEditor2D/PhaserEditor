// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.lic;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import phasereditor.lic.internal.ui.ActivateDialog;
import phasereditor.lic.internal.ui.EvaluationDialog;

/**
 * @author arian
 *
 */
public class LicCore {
	private static final int EVALUATION_MESSAGE_DELAY_MINUTES = 2;

	/**
	 * 
	 */
	public static final String NO_NAME = "<no name>";

	private static String HTTPS_API_GUMROAD_COM_V2_LICENSES_VERIFY = "https://api.gumroad.com/v2/licenses/verify";
	// private static String HTTPS_API_GUMROAD_COM_V2_LICENSES_VERIFY =
	// "http://localhost/gumroad/success.json";

	public static final String PRODUCT_NAME = "Phaser Editor v1.1.2";
	private static final String PRODUCT_KEY = "axjH";
	private static String SECRET_KEY = "30b2575d-728b-49d4-a410-f454fe02d4c4";

	private static CryptoUtil _cryptoUtil = new CryptoUtil();

	public static LicenseInfo activateProduct(String lickey)
			throws IOException, BackingStoreException, URISyntaxException {
		String proxyHost = null;
		int proxyPort = 0;
		String proxyUser = null;
		String proxyPwd = null;

		IProxyService service = PlatformUI.getWorkbench().getService(IProxyService.class);

		IProxyData[] proxyData = service.select(new URI(HTTPS_API_GUMROAD_COM_V2_LICENSES_VERIFY));

		for (IProxyData data : proxyData) {
			proxyHost = data.getHost();
			proxyPort = data.getPort();
			proxyUser = data.getUserId();
			proxyPwd = data.getPassword();

			if (proxyHost != null) {
				break;
			}
		}

		return activateProduct(proxyHost, proxyPort, proxyUser, proxyPwd, lickey);
	}

	private static LicenseInfo activateProduct(String proxyHost, int proxyPort, String proxyUser, String proxyPwd,
			String lickey) throws IOException, BackingStoreException {

		if (proxyUser != null) {
			System.setProperty("http.proxyUser", proxyUser);
			System.setProperty("http.proxyPassword", proxyPwd);
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(proxyUser, proxyPwd.toCharArray());
				}
			});
		}

		URL url = new URL(HTTPS_API_GUMROAD_COM_V2_LICENSES_VERIFY);

		HttpURLConnection con;

		if (proxyHost == null) {
			con = (HttpURLConnection) url.openConnection();
		} else {
			InetSocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
			Proxy proxy = new Proxy(Type.HTTP, addr);
			con = (HttpURLConnection) url.openConnection(proxy);
		}

		con.setRequestMethod("POST");
		con.setDoOutput(true);

		String urlParameters = "product_permalink=" + PRODUCT_KEY + "&license_key=" + lickey;

		try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
			wr.writeBytes(urlParameters);
		}

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		StringBuffer output;
		if (responseCode == 404) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
				String inputLine;
				output = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					output.append(inputLine);
				}
			}
		} else {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String inputLine;
				output = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					output.append(inputLine);
				}
			}
		}

		String rawJson = output.toString();
		LicenseInfo info = getLicenseInfo(new JSONObject(rawJson));

		if (!isEvaluationProduct(info)) {
			registerLicense(info);
		}

		return info;
	}

	public static void registerLicense(LicenseInfo info) throws BackingStoreException {
		Preferences prefs = getLicensePrefsNode();
		try {

			String encryptResponse = _cryptoUtil.encrypt(SECRET_KEY, info.response.toString());
			prefs.put("response", encryptResponse);
			prefs.flush();
		} catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | UnsupportedEncodingException | IllegalBlockSizeException
				| BadPaddingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static LicenseInfo getCurrentLicense() throws Exception {
		Preferences prefs = getLicensePrefsNode();
		String encryptJson = prefs.get("response", null);

		if (encryptJson == null) {
			return null;
		}

		String json = _cryptoUtil.decrypt(SECRET_KEY, encryptJson);
		LicenseInfo info = getLicenseInfo(new JSONObject(json));

		return info;
	}

	private static Preferences getLicensePrefsNode() {
		IEclipsePreferences root = Platform.getPreferencesService().getRootNode();
		Preferences prefs = root.node(ConfigurationScope.SCOPE).node("phasereditor.lic.gumroad." + PRODUCT_NAME);
		return prefs;
	}

	private static LicenseInfo getLicenseInfo(JSONObject resp) {
		LicenseInfo info = new LicenseInfo();

		boolean success = resp.getBoolean("success");

		if (success) {
			JSONObject purchase = resp.getJSONObject("purchase");
			String created = purchase.getString("created_at");
			info.created = LocalDateTime.parse(created, DateTimeFormatter.ISO_DATE_TIME);
			info.refunded = purchase.getBoolean("refunded");
			info.chargebacked = purchase.getBoolean("chargebacked");
			info.id = purchase.getString("id");
			info.productName = purchase.getString("product_name");
			info.email = purchase.getString("email");
			info.fullName = purchase.optString("full_name", NO_NAME);
			info.uses = resp.getInt("uses");
			info.response = resp;
		} else {
			info.message = resp.getString("message");
		}

		info.success = success;

		return info;
	}

	public static void startEvaluationThread() {
		boolean evaluation = isEvaluationProduct();

		if (evaluation) {
			// show evaluation message
			new Thread(new Runnable() {

				@Override
				public void run() {
					long millis = TimeUnit.MINUTES.toMillis(EVALUATION_MESSAGE_DELAY_MINUTES);
					while (isEvaluationProduct()) {
						try {
							Thread.sleep(millis);
							if (isEvaluationProduct()) {
								out.println("This is an evaluation copy.");
								Display display = PlatformUI.getWorkbench().getDisplay();
								display.syncExec(new Runnable() {

									@Override
									public void run() {
										Shell shell = display.getActiveShell();
										if (shell != null) {
											EvaluationDialog dlg = new EvaluationDialog(shell, SWT.NONE);
											dlg.open();
										}
									}
								});
							}
						} catch (Exception e) {
							out.println(e.getMessage());
							// e.printStackTrace();
						}
					}
				}
			}).start();
		}
	}

	public static boolean isEvaluationProduct() {
		boolean evaluation = true;

		try {
			LicenseInfo info = getCurrentLicense();
			evaluation = isEvaluationProduct(info);
		} catch (Exception e1) {
			e1.printStackTrace();
			MessageDialog.openError(Display.getDefault().getActiveShell(), "License Error",
					e1.getClass().getSimpleName() + ": " + e1.getMessage());
		}
		return evaluation;
	}

	@SuppressWarnings("boxing")
	public static boolean isEvaluationProduct(LicenseInfo info) {
		if (info == null) {
			return true;
		}

		if (!info.success || info.chargebacked || info.refunded) {
			out.println(String.format("Evaluation Product: success (%s) chargebacked (%s) refunded (%s)", info.success,
					info.chargebacked, info.refunded));
			return true;
		}

		return false;
	}

	/**
	 * 
	 */
	public static void openActivationDialog() {
		Shell shell = Display.getDefault().getActiveShell();
		try {
			LicenseInfo info = LicCore.getCurrentLicense();
			if (!LicCore.isEvaluationProduct(info)) {
				if (!MessageDialog.openConfirm(shell, "Activation", "This product is licensed by " + info.fullName + " "
						+ info.email + ", do you want to activate it again?")) {
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		ActivateDialog dlg = new ActivateDialog(shell);
		dlg.open();
	}

}
