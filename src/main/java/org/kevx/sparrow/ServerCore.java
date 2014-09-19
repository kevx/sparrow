package org.kevx.sparrow;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

/**
 * @author kevx
 * @since  2014/09
 */
public class ServerCore {
	
	private static final Properties serverConf = new Properties();
	private static final Properties handlerConf = new Properties();
	private static final Properties filterConf = new Properties();
	
	private static Map<String, HttpHandler> handlers;
	private static List<Filter> filters;
	
	public static void main(String[] args) {
		readConf();
		handlers = initHandlers();
		filters = initFilters();
		
		String[] serverModes = StringUtils.split(serverConf.getProperty("SERVER_MODE"), ',');
		if (serverModes.length < 1 || serverModes.length > 2) {
			System.out.println("invalid_server_mode");
			return;
		}
		
		for (String sm : serverModes) {
			if ("http".equalsIgnoreCase(sm)) initHttpServer();
			if ("https".equalsIgnoreCase(sm)) initHttpsServer();
		}
		System.out.println("sparrow_started!");
	}

	static void initHttpServer() {
		InetSocketAddress addr = new InetSocketAddress(
			NumberUtils.toInt(serverConf.getProperty("SERVER_CORE_PORT"))
		);
		try {
			HttpServer s = HttpServer.create(addr, 0);
			Executor exe = new ForkJoinPool();
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
	
	static void initHttpsServer() {
		try {
			Class<ServerCore> cls = ServerCore.class;
			char[] passwd = serverConf.getProperty("KEYSTORE_PASSWORD").toCharArray();
			String certfile = serverConf.getProperty("CERT_FILE");
			SSLContext sslContext = SSLContext.getInstance("SSLv3");
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			KeyStore ks = KeyStore.getInstance("JKS");  
			ks.load(cls.getResourceAsStream(certfile), passwd);
			kmf.init(ks, passwd);
			sslContext.init(kmf.getKeyManagers(), null, null);
			
			HttpsConfigurator conf = new HttpsConfigurator(sslContext);
			InetSocketAddress addr = new InetSocketAddress(
				NumberUtils.toInt(serverConf.getProperty("SERVER_HTTPS_PORT"))
			);
			
			HttpsServer s = HttpsServer.create(addr, 0);
			int poolsize = NumberUtils.toInt(
				serverConf.getProperty("POOL_THREADS_COUNT"), 
				Runtime.getRuntime().availableProcessors() * 4
			);
			Executor exe = Executors.newFixedThreadPool(poolsize);
			s.setExecutor(exe);
			s.setHttpsConfigurator(conf);
			for (Entry<String, HttpHandler> e : handlers.entrySet()) {
				HttpContext hc = s.createContext(e.getKey(), e.getValue());
				hc.getFilters().addAll(filters);
			}
			s.start();
		} catch (Exception e) {
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
