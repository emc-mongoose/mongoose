package com.emc.mongoose.base.item.op.data;

import static com.emc.mongoose.base.item.DataItem.rangeCount;
import static com.github.akurilov.commons.system.SizeInBytes.formatFixedSize;

import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.op.OperationsBuilderImpl;
import com.emc.mongoose.base.item.op.composite.data.CompositeDataOperationImpl;
import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.math.Random;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Created by kurila on 14.07.16. */
public class DataOperationsBuilderImpl<I extends DataItem, O extends DataOperation<I>>
				extends OperationsBuilderImpl<I, O> implements DataOperationsBuilder<I, O> {

	private final Random rnd = new Random();

	protected volatile List<I> srcItemsForConcat = null;
	protected volatile int srcItemsCount = 0;
	protected volatile int srcItemsCountMin = 0;
	protected volatile int srcItemsCountMax = 0;
	protected volatile List<Range> fixedRanges = null;
	protected volatile int randomRangesCount = 0;
	protected volatile long sizeThreshold = 0;

	public DataOperationsBuilderImpl(final int originIndex) {
		super(originIndex);
	}

	@Override
	public DataOperationsBuilderImpl<I, O> fixedRanges(final List<Range> fixedRanges) {
		this.fixedRanges = fixedRanges;
		return this;
	}

	@Override
	public DataOperationsBuilderImpl<I, O> randomRangesCount(final int count) {
		this.randomRangesCount = count;
		return this;
	}

	@Override
	public DataOperationsBuilderImpl<I, O> sizeThreshold(final long sizeThreshold) {
		this.sizeThreshold = sizeThreshold > 0 ? sizeThreshold : Long.MAX_VALUE;
		return this;
	}

	@Override
	public DataOperationsBuilderImpl<I, O> srcItemsCount(final int min, final int max) {
		this.srcItemsCountMin = min;
		this.srcItemsCountMax = max;
		return this;
	}

	@Override
	public DataOperationsBuilderImpl<I, O> srcItemsForConcat(final List<I> srcItemsForConcat) {
		this.srcItemsForConcat = srcItemsForConcat;
		if (this.srcItemsForConcat != null) {
			this.srcItemsCount = srcItemsForConcat.size();
		}
		return this;
	}

	@Override
	public List<Range> fixedRanges() {
		return fixedRanges;
	}

	@Override
	public int randomRangesCount() {
		return randomRangesCount;
	}

	@Override
	public long sizeThreshold() {
		return sizeThreshold;
	}

	@Override
	@SuppressWarnings("unchecked")
	public O buildOp(final I dataItem) throws IOException, IllegalArgumentException {
		final String uid;
		final String outputPath = getNextOutputPath();
		if (dataItem.size() > sizeThreshold) {
			if (randomRangesCount > 0 || (fixedRanges != null && fixedRanges.size() > 0)) {
				throw new IllegalArgumentException(
								"Not supported - both byte ranges configured and size threshold");
			}
			return (O) new CompositeDataOperationImpl<>(
							originIndex,
							opType,
							dataItem,
							inputPath,
							outputPath,
							getNextCredential(outputPath),
							fixedRanges,
							randomRangesCount,
							sizeThreshold);
		} else if (srcItemsCount > 0) {
			return (O) new DataOperationImpl<>(
							originIndex,
							opType,
							dataItem,
							inputPath,
							outputPath,
							getNextCredential(outputPath),
							fixedRanges,
							randomRangesCount,
							getNextSrcItemsForConcat());
		} else {
			if (randomRangesCount > rangeCount(dataItem.size())) {
				throw new IllegalArgumentException(
								"Configured random ranges count ("
												+ randomRangesCount
												+ ") is more than "
												+ "allowed for the data item w/ size "
												+ formatFixedSize(dataItem.size()));
			}
			return (O) new DataOperationImpl<>(
							originIndex,
							opType,
							dataItem,
							inputPath,
							outputPath,
							getNextCredential(outputPath),
							fixedRanges,
							randomRangesCount);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void buildOps(final List<I> items, final List<O> buff)
					throws IOException, IllegalArgumentException {
		String uid;
		String outputPath;
		for (final I nextItem : items) {
			outputPath = getNextOutputPath();
			if (nextItem.size() > sizeThreshold) {
				if (randomRangesCount > 0 || (fixedRanges != null && fixedRanges.size() > 0)) {
					throw new IllegalArgumentException(
									"Not supported - both byte ranges configured and size threshold");
				}
				buff.add(
								(O) new CompositeDataOperationImpl<>(
												originIndex,
												opType,
												nextItem,
												inputPath,
												outputPath,
												getNextCredential(outputPath),
												fixedRanges,
												randomRangesCount,
												sizeThreshold));
			} else if (srcItemsCount > 0) {
				buff.add(
								(O) new DataOperationImpl<>(
												originIndex,
												opType,
												nextItem,
												inputPath,
												outputPath,
												getNextCredential(outputPath),
												fixedRanges,
												randomRangesCount,
												getNextSrcItemsForConcat()));
			} else {
				if (randomRangesCount > rangeCount(nextItem.size())) {
					throw new IllegalArgumentException(
									"Configured random ranges count ("
													+ randomRangesCount
													+ ") is more than "
													+ "allowed for the data item w/ size "
													+ formatFixedSize(nextItem.size()));
				}
				buff.add(
								(O) new DataOperationImpl<>(
												originIndex,
												opType,
												nextItem,
												inputPath,
												outputPath,
												getNextCredential(outputPath),
												fixedRanges,
												randomRangesCount));
			}
		}
	}

	@Override
	public void close() {
		super.close();
		if (srcItemsForConcat != null) {
			srcItemsForConcat.clear();
			srcItemsForConcat = null;
		}
		if (fixedRanges != null) {
			fixedRanges.clear();
			fixedRanges = null;
		}
	}

	protected List<I> getNextSrcItemsForConcat() {
		final int n = srcItemsCountMin < srcItemsCountMax
						? srcItemsCountMin + rnd.nextInt(srcItemsCountMax - srcItemsCountMin + 1)
						: srcItemsCountMin;
		final List<I> selectedItems = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			selectedItems.add(srcItemsForConcat.get(rnd.nextInt(srcItemsCount)));
		}
		return selectedItems;
	}
}
