package com.emc.mongoose.storage.driver.preempt.mock;

import com.emc.mongoose.base.config.IllegalConfigurationException;
import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.storage.Credential;
import com.emc.mongoose.storage.driver.preempt.PreemptStorageDriverBase;

import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.math.Random;

import com.github.akurilov.confuse.Config;

import java.io.IOException;
import java.util.List;

public class PreemptStorageDriverMock<I extends Item, O extends Operation<I>>
				extends PreemptStorageDriverBase<I, O> {

	private final Random rnd = new Random();

	public PreemptStorageDriverMock(
					final String stepId, final DataInput itemDataInput, final Config storageConfig, final boolean verifyFlag) throws
		IllegalConfigurationException {
		super(stepId, itemDataInput, storageConfig, verifyFlag);
	}

	@Override
	protected void execute(final O op) {
		op.startRequest();
		op.finishRequest();
		op.startResponse();
		if (op instanceof DataOperation) {
			final DataOperation dataOp = (DataOperation) op;
			final DataItem dataItem = dataOp.item();
			switch (dataOp.type()) {
			case CREATE:
				try {
					dataOp.countBytesDone(dataItem.size());
				} catch (final IOException ignored) {}
				break;
			case READ:
				dataOp.startDataResponse();
				break;
			case UPDATE:
				final List<Range> fixedRanges = dataOp.fixedRanges();
				if (fixedRanges == null || fixedRanges.isEmpty()) {
					if (dataOp.hasMarkedRanges()) {
						dataOp.countBytesDone(dataOp.markedRangesSize());
					} else {
						try {
							dataOp.countBytesDone(dataItem.size());
						} catch (final IOException ignored) {}
					}
				} else {
					dataOp.countBytesDone(dataOp.markedRangesSize());
				}
				break;
			default:
				break;
			}
		}
		op.finishResponse();
		op.status(Operation.Status.SUCC);
	}

	@Override
	protected String requestNewPath(final String path) {
		return path;
	}

	@Override
	protected String requestNewAuthToken(final Credential credential) {
		return Long.toHexString(rnd.nextLong());
	}

	@Override
	public List<I> list(
					final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
					final I lastPrevItem, final int count) throws IOException {
		return null;
	}

	@Override
	public void adjustIoBuffers(final long avgTransferSize, final OpType opType) {

	}
}
