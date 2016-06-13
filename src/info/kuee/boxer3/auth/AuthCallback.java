package info.kuee.boxer3.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWebAuth;

public class AuthCallback extends HttpServlet {

	private static final long serialVersionUID = -4459662300795783870L;

	public AuthCallback() {}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		try {
			DropboxAuth.finish(request);

			response.setContentType("text/html; charset=UTF-8");
			response.getWriter().println("Hello, " + DropboxAuth.getClient().getAccountInfo().displayName + "! Authorization complete.");
		} catch (DbxException ex) {
			System.err.println("Error in DbxWebAuth.authorize: " + ex.getMessage());
			response.setContentType("text/html; charset=UTF-8");
			response.getWriter().println("<H1>Failed to get code.</H1>");
		} catch (DbxWebAuth.BadRequestException ex) {
			log("On /callback: Bad request: " + ex.getMessage());
			response.sendError(400);
		} catch (DbxWebAuth.BadStateException ex) {
			// Send them back to the start of the auth flow.
			response.sendRedirect(AuthServer.getAuthURL());
		} catch (DbxWebAuth.CsrfException ex) {
			log("On /callback: CSRF mismatch: " + ex.getMessage());
			response.sendError(403, "Forbidden.");
		} catch (DbxWebAuth.NotApprovedException ex) {
			// When Dropbox asked "Do you want to allow this app to access your
			// Dropbox account?", the user clicked "No".
			log("On /callback: Not confirmed: " + ex.getMessage());
			response.sendError(403, "Forbidden.");
		} catch (DbxWebAuth.ProviderException ex) {
			log("On /callback: Auth failed: " + ex.getMessage());
			response.sendError(503, "Error communicating with Dropbox.");
		}
	}
}
