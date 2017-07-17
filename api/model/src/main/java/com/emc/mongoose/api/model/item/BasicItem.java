package com.emc.mongoose.api.model.item;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 Created by kurila on 27.10.15.
 */
public class BasicItem
implements Item {
	
	protected volatile String name = null;
	private int hashCode;
	
	public BasicItem() {
	}
	
	public BasicItem(final String value) {
		if(value == null || value.isEmpty()) {
			throw new IllegalArgumentException("Empty/null item value");
		}
		this.name = value;
		this.hashCode = hashCode();
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public String toString(final String itemPath) {
		return itemPath;
	}

	@Override
	public final String getName() {
		return name;
	}
	
	@Override
	public final void setName(final String name) {
		this.name = name;
	}
	
	@Override
	public void reset() {
	}
	
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
		return this.hashCode == other.hashCode;
	}
	
	@Override
	public int hashCode() {
		return hashCode;
	}
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeUTF(name);
		out.writeInt(hashCode);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		name = in.readUTF();
		hashCode = in.readInt();
	}
}
