package com.emc.mongoose.common.net;

import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 Created by kurila on 11.07.16.
 */
public abstract class NetUtil {

	private final static Logger LOG = LogManager.getLogger();

	public static String getHostAddr() {
		InetAddress addr = null;
		//
		try {
			final Enumeration<NetworkInterface> netIfaces = NetworkInterface.getNetworkInterfaces();
			NetworkInterface nextNetIface;
			while(netIfaces.hasMoreElements()) {
				nextNetIface = netIfaces.nextElement();
				if(!nextNetIface.isLoopback() && nextNetIface.isUp()) {
					final Enumeration<InetAddress> addrs = nextNetIface.getInetAddresses();
					while(addrs.hasMoreElements()) {
						addr = addrs.nextElement();
						if(Inet4Address.class.isInstance(addr)) {
							LOG.debug(
								Markers.MSG, "Resolved external interface \"{}\" address: {}",
								nextNetIface.getDisplayName(), addr.getHostAddress()
							);
							break;
						}
					}
				} else {
					LOG.debug(
						Markers.MSG, "Interface \"{}\" is loopback or is not up, skipping",
						nextNetIface.getDisplayName()
					);
				}
			}
		} catch(final SocketException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to get an external interface address");
		}
		//
		if(addr == null) {
			LOG.warn(
				Markers.ERR, "No valid external interface have been found, falling back to loopback"
			);
			addr = InetAddress.getLoopbackAddress();
		}
		//
		return addr.getHostAddress();
	}
	//
	public static long getHostAddrCode() {
		return getHostAddr().hashCode();
	}
}
