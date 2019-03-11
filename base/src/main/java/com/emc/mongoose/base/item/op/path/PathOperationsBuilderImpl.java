package com.emc.mongoose.base.item.op.path;

import com.emc.mongoose.base.item.PathItem;
import com.emc.mongoose.base.item.op.OperationsBuilderImpl;
import java.io.IOException;
import java.util.List;

/** Created by kurila on 30.01.17. */
public class PathOperationsBuilderImpl<I extends PathItem, O extends PathOperation<I>>
				extends OperationsBuilderImpl<I, O> implements PathOperationsBuilder<I, O> {

	public PathOperationsBuilderImpl(final int originIndex) {
		super(originIndex);
	}

	@Override
	@SuppressWarnings("unchecked")
	public O buildOp(final I pathItem) throws IOException {
		final String outputPath = getNextOutputPath();
		return (O) new PathOperationImpl<>(originIndex, opType, pathItem, getNextCredential(outputPath));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void buildOps(final List<I> items, final List<O> buff) throws IOException {
		String outputPath;
		for (final I nextItem : items) {
			outputPath = getNextOutputPath();
			buff.add(
							(O) new PathOperationImpl<>(
											originIndex, opType, nextItem, getNextCredential(outputPath)));
		}
	}
}
