package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.data.model.GenericContainer;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
/**
 The implementation should have a state representing the actual position in the container listing
 */
public abstract class GenericContainerItemInputBase<T extends DataItem>
extends ListItemInput<T>
implements DataItemInput<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final GenericContainer<T> container;
	protected final String nodeAddr;
	protected final Constructor<T> itemConstructor;
	//
	protected GenericContainerItemInputBase(
		final GenericContainer<T> container, final String nodeAddr, final Class<T> itemCls
	) throws IllegalStateException {
		super(new ArrayList<T>(RunTimeConfig.getContext().getBatchSize()));
		this.container = container;
		this.nodeAddr = nodeAddr;
		try {
			this.itemConstructor = itemCls.getConstructor(
				String.class, Long.class, Long.class
			);
		} catch(final NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}
	/**
	 The method should fill the listPageBuffer and return its list iterator
	 @return the list iterator for the buffered items list
	 @throws EOFException if no more items is available from the storage side
	 @throws IOException
	 */
	protected abstract void loadNextPage()
	throws EOFException, IOException;
	//
	protected void loadNewPageIfNecessary()
	throws EOFException, IOException {
		if(i == items.size() || 0 == items.size()) {
			items.clear();
			loadNextPage();
			i = 0;
		}
	}
	//
	@Override
	public final T read()
	throws EOFException, IOException {
		loadNewPageIfNecessary();
		return super.read();
	}
	//
	@Override
	public final int read(final List<T> buffer, final int maxCount)
	throws IOException {
		loadNewPageIfNecessary();
		return super.read(buffer, maxCount);
	}
	/**
	 Read the items from the beginning of the container listing
	 @throws IOException
	 */
	@Override
	public void reset()
	throws IOException {
		i = 0;
	}
	/**
	 The default implementation does nothing
	 @throws IOException doesn't throw
	 */
	@Override
	public void close()
	throws IOException {
	}
	//
	@Override
	public String toString() {
		return "containerItemInput<" + container.getName() + ">";
	}
}
