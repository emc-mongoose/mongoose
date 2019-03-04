package com.emc.mongoose.base.item.op.partial.data;

import com.emc.mongoose.base.item.op.composite.data.CompositeDataOperation;
import com.emc.mongoose.base.item.op.data.DataOperationImpl;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.storage.Credential;

/** Created by andrey on 23.11.16. */
public class PartialDataOperationImpl<I extends DataItem> extends DataOperationImpl<I>
				implements PartialDataOperation<I> {

	private int partNumber;
	private CompositeDataOperation<I> parent;

	public PartialDataOperationImpl() {
		super();
	}

	public PartialDataOperationImpl(
					final int originIndex,
					final OpType opType,
					final I part,
					final String srcPath,
					final String dstPath,
					final Credential credential,
					final int partNumber,
					final CompositeDataOperation<I> parent) {
		super(originIndex, opType, part, srcPath, dstPath, credential, null, 0);
		this.partNumber = partNumber;
		this.parent = parent;
	}

	protected PartialDataOperationImpl(final PartialDataOperationImpl<I> other) {
		super(other);
		this.partNumber = other.partNumber;
		this.parent = other.parent;
	}

	@Override
	public PartialDataOperationImpl<I> result() {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return new PartialDataOperationImpl<>(this);
	}

	@Override
	public final int partNumber() {
		return partNumber;
	}

	@Override
	public final CompositeDataOperation<I> parent() {
		return parent;
	}

	@Override
	public final void finishResponse() {
		super.finishResponse();
		parent.markSubTaskCompleted();
	}
}
