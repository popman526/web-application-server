package webserver;

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
//        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//        	String line = br.readLine();

        	HttpRequest request = new HttpRequest(in);
        	HttpResponse response = new HttpResponse(out);
        	
        	String path = getDefualtPath(request.getPath());
        	
        	if ("/user/create".equals(path)) {
        		User user = new User(
        				request.getParameter("userId"),
        				request.getParameter("password"),
        				request.getParameter("name"),
        				request.getParameter("email"));
        		log.debug("user : {}", user);
        		DataBase.addUser(user);
        		response.sendRedirect("/index.html");
        	} else if ("/user/login".equals(path)) {
        		User user = DataBase.findUserById(request.getParameter("userId"));
        		if (user != null) {
        			if (user.login(request.getParameter("password"))) {
        				response.addHeader("Set-Cookie", "logined=true");
        				response.sendRedirect("/index.html");
        			} else {
        				response.sendRedirect("/user/login_failed.html");
        			}
        		} else {
        			response.sendRedirect("/user/login_failed.html");
        		}
        	} else if ("/user/list".equals(path)) {
        		if (!isLogin(request.getHeader("Cookie"))) {
        			response.sendRedirect("/user/login.html");
        			return;
        		}
        		
        		Collection<User> users = DataBase.findAll();
        		StringBuilder sb = new StringBuilder();
        		sb.append("<table border='1'>");
        		for (User user : users) {
        			sb.append("<tr>");
        			sb.append("<td>" + user.getUserId() + "</td>");
        			sb.append("<td>" + user.getName() + "</td>");
        			sb.append("<td>" + user.getEmail() + "</td>");
        			sb.append("</tr>");
        		}
        		response.forwardBody(sb.toString());
        	} else {
        		response.forward(path);
        	}
//    		} else if (path.endsWith(".css")) {
//        		DataOutputStream dos = new DataOutputStream(out);
//        		//            byte[] body = "Hello World".getBytes();
//        		byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());
//        		response200HeaderWithCss(dos, body.length);
//        		responseBody(dos, body);
//        	} else {
//	            DataOutputStream dos = new DataOutputStream(out);
//	//            byte[] body = "Hello World".getBytes();
//	            byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());
//	            response200Header(dos, body.length);
//	            responseBody(dos, body);
//        	}
        	
//        	if (url.startsWith("/user/create")) {
//        		String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
//        		log.debug("Rquest Body : {}", requestBody);
//        		Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
//        		User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
//        		log.debug("User : {}", user);
//        		
//        		DataBase.addUser(user);
//        		
//        		DataOutputStream dos = new DataOutputStream(out);
//        		response302Header(dos);
//        	} else if (url.equals("/user/login")) {
//        		String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
//        		log.debug("Rquest Body : {}", requestBody);
//        		Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
//        		log.debug("UserId : {}, password : {}", params.get("userId"), params.get("password"));
//        		
//        		User user = DataBase.findUserById(params.get("userId"));
//        		
//        		if (user == null) {
//        			log.debug("User Not Found!");
//        			DataOutputStream dos = new DataOutputStream(out);
//        			response302Header(dos);
//        		} else {
//        			if (user.getPassword().equals(params.get("password"))) {
//        				log.debug("login sucess!!");
//        				DataOutputStream dos = new DataOutputStream(out);
//        				response302HeaderWidthCookie(dos, "logined=true");
//        			} else {
//        				log.debug("longin fail!");
//        				DataOutputStream dos = new DataOutputStream(out);
//        				response302Header(dos);
//        			}
//        		}
//        	
//        	} else if (url.endsWith(".css")) {
//        		DataOutputStream dos = new DataOutputStream(out);
//        		//            byte[] body = "Hello World".getBytes();
//        		byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
//        		response200HeaderWithCss(dos, body.length);
//        		responseBody(dos, body);
//        	} else {
//	            DataOutputStream dos = new DataOutputStream(out);
//	//            byte[] body = "Hello World".getBytes();
//	            byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
//	            response200Header(dos, body.length);
//	            responseBody(dos, body);
//        	}
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private boolean isLogin(String cookieValue) {
		Map<String, String> cookies = HttpRequestUtils.parseCookies(cookieValue);
		String value = cookies.get("logined");
		if (value == null) {
			return false;
		}
		return Boolean.parseBoolean(value);
	}

	private String getDefualtPath(String path) {
		if (path.equals("/")) {
			return "/index.html";
		}
		return path;
	}

//	private void response302HeaderWidthCookie(DataOutputStream dos, String cookie) {
//    	try {
//    		dos.writeBytes("HTTP/1.1 302 Found \r\n");
//    		dos.writeBytes("Location: /index.html\r\n");
//    		dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
//    		dos.writeBytes("\r\n");
//    	} catch (IOException e) {
//    		log.error(e.getMessage());
//    	}
//    }
//
//    private void response302Header(DataOutputStream dos) {
//    	try {
//    		dos.writeBytes("HTTP/1.1 302 Found \r\n");
//    		dos.writeBytes("Location: /index.html\r\n");
//    		dos.writeBytes("\r\n");
//    	} catch (IOException e) {
//    		log.error(e.getMessage());
//    	}
//    }
//    
//    private void response200HeaderWithCss(DataOutputStream dos, int lengthOfBodyContent) {
//    	try {
//    		dos.writeBytes("HTTP/1.1 200 OK \r\n");
//    		dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
//    		dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
//    		dos.writeBytes("\r\n");
//    	} catch (IOException e) {
//    		log.error(e.getMessage());
//    	}
//    }
//
//    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
//        try {
//            dos.writeBytes("HTTP/1.1 200 OK \r\n");
//            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
//            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
//            dos.writeBytes("\r\n");
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }
//    }
//
//    private void responseBody(DataOutputStream dos, byte[] body) {
//        try {
//            dos.write(body, 0, body.length);
//            dos.flush();
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }
//    }
}
