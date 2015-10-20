package com.emc.mongoose.core.impl.container;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
/**
 Created by kurila on 20.10.15.
 */
public class BasicContainer<T extends DataItem>
implements Container {
	//
	protected volatile String name;
	//
	public BasicContainer() {
		this.name = null;
	}
	//
	public BasicContainer(final String name) {
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return name;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals(final Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof BasicContainer)) {
			return false;
		}
		final BasicContainer other = (BasicContainer) o;
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
}
