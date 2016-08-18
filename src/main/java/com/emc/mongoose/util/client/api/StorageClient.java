package com.emc.mongoose.util.client.api;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.common.io.Output;
//
import java.io.Closeable;
import java.io.IOException;

/**
 Created by kurila on 15.06.15.
 The client class supporting the following storage I/O methods: create, write, copy, update, read, delete.
 Note that all the methods are blocking. Use a low-level load execution
 builders and jobs interface if a non-blocking approach is required.
 <p>Every method accepts an {@link Input} stream as a 1st
 argument which is used as the source of the data items descriptors which should be processed
 (e.g. written/read/deleted/etc). The resulting behavior is different for write methods and the
 remaining methods. If the value of the 1st argument is null write methods will generate new data
 items and read/delete/update/append methods will try to get the data items list from the specified
 bucket/container. Otherwise (1st argument is not null) write methods will re-create (possibly
 overwrite) exactly the same data items (same ids, sizes and contents) as specified by the source
 stream content and the remaining methods will process them as expected (read/delete/etc).</p>
 <p>The 2nd argument is always a destination for the data items which is used to serialize them
 after being processed successfully.</p>
 <p>Also each method returns the count of the successfully processed items.</p>
 */
public interface StorageClient<T extends Item>
extends Closeable {

	/**
	 Create <b><i>new</i></b> items of the specified size infinitely, do not store the written data items info
	 @param size the size of the data items to write.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	long create(final long size)
	throws IllegalArgumentException, InterruptedException, IOException;

	/**
	 Create new items in a customized way using fixed data items sizes.
	 @param itemOutput data items info destination, may be null
	 @param countLimit the count limit of the data items to write, 0 means no limit.
	 @param connPerNodeCount the count of the concurrent connections per storage node
	 @param size the size of the data items to write.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	long create(
		final Output<T> itemOutput, final long countLimit, final int connPerNodeCount,
		final long size
	) throws IllegalArgumentException, InterruptedException, IOException;

	/**
	 Create new items in a customized way using specific data items sizes distribution.
	 @param itemOutput data items info destination, may be null
	 @param countLimit the count limit of the data items to write, 0 means no limit.
	 @param connPerNodeCount the count of the concurrent connections per storage node
	 @param minSize the minimum data item size
	 @param maxSize the maximum data item size
	 @param sizeBias see the
	 <a href="https://asdwiki.isus.emc.com:8443/display/OS/Mongoose+HowTo#MongooseHowTo-Howtodealwithdataitemsizedistribution">doc regarding this feature</a>
	 @return the count of the items written actually
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	long create(
		final Output<T> itemOutput, final long countLimit, final int connPerNodeCount,
		final long minSize, final long maxSize, final float sizeBias
	) throws IllegalArgumentException, InterruptedException, IOException;

	/**
	 Write the items (possibly existing)
	 @param itemInput
	 @param itemOutput
	 @param countLimit
	 @param connPerNodeCount
	 @throws IllegalArgumentException
	 @throws InterruptedException
	 @throws IOException
	 */
	long write(
		final Input<T> itemInput, final Output<T> itemOutput, final long countLimit,
		final int connPerNodeCount
	) throws IllegalArgumentException, InterruptedException, IOException;

	/**
	 Copy the items from the source container to the destionation one
	 @param itemInput
	 @param itemOutput
	 @param srcContainer
	 @param dstContainer
	 @param countLimit
	 @param connPerNodeCount
	 @throws IllegalArgumentException
	 @throws InterruptedException
	 @throws IOException
	 */
	/*long copy(
		final Input<T> itemInput, final Output<T> itemOutput,
		final String srcContainer, final String dstContainer,
		final long countLimit, final int connPerNodeCount
	) throws IllegalArgumentException, InterruptedException, IOException;*/

	/**
	 Write the partial data (update random ranges)
	 @param itemInput
	 @param itemOutput
	 @param countLimit
	 @param connPerNodeCount
	 @param randomRangesCount
	 @throws IllegalArgumentException
	 @throws InterruptedException
	 @throws IOException
	 */
	long update(
		final Input<T> itemInput, final Output<T> itemOutput, final long countLimit,
		final int connPerNodeCount, final int randomRangesCount
	) throws IllegalArgumentException, InterruptedException, IOException;

	/**
	 Write the partial data (update ranges/append)
	 @param itemInput
	 @param itemOutput
	 @param countLimit
	 @param connPerNodeCount
	 @param fixedByteRanges
	 @throws IllegalArgumentException
	 @throws InterruptedException
	 @throws IOException
	 */
	long update(
		final Input<T> itemInput, final Output<T> itemOutput, final long countLimit,
		final int connPerNodeCount, final String fixedByteRanges
	) throws IllegalArgumentException, InterruptedException, IOException;

	/**
	 Read the data items using the specified items input, do not store the output data items info.
	 @param itemInput data items info source
	 @throws java.lang.IllegalStateException if no data items list is available and no bucket/container is specified
	 */
	long read(final Input<T> itemInput)
	throws IllegalStateException, InterruptedException, IOException;

	/**
	 Read the data items in a customized way.
	 @param itemInput items info source
	 @param itemOutput items info destination, may be null
	 @param countLimit the count limit of the items to write, 0 means no limit.
	 @param connPerNodeCount the count of the concurrent connections per storage node
	 @param verifyContentFlag To verify the content integrity or to not verify.
	 @throws java.lang.IllegalStateException if no data items list is available and no bucket/container is specified
	 */
	long read(
		final Input<T> itemInput, final Output<T> itemOutput,
		final long countLimit, final int connPerNodeCount, final boolean verifyContentFlag
	) throws IllegalStateException, InterruptedException, IOException;

	/**
	 Partial read
	 @param itemInput data items info source
	 @param itemOutput data items info destination, may be null
	 @param countLimit the count limit of the data items to write, 0 means no limit.
	 @param connPerNodeCount the count of the concurrent connections per storage node
	 @param verifyContentFlag To verify the content integrity or to not verify.
	 @param randomRangesCount
	 @throws java.lang.IllegalStateException if no data items list is available and no bucket/container is specified
	 */
	long read(
		final Input<T> itemInput, final Output<T> itemOutput, final long countLimit, final int connPerNodeCount,
		final boolean verifyContentFlag, final int randomRangesCount
	) throws IllegalStateException, InterruptedException, IOException;

	/**
	 Partial read
	 @param itemInput data items info source
	 @param itemOutput data items info destination, may be null
	 @param countLimit the count limit of the data items to write, 0 means no limit.
	 @param connPerNodeCount the count of the concurrent connections per storage node
	 @param verifyContentFlag To verify the content integrity or to not verify.
	 @param fixedByteRanges
	 @throws java.lang.IllegalStateException if no data items list is available and no bucket/container is specified
	 */
	long read(
		final Input<T> itemInput, final Output<T> itemOutput, final long countLimit, final int connPerNodeCount,
		final boolean verifyContentFlag, final String fixedByteRanges
	) throws IllegalStateException, InterruptedException, IOException;

	/**
	 Delete the data items using the specified data items source, do not store the output data items info.
	 @param itemInput data items info source
	 @throws java.lang.IllegalStateException if no data items list is available and no bucket/container is specified
	 */
	long delete(final Input<T> itemInput)
	throws IllegalStateException, InterruptedException, IOException;

	/**
	 Delete the data items in a customized way.
	 @param itemInput data items info source
	 @param itemOutput data items info destination, may be null
	 @param countLimit the count limit of the data items to write, 0 means no limit.
	 @param connPerNodeCount the count of the concurrent connections per storage node
	 @throws java.lang.IllegalStateException if no data items list is available and no bucket/container is specified
	 */
	long delete(
		final Input<T> itemInput, final Output<T> itemOutput,
		final long countLimit, final int connPerNodeCount
	) throws IllegalStateException, InterruptedException, IOException;
}
