package com.emc.mongoose.util.client.api;
//
import com.emc.mongoose.core.api.Item;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 17.06.15.
 */
public interface StorageClientBuilder<T extends Item, U extends StorageClient<T>> {
	/**
	 Set the storage API to use.
	 @param api The value should match to any child package name from the
	 "com.emc.mongoose.storage.web.adapter" package ("atmos", "s3", "swift", etc).
	 @return self.
	 @throws IllegalArgumentException if there's no package having the specified name.
	 */
	StorageClientBuilder<T, U> setAPI(final String api)
	throws IllegalArgumentException;

	/**
	 Set the storage node address list.
	 @param nodeAddrs Storage nodes list. FQDNs and IPs are acceptable.
	 Individual ports may be specified also, for example:
	 new String[] { "10.123.45.67:9020", "10.123.45.68:9021", "10.123.45.69:9022" }
	 @return self.
	 @throws IllegalArgumentException if null or empty
	 */
	StorageClientBuilder<T, U> setNodes(final String nodeAddrs[])
	throws IllegalArgumentException;

	/**
	 Change the run mode to load client mode, set the list of the remote load servers.
	 @param loadServers Load servers list. FQDNs and IPs are acceptable.
	 Null/empty value just switches (back) to the standalone mode.
	 @return self.
	 */
	StorageClientBuilder<T, U> setClientMode(final String loadServers[]);

	/**
	 Set credentials necessary to access the storage.
	 @param id user id, for example "wuser1@sanity.local"
	 @param secret the secret
	 @return self.
	 */
	StorageClientBuilder<T, U> setAuth(final String id, final String secret);

	/**
	 Set the target namespace for data used in some storage APIs (S3, Swift, etc).
	 @param value the namespace value, for example "s3"
	 @return self
	 */
	StorageClientBuilder<T, U> setNamespace(final String value);

	/**
	 Set the target S3 bucket for writing to/reading from/etc.
	 @param value The name of the bucket, for example "sanity-nh-bucket1"
	 @return self.
	 */
	StorageClientBuilder<T, U> setS3Bucket(final String value);

	/**
	 Set the target Swift container for writing to/reading from/etc.
	 @param value The name of the container, for example "sanity-nh-container1"
	 @return self.
	 */
	StorageClientBuilder<T, U> setSwiftContainer(final String value);

	/**
	 Set the Atmos-specific subtenant to use.
	 @param value The subtenant string value.
	 @return self.
	 */
	StorageClientBuilder<T, U> setAtmosSubtenant(final String value);

	/**
	 Set the Swift-specific authentication token to use.
	 @param value The authentication token string value
	 @return self.
	 */
	StorageClientBuilder<T, U> setSwiftAuthToken(final String value);

	/**
	 Limit the storage I/O methods execution by data items count.
	 @param count the max count of the data items to write/read/delete/etc,
	 0 means no limit (infinite)
	 @return self.
	 @throws java.lang.IllegalArgumentException if less than 0
	 */
	StorageClientBuilder<T, U> setLimitCount(final long count)
	throws IllegalArgumentException;

	/**
	 Limit the storage I/O methods execution by the time.
	 @param timeOut the maximum time to perform a I/O invocation,
	 0 time value or {0, null} arg values pair both mean no limit (infinite)
	 @param timeUnit the time unit.
	 @return self.
	 @throws java.lang.IllegalArgumentException if time out is negative or time unit is null
	 */
	StorageClientBuilder<T, U> setLimitTime(final long timeOut, final TimeUnit timeUnit)
	throws IllegalArgumentException;

	/**
	 Limit the storage I/O methods by the rate.
	 @param rate the maximum rate (data items/sec) which should be sustained, 0 means no limit
	 @return self.
	 @throws java.lang.IllegalArgumentException if negative value is passed
	 */
	StorageClientBuilder<T, U> setLimitRate(final float rate)
	throws IllegalArgumentException;

	/**
	 Set the load subject class
	 @param itemCls may one of the predefined values ("data", "container", ...)
	 @return self.
	 @throws IllegalArgumentException if the argument value is not one of the predefined values
	 */
	StorageClientBuilder<T, U> setItemClass(final String itemCls)
	throws IllegalArgumentException;

	/**
	 Enable/disable the versioning
	 @param enabledFlag enable the versioning if true
	 @return self.
	 */
	StorageClientBuilder<T, U> setVersioning(final boolean enabledFlag)
	throws IllegalArgumentException;

	/**
	 Set the file access mode.
	 @param enabledFlag enable/disable file access mode flag
	 @return self.
	 */
	StorageClientBuilder<T, U> setFileAccess(final boolean enabledFlag)
	throws IllegalArgumentException;

	/**
	 Set the storage directory for writing data items to/reading data items from it
	 @param path the storage side path, shouldn't begin or end with "/"
	 @return self.
	 @throws IllegalArgumentException if the path begins or ends with "/"
	 */
	StorageClientBuilder<T, U> setPath(final String path)
	throws IllegalArgumentException;

	/**
	 Set the manual delay between each two requests to the storage inside single thread/connection.
	 @param milliSec the time in milliseconds
	 @throws java.lang.IllegalArgumentException if negative value is passed
 	 */
	StorageClientBuilder<T, U> setReqThinkTime(final int milliSec)
	throws IllegalArgumentException;

	/** Build the storage client instance */
	U build();
}
