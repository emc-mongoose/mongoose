package com.emc.mongoose.base.item.op.path;

import com.emc.mongoose.base.item.PathItem;
import com.emc.mongoose.base.item.op.OperationsBuilderImpl;
import com.emc.mongoose.base.storage.Credential;
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
		final String uid;
		return (O) new PathOperationImpl<>(
						originIndex,
						opType,
						pathItem,
						Credential.getInstance(uid = getNextUid(), getNextSecret(uid)));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void buildOps(final List<I> items, final List<O> buff) throws IOException {
		String uid;
		for (final I nextItem : items) {
			buff.add(
							(O) new PathOperationImpl<>(
											originIndex,
											opType,
											nextItem,
											Credential.getInstance(uid = getNextUid(), getNextSecret(uid))));
		}
	}
}
