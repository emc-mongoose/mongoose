package com.emc.mongoose.util.client.api;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 17.06.15.
 */
public interface StorageClientBuilder<U extends StorageClient> {
	/**
	 Set the storage API to use.
	 @param api The value should match to any child package name from the
	 "com.emc.mongoose.storage.adapter" package ("atmos", "s3", "swift", etc).
	 @return self.
	 @throws IllegalArgumentException if there's no package having the specified name.
	 */
	StorageClientBuilder<U> setAPI(final String api)
	throws IllegalArgumentException;

	/**
	 Set the storage node address list.
	 @param nodeAddrs Storage nodes list. FQDNs and IPs are acceptable.
	 Individual ports may be specified also, for example:
	 new String[] { "10.123.45.67:9020", "10.123.45.68:9021", "10.123.45.69:9022" }
	 @return self.
	 @throws IllegalArgumentException if null or empty
	 */
	StorageClientBuilder<U> setNodes(final String nodeAddrs[])
	throws IllegalArgumentException;

	/**
	 Change the run mode to load client mode, set the list of the remote load servers.
	 @param loadServers Load servers list. FQDNs and IPs are acceptable.
	 @return self.
	 @throws IllegalArgumentException if null or empty
	 */
	StorageClientBuilder<U> setClientMode(final String loadServers[])
	throws IllegalArgumentException;

	/**
	 Set credentials necessary to access the storage.
	 @param id user id, for example "wuser1@sanity.local"
	 @param secret the secret
	 @return self.
	 @throws IllegalArgumentException if either id or secret is null or empty
	 */
	StorageClientBuilder<U> setAuth(final String id, final String secret)
	throws IllegalArgumentException;

	/**
	 Set the target S3 bucket for writing to/reading from/etc.
	 @param value The name of the bucket, for example "sanity-nh-bucket1"
	 @return self.
	 */
	StorageClientBuilder<U> setS3Bucket(final String value);

	/**
	 Set the target Swift container for writing to/reading from/etc.
	 @param value The name of the container, for example "sanity-nh-container1"
	 @return self.
	 */
	StorageClientBuilder<U> setSwiftContainer(final String value);

	/**
	 Set the Atmos-specific subtenant to use.
	 @param value The subtenant string value.
	 @return self.
	 */
	StorageClientBuilder<U> setAtmosSubtenant(final String value);

	/**
	 Set the Swift-specific authentication token to use.
	 @param value The authentication token string value
	 @return self.
	 */
	StorageClientBuilder<U> setSwiftAuthToken(final String value);

	/**
	 Limit the storage I/O methods execution by data items count.
	 @param count the max count of the data items to write/read/delete/etc, 0 means no limit
	 @return self.
	 @throws java.lang.IllegalArgumentException if less than 0
	 */
	StorageClientBuilder<U> setLimitCount(final long count)
	throws IllegalArgumentException;

	/**
	 Limit the storage I/O methods execution by the time.
	 @param timeOut the maximum time to perform a I/O invocation, 0 means no limit
	 @param timeUnit the time unit.
	 @return self.
	 @throws java.lang.IllegalArgumentException if time out is negative or time unit is null
	 */
	StorageClientBuilder<U> setLimitTime(final long timeOut, final TimeUnit timeUnit)
	throws IllegalArgumentException;

	/**
	 Limit the storage I/O methods by the rate.
	 @param rate the maximum rate (data items/sec) which should be sustained, 0 means no limit
	 @return self.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	StorageClientBuilder<U> setLimitRate(final float rate)
	throws IllegalArgumentException;

	/** Build the storage client instance */
	U build();
}
