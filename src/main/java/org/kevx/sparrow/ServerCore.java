package org.kevx.sparrow;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author kevx
 * @since  2014/09
 */
public class ServerCore {
	
	private static final Properties serverConf = new Properties();
	private static final Properties handlerConf = new Properties();
	private static final Properties filterConf = new Properties();
	
	public static void main(String[] args) {
		readConf();
		Map<String, HttpHandler> handlers = initHandlers();
		List<Filter> filters = initFilters();
		
		InetSocketAddress addr = new InetSocketAddress(
			NumberUtils.toInt(serverConf.getProperty("SERVER_CORE_PORT"))
		);
		try {
			HttpServer s = HttpServer.create(addr, 0);
			Executor exe = new ForkJoinPool();//Executors.newFixedThreadPool(8);
			s.setExecutor(exe);
			for (Entry<String, HttpHandler> e : handlers.entrySet()) {
				HttpContext hc = s.createContext(e.getKey(), e.getValue());
				hc.getFilters().addAll(filters);
			}
			s.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void readConf() {
		try {
			Class<ServerCore> cls = ServerCore.class;
			serverConf.load(cls.getResourceAsStream("/conf.properties"));
			handlerConf.load(cls.getResourceAsStream("/handlers.properties"));
			filterConf.load(cls.getResourceAsStream("/filters.properties"));
			
			String port = serverConf.getProperty("SERVER_CORE_PORT");
			Validate.isTrue(StringUtils.isNumeric(port));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static Map<String, HttpHandler> initHandlers() {
		Map<String, HttpHandler> m = Maps.newHashMap();
		for (Entry<Object, Object> e : handlerConf.entrySet()) {
			String uri = e.getKey().toString();
			String cls = e.getValue().toString();
			try {
				Class<?> handlerCls = Class.forName(cls);
				HttpHandler h = (HttpHandler) handlerCls.newInstance();
				m.put(uri, h);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}
		}
		return m;
	}
	
	static List<Filter> initFilters() {
		List<Filter> m = Lists.newArrayList();
		for (Entry<Object, Object> e : filterConf.entrySet()) {
			String cls = e.getValue().toString();
			try {
				Class<?> handlerCls = Class.forName(cls);
				Filter h = (Filter) handlerCls.newInstance();
				m.add(h);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}
		}
		return m;
	}
}
