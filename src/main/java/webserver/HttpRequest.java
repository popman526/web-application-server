package webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.HttpRequestUtils;
import util.IOUtils;

public class HttpRequest {

	private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);
	
//	private String method;
//	private String path;
	private RequestLine requestLine;
	private Map<String, String> headers = new HashMap<>();
	private Map<String, String> params = new HashMap<>();

	public HttpRequest(InputStream in) {
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String line = br.readLine();
			if (line == null) {
				return;
			}
			
			requestLine = new RequestLine(line);
			//processRequestLine(line);
			
			line = br.readLine();
			while (line != null && !("").equals(line)) {
				log.debug("header : {}", line);
				String[] tokens = line.split(":");
				headers.put(tokens[0].trim(), tokens[1].trim());
				line = br.readLine();
			}
			
			if (HttpMethod.POST.equals(getMethod())) {
				String body = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
				params = HttpRequestUtils.parseQueryString(body);
			} else {
				params = requestLine.getParams();
			}
			// TODO Auto-generated catch block
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

//	private void processRequestLine(String requestLine) {
//
//		log.debug("request line : {}", requestLine);
//		String[] tokens = requestLine.split(" ");
//		method = tokens[0];
//		
//		int index = tokens[1].indexOf("?");
//		if (index == -1) {
//			path = tokens[1];
//		} else {
//			path = tokens[1].substring(0, index);
//			params = HttpRequestUtils.parseQueryString(tokens[1].substring(index + 1));
//		}
//	}

	public HttpMethod getMethod() {
		// TODO Auto-generated method stub
		return requestLine.getMethod();
	}

	public String getPath() {
		// TODO Auto-generated method stub
		return requestLine.getPath();
	}

	public String getHeader(String name) {
		// TODO Auto-generated method stub
		return headers.get(name);
	}

	public String getParameter(String name) {
		return params.get(name);
	}

}
