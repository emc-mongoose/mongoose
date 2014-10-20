package com.emc.mongoose.web.load.type;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.persist.FileProducer;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.base.load.type.AppendLoadBase;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObjectImpl;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.web.load.WSLoadHelper;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
/**
 Created by kurila on 23.09.14.
 */
public class AppendImpl<T extends WSObjectImpl>
extends AppendLoadBase<T>
implements WSLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public AppendImpl(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final WSRequestConfig<T> sharedReqConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minAppendSize, final long maxAppendSize
	) {
		super(
			runTimeConfig, addrs, sharedReqConf, maxCount, threadsPerNode, listFile,
			minAppendSize, maxAppendSize
		);
	}
	//
	@Override
	protected final void initClient(final String addrs[], final RequestConfig<T> reqConf) {
		final WSRequestConfig<T> wsReqConf = (WSRequestConfig<T>) reqConf;
		wsReqConf.setClient(
			WSLoadHelper.initClient(
				addrs.length * threadsPerNode, // total thread/connections count per load
				(int) runTimeConfig.getDataPageSize(),
				wsReqConf
			)
		);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final void initNodeExecutors(final String addrs[], final RequestConfig<T> reqConf) {
		WSLoadHelper.initNodeExecutors(
			addrs, runTimeConfig, WSRequestConfig.class.cast(reqConf),
			threadsPerNode, metrics, getName(), nodes
		);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final void setFileBasedProducer(final String listFile) {
		// if path specified use the file as producer
		if(listFile != null && listFile.length() > 0) {
			try {
				producer = (Producer<T>) new FileProducer<>(listFile, WSObjectImpl.class);
				producer.setConsumer(this);
			} catch(final NoSuchMethodException e) {
				LOG.fatal(Markers.ERR, "Failed to get the constructor", e);
			} catch(final IOException e) {
				LOG.warn(Markers.ERR, "Failed to use object list file \"{}\"for reading", listFile);
				LOG.debug(Markers.ERR, e.toString(), e.getCause());
			}
		}
	}
}
