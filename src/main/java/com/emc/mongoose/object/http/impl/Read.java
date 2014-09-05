package com.emc.mongoose.object.http.impl;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.WSLoadExecutor;
import com.emc.mongoose.object.http.api.WSRequestConfig;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.NoSuchElementException;
/**
 Created by kurila on 06.05.14.
 */
public class Read
extends WSLoadExecutor {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static boolean VERIFY_CONTENT;
	static {
		boolean flag = false;
		try {
			flag = RunTimeConfig.getBoolean("load.read.verify.content");
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, "Failed to get the value for property \"load.read.verify.checksum\"");
		}
		VERIFY_CONTENT = flag;
	}
	//
	public Read(
		final String[] addrs, final WSRequestConfig reqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(addrs, reqConf, maxCount, threadsPerNode, listFile);
		LOG.info(Markers.MSG, "Verify data checksum during read: {}", VERIFY_CONTENT);
	}
}
