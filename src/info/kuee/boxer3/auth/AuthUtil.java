package info.kuee.boxer3.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxStandardSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.DbxWebAuth.BadRequestException;
import com.dropbox.core.DbxWebAuth.BadStateException;
import com.dropbox.core.DbxWebAuth.CsrfException;
import com.dropbox.core.DbxWebAuth.NotApprovedException;
import com.dropbox.core.DbxWebAuth.ProviderException;
import com.dropbox.core.v1.DbxClientV1;
import com.dropbox.core.v2.DbxClientV2;

import info.kuee.boxer3.Activator;
import info.kuee.boxer3.ui.dialogs.AccessKeyDialog;
import info.kuee.boxer3.ui.dialogs.LoginDialog;
import info.kuee.boxer3.util.ConsoleUtil;
import info.kuee.boxer3.util.Error;

public class AuthUtil {
	private static final MessageConsole console = ConsoleUtil.findConsole(AuthUtil.class.getSimpleName());

	private static final Logger logger = LoggerFactory.getLogger(AuthUtil.class);

	public static final String PREF_KEY_AUTH_ACCESSKEY = "AUTH.ACCESS_KEY";
	public static final String PREF_KEY_AUTH_SECRETKEY = "AUTH.SECRET_KEY";

	private static final String PREF_KEY_CSRF = "CSRF";
	private static final String PREF_KEY_ACCESSTOKEN = "ACCESSTOKEN";
	private static final String SESSION_KEY = "dropbox-auth-csrf-token";


	public static class AccessKeyPair{
		public String AccessKey;
		public String SecretKey;
		public AccessKeyPair(String accessKey, String secretKey) {
			AccessKey = accessKey;
			SecretKey = secretKey;
		}
//		public boolean isValid(){
//			return null != SecretKey && null != AccessKey && !SecretKey.isEmpty() && !AccessKey.isEmpty();
//		}
	}

	private static Preferences pref = null;
	private static DbxClientV1 client = null;
	private static DbxClientV2 clientv2 = null;

	private static Preferences myPreferences(){
		if(null == pref){
			pref = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		}
		return pref;
	}

	public static DbxRequestConfig getRequestConfig() {
		return DbxRequestConfig.newBuilder(Activator.PLUGIN_ID).build();
	}

	public static DbxWebAuth getWebAuth() {
		// OAuth 認証サービスの作成
		AccessKeyPair keyPair = getAccessKeyPair();
		DbxAppInfo appInfo = new DbxAppInfo(keyPair.AccessKey, keyPair.SecretKey);
		return new DbxWebAuth(getRequestConfig(), appInfo);
	}

	public static boolean isLoggedIn() {
		boolean hasCSRF = null != myPreferences().get(PREF_KEY_CSRF, null);
		boolean hasToken = null != myPreferences().get(PREF_KEY_ACCESSTOKEN, null);
		return hasCSRF && hasToken;
	}

	public static void storeCSRF(String csrf){
		Preferences pref = myPreferences();
		pref.put(PREF_KEY_CSRF, csrf);
		try {
			pref.flush();
		} catch (BackingStoreException e) {
			logger.error(null, e);
		}
	}

	public static String getCSRF(){
		return myPreferences().get(PREF_KEY_CSRF, null);
	}

	public static void storeAccessToken(String token){
		Preferences pref = myPreferences();
		pref.put(PREF_KEY_ACCESSTOKEN, token);
		try {
			pref.flush();
		} catch (BackingStoreException e) {
			logger.error(null, e);
		}
	}

	public static String getAccessToken(){
		return myPreferences().get(PREF_KEY_ACCESSTOKEN, null);
	}

	public static void forgetToken(){
		Preferences pref = myPreferences();
		pref.remove(PREF_KEY_CSRF);
		pref.remove(PREF_KEY_CSRF);
		try {
			pref.flush();
		} catch (BackingStoreException e) {
			logger.error(null, e);
		}
	}

