package org.kevx.sparrow.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PerfTestHandler implements HttpHandler {

	private static final SecureRandom sr = new SecureRandom();
	
	private final AtomicLong nr = new AtomicLong();
	
	@Override
	public void handle(HttpExchange ex) throws IOException {
		if (nr.incrementAndGet() > 2000) {
			nr.decrementAndGet();
			return;
		}
		String resp = mockResp();
		byte[] b = resp.getBytes();
		ex.sendResponseHeaders(HttpURLConnection.HTTP_OK, b.length);  
		final OutputStream os = ex.getResponseBody();  
		os.write(b);  
		os.close();
		nr.decrementAndGet();
	}
	
	public static String mockResp() {
		int n = Math.abs(sr.nextInt()) % 8192 + 1;
		StringBuilder sb = new StringBuilder();
		while (sb.length() < n) {
			sb.append(Math.abs(sr.nextInt()));
		}

		return sb.toString();
	}
}
