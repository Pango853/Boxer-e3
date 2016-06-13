package info.kuee.boxer3.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthPage extends HttpServlet {
	private static final long serialVersionUID = -3428174890219532381L;

	public AuthPage() {}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect(AuthUtil.getAuthURL(request));
	}
}
