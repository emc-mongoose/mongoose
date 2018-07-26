package com.emc.mongoose.item.op.partial.data;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.composite.data.CompositeDataOperation;
import com.emc.mongoose.item.op.data.DataOperationImpl;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.storage.Credential;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 Created by andrey on 23.11.16.
 */
public class PartialDataOperationImpl<I extends DataItem>
extends DataOperationImpl<I>
implements PartialDataOperation<I> {

	private int partNumber;
	private CompositeDataOperation<I> parent;

	public PartialDataOperationImpl() {
		super();
	}

	public PartialDataOperationImpl(
		final int originIndex, final OpType opType, final I part, final String srcPath,
		final String dstPath, final Credential credential, final int partNumber,
		final CompositeDataOperation<I> parent
	) {
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

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeInt(partNumber);
		out.writeObject(parent);
	}

	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.partNumber = in.readInt();
		this.parent = (CompositeDataOperation<I>) in.readObject();
	}
}