	public static DbxClientV1 getClient(){
		if(null == client)
			client = new DbxClientV1(AuthUtil.getRequestConfig(), getAccessToken());
		return client;
	}

	public static DbxClientV2 getClientV2(String identifier){
		if(null == clientv2){
			// Create a DbxClientV2, which is what you use to make API calls.
			DbxRequestConfig requestConfig = new DbxRequestConfig(identifier);
			clientv2 = new DbxClientV2(requestConfig, getAccessToken());
		}
		return clientv2;
	}


	private static DbxSessionStore getSessionStore(final HttpSession session) {
		// Select a spot in the session for DbxWebAuth to store the CSRF token.
		return new DbxStandardSessionStore(session, SESSION_KEY);
	}

	public static DbxSessionStore prepareSessionStore(final HttpServletRequest request) {
		HttpSession session = request.getSession(true);
		// HACK: Manual add key
		session.setAttribute(SESSION_KEY, AuthUtil.getCSRF());
		return getSessionStore(session);
	}

	public static DbxAuthFinish finish(HttpServletRequest request) throws DbxException, BadRequestException, BadStateException, CsrfException, NotApprovedException, ProviderException{
		DbxAuthFinish authFinish = AuthUtil.getWebAuth().finishFromRedirect(AuthServer.getCallbackURL(),
				AuthUtil.prepareSessionStore(request), request.getParameterMap());
		storeAccessToken(authFinish.getAccessToken());
		return authFinish;
	}

	public static String getAuthURL(final HttpServletRequest request) {
		DbxWebAuth webAuth = getWebAuth();

		final HttpSession session = request.getSession(true);
		DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder().withRedirectUri(AuthServer.getCallbackURL(), getSessionStore(session)).build();
		String authorizeUrl = webAuth.authorize(webAuthRequest);

		// Save for later use
		storeCSRF((String)session.getAttribute(SESSION_KEY));
		return authorizeUrl;
	}

	public static AccessKeyPair getAccessKeyPair() {
		Preferences pref = myPreferences();
		return new AccessKeyPair(pref.get(PREF_KEY_AUTH_ACCESSKEY, null), pref.get(PREF_KEY_AUTH_SECRETKEY, null));
	}

	public static void storeAccessKeyPair(String accessKey, String secretKey) throws BackingStoreException {
		Preferences pref = myPreferences();
		pref.put(PREF_KEY_AUTH_ACCESSKEY, accessKey);
		pref.put(PREF_KEY_AUTH_SECRETKEY, secretKey);
		pref.flush();
	}

	public static boolean isAccessKeyReady() {
		boolean hasAccessKey = null != myPreferences().get(PREF_KEY_AUTH_ACCESSKEY, null);
		boolean hasSecretKey = null != myPreferences().get(PREF_KEY_AUTH_SECRETKEY, null);
		return hasAccessKey && hasSecretKey;
	}

	public static void loginIfNot(Shell shell) {
		// Set access key and secret key if not yet set
		if(!AuthUtil.isAccessKeyReady()){
			Dialog diag = new AccessKeyDialog(shell);
			int result = diag.open();
			if (result == Window.OK) {
				
			}
		}
		
		// Start login
		if(!AuthUtil.isLoggedIn()){
			// Start local login server
			MessageConsoleStream out = console.newMessageStream();
			try {
				out.println("Start auth server...");
				AuthServer.start();

				out.println("Login...");
				Dialog diag = new LoginDialog(shell);
				int result = diag.open();
				if (result == Window.OK) {
					out.println("Login ended.");
					// TODO
				}
			} catch (Exception e) {
				logger.error(Error.AUTH_SERVER_FAILURE, e);
			} finally {
				try{
					out.println("Stop auth server...");
					AuthServer.stop();
				} catch (Exception e) {
					logger.error(Error.AUTH_SERVER_FAILURE, e);
				}

				if(null != out && !out.isClosed()){
					try {
						out.close();
					} catch (IOException e) {}
				}
			}
		}
	}

}
