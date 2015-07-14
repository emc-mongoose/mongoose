package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
//
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
/**
 The data item input implementation deserializing the data items from the specified stream
 */
public class BinItemInput<T extends DataItem>
implements DataItemInput<T> {
	//
	protected final ObjectInputStream itemsSrc;
	//
	public BinItemInput(final ObjectInputStream itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public T read()
	throws IOException {
		try {
			return (T) itemsSrc.readUnshared();
		} catch(final ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public int read(final List<T> buffer)
	throws IOException {
		int done = 0;
		try {
			final Object o = itemsSrc.readUnshared();
			if(List.class.isInstance(o)) {
				final List<T> l = (List<T>) o;
				buffer.addAll(l);
				done = l.size();
			} else if(DataItem.class.isInstance(o)) {
				buffer.add((T) o);
				done = 1;
			}
		} catch(final ClassNotFoundException | ClassCastException e) {
			throw new IOException(e);
		}
		return done;
	}
	//
	@Override
	public void reset()
	throws IOException {
		itemsSrc.reset();
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsSrc.close();
	}
}
