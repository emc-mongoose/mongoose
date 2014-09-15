package com.emc.mongoose.object;
//
import com.emc.mongoose.data.DataRanges;
import com.emc.mongoose.data.UniformDataSource;
//
import java.io.IOException;
import java.io.ObjectInput;
/**
 Created by kurila on 01.05.14.
 Differs from base class with existence of identifier.
 */
public class DataObject
extends DataRanges {
	//
	protected long id;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public DataObject() {
		super();
		this.id = offset;
	}
	//
	public DataObject(final String metaInfo) {
		super();
		fromString(metaInfo);
		this.id = offset;
	}
	//
	public DataObject(final long size) {
		super(size);
		this.id = offset;
	}
	//
	public DataObject(final long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
		this.id = offset;
	}
	//
	public DataObject(final long id, final long size) {
		super(id, size);
		this.id = offset;
	}
	//
	public DataObject(final long id, final long size, final UniformDataSource dataSrc) {
		super(id, size, dataSrc);
		this.id = offset;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final long getId() {
		return id;
	}
	//
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
