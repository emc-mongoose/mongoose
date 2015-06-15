package com.emc.mongoose.storage.client.api;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 15.06.15.
 */
public interface StorageClient<T extends DataItem>
extends Closeable {

	/**
	 Set the storage API to use.
	 @param api The value should match to any child package name from the
	 "com.emc.mongoose.storage.adapter" package ("atmos", "s3", "swift", etc).
	 @return self.
	 @throws IllegalArgumentException if there's no package having the specified name.
	 */
	StorageClient<T> api(final String api)
	throws IllegalArgumentException;

	/**
	 Set the storage node address list.
	 @param nodeAddrs Storage nodes list. FQDNs and IPs are acceptable.
	 Individual ports may be specified also, for example:
	 new String[] { "10.123.45.67:9020", "10.123.45.68:9021", "10.123.45.69:9022" }
	 @return self.
	 @throws IllegalArgumentException if null or empty
	 */
	StorageClient<T> nodes(final String nodeAddrs[])
	throws IllegalArgumentException;

	/**
	 Change the run mode to load client mode, set the list of the remote load servers.
	 @param loadServers Load servers list. FQDNs and IPs are acceptable.
	 @return self.
	 @throws IllegalArgumentException if null or empty
	 */
	StorageClient<T> remote(final String loadServers[])
	throws IllegalArgumentException;

	/**
	 Set credentials necessary to access the storage.
	 @param id user id, for example "wuser1@sanity.local"
	 @param secret the secret
	 @return self.
	 @throws IllegalArgumentException if either id or secret is null or empty
	 */
	StorageClient<T> auth(final String id, final String secret)
	throws IllegalArgumentException;

	/**
	 Set the target S3 bucket for writing to/reading from/etc.
	 @param value The name of the bucket, for example "sanity-nh-bucket1"
	 @return self.
	 */
	StorageClient<T> bucket(final String value);

	/**
	 Set the target Swift container for writing to/reading from/etc.
	 @param value The name of the container, for example "sanity-nh-container1"
	 @return self.
	 */
	StorageClient<T> container(final String value);

	/**
	 Set the Atmos-specific subtenant to use.
	 @param value The subtenant string value.
	 @return self.
	 */
	StorageClient<T> subtenant(final String value);

	/**
	 Set the Swift-specific authentication token to use.
	 @param value The authentication token string value
	 @return self.
	 */
	StorageClient<T> authToken(final String value);

	/**
	 Limit the storage I/O invocation (write/read/etc) by data items count.
	 @param count the max count of the data items to write/read/delete/etc, 0 means no limit
	 @return self.
	 @throws java.lang.IllegalArgumentException if less than 0
	 */
	StorageClient<T> limitCount(final long count)
	throws IllegalArgumentException;

	/**
	 Limit the storage I/O invocation (write/read/etc) by the time.
	 @param timeOut the maximum time to perform a I/O invocation, 0 means no limit
	 @param timeUnit the time unit.
	 @return self.
	 @throws java.lang.IllegalArgumentException if time out is negative or time unit is null
	 */
	StorageClient<T> limitTime(final long timeOut, final TimeUnit timeUnit)
	throws IllegalArgumentException;

	/**
	 Limit the storage I/O invocation (write/read/etc) by the rate.
	 @param rate the maximum rate (data items/sec) which should be sustained, 0 means no limit
	 @return self.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	StorageClient<T> limitRate(final float rate)
	throws IllegalArgumentException;

	/**
	 Write the fixed-sized data items on the storage.
	 @param size the size of the data items to write.
	 @return self.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	StorageClient<T> write(final long size)
	throws IllegalArgumentException;

	/**
	 Write the randomly sized data items on the target storage.
	 @param minSize the minimum data item size
	 @param maxSize the maximum data item size
	 @param sizeBias see the
	 <a href="https://asdwiki.isus.emc.com:8443/display/OS/Mongoose+HowTo#MongooseHowTo-Howtodealwithdataitemsizedistribution">doc regarding this feature</a>
	 @return self.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	StorageClient<T> write(final long minSize, final long maxSize, final float sizeBias)
	throws IllegalArgumentException;

	/**
	 Read the data items from the storage.
	 Tries to use the data items list successfully written in the previous I/O invocation.
	 If no I/O operation has been invoked yet will try to read from the specified bucket/container.
	 Fails otherwise.
	 @return self.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	StorageClient<T> read()
	throws IllegalStateException;

	/**
	 The same as {@link #read()} but with ability to control the content verification
	 @param verifyContent To verify the content integrity or to not verify.
	 @return self.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	StorageClient<T> read(final boolean verifyContent)
	throws IllegalStateException;

	/**
	 Delete the data items on the storage.
	 Tries to use the data items list successfully written in the previous I/O invocation.
	 If no I/O operation has been invoked yet will try to delete from the specified bucket/container.
	 Fails otherwise.
	 @return self.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	StorageClient<T> delete()
	throws IllegalStateException;

	/**
	 Update the data items on the storage.
	 Tries to use the data items list successfully written in the previous I/O invocation.
	 If no I/O operation has been invoked yet will try to update in the specified bucket/container.
	 Fails otherwise.
	 @return self.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	StorageClient<T> update()
	throws IllegalStateException;

	/**
	 The same as {@link #update()} but with ability to specify the count of the updated ranges per request.
	 @param countPerTime the count of the non-overlapping ranges to update per one request
	 @return self.
	 @throws java.lang.IllegalArgumentException if non-positive value is passed
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	StorageClient<T> update(final int countPerTime)
	throws IllegalArgumentException, IllegalStateException;

	/**
	 Append the data items on the storage. By default makes each data item size twice larger(rly?).
	 Tries to use the data items list successfully written in the previous I/O invocation.
	 If no I/O operation has been invoked yet will try to update in the specified bucket/container.
	 Fails otherwise.
	 @return self.
	 @throws java.lang.IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	StorageClient<T> append()
	throws IllegalStateException;

	/**
	 The same as {@link #append()} but with ability to customize the size of the augment to append.
	 @param augmentSize the size of the data item augment to append
	 @return self.
	 @throws IllegalArgumentException if non-positive value is passed
	 @throws IllegalStateException if no data items list is available and
	 no bucket/container is specified
	 */
	StorageClient<T> append(final long augmentSize)
	throws IllegalArgumentException, IllegalStateException;
}
