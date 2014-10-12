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
/**
 Created by kurila on 06.05.14.
 */
public class Read<T extends WSObjectImpl>
extends WSLoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final boolean verifyContentFlag;
	//
	public Read(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final WSRequestConfig<T> reqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(runTimeConfig, addrs, reqConf, maxCount, threadsPerNode, listFile);
		verifyContentFlag = runTimeConfig.getReadVerifyContent();
		LOG.info(Markers.MSG, "Verify data integrity during read: {}", verifyContentFlag);
	}
}
