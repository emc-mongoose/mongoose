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
import com.emc.mongoose.core.api.load.model.Consumer;
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
	protected long executeLoad(
		final Producer<T> producer, final LoadExecutor<T> loadExecutor, final Consumer<T> consumer
	) throws IOException {
		//
		final long countSucc;
		//
		if(consumer != null) {
			loadExecutor.setConsumer(consumer);
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
		return write(itemsInput, itemsOutput, size, size, 0);
	}
	//
	@Override
	public long write(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long minSize, final long maxSize, final float sizeBias
	) throws IllegalArgumentException, IOException {
		//
		final long countSucc;
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
			null : new DataItemOutputConsumer<>(itemsOutput);
		//
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.CREATE)
				.setMinObjSize(minSize)
				.setMaxObjSize(maxSize)
				.setObjSizeBias(sizeBias)
				.build()
		) {
			countSucc = executeLoad(producer, loadJobExecutor, consumer);
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException, IOException {
		return read(itemsInput, itemsOutput, rtConfig.getReadVerifyContent());
	}
	//
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final boolean verifyContentFlag
	) throws IllegalStateException, IOException {
		//
		final long countSucc;
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
			null : new DataItemOutputConsumer<>(itemsOutput);
		//
		loadBuilder.getRequestConfig().setVerifyContentFlag(verifyContentFlag);
		//
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.READ)
				.build()
		) {
			countSucc = executeLoad(producer, loadJobExecutor, consumer);
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long delete(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException, IOException {
		//
		final long countSucc;
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
			null : new DataItemOutputConsumer<>(itemsOutput);
		//
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.DELETE)
				.build()
		) {
			countSucc = executeLoad(producer, loadJobExecutor, consumer);
		}
		//
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException, IOException {
		return update(itemsInput, itemsOutput, rtConfig.getUpdateCountPerTime());
	}
	//
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final int countPerTime
	) throws IllegalArgumentException, IllegalStateException, IOException {
		//
		final long countSucc;
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
			null : new DataItemOutputConsumer<>(itemsOutput);
		//
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.UPDATE)
				.setUpdatesPerItem(countPerTime)
				.build()
		) {
			countSucc = executeLoad(producer, loadJobExecutor, consumer);
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
			itemsInput, itemsOutput,
			rtConfig.getDataSizeMin(), rtConfig.getDataSizeMax(), rtConfig.getDataSizeBias()
		);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long augmentSize
	) throws IllegalArgumentException, IllegalStateException, IOException {
		return append(itemsInput, itemsOutput, augmentSize, augmentSize, 0);
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long augmentSizeMin, final long augmentSizeMax, final float augmentSizeBias
	) throws IllegalArgumentException, IllegalStateException, IOException {
		//
		final long countSucc;
		final DataItemInputProducer<T> producer = itemsInput == null ?
			null : new DataItemInputProducer<>(itemsInput);
		final DataItemOutputConsumer<T> consumer = itemsOutput == null ?
			null : new DataItemOutputConsumer<>(itemsOutput);
		//
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.APPEND)
				.setMinObjSize(augmentSizeMin)
				.setMaxObjSize(augmentSizeMax)
				.setObjSizeBias(augmentSizeBias)
				.build()
		) {
			countSucc = executeLoad(producer, loadJobExecutor, consumer);
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
