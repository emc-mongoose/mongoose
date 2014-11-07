package com.emc.mongoose.web.load.impl.type;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.persist.FileProducer;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.base.load.type.CreateLoadBase;
import com.emc.mongoose.util.logging.MessageFactoryImpl;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.data.impl.BasicWSObject;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.web.load.impl.WSLoadHelper;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
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
	private final Logger log;
	//
	@SuppressWarnings("unchecked")
	public Create(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final WSRequestConfig<T> recConf, final long maxCount,
		final int threadsPerNode, final String listFile,
		final long minObjSize, final long maxObjSize
	) throws IOException, CloneNotSupportedException {
		super(
			runTimeConfig, addrs, recConf, maxCount, threadsPerNode, listFile,
			minObjSize, maxObjSize
		);
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
	}
	//
	@Override
	protected Producer<T> newDataItemProducer(final long minObjSize, final long maxObjSize) {
		return new WSObjectProducer(minObjSize, maxObjSize);
	}
	//
	protected class WSObjectProducer
	extends DataItemProducerBase {
		//
		protected WSObjectProducer(final long minObjSize, final long maxObjSize) {
			super(minObjSize, maxObjSize);
		}
		//
		@SuppressWarnings("unchecked")
		protected void produceNextAndFeed()
		throws IOException, InterruptedException, RejectedExecutionException {
			newDataConsumer.submit(
				(T) new BasicWSObject(
					ThreadLocalRandom.current().nextLong(minObjSize, maxObjSize + 1)
				)
			);
		}
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
				producer = (Producer<T>) new FileProducer<>(listFile, BasicWSObject.class);
				producer.setConsumer(this);
			} catch(final NoSuchMethodException e) {
				log.fatal(Markers.ERR, "Failed to get the constructor", e);
			} catch(final IOException e) {
				log.warn(Markers.ERR, "Failed to use object list file \"{}\"for reading", listFile);
				log.debug(Markers.ERR, e.toString(), e.getCause());
			}
		}
	}
}
