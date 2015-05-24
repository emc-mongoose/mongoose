package org.apache.http.nio.pool;
//
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.pool.BasicNIOPoolEntry;
import org.apache.http.pool.ConnPool;
import org.apache.http.pool.ConnPoolControl;
//
import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 23.05.15.
 */ //
public interface AdvancedConnPool
extends ConnPool<HttpHost, BasicNIOPoolEntry>, ConnPoolControl<HttpHost> {
	//
	void closeExpired();
	//
	void closeIdle(final long idletime, final TimeUnit tunit);
	//
	void shutdown(final long waitMilliSec)
		throws IOException;
	//
	boolean isShutdown();
	//
	HttpHost getMostFreeRoute();
}
