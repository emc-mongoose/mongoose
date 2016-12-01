package com.emc.mongoose.model.item;

import com.emc.mongoose.model.data.ContentSource;

import java.io.IOException;

/**
 Created by kurila on 21.09.16.
 */
public class BasicDataItemFactory<I extends DataItem>
extends BasicItemFactory<I>
implements DataItemFactory<I> {
	
	private transient volatile ContentSource contentSrc;
	
	public BasicDataItemFactory(final ContentSource contentSrc) {
		this.contentSrc = contentSrc;
	}
	
	@Override
	public I getItem(final String name, final long id, final long size) {
		return (I) new BasicDataItem(name, id, size, contentSrc);
	}
	
	@Override
	public I getItem(final String line) {
		return (I) new BasicDataItem(line, contentSrc);
	}

	@Override
	public Class<I> getItemClass() {
		return (Class<I>) BasicDataItem.class;
	}

	@Override
	public final ContentSource getContentSource() {
		return contentSrc;
	}

	@Override
	public final void setContentSource(final ContentSource contentSrc) {
		this.contentSrc = contentSrc;
	}

	@Override
	public void close()
	throws IOException {
		super.close();
		contentSrc.close();
	}
}
