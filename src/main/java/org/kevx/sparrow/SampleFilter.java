package org.kevx.sparrow;

import java.io.IOException;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class SampleFilter extends Filter {

	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		chain.doFilter(exchange);
	}

	@Override
	public String description() {
		return "sample";
	}

}
