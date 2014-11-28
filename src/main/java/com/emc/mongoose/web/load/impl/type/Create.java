package com.emc.mongoose.web.load.impl.type;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.persist.FileProducer;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.base.load.type.CreateLoadBase;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.data.impl.BasicWSObject;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.web.load.impl.WSLoadHelper;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 23.04.14.
 */
public class Create<T extends WSObject>
extends CreateLoadBase<T>
implements WSLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@SuppressWarnings("unchecked")
	public Create(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final WSRequestConfig<T> recConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minObjSize, final long maxObjSize, final float objSizeBias
	) throws IOException, CloneNotSupportedException {
		super(
			runTimeConfig, addrs, recConf, maxCount, threadsPerNode, listFile,
			minObjSize, maxObjSize, objSizeBias
		);
	}
	//
	@Override
	protected Producer<T> newDataItemProducer(
		final long minObjSize, final long maxObjSize, final float objSizeBias
	) {
		return new WSObjectProducer(minObjSize, maxObjSize, objSizeBias);
	}
	//
	protected class WSObjectProducer
	extends DataItemProducerBase {
		//
		protected WSObjectProducer(
			final long minObjSize, final long maxObjSize, final float objSizeBias
		) {
			super(minObjSize, maxObjSize, objSizeBias);
		}
		//
		@SuppressWarnings("unchecked")
		protected T produceSpecificDataItem(final long nextSize)
		throws IOException {
			return (T) new BasicWSObject(nextSize);
		}
	}
	//
	@Override
	protected final Closeable initClient(final String addrs[], final RequestConfig<T> reqConf) {
		return WSLoadHelper.initClient(
			addrs.length * threadsPerNode, runTimeConfig, (WSRequestConfig<T>) reqConf
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
				producer = (Producer<T>) new FileProducer<>(listFile, BasicWSObject.class);
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
