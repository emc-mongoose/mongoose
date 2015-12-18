package com.emc.mongoose.core.impl;
//
import com.emc.mongoose.core.api.item.base.Item;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
/**
 Created by kurila on 27.10.15.
 */
public class BasicItem
implements Item {
	//
	protected volatile String name;
	//
	public BasicItem() {
		this(null);
	}
	//
	public BasicItem(final String name) {
		this.name = name;
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final void setName(final String name) {
		this.name = name;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		final byte nameBytes[] = name.getBytes(StandardCharsets.UTF_8);
		out.writeInt(nameBytes.length);
		out.write(nameBytes, 0, nameBytes.length);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final byte nameBytes[] = new byte[in.readInt()];
		in.readFully(nameBytes);
		name = new String(nameBytes, StandardCharsets.UTF_8);
	}
	//
	@Override
	public boolean equals(final Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof BasicItem)) {
			return false;
		}
		final BasicItem other = (BasicItem) o;
		if(name == null) {
			return other.name == null;
		}
		return name.equals(other.name);
	}
	//
	@Override
	public int hashCode() {
		return name == null ? 0 : name.hashCode();
	}
	//
	@Override
	public String toString() {
		return name;
	}
}
