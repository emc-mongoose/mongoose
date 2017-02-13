package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import static java.lang.System.nanoTime;

import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.DataItemFactory;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 05.10.16.
 */
public final class WrappingStorageDriverSvc<
	I extends Item, O extends IoTask<I, R>, R extends IoResult<I>
>
implements StorageDriverSvc<I, O, R> {

	private static final Logger LOG = LogManager.getLogger();

	private final int port;
	private final StorageDriver<I, O, R> driver;
	private final ContentSource contentSrc;

	public WrappingStorageDriverSvc(
		final int port, final StorageDriver<I, O, R> driver, final ContentSource contentSrc,
		final long metricsPeriodSec
	) {
		this.port = port;
		this.driver = driver;
		this.contentSrc = contentSrc;
		LOG.info(Markers.MSG, "Service started: " + ServiceUtil.create(this, port));
		if(metricsPeriodSec > 0 && metricsPeriodSec < Long.MAX_VALUE) {
			SVC_TASKS.put(this, new StateReportingTask(this, metricsPeriodSec));
		}
	}
	
	private final static class StateReportingTask
	implements Runnable {
		
		private final StorageDriverSvc storageDriver;
		private final long metricsPeriodNanoSec;
		
		private long prevNanoTimeStamp;
		private long nextNanoTimeStamp;
		
		public StateReportingTask(
			final StorageDriverSvc storageDriver, final long metricsPeriodSec
		) {
			this.storageDriver = storageDriver;
			this.metricsPeriodNanoSec = TimeUnit.SECONDS.toNanos(metricsPeriodSec);
			this.prevNanoTimeStamp = 0;
		}
		
		@Override
		public final void run() {
			nextNanoTimeStamp = nanoTime();
			if(metricsPeriodNanoSec < nextNanoTimeStamp - prevNanoTimeStamp) {
				prevNanoTimeStamp = nextNanoTimeStamp;
				try {
					LOG.info(
						Markers.MSG,
						"{} I/O tasks: scheduled={}, active={}, completed={}, recycled={}",
						storageDriver.getName(), storageDriver.getScheduledTaskCount(),
						storageDriver.getActiveTaskCount(), storageDriver.getCompletedTaskCount(),
						storageDriver.getRecycledTaskCount()
					);
				} catch(final RemoteException ignored) {
				}
			}
		}
	}

	@Override
	public final int getRegistryPort()
	throws RemoteException {
		return port;
	}

	@Override
	public final String getName()
	throws RemoteException {
		return driver.toString();
	}

	@Override
	public final void start()
	throws IllegalStateException, RemoteException {
		driver.start();
	}

	@Override
	public final List<R> getResults()
	throws IOException {
		return driver.getResults();
	}

	@Override
	public final void close()
	throws IOException {
		SVC_TASKS.remove(this);
		driver.close();
		contentSrc.close();
		LOG.info(Markers.MSG, "Service closed: " + ServiceUtil.close(this));
	}

	@Override
	public final boolean put(final O ioTask)
	throws IOException {
		if(ioTask instanceof DataIoTask) {
			((DataItem) ioTask.getItem()).setContentSrc(contentSrc);
		}
		return driver.put(ioTask);
	}

	@Override
	public final int put(final List<O> buffer, final int from, final int to)
	throws IOException {
		if(buffer.get(from) instanceof DataIoTask) {
			for(int i = from; i < to; i ++) {
				((DataItem) buffer.get(i).getItem()).setContentSrc(contentSrc);
			}
		}
		return driver.put(buffer, from, to);
	}

	@Override
	public final int put(final List<O> buffer)
	throws IOException {
		if(buffer.get(0) instanceof DataItem) {
			for(final O ioTask : buffer) {
				((DataItem) ioTask.getItem()).setContentSrc(contentSrc);
			}
		}
		return driver.put(buffer);
	}

	// just wrapping methods below

	@Override
	public final boolean isStarted()
	throws RemoteException {
		return driver.isStarted();
	}

	@Override
	public final void shutdown()
	throws IllegalStateException, RemoteException {
		driver.shutdown();
	}

	@Override
	public final boolean isShutdown()
	throws RemoteException {
		return driver.isShutdown();
	}

	@Override
	public final void await()
	throws InterruptedException, RemoteException {
		driver.await();
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		return driver.await(timeout, timeUnit);
	}

	@Override
	public final void interrupt()
	throws IllegalStateException, RemoteException {
		driver.interrupt();
	}

	@Override
	public final boolean isInterrupted()
	throws RemoteException {
		return driver.isInterrupted();
	}

	@Override
	public final boolean isClosed()
	throws RemoteException {
		return driver.isClosed();
	}

	@Override
	public final Input<O> getInput()
	throws IOException {
		throw new RemoteException();
	}
	
	@Override
	public final boolean createPath(final String path)
	throws RemoteException {
		return driver.createPath(path);
	}

	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {
		if(itemFactory instanceof DataItemFactory) {
			((DataItemFactory) itemFactory).setContentSource(contentSrc);
		}
		return driver.list(itemFactory, path, prefix, idRadix, lastPrevItem, count);
	}

	@Override
	public final String getAuthToken()
	throws RemoteException {
		return driver.getAuthToken();
	}

	@Override
	public final void setAuthToken(final String authToken)
	throws RemoteException {
		driver.setAuthToken(authToken);
	}

	@Override
	public final int getActiveTaskCount()
	throws RemoteException {
		return driver.getActiveTaskCount();
	}
	
	@Override
	public final long getScheduledTaskCount()
	throws RemoteException {
		return driver.getScheduledTaskCount();
	}
	
	@Override
	public final long getCompletedTaskCount()
	throws RemoteException {
		return driver.getCompletedTaskCount();
	}
	@Override
	public final long getRecycledTaskCount()
	throws RemoteException {
		return driver.getRecycledTaskCount();
	}
	@Override
	public final boolean isIdle()
	throws RemoteException {
		return driver.isIdle();
	}

	@Override
	public final void adjustIoBuffers(final SizeInBytes avgDataItemSize, final IoType ioType)
	throws RemoteException {
		driver.adjustIoBuffers(avgDataItemSize, ioType);
	}
}
