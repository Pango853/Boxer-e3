package info.kuee.boxer3.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.osgi.service.prefs.Preferences;

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

import info.kuee.boxer3.Activator;
import info.kuee.boxer3.dialogs.AuthPreferenceDialog;

public class DropboxAuth extends HttpServlet {
	private static final long serialVersionUID = -3428174890219532381L;

	private static String SESSION_KEY = "dropbox-auth-csrf-token";

	public DropboxAuth() {}

	private static DbxSessionStore getSessionStore(final HttpSession session) {
		// Select a spot in the session for DbxWebAuth to store the CSRF token.
		return new DbxStandardSessionStore(session, SESSION_KEY);
	}

	private static DbxRequestConfig config = null;
	private static DbxWebAuth webAuth = null;
	private static String csrf = null;
	private static String accessToken = null;

	public static DbxClientV1 getClient(){
		return new DbxClientV1(config, accessToken);
	}

	public static DbxAuthFinish finish(HttpServletRequest request) throws DbxException, BadRequestException, BadStateException, CsrfException, NotApprovedException, ProviderException{
		HttpSession session = request.getSession(true);
		// HACK: Manual add key
		session.setAttribute(SESSION_KEY, csrf);
		DbxSessionStore store = getSessionStore(session);
		DbxAuthFinish authFinish = webAuth.finishFromRedirect(AuthServer.getCallbackURL(), store, request.getParameterMap());
		accessToken = authFinish.getAccessToken();
		return authFinish;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// OAuth 認証サービスの作成
		Preferences pref = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		DbxAppInfo appInfo = new DbxAppInfo(pref.get(AuthPreferenceDialog.PREF_KEY_AUTH_ACCESSKEY, ""), pref.get(AuthPreferenceDialog.PREF_KEY_AUTH_SECRETKEY, ""));
		config = DbxRequestConfig.newBuilder(Activator.PLUGIN_ID).build();
		webAuth = new DbxWebAuth(config, appInfo);

		DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder().withRedirectUri(AuthServer.getCallbackURL(), getSessionStore(request.getSession(true))).build();
		String authorizeUrl = webAuth.authorize(webAuthRequest);

		csrf = (String)request.getSession(false).getAttribute(SESSION_KEY);
		response.sendRedirect(authorizeUrl);
	}
}
