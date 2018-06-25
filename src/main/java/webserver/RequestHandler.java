package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

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

/*        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        	String line = br.readLine();
        	if (line == null) {
        		return;
        	}

        	log.debug("request line : {}", line);
        	String[] tokens = line.split(" ");
        	
        	int contentLength = 0;
        	boolean logined = false;
        	
        	while (!line.equals("")) {
        		line = br.readLine();
        		log.debug("header : {}", line);
        		
        		if (line.contains("Content-Length")) {
        			contentLength = getContentLength(line);
        			log.debug("contentLength : {}", contentLength);
        		}
        		
        		if (line.contains("Cookie")) {
        			logined = isLogin(line);
        		}
        	}
        	
        	String url = tokens[1];
*/        	
        	HttpRequest request = new HttpRequest(in);
        	HttpResponse response = new HttpResponse(out);
        	
        	String path = getDefaultPath(request.getPath());

        	if ("/user/create".startsWith(path)) {
        		create(request, response);
        	} else if ("/user/login".equals(path)) {
        		login(request, response);
        	} else if ("/user/list".equals(path)) {
        		list(request, response);
        	} else if (path.endsWith(".css")) {
        		cssResponse(out, path);
        	} else {
//        		responseResource(out, path);
        		response.forward(path);
        	}
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

	private void list(HttpRequest request, HttpResponse response) {
		if (!isLogin(request.getHeader("Cookie"))) {
			response.sendRedirect("/user/login.html");
			return;
		}
		Collection<User> users = DataBase.findAll();
		StringBuilder sb = new StringBuilder();
		sb.append("<table border = '1'>");
		for (User user : users) {
			sb.append("<tr>");
			sb.append("<td>" + user.getUserId() + "</td>");
			sb.append("<td>" + user.getName() + "</td>");
			sb.append("<td>" + user.getEmail() + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		response.forwardBody(sb.toString());
		return;
	}

	private void login(HttpRequest request, HttpResponse response) {
		User user = DataBase.findUserById(request.getParameter("userId"));
		if (user != null) {
			if (user.getPassword().equals(request.getParameter("password"))) {
				response.addHeader("Set-Cookie", "logined=true");
				response.sendRedirect("/index.html");
				return;
			} else {
				response.sendRedirect("/user/login_failed.html");
			}
		} else {
			response.sendRedirect("/user/login_failed.html");
		}
	}

	private void create(HttpRequest request, HttpResponse response) {
		User user = new User(request.getParameter("userId"), request.getParameter("password"), request.getParameter("name"), request.getParameter("email"));
		log.debug("User : {}", user);
		DataBase.addUser(user);
		response.sendRedirect("/index.html");
	}

	private void list(OutputStream out, HttpRequest request) throws IOException {
		if (!isLogin(request.getHeader("Cookie"))) {
			responseResource(out, "/user/login.html");
			return;
		}
		Collection<User> users = DataBase.findAll();
		StringBuilder sb = new StringBuilder();
		sb.append("<table border = '1'>");
		for (User user : users) {
			sb.append("<tr>");
			sb.append("<td>" + user.getUserId() + "</td>");
			sb.append("<td>" + user.getName() + "</td>");
			sb.append("<td>" + user.getEmail() + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		byte[] body = sb.toString().getBytes();
		DataOutputStream dos = new DataOutputStream(out);
		response200Header(dos, body.length);
		responseBody(dos, body);
	}

	private void login(OutputStream out, HttpRequest request) throws IOException {
		User user = DataBase.findUserById(request.getParameter("userId"));
		if (user == null) {
			responseResource(out, "/user/login_failed.html");
			return;
		}
		if (user.getPassword().equals(request.getParameter("password"))) {
			DataOutputStream dos = new DataOutputStream(out);
			response302LoginSuccessHeader(dos, "logined=true");
			log.debug("redirect: login success");
		} else {
			responseResource(out, "/user/login_failed.html");
		}
	}

	private void create(OutputStream out, HttpRequest request) {
		User user = new User(request.getParameter("userId"), request.getParameter("password"), request.getParameter("name"), request.getParameter("email"));
		log.debug("User : {}", user);
		DataBase.addUser(user);
		DataOutputStream dos = new DataOutputStream(out);
		response302Header(dos, "/index.html");
	}

	private String getDefaultPath(String path) {
		if (path.equals("/")) {
			return "/index.html";
		}
		
		return path;
	}

	private void cssResponse(OutputStream out, String url) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
		response200CssHeader(dos, body.length);
		responseBody(dos, body);
	}

	private void response200CssHeader(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			dos.writeBytes("Content-Type: text/css\r\n");
			dos.writeBytes("content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void list(OutputStream out, boolean logined) throws IOException {
		if (!logined) {
			responseResource(out, "user/login.html");
			return;
		}
		Collection<User> users = DataBase.findAll();
		StringBuilder sb = new StringBuilder();
		sb.append("<table border = '1'>");
		for (User user : users) {
			sb.append("<tr>");
			sb.append("<td>" + user.getUserId() + "</td>");
			sb.append("<td>" + user.getName() + "</td>");
			sb.append("<td>" + user.getEmail() + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		byte[] body = sb.toString().getBytes();
		DataOutputStream dos = new DataOutputStream(out);
		response200Header(dos, body.length);
		responseBody(dos, body);
	}

	private boolean isLogin(String cookieValue) {
        Map<String, String> cookies = HttpRequestUtils.parseCookies(cookieValue);
        String value = cookies.get("logined");
        if (value == null) {
            return false;
        }
        
        return Boolean.parseBoolean(value);
	}

	private void login(OutputStream out, BufferedReader br, int contentLength) throws IOException {
		String body = IOUtils.readData(br, contentLength);
		Map<String, String> params = HttpRequestUtils.parseQueryString(body);
		User user = DataBase.findUserById(params.get("userId"));
		if (user == null) {
			responseResource(out, "/user/login_failed.html");
			return;
		}
		if (user.getPassword().equals(params.get("password"))) {
			DataOutputStream dos = new DataOutputStream(out);
			response302LoginSuccessHeader(dos, "logined=true");
			log.debug("redirect: login success");
		} else {
			responseResource(out, "/user/login_failed.html");
		}
	}

	private void create(OutputStream out, BufferedReader br, int contentLength) throws IOException {
		String body = IOUtils.readData(br, contentLength);
		Map<String, String> params = HttpRequestUtils.parseQueryString(body);
		User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
		log.debug("User : {}", user);
		DataOutputStream dos = new DataOutputStream(out);
		response302Header(dos, "/index.html");
		DataBase.addUser(user);
	}
	
	private void response302LoginSuccessHeader(DataOutputStream dos, String cookie) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			dos.writeBytes("Set-Cookie: " + cookie + "; path=/ \r\n");
			dos.writeBytes("location: /index.html \r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void responseResource(OutputStream out, String url) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
		response200Header(dos, body.length);
		responseBody(dos, body);
	}

	private int getContentLength(String line) {
		String[] headerTokens = line.split(":");
        return Integer.parseInt(headerTokens[1].trim());
	}

    private void response302Header(DataOutputStream dos, String url) {
    	try {
    		dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
    		dos.writeBytes("Location: " + url + "\r\n");
    		dos.writeBytes("\r\n");
    	} catch (IOException e) {
    		log.error(e.getMessage());
    	}
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
