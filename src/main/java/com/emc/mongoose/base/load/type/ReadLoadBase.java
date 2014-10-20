package com.emc.mongoose.base.load.type;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutorBase;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 20.10.14.
 */
public abstract class ReadLoadBase<T extends DataItem>
extends LoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final boolean verifyContentFlag;
	//
	protected ReadLoadBase(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final RequestConfig<T> reqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) {
		super(runTimeConfig, addrs, reqConf, maxCount, threadsPerNode, listFile);
		verifyContentFlag = runTimeConfig.getReadVerifyContent();
		LOG.info(Markers.MSG, "Verify data integrity during read: {}", verifyContentFlag);
	}
}
