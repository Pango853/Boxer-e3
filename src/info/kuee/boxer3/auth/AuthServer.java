package info.kuee.boxer3.auth;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class AuthServer {
	private static final String AUTH_PATH = "/auth";
	private static final String CALLBACK_PATH = "/callback";
	private static final int PORT = 18089;

	private static Server server;

	public static String getAuthURL(){
		return String.format("http://localhost:%d%s", PORT, AUTH_PATH);
	}

	public static String getCallbackURL(){
		return String.format("http://localhost:%d%s", PORT, CALLBACK_PATH);
	}

	public static void start() throws Exception {
		server = new Server(PORT);

		ServletContextHandler context0 = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context0.setContextPath(AUTH_PATH);
		context0.addServlet(new ServletHolder(new AuthPage()), "/*");

		ServletContextHandler context1 = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context1.setContextPath(CALLBACK_PATH);
		context1.addServlet(new ServletHolder(new CallBackPage()), "/*");

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(new Handler[] { context0, context1 });
		server.setHandler(contexts);

		server.start();
	}

	public static void stop() throws Exception {
		if(null != server && server.isStarted()){
			server.stop();
		}
	}
}
