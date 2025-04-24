package com.sho.ss.asuna.engine.core.utils;

import com.sho.ss.asuna.engine.core.proxy.Proxy;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Pooled Proxy Object
 * 
 * @author yxssfxwzy@sina.com <br>
 * @since 0.5.1
 */

public class ProxyUtils {

//	private static final Logger logger = LoggerFactory.getLogger(ProxyUtils.class);

	public static boolean validateProxy(Proxy p) {
		Socket socket = null;
		try {
			socket = new Socket();
			InetSocketAddress endpointSocketAddr = new InetSocketAddress(p.getHost(), p.getPort());
			socket.connect(endpointSocketAddr, 3000);
			return true;
		} catch (IOException e) {
//			logger.warn("FAILURE - CAN not connect!  remote: " + p);
			System.err.println("FAILURE - CAN not connect!  remote: " + p);
			return false;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
//					logger.warn("Error occurred while closing socket of validating proxy", e);
					System.err.println("Error occurred while closing socket of validating proxy: " + e.getMessage());
				}
			}
		}

	}

}
