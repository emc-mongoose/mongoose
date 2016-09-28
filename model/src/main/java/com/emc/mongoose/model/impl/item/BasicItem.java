package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.item.Item;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 Created by kurila on 27.10.15.
 */
public class BasicItem
implements Item {
	//
	protected String path = "";
	protected String name = null;
	//
	public BasicItem() {
	}
	//
	public BasicItem(final String value) {
		if(value == null || value.isEmpty()) {
			throw new IllegalArgumentException("Empty/null item value");
		}
		final int lastSlashPos = value.lastIndexOf(SLASH);
		if(lastSlashPos < 0) {
			this.name = value;
		} else {
			path = value.substring(0, lastSlashPos + 1);
			name = value.substring(lastSlashPos + 1);
		}
	}
	//
	public BasicItem(final String path, final String name) {
		this.path = path;
		this.name = name;
	}
	//
	@Override
	public String toString() {
		if(path == null) {
			return name;
		} else if(path.endsWith(SLASH)) {
			return path + name;
		} else {
			return path + SLASH + name;
		}
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
	
	@Override
	public void reset() {
	}
	
	@Override
	public final String getPath() {
		return path;
	}
	
	@Override
	public final void setPath(final String path) {
		this.path = path;
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
		return path.equals(other.path) && name.equals(other.name);
	}
	//
	@Override
	public int hashCode() {
		return (name == null ? 0 : name.hashCode()) ^ (path == null ? 0 : path.hashCode());
	}
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeObject(path);
		out.writeObject(name);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		path = (String) in.readObject();
		name = (String) in.readObject();
	}
}
