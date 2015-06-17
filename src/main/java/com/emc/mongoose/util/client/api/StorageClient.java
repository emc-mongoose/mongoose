package com.emc.mongoose.util.client.api;
//
import java.io.Closeable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
/**
 Created by kurila on 15.06.15.
 The client class supporting the following storage I/O methods: write, read, delete, update, append.
 Note that all the methods are blocking. Use a low-level load execution
 builders and jobs interface if a non-blocking approach is required.
 <p>Every method accepts an {@link java.io.ObjectOutputStream} stream as a 1st argument which is
 used as the source of the data items descriptors which should be processed
 (e.g. written/read/deleted/etc). The resulting behavior is different for write methods and the
 remaining methods. If the value of the 1st argument is null write methods will generate new data
 items and read/delete/update/append methods will try to get the data items list from the specified
 bucket/container. Otherwise (1st argument is not null) write methods will re-create (possibly
 overwrite) exactly the same data items (same ids, sizes and contents) as specified by the source
 stream content and the remaining methods will process them as expected (read/delete/etc).</p>
 <p>The 2nd argument is always destination items stream which is used to serialize the data items
 after being processed successfully.</p>
 <p>Also each method returns the count of the successfully processed data items.</p>
 */
public interface StorageClient
extends Closeable {

	/**
	 Write the fixed-sized data items on the storage.
	 @param size the size of the data items to write.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	long write(
		final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream,
		final long size
	) throws IllegalArgumentException;

	/**
	 Write the randomly sized data items on the target storage.
	 @param minSize the minimum data item size
	 @param maxSize the maximum data item size
	 @param sizeBias see the
	 <a href="https://asdwiki.isus.emc.com:8443/display/OS/Mongoose+HowTo#MongooseHowTo-Howtodealwithdataitemsizedistribution">doc regarding this feature</a>
	 @return self.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	long write(
		final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream,
		final long minSize, final long maxSize, final float sizeBias
	) throws IllegalArgumentException;

	/**
	 Read the data items from the storage.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	long read(final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream)
	throws IllegalStateException;

	/**
	 The same as {@link #read(ObjectInputStream, ObjectOutputStream)} but with ability to control
	 the content verification
	 @param verifyContentFlag To verify the content integrity or to not verify.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	long read(
		final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream,
		final boolean verifyContentFlag
	) throws IllegalStateException;

	/**
	 Delete the data items on the storage.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	long delete(final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream)
	throws IllegalStateException;

	/**
	 Update the data items on the storage.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	long update(final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream)
	throws IllegalStateException;

	/**
	 The same as {@link #update(java.io.ObjectInputStream, java.io.ObjectOutputStream)} but with
	 ability to specify the count of the updated ranges per request.
	 @param countPerTime the count of the non-overlapping ranges to update per one request
	 @throws java.lang.IllegalArgumentException if non-positive value is passed
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	long update(
		final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream,
		final int countPerTime
	) throws IllegalArgumentException, IllegalStateException;

	/**
	 Append the data items on the storage. By default makes each data item size twice larger (rly?).
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	long append(final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream)
	throws IllegalStateException;

	/**
	 The same as {@link #append(java.io.ObjectInputStream, java.io.ObjectOutputStream)} but with
	 ability to customize the size of the augment to append.
	 @param augmentSize the size of the data item augment to append
	 @throws IllegalArgumentException if non-positive value is passed
	 @throws IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	long append(
		final ObjectInputStream srcItemsStream, final ObjectOutputStream dstItemsStream,
		final long augmentSize
	) throws IllegalArgumentException, IllegalStateException;
}
