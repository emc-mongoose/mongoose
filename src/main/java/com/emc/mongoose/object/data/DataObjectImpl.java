package com.emc.mongoose.object.data;
//
import com.emc.mongoose.base.data.impl.DataRanges;
import com.emc.mongoose.base.data.impl.UniformDataSource;
import com.emc.mongoose.object.data.DataObject;
//
import java.io.IOException;
import java.io.ObjectInput;
/**
 Created by kurila on 01.05.14.
 Basic data object implementation extending DataRanges.
 */
public class DataObjectImpl
extends DataRanges
implements DataObject {
	//
	protected long id;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public DataObjectImpl() {
		super();
		this.id = offset;
	}
	//
	public DataObjectImpl(final String metaInfo) {
		super();
		fromString(metaInfo);
		this.id = offset;
	}
	//
	public DataObjectImpl(final long size) {
		super(size);
		this.id = offset;
	}
	//
	public DataObjectImpl(final long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
		this.id = offset;
	}
	//
	public DataObjectImpl(final long id, final long size) {
		super(id, size);
		this.id = offset;
	}
	//
	public DataObjectImpl(final long id, final long size, final UniformDataSource dataSrc) {
		super(id, size, dataSrc);
		this.id = offset;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final long getId() {
		return id;
	}
	//
	@Override
	public final void setId(final long id)
	throws IOException {
		this.id = id;
		setOffset(id, 0);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		id = offset;
	}
	//
	@Override
	public void fromString(final String v) {
		super.fromString(v);
		id = offset;
	}
}
