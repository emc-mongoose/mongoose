package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.util.DataItemInput;
import com.emc.mongoose.core.api.data.util.DataItemOutput;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.util.client.api.StorageClient;
//
import java.io.IOException;
import java.rmi.RemoteException;
/**
 Created by kurila on 19.06.15.
 */
public class BasicStorageClient<T extends DataItem>
implements StorageClient<T> {
	//
	protected final static short THREAD_COUNT_DEFAULT = 1;
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
	) throws IOException {
		//
		final long countSucc;
		//
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
		//
		try {
			loadExecutor.await(
				rtConfig.getLoadLimitTimeValue(), rtConfig.getLoadLimitTimeUnit()
			);
		} catch(final InterruptedException | RemoteException ignored) {
		} finally {
			countSucc = loadExecutor.getLoadState().getCountSucc();
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long write(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long size
	) throws IllegalArgumentException, IOException {
		return write(itemsInput, itemsOutput, THREAD_COUNT_DEFAULT, size, size, 0);
	}
	//
	@Override
	public long write(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount, final long size
	) throws IllegalArgumentException, IOException {
		return write(itemsInput, itemsOutput, threadCount, size, size, 0);
	}
	//
	@Override
	public long write(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long minSize, final long maxSize, final float sizeBias
	) throws IllegalArgumentException, IOException {
		return write(itemsInput, itemsOutput, THREAD_COUNT_DEFAULT, minSize, maxSize, sizeBias);
	}
	//
	@Override
	public long write(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount, final long minSize, final long maxSize, final float sizeBias
	) throws IllegalArgumentException, IOException {
		//
		final long countSucc;
		loadBuilder.getRequestConfig().setAnyDataProducerEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.CREATE)
					.setThreadsPerNodeFor(threadCount, IOTask.Type.CREATE)
					.setMinObjSize(minSize)
					.setMaxObjSize(maxSize)
					.setObjSizeBias(sizeBias)
					.build()
			) {
				countSucc = executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException, IOException {
		return read(itemsInput, itemsOutput, THREAD_COUNT_DEFAULT, rtConfig.getReadVerifyContent());
	}
	//
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount
	) throws IllegalStateException, IOException {
		return read(itemsInput, itemsOutput, threadCount, rtConfig.getReadVerifyContent());
	}
	//
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final boolean verifyContentFlag
	) throws IllegalStateException, IOException {
		return read(itemsInput, itemsOutput, THREAD_COUNT_DEFAULT, verifyContentFlag);
	}
	//
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount, final boolean verifyContentFlag
	) throws IllegalStateException, IOException {
		//
		final long countSucc;
		loadBuilder.getRequestConfig().setVerifyContentFlag(verifyContentFlag);
		loadBuilder.getRequestConfig().setAnyDataProducerEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.READ)
					.setThreadsPerNodeFor(threadCount, IOTask.Type.READ)
					.build()
			) {
				countSucc = executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long delete(final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput)
	throws IllegalStateException, IOException {
		return delete(itemsInput, itemsOutput, THREAD_COUNT_DEFAULT);
	}
	//
	@Override
	public long delete(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount
	) throws IllegalStateException, IOException {
		//
		final long countSucc;
		loadBuilder.getRequestConfig().setAnyDataProducerEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.DELETE)
					.setThreadsPerNodeFor(threadCount, IOTask.Type.DELETE)
					.build()
			) {
				countSucc = executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException, IOException {
		return update(
			itemsInput, itemsOutput, THREAD_COUNT_DEFAULT, rtConfig.getUpdateCountPerTime()
		);
	}
	//
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount
	) throws IllegalStateException, IOException {
		return update(
			itemsInput, itemsOutput, threadCount, rtConfig.getUpdateCountPerTime()
		);
	}
	//
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final int countPerTime
	) throws IllegalStateException, IOException {
		return update(
			itemsInput, itemsOutput, THREAD_COUNT_DEFAULT, countPerTime
		);
	}
	//
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount, final int countPerTime
	) throws IllegalArgumentException, IllegalStateException, IOException {
		//
		final long countSucc;
		loadBuilder.getRequestConfig().setAnyDataProducerEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.UPDATE)
					.setThreadsPerNodeFor(threadCount, IOTask.Type.UPDATE)
					.setUpdatesPerItem(countPerTime)
					.build()
			) {
				countSucc = executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException, IOException {
		return append(
			itemsInput, itemsOutput, THREAD_COUNT_DEFAULT,
			rtConfig.getDataSizeMin(), rtConfig.getDataSizeMax(), rtConfig.getDataSizeBias()
		);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount
	) throws IllegalStateException, IOException {
		return append(
			itemsInput, itemsOutput, threadCount,
			rtConfig.getDataSizeMin(), rtConfig.getDataSizeMax(), rtConfig.getDataSizeBias()
		);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long size
	) throws IllegalArgumentException, IllegalStateException, IOException {
		return append(itemsInput, itemsOutput, THREAD_COUNT_DEFAULT, size, size, 0);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount, final long size
	) throws IllegalArgumentException, IllegalStateException, IOException {
		return append(itemsInput, itemsOutput, threadCount, size, size, 0);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long sizeMin, final long sizeMax, final float sizeBias
	) throws IllegalArgumentException, IllegalStateException, IOException {
		return append(itemsInput, itemsOutput, THREAD_COUNT_DEFAULT, sizeMin, sizeMax, sizeBias);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final short threadCount, final long sizeMin, final long sizeMax, final float sizeBias
	) throws IllegalArgumentException, IllegalStateException, IOException {
		//
		final long countSucc;
		loadBuilder.getRequestConfig().setAnyDataProducerEnabled(itemsInput == null);
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		try(
			final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
				null : new DataItemOutputConsumer<>(itemsOutput)
		) {
			try(
				final LoadExecutor<T> loadJobExecutor = loadBuilder
					.setLoadType(IOTask.Type.APPEND)
					.setThreadsPerNodeFor(threadCount, IOTask.Type.APPEND)
					.setMinObjSize(sizeMin)
					.setMaxObjSize(sizeMax)
					.setObjSizeBias(sizeBias)
					.build()
			) {
				countSucc = executeLoadJob(producer, loadJobExecutor, consumer);
			}
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void close()
	throws IOException {
		loadBuilder.close();
	}
}
