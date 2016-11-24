package com.emc.mongoose.model.item;

import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.DataCorruptionException;
import com.emc.mongoose.model.data.DataSizeException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 Created by kurila on 11.07.16.
 */
public interface DataItem
extends Item, SeekableByteChannel {
	
	ContentSource getContentSrc();
	
	void setContentSrc(final ContentSource contentSrc);
	
	void reset();
	
	int layer();

	void layer(final int layerNum);
	
	void size(final long size);
	
	long offset();
	
	void offset(final long offset);

	<D extends DataItem> D slice(final long from, final long to);
	
	/**
	 * @return The number of bytes written, possibly zero
	 * @throws NonWritableChannelException
	 *          If this channel was not opened for writing
	 * @throws ClosedChannelException
	 *          If this channel is closed
	 * @throws AsynchronousCloseException
	 *          If another thread closes this channel
	 *          while the write operation is in progress
	 * @throws ClosedByInterruptException
	 *          If another thread interrupts the current thread
	 *          while the write operation is in progress, thereby
	 *          closing the channel and setting the current thread's
	 *          interrupt status
	 * @throws  IOException
	 *          If some other I/O error occurs
	 */
	int write(final WritableByteChannel chanDst, final long maxCount)
	throws IOException;
	
	int readAndVerify(final ReadableByteChannel chanSrc, final ByteBuffer buff)
	throws DataSizeException, DataCorruptionException, IOException;
}
