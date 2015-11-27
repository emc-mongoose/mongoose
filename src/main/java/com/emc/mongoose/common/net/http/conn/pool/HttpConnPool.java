package com.emc.mongoose.common.net.http.conn.pool;
//
import org.apache.http.pool.ConnPool;
import org.apache.http.pool.ConnPoolControl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 19.11.15.
 */
public interface HttpConnPool<T, E>
extends ConnPool<T, E>, ConnPoolControl<T> {
	//
	void closeExpired();
	//
	void closeIdle(final long idletime, final TimeUnit tunit);
	//
	boolean isShutdown();
	//
	void shutdown(final long waitMs)
	throws IOException;
}
