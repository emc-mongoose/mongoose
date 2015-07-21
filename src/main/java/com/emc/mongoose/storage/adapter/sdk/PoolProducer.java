package com.emc.mongoose.storage.adapter.sdk;
//

import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
//import com.filepool.fplibrary.FPLibraryException;
//import com.filepool.fplibrary.FPPool;
import com.filepool.fplibrary.FPLibraryException;
import com.filepool.fplibrary.FPPool;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

//
//
//

/**
 Created by kurila on 08.10.14.
 */
public final class PoolProducer<T extends DataObject>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile Consumer<T> consumer = null;
	private final PoolImpl<T> pool;
	private final Constructor<T> dataConstructor;
	private final long maxCount;
	private final String addr;
	//
	@SuppressWarnings("unchecked")
	public PoolProducer(
			final PoolImpl<T> pool, final Class<? extends WSObject> dataCls, final long maxCount,
			final String addr
	) throws ClassCastException, NoSuchMethodException {
		super("pool-" + pool + "-producer");
		this.pool = pool;
		this.dataConstructor = (Constructor<T>) dataCls.getConstructor(
			String.class, Long.class, Long.class
		);
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.addr = addr;
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		this.consumer = consumer;
	}
	//
	@Override
	public final Consumer<T> getConsumer() {
		return consumer;
	}
	//
	@Override
	public final void await()
	throws InterruptedException {
		join();
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		timeUnit.timedJoin(this, timeOut);
	}
	//
	@Override
	public final void run() {
		//
//pool code
		try {

			FPPool pool = new FPPool(addr);
			System.out.println("Pool open @ "+addr);

		}catch (FPLibraryException e){

		}catch (IllegalArgumentException e){

		}

	}
	//
}
