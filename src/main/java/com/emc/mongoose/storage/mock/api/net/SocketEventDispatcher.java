package com.emc.mongoose.storage.mock.api.net;
import java.io.Closeable;
/**
 Created by andrey on 26.06.15.
 */
public interface SocketEventDispatcher
extends Runnable, Closeable {

	void start();

	void join()
	throws InterruptedException;

}
