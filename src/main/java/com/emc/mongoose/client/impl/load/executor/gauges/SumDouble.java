package com.emc.mongoose.client.impl.load.executor.gauges;
//
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import com.codahale.metrics.Gauge;
//
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.Map;
/**
 Created by kurila on 19.12.14.
 */
public final class SumDouble
implements Gauge<Double> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final String domain, attrName, fqMBeanName;
	private final Map<String, MBeanServerConnection> mBeanSrvConnMap;
	//
	public SumDouble(
		final String loadName, final String domain, final String name, final String attrName,
		final Map<String, MBeanServerConnection> mBeanSrvConnMap
	) {
		this.domain = domain;
		this.attrName = attrName;
		fqMBeanName = loadName.substring(0, loadName.lastIndexOf('x')) + '.' + name;
		this.mBeanSrvConnMap = mBeanSrvConnMap;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final Double getValue() {
		//
		double value = 0;
		MBeanServerConnection nextMBeanConn;
		ObjectName objectName;
		//
		for(final String addr: mBeanSrvConnMap.keySet()) {
			nextMBeanConn = mBeanSrvConnMap.get(addr);
			objectName = null;
			try {
				objectName = new ObjectName(domain, LoadClient.KEY_NAME, fqMBeanName);
			} catch(final MalformedObjectNameException e) {
				LogUtil.exception(LOG, Level.WARN, e, "No such remote object");
			}
			//
			if(objectName != null) {
				try {
					value += (double) nextMBeanConn.getAttribute(objectName, attrName);
				} catch(final AttributeNotFoundException e) {
					LOG.warn(
						LogUtil.ERR, "Attribute \"{}\" not found for MBean \"{}\" @ {}",
						attrName, objectName.getCanonicalName(), addr
					);
				} catch(final IOException|MBeanException|InstanceNotFoundException|ReflectionException e) {
					LogUtil.exception(
						LOG, Level.DEBUG, e, LoadClient.FMT_MSG_FAIL_FETCH_VALUE,
						objectName.getCanonicalName() + "." + attrName, addr
					);
				}
			}
		}
		//
		return value;
	}
}
