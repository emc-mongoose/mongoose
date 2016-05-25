package com.emc.mongoose.core.impl.item.base;

import com.emc.mongoose.core.api.item.base.Item;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 Created by kurila on 27.10.15.
 */
public class BasicItem
implements Item {
	//
	protected volatile String name = null;
	protected volatile String path = null;
	//
	public BasicItem() {
	}
	//
	public BasicItem(final String path, final String name)
	throws IllegalArgumentException, NullPointerException {
		if(path == null) {
			throw new NullPointerException();
		}
		if(!path.startsWith(SLASH)) {
			throw new IllegalArgumentException("Path should start with /");
		}
		if(name == null) {
			throw new NullPointerException();
		}
		this.name = name;
	}
	//
	public BasicItem(final String fullPath)
	throws IllegalArgumentException {
		fromString(fullPath);
	}
	//
	protected void fromString(final String fullPath)
	throws IllegalArgumentException {
		if(!fullPath.startsWith(SLASH)) {
			throw new IllegalArgumentException("Path should start with " + SLASH);
		}
		final int lastSlashPos = fullPath.lastIndexOf(SLASH);
		if(lastSlashPos == 0) {
			path = SLASH;
			if(fullPath.length() > 1) {
				name = fullPath.substring(1);
			} else {
				throw new IllegalArgumentException(
					"No item name is in the full path: \"" + fullPath + "\""
				);
			}
		} else if(lastSlashPos > 0) {
			if(lastSlashPos < fullPath.length() - 1) {
				path = fullPath.substring(0, lastSlashPos);
				name = fullPath.substring(lastSlashPos + 1);
			} else {
				throw new IllegalArgumentException(
					"No item name is in the full path: \"" + fullPath + "\""
				);
			}
		}
	}
	//
	@Override
	public String toString() {
		return path + SLASH + name;
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
	public final String getPath() {
		return path;
	}
	//
	@Override
	public final void setPath(final String path) {
		this.path = path;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		final byte nameBytes[] = name.getBytes(UTF_8);
		out.writeInt(nameBytes.length);
		out.write(nameBytes, 0, nameBytes.length);
		final byte pathBytes[] = path.getBytes(UTF_8);
		out.writeInt(pathBytes.length);
		out.write(pathBytes, 0, pathBytes.length);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final byte nameBytes[] = new byte[in.readInt()];
		in.readFully(nameBytes);
		name = new String(nameBytes, UTF_8);
		final byte pathBytes[] = new byte[in.readInt()];
		in.readFully(pathBytes);
		path = new String(pathBytes, UTF_8);
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
		return (name == null ? 0 : name.hashCode()) ^ (path == null ? 0 : path.hashCode());
	}
}
