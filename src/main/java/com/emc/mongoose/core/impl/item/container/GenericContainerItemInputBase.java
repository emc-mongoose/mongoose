package com.emc.mongoose.core.impl.item.container;
//
import com.emc.mongoose.common.conf.BasicConfig;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.data.ContainerHelper;
//
import com.emc.mongoose.core.impl.item.base.ListItemInput;
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
public abstract class GenericContainerItemInputBase<T extends DataItem, C extends Container<T>>
extends ListItemInput<T>
implements Input<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final ContainerHelper<T, C> containerHelper;
	protected final Constructor<T> itemConstructor;
	protected final long maxCount;
	protected final String path;
	//
	protected String lastItemId = null;
	//
	protected GenericContainerItemInputBase(
		final String path, final ContainerHelper<T, C> containerHelper, final Class<T> itemCls,
		final long maxCount
	) throws IllegalStateException {
		super(new ArrayList<T>(BasicConfig.THREAD_CONTEXT.get().getItemSrcBatchSize()));
		this.containerHelper = containerHelper;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.path = path;
		try {
			this.itemConstructor = itemCls.getConstructor(
				String.class, String.class, Long.class, Long.class, Integer.class,
				ContentSource.class
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
	public final T get()
	throws EOFException, IOException {
		loadNewPageIfNecessary();
		return super.get();
	}
	//
	@Override
	public final int get(final List<T> buffer, final int maxCount)
	throws IOException {
		loadNewPageIfNecessary();
		return super.get(buffer, maxCount);
	}
	//
	/**
	 * Does nothing
	 * @param itemsCount count of bytes should be skipped from the input stream
	 * @throws IOException doesn't throw
	 */
	@Override
	public void skip(final long itemsCount)
	throws IOException {
	}
	//
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
		containerHelper.close();
	}
	//
	@Override
	public String toString() {
		return "containerItemInput<" + containerHelper + ">";
	}
}
