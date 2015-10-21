package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.Collection;
/**
 Created by kurila on 31.07.15.
 */
public final class BasicObjectContainerMock<T extends MutableDataItemMock>
extends LRUMap<String, T>
implements ObjectContainerMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final String name;
	//
	public BasicObjectContainerMock(final String name, final int capacity) {
		super(capacity);
		this.name = name;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected final void moveToMRU(final LinkEntry<String, T> entry) {
		// disable entry moving to MRU in case of access
		// it's required to make list method (right below) working (keeping the linked list order)
	}
	//
	@Override
	public final T list(final String afterOid, final Collection<T> buffDst, final int limit) {
		if(isEmpty()) {
			return null;
		}
		LinkEntry<String, T> nextEntry = getEntry(afterOid);
		for(int i = 0; i < limit; i ++) {
			nextEntry = nextEntry == null ? getEntry(firstKey()) : entryAfter(nextEntry);
			if(nextEntry == null || nextEntry.getKey() == null) {
				break;
			}
			buffDst.add(nextEntry.getValue());
		}
		LOG.debug(
			Markers.MSG, "Container \"{}\": listed {} objects beginning after oid \"{}\"",
			name, buffDst.size(), afterOid
		);
		return (nextEntry == null || nextEntry.getKey() == null) ? null : nextEntry.getValue();
	}
}
