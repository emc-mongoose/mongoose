package com.emc.mongoose.common.net;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 Created by kurila on 11.07.16.
 */
public interface NetUtil {

	/**
	 Tries to resolve 1st enabled external network interface IP address.
	 Tries to fall back to loopback interface if no valid external interface found.
	 @return IP address
	 @throws OmgLookAtMyConsoleException if failed to resolve an interface address
	 @throws OmgDoesNotPerformException
	 */
	static InetAddress getHostAddr()
	throws OmgLookAtMyConsoleException, OmgDoesNotPerformException {
		InetAddress addr = null;
		final Enumeration<NetworkInterface> netIfaces;
		try {
			netIfaces = NetworkInterface.getNetworkInterfaces();
		} catch(final SocketException e) {
			throw new OmgLookAtMyConsoleException(e);
		}
		NetworkInterface nextNetIface;
		while(netIfaces.hasMoreElements()) {
			nextNetIface = netIfaces.nextElement();
			try {
				if(!nextNetIface.isLoopback() && nextNetIface.isUp()) {
					final Enumeration<InetAddress> addrs = nextNetIface.getInetAddresses();
					while(addrs.hasMoreElements()) {
						addr = addrs.nextElement();
						if(Inet4Address.class.isInstance(addr)) {
							// resolved the external interface address
							break;
						}
					}
				}
			} catch(final SocketException e) {
				throw new OmgLookAtMyConsoleException(e);
			}
		}

		if(addr == null) {
			addr = InetAddress.getLoopbackAddress();
		}

		if(addr == null) {
			throw new OmgDoesNotPerformException("");
		}
		return addr;
	}


	static String getHostAddrString()
	throws DanShootHisFootException {
		return getHostAddr().getHostAddress();
	}

	static long getHostAddrCode()
	throws DanShootHisFootException {
		return getHostAddrString().hashCode();
	}
}
