package com.emc.mongoose.base.item.op.token;

import com.emc.mongoose.base.item.TokenItem;
import com.emc.mongoose.base.item.op.OperationsBuilderImpl;
import java.io.IOException;
import java.util.List;

/** Created by kurila on 14.07.16. */
public class TokenOperationsBuilderImpl<I extends TokenItem, O extends TokenOperation<I>>
				extends OperationsBuilderImpl<I, O> implements TokenOperationsBuilder<I, O> {

	public TokenOperationsBuilderImpl(final int originIndex) {
		super(originIndex);
	}

	@Override
	@SuppressWarnings("unchecked")
	public O buildOp(final I item) throws IOException {
		final String outputPath = getNextOutputPath();
		return (O) new TokenOperationImpl<>(originIndex, opType, item, getNextCredential(outputPath));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void buildOps(final List<I> items, final List<O> buff) throws IOException {
		String outputPath;
		for (final I item : items) {
			outputPath = getNextOutputPath();
			buff.add(
							(O) new TokenOperationImpl<>(originIndex, opType, item, getNextCredential(outputPath)));
		}
	}
}
