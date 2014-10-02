package com.emc.mongoose.object.load.type.ws;
//
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.object.load.WSLoadExecutorBase;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.NoSuchElementException;
/**
 Created by kurila on 06.05.14.
 */
public class Read<T extends WSObjectImpl>
extends WSLoadExecutorBase<T> {
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
		final String[] addrs, final WSRequestConfig<T> reqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(addrs, reqConf, maxCount, threadsPerNode, listFile);
		LOG.info(Markers.MSG, "Verify data integrity during read: {}", VERIFY_CONTENT);
	}
}
