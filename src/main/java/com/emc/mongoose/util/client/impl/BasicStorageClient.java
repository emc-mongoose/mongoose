package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.data.model.DataItemOutput;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
import com.emc.mongoose.core.api.load.model.Producer;
//
import com.emc.mongoose.core.impl.load.model.DataItemInputProducer;
import com.emc.mongoose.core.impl.load.model.DataItemOutputConsumer;
//
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 19.06.15.
 */
public class BasicStorageClient<T extends DataItem>
implements StorageClient<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	protected final static int DEFAULT_CONN_PER_NODE_COUNT = 1;
	//
	protected RunTimeConfig rtConfig;
	protected LoadBuilder<T, LoadExecutor<T>> loadBuilder;
	//
	public BasicStorageClient(
		final RunTimeConfig rtConfig, final LoadBuilder<T, LoadExecutor<T>> loadBuilder
	) {
		this.rtConfig = rtConfig;
		this.loadBuilder = loadBuilder;
	}
	//
	protected long executeLoadJob(
		final Producer<T> producer, final LoadExecutor<T> loadExecutor,
		final AsyncConsumer<T> consumer
	) throws RemoteException {
		if(consumer != null) {
			loadExecutor.setConsumer(consumer);
			consumer.start();
		}
		if(producer != null) {
			producer.setConsumer(loadExecutor);
		}
		loadExecutor.start();
		if(producer != null) {
			producer.start();
		}
		final long timeOut = rtConfig.getLoadLimitTimeValue();
		final TimeUnit timeUnit = rtConfig.getLoadLimitTimeUnit();
		try {
			loadExecutor.await(
				timeOut == 0 ? Long.MAX_VALUE : timeOut, timeUnit == null ? TimeUnit.DAYS : timeUnit
			);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted the load job \"{}\"", loadExecutor);
		} finally {
			loadExecutor.interrupt();
		}
		return loadExecutor.getLoadState().getCountSucc();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long write(final long size)
	throws IllegalArgumentException, RemoteException, IOException {
		return write(null, null, 0, DEFAULT_CONN_PER_NODE_COUNT, size, size, 0);
	}
	//
	@Override
	public long write(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long maxCount, final int connPerNodeCount, final long size
	) throws IllegalArgumentException, RemoteException, IOException {
		return write(itemsInput, itemsOutput, maxCount, connPerNodeCount, size, size, 0);
	}
	//
	@Override
	public long write(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long maxCount, final int connPerNodeCount,
		final long minSize, final long maxSize, final float sizeBias
	) throws IllegalArgumentException, RemoteException, IOException {
		//
		loadBuilder.getRequestConfig().setContainerInputEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput, maxCount)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.CREATE)
					.setMaxCount(maxCount)
					.setThreadCountFor(connPerNodeCount, IOTask.Type.CREATE)
					.setMinObjSize(minSize)
					.setMaxObjSize(maxSize)
					.setObjSizeBias(sizeBias)
					.build()
			) {
				return executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long read(final DataItemInput<T> itemsInput)
	throws IllegalStateException, RemoteException, IOException {
		return read(itemsInput, null, 0, DEFAULT_CONN_PER_NODE_COUNT, rtConfig.getReadVerifyContent());
	}
	//
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long maxCount, final int connPerNodeCount, final boolean verifyContentFlag
	) throws IllegalStateException, RemoteException, IOException {
		loadBuilder.getRequestConfig().setVerifyContentFlag(verifyContentFlag);
		loadBuilder.getRequestConfig().setContainerInputEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput, maxCount)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.READ)
					.setMaxCount(maxCount)
					.setConnPerNodeFor(connPerNodeCount, IOTask.Type.READ)
					.build()
			) {
				return executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long delete(final DataItemInput<T> itemsInput)
	throws IllegalStateException, RemoteException, IOException {
		return delete(itemsInput, null, 0, DEFAULT_CONN_PER_NODE_COUNT);
	}
	//
	@Override
	public long delete(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long maxCount, final int connPerNodeCount
	) throws IllegalStateException, RemoteException, IOException {
		loadBuilder.getRequestConfig().setContainerInputEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput, maxCount)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.DELETE)
					.setMaxCount(maxCount)
					.setConnPerNodeFor(connPerNodeCount, IOTask.Type.DELETE)
					.build()
			) {
				return executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long update(final DataItemInput<T> itemsInput)
	throws IllegalStateException, RemoteException, IOException {
		return update(itemsInput, null, 0, DEFAULT_CONN_PER_NODE_COUNT, rtConfig.getUpdateCountPerTime());
	}
	//
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long maxCount, final int connPerNodeCount, final int countPerTime
	) throws IllegalArgumentException, IllegalStateException, RemoteException, IOException {
		loadBuilder.getRequestConfig().setContainerInputEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput, maxCount)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.UPDATE)
					.setMaxCount(maxCount)
					.setConnPerNodeFor(connPerNodeCount, IOTask.Type.UPDATE)
					.setUpdatesPerItem(countPerTime)
					.build()
			) {
				return executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long append(final DataItemInput<T> itemsInput, final long size)
	throws IllegalStateException, RemoteException, IOException {
		return append(itemsInput, null, 0, DEFAULT_CONN_PER_NODE_COUNT, size, size, 0);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long maxCount, final int connPerNodeCount, final long size
	) throws IllegalArgumentException, IllegalStateException, RemoteException, IOException {
		return append(itemsInput, itemsOutput, maxCount, connPerNodeCount, size, size, 0);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long maxCount, final int connPerNodeCount,
		final long sizeMin, final long sizeMax, final float sizeBias
	) throws IllegalArgumentException, IllegalStateException, RemoteException, IOException {
		loadBuilder.getRequestConfig().setContainerInputEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput, maxCount)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.APPEND)
					.setConnPerNodeFor(connPerNodeCount, IOTask.Type.APPEND)
					.setMinObjSize(sizeMin)
					.setMaxObjSize(sizeMax)
					.setObjSizeBias(sizeBias)
					.build()
			) {
				return executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void close()
	throws IOException {
		loadBuilder.close();
	}
}
