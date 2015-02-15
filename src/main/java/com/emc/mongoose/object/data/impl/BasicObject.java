package com.emc.mongoose.object.data.impl;
//
import com.emc.mongoose.base.data.impl.DataRanges;
import com.emc.mongoose.base.data.impl.UniformDataSource;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
/**
 Created by kurila on 01.05.14.
 Basic data object implementation extending DataRanges.
 */
public class BasicObject
extends DataRanges
implements DataObject {
	//
	protected String id = null;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public BasicObject() {
		super();
	}
	//
	public BasicObject(final String metaInfo) {
		super();
		fromString(metaInfo);
	}
	//
	public BasicObject(final Long size) {
		super(size);
	}
	//
	public BasicObject(final Long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
	}
	//
	public BasicObject(final String id, final Long size) {
		super(size);
		this.id = id;
	}
	//
	public BasicObject(final String id, final Long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
		this.id = id;
	}
	//
	public BasicObject(final String id, final Long offset, final long size) {
		super(offset, size);
		this.id = id;
	}
	//
	public BasicObject(
		final String id, final Long offset, final Long size, final UniformDataSource dataSrc
	) {
		super(offset, size, dataSrc);
		this.id = id;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String getId() {
		return id;
	}
	//
	@Override
	public final void setId(final String id) {
		this.id = id;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(id);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		id = String.class.cast(in.readObject());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable serialization implementation /////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final static String FMT2STR = "%s" + RunTimeConfig.LIST_SEP + "%s";
	//
	@Override
	public String toString() {
		return String.format(FMT2STR, id, super.toString());
	}
	//
	@Override
	public void fromString(final String v)
	throws IllegalArgumentException {
		final int posSep = v.indexOf(RunTimeConfig.LIST_SEP);
		if(posSep > 0 && posSep < v.length() - 1) {
			id = v.substring(0, posSep);
		} else {
			throw new IllegalArgumentException(
				String.format("Failed to get an object id from the string \"%s\"", v)
			);
		}
		super.fromString(v.substring(posSep + 1));
	}
}
