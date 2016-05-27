package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.core.api.item.base.Item;
//
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
	protected String path = "";
	protected String name = null;
	//
	public BasicItem() {
	}
	//
	public BasicItem(final String value) {
		fromString(value);
	}
	//
	public BasicItem(final String path, final String name) {
		this.path = path;
		this.name = name;
	}
	//
	protected void fromString(final String value) {
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
		if(path == null) {
			out.writeInt(0);
		} else {
			final byte pathBytes[] = path.getBytes(UTF_8);
			out.writeInt(pathBytes.length);
			out.write(pathBytes, 0, pathBytes.length);
		}
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final byte nameBytes[] = new byte[in.readInt()];
		in.readFully(nameBytes);
		name = new String(nameBytes, UTF_8);
		final int pathBytesCount = in.readInt();
		if(pathBytesCount > 0) {
			final byte pathBytes[] = new byte[pathBytesCount];
			in.readFully(pathBytes);
			path = new String(pathBytes, UTF_8);
		} else {
			path = null;
		}
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
		return path.equals(other.path) && name.equals(other.name);
	}
	//
	@Override
	public int hashCode() {
		return (name == null ? 0 : name.hashCode()) ^ (path == null ? 0 : path.hashCode());
	}
}
