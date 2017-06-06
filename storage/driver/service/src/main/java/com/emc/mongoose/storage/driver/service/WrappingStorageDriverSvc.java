package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.common.concurrent.SvcTaskBase;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.DataItemFactory;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static java.lang.System.nanoTime;

/**
 Created by andrey on 05.10.16.
 */
public final class WrappingStorageDriverSvc<I extends Item, O extends IoTask<I>>
implements StorageDriverSvc<I, O> {
	
	private final int port;
	private final StorageDriver<I, O> driver;
	private final ContentSource contentSrc;
	private final SvcTask stateReportSvcTask;

	public WrappingStorageDriverSvc(
		final int port, final StorageDriver<I, O> driver, final ContentSource contentSrc,
		final long metricsPeriodSec, final String stepName
	) throws RemoteException {
		if(metricsPeriodSec > 0 && metricsPeriodSec < Long.MAX_VALUE) {
			stateReportSvcTask = new StateReportingTask(driver, metricsPeriodSec, stepName);
		} else {
			stateReportSvcTask = null;
		}
		this.port = port;
		this.driver = driver;
		this.contentSrc = contentSrc;
		Loggers.MSG.info("Service started: " + ServiceUtil.create(this, port));
	}
	
	private final static class StateReportingTask
	extends SvcTaskBase {

		private final StorageDriver driver;
		private final long metricsPeriodNanoSec;
		private final String stepName;
		
		private long prevNanoTimeStamp;
		private long nextNanoTimeStamp;
		
		public StateReportingTask(
			final StorageDriver driver, final long metricsPeriodSec, final String stepName
		) throws RemoteException {
			super(driver.getSvcTasks());
			this.driver = driver;
			this.metricsPeriodNanoSec = TimeUnit.SECONDS.toNanos(metricsPeriodSec);
			this.stepName = stepName;
			this.prevNanoTimeStamp = 0;
		}
		
		@Override
		protected final void invoke() {
			try(
				final Instance ctx = CloseableThreadContext
					.put(KEY_STEP_NAME, stepName)
					.put(KEY_CLASS_NAME, getClass().getSimpleName())
			) {
				nextNanoTimeStamp = nanoTime();
				if(metricsPeriodNanoSec < nextNanoTimeStamp - prevNanoTimeStamp) {
					prevNanoTimeStamp = nextNanoTimeStamp;
					try {
						Loggers.MSG.info(
							"{} I/O tasks: scheduled={}, active={}, completed={}",
							driver.toString(), driver.getScheduledTaskCount(),
							driver.getActiveTaskCount(), driver.getCompletedTaskCount()
						);
					} catch(final RemoteException ignored) {
					}
				}
			}
		}
		
		@Override
		protected final void doClose() {
			prevNanoTimeStamp = Long.MAX_VALUE;
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
	public final List<SvcTask> getSvcTasks() {
		throw new AssertionError("Shouldn't be invoked");
	}
	
	@Override
	public final State getState()
	throws RemoteException {
		return driver.getState();
	}
	
	@Override
	public final void start()
	throws IllegalStateException {
		try {
			driver.start();
			if(stateReportSvcTask != null) {
				driver.getSvcTasks().add(stateReportSvcTask); // start
			}
		} catch(final RemoteException e) {
			throw new AssertionError(e);
		}
	}
	
	@Override
	public final O get()
	throws IOException {
		return driver.get();
	}

	@Override
	public final List<O> getAll()
	throws IOException {
		return driver.getAll();
	}
	
	@Override
	public final long skip(final long count)
	throws IOException {
		return driver.skip(count);
	}
	
	@Override
	public final void close()
	throws IOException {
		driver.close();
		contentSrc.close();
		Loggers.MSG.info("Service closed: " + ServiceUtil.close(this));
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
		if(buffer.get(0) instanceof DataIoTask) {
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
	throws IllegalStateException {
		try {
			driver.interrupt();
			if(stateReportSvcTask != null) {
				stateReportSvcTask.close();
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Storage driver wrapping service failed on interrupt");
		}
	}

	@Override
	public final boolean isInterrupted() {
		try {
			return driver.isInterrupted();
		} catch(final RemoteException e) {
			throw new AssertionError(e);
		}
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
	public final int getConcurrencyLevel()
	throws RemoteException {
		return driver.getConcurrencyLevel();
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
