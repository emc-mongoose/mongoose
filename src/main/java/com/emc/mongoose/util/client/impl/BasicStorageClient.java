package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.util.DataItemInput;
import com.emc.mongoose.core.api.data.util.DataItemOutput;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
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
		long countSucc = 0;
		final DataItemInputProducer<T> producer = new DataItemInputProducer<>(itemsInput);
		final DataItemOutputConsumer<T> consumer = new DataItemOutputConsumer<>(itemsOutput);
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setMinObjSize(minSize)
				.setMaxObjSize(maxSize)
				.setObjSizeBias(sizeBias)
				.build()
		) {
			loadJobExecutor.setConsumer(consumer);
			producer.setConsumer(loadJobExecutor);
			loadJobExecutor.start();
			producer.start();
			try {
				loadJobExecutor.await(
					rtConfig.getLoadLimitTimeValue(), rtConfig.getLoadLimitTimeUnit()
				);
			} catch(final InterruptedException | RemoteException ignored) {
			} finally {
				countSucc = loadJobExecutor.getLoadState().getCountSucc();
			}
		}
		return countSucc;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException {
		return 0;
	}
	//
	@Override
	public long read(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final boolean verifyContentFlag
	) throws IllegalStateException {
		return 0;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long delete(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException {
		return 0;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException {
		return 0;
	}
	//
	@Override
	public long update(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final int countPerTime
	) throws IllegalArgumentException, IllegalStateException {
		return 0;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput
	) throws IllegalStateException {
		return 0;
	}
	//
	@Override
	public long append(
		final DataItemInput<T> itemsInput, final DataItemOutput<T> itemsOutput,
		final long augmentSize
	) throws IllegalArgumentException, IllegalStateException {
		return 0;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void close()
	throws IOException {
		loadBuilder.close();
	}
}
