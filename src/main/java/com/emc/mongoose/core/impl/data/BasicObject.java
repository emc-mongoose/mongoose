package com.emc.mongoose.core.impl.data;
// mongoose-common
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.DataObject;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
/**
 Created by kurila on 01.05.14.
 Basic data object implementation extending DataRanges.
 */
public class BasicObject
extends RangeLayerData
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
	public BasicObject(final String id, final Long offset, final long size) {
		super(offset, size);
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
		final byte idBytes[] = id.getBytes(StandardCharsets.UTF_8);
		out.writeInt(idBytes.length);
		out.write(idBytes, 0, idBytes.length);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		final byte idBytes[] = new byte[in.readInt()];
		in.readFully(idBytes);
		id = new String(idBytes, StandardCharsets.UTF_8);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable serialization implementation /////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static ThreadLocal<StringBuilder> THR_LOCAL_STR_BUILDER = new ThreadLocal<>();
	@Override
	public String toString() {
		StringBuilder strBuilder = THR_LOCAL_STR_BUILDER.get();
		if(strBuilder == null) {
			strBuilder = new StringBuilder();
			THR_LOCAL_STR_BUILDER.set(strBuilder);
		} else {
			strBuilder.setLength(0); // reset
		}
		return strBuilder
			.append(id) .append(RunTimeConfig.LIST_SEP)
			.append(super.toString()).toString();
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
				"Failed to get an object id from the string \"" + v + "\""
			);
		}
		super.fromString(v.substring(posSep + 1));
	}
}
