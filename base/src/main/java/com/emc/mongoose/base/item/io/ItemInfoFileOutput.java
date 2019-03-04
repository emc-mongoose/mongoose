package com.emc.mongoose.base.item.io;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.env.FsUtil;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.io.file.TextFileOutput;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Created by kurila on 09.01.17. */
public final class ItemInfoFileOutput<I extends Item, O extends Operation> implements Output<O> {

	private final Output<String> itemInfoOutput;

	public ItemInfoFileOutput(final Path filePath) throws IOException {
		FsUtil.createParentDirsIfNotExist(filePath);
		itemInfoOutput = new TextFileOutput(filePath);
	}

	@Override
	public final boolean put(final O ioResult) {
		if (ioResult == null) { // poison
			try {
				close();
			} catch (final Exception e) {
				throwUnchecked(e);
			}
			return true;
		}
		return itemInfoOutput.put(ioResult.item().toString());
	}

	@Override
	public final int put(final List<O> ioResults, final int from, final int to) {
		final int n = to - from;
		final List<String> itemsInfo = new ArrayList<>(n);
		O ioResult;
		for (int i = from; i < to; i++) {
			ioResult = ioResults.get(i);
			if (ioResult == null) { // poison
				try {
					return itemInfoOutput.put(itemsInfo, 0, i);
				} finally {
					try {
						close();
					} catch (final Exception e) {
						throwUnchecked(e);
					}
				}
			}
			itemsInfo.add(ioResult.item().toString());
		}
		return itemInfoOutput.put(itemsInfo, 0, n);
	}

	@Override
	public final int put(final List<O> ioResults) {
		final List<String> itemsInfo = new ArrayList<>(ioResults.size());
		for (final O nextIoResult : ioResults) {
			if (nextIoResult == null) { // poison
				try {
					return itemInfoOutput.put(itemsInfo);
				} finally {
					try {
						close();
					} catch (final Exception e) {
						throwUnchecked(e);
					}
				}
			}
			itemsInfo.add(nextIoResult.item().toString());
		}
		return itemInfoOutput.put(itemsInfo);
	}

	@Override
	public final Input<O> getInput() {
		throw new AssertionError();
	}

	@Override
	public final void close() throws Exception {
		itemInfoOutput.close();
	}
}
