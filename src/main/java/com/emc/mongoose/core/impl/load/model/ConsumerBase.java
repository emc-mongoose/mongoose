package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
//
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
//
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
/**
 Created by kurila on 26.05.15.
 */
public abstract class ConsumerBase<T extends DataItem>
extends Thread
implements Consumer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static boolean COMPRESSION_ENABLED = false;
	// configuration params
	protected final RunTimeConfig runTimeConfig;
	private final long maxCount;
	protected final int submTimeOutMilliSec, maxQueueSize;
	// states
	private final AtomicLong
		counterVolatile = new AtomicLong(0),
		counterPersisted = new AtomicLong(0);
	protected final AtomicBoolean
		isStarted = new AtomicBoolean(false),
		isShutdown = new AtomicBoolean(false),
		isAllSubm = new AtomicBoolean(false);
	// volatile
	private final BlockingQueue<T> volatileQueue;
	// persistent
	private final File persistentQueueFile;
	private final BufferedWriter persistentQueueOutput;
	private volatile FileProducer<T> persistentQueueWorker = null;
	protected final Class<T> dataCls;
	//
	protected ConsumerBase(
		final Class<T> dataCls, final RunTimeConfig runTimeConfig, final long maxCount
	) throws IllegalStateException {
		this.dataCls = dataCls;
		this.runTimeConfig = runTimeConfig;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		maxQueueSize = (int) Math.min(
			this.maxCount, runTimeConfig.getRunRequestQueueSize()
		);
		volatileQueue = new ArrayBlockingQueue<>(maxQueueSize);
		submTimeOutMilliSec = runTimeConfig.getRunSubmitTimeOutMilliSec();
		//
		final Path tmpFilePath = Paths.get(
			System.getProperty("java.io.tmpdir"),
			runTimeConfig.getRunName() + "-v" + runTimeConfig.getRunVersion()
		);
		if(!tmpFilePath.toFile().exists() && !tmpFilePath.toFile().mkdirs()) {
			LOG.warn(LogUtil.ERR, "Failed to create the directory: \"{}\"", tmpFilePath);
		}
		try {
			persistentQueueFile = Files.createTempFile(
				tmpFilePath, runTimeConfig.getRunId(), COMPRESSION_ENABLED ? ".gz" : null
			).toFile();
			persistentQueueFile.deleteOnExit();
		} catch(final IOException e) {
			throw new IllegalStateException(
				"Failed to create the temporary file in " + tmpFilePath.toAbsolutePath(), e
			);
		}
		try {
			persistentQueueOutput = new BufferedWriter(
				new OutputStreamWriter(
					COMPRESSION_ENABLED ?
						new GZIPOutputStream(Files.newOutputStream(persistentQueueFile.toPath()))
						: Files.newOutputStream(persistentQueueFile.toPath())
				)
			);
		} catch(final IOException e) {
			throw new IllegalStateException(
				"Failed to open the temporary file in " + tmpFilePath.toAbsolutePath(), e
			);
		}
	}
	//
	@Override
	public void start() {
		if(isStarted.compareAndSet(false, true)) {
			super.start();
			LOG.debug(
				LogUtil.MSG, "{}: started, the further consuming will go through volatile queue",
				getName()
			);
			if(counterPersisted.get() > 0) {
				LOG.debug(
					LogUtil.MSG, "{}: there's a {} data items compressed data in the file \"{}\"",
					getName(), SizeUtil.formatSize(persistentQueueFile.length()),
					persistentQueueFile.getAbsolutePath()
				);
				try {
					persistentQueueWorker = new FileProducer<>(
						maxCount, persistentQueueFile.getAbsolutePath(), dataCls,
						/*nested=*/true, COMPRESSION_ENABLED
					);
					persistentQueueWorker.setDaemon(true); // do not block process exit
					persistentQueueWorker.setConsumer(this); // go through the volatile queue
					persistentQueueWorker.start();
				} catch(final IOException | NoSuchMethodException e) {
					LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to consume the persistent queue from \"{}\"",
						persistentQueueFile.getAbsolutePath()
					);
				}
			}
		}
	}
	/**
	 May block the executing thread until the queue becomes able to ingest more
	 @param dataItem
	 @throws RemoteException
	 @throws InterruptedException
	 @throws RejectedExecutionException
	 */
	@Override
	public void submit(final T dataItem)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		//
		if(dataItem == null || counterVolatile.get() >= maxCount) {
			shutdown();
		}
		if(isShutdown.get()) {
			throw new InterruptedException("Shut down already");
		}
		//
		if(isStarted.get()) {
			if(volatileQueue.offer(dataItem, submTimeOutMilliSec, TimeUnit.MILLISECONDS)) {
				counterVolatile.incrementAndGet();
			} else {
				throw new RejectedExecutionException("Submit queue timeout");
			}
		} else {
			try {
				if(counterPersisted.get() < maxCount) {
					synchronized(persistentQueueOutput) {
						persistentQueueOutput.write(dataItem.toString());
						persistentQueueOutput.newLine();
					}
					counterPersisted.incrementAndGet();
				} else {
					throw new InterruptedException("Max count reached: " + maxCount);
				}
			} catch(final IOException e) {
				throw new RejectedExecutionException(e);
			}
		}
	}
	/** Consumes the queue */
	@Override
	public final void run() {
		LOG.debug(
			LogUtil.MSG, "Determined submit queue capacity of {} for \"{}\"",
			volatileQueue.remainingCapacity(), getName()
		);
		T nextDataItem;
		try {
			while(volatileQueue.size() > 0 || !isShutdown.get()) {
				nextDataItem = volatileQueue.poll(submTimeOutMilliSec, TimeUnit.MILLISECONDS);
				if(nextDataItem != null) {
					submitSync(nextDataItem);
				}
			}
			LOG.debug(LogUtil.MSG, "{}: consuming finished", getName());
		} catch(final InterruptedException e) {
			LOG.debug(LogUtil.MSG, "{}: consuming interrupted", getName());
		} catch(final RejectedExecutionException e) {
			LOG.debug(LogUtil.MSG, "{}: consuming rejected", getName());
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Submit data item failure");
		} finally {
			shutdown();
			isAllSubm.set(true);
		}
	}
	//
	protected abstract void submitSync(final T dataItem)
	throws InterruptedException, RemoteException;
	//
	@Override
	public void shutdown() {
		if(!isStarted.get()) {
			LOG.debug(
				LogUtil.MSG, "{}: there are {} data items persisted in \"{}\"",
				getName(), counterPersisted.get(), persistentQueueFile
			);
			try {
				persistentQueueOutput.close();
				LOG.debug(
					LogUtil.MSG, "{}: closed the file \"{}\" for writing",
					getName(), persistentQueueFile.getAbsolutePath()
				);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to close the persistent queue file: \"{}\"",
					persistentQueueFile.getAbsolutePath()
				);
			}
		} else if(isShutdown.compareAndSet(false, true)) {
			LOG.debug(LogUtil.MSG, "{}: consumed {} data items", getName(), counterVolatile.get());
		}
	}
	//
	@Override
	public long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public synchronized void interrupt() {
		shutdown();
		if(
			persistentQueueWorker != null &&
			(persistentQueueWorker.isAlive() || !persistentQueueWorker.isInterrupted() )
		) {
			persistentQueueWorker.interrupt();
			persistentQueueWorker = null;
			LOG.debug(
				LogUtil.MSG, "{}: interrupted persistent queue producer for the file \"{}\"",
				getName(), persistentQueueFile.getAbsolutePath()
			);
		}
		if(!super.isInterrupted()) {
			super.interrupt();
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		shutdown();
		if(!persistentQueueFile.delete()) {
			LOG.debug(LogUtil.ERR, "Failed to delete the file \"{}\"", persistentQueueFile);
		}
		final int dropCount = volatileQueue.size();
		if(dropCount > 0) {
			LOG.debug(LogUtil.MSG, "Dropped {} submit tasks", dropCount);
		}
		volatileQueue.clear(); // dispose
	}
}
