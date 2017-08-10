package webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

        	HttpRequest request = new HttpRequest(in);
        	HttpResponse response = new HttpResponse(out);
        	
        	Controller controller = RequestMapping.getController(request.getPath());
        	if (controller == null) {
        		String path = getDefualtPath(request.getPath());
				response.forward(path);
        	} else {
        		controller.service(request, response);
        	}
        	
//        	String path = getDefualtPath(request.getPath());
//        	
//        	if ("/user/create".equals(path)) {
//        		createUser(request, response);
//        	} else if ("/user/login".equals(path)) {
//        		login(request, response);
//        	} else if ("/user/list".equals(path)) {
//        		listUser(request, response);
//        	} else {
//        		response.forward(path);
//        	}
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
//    
//    private void listUser(HttpRequest request, HttpResponse response) {
//    	if (!isLogin(request.getHeader("Cookie"))) {
//			response.sendRedirect("/user/login.html");
//			return;
//		}
//		
//		Collection<User> users = DataBase.findAll();
//		StringBuilder sb = new StringBuilder();
//		sb.append("<table border='1'>");
//		for (User user : users) {
//			sb.append("<tr>");
//			sb.append("<td>" + user.getUserId() + "</td>");
//			sb.append("<td>" + user.getName() + "</td>");
//			sb.append("<td>" + user.getEmail() + "</td>");
//			sb.append("</tr>");
//		}
//		response.forwardBody(sb.toString());
//	}
//
//	private void login(HttpRequest request, HttpResponse response) {
//    	User user = DataBase.findUserById(request.getParameter("userId"));
//		if (user != null) {
//			if (user.login(request.getParameter("password"))) {
//				response.addHeader("Set-Cookie", "logined=true");
//				response.sendRedirect("/index.html");
//			} else {
//				response.sendRedirect("/user/login_failed.html");
//			}
//		} else {
//			response.sendRedirect("/user/login_failed.html");
//		}
//	}
//
//	private void createUser(HttpRequest request, HttpResponse response) {
//    	User user = new User(
//				request.getParameter("userId"),
//				request.getParameter("password"),
//				request.getParameter("name"),
//				request.getParameter("email"));
//		log.debug("user : {}", user);
//		DataBase.addUser(user);
//		response.sendRedirect("/index.html");
//	}
//
//	private boolean isLogin(String cookieValue) {
//		Map<String, String> cookies = HttpRequestUtils.parseCookies(cookieValue);
//		String value = cookies.get("logined");
//		if (value == null) {
//			return false;
//		}
//		return Boolean.parseBoolean(value);
//	}

	private String getDefualtPath(String path) {
		if (path.equals("/")) {
			return "/index.html";
		}
		return path;
	}
}
