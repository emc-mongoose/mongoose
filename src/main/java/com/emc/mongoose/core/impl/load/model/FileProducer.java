package com.emc.mongoose.core.impl.load.model;
//mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//mongoose-core-api.jar
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Producer;
//mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.reader.RandomFileReader;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 12.05.14.
 A data item producer which constructs data items while reading the special input file.
 */
public class FileProducer<T extends DataItem>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static short
		MAX_COUNT_TO_ESTIMATE_SIZES = 100,
		MAX_WAIT_TO_ESTIMATE_MILLIS = 10000;
	//
	private final Path fPath;
	private final Constructor<T> dataItemConstructor;
	private final long maxCount;
	private long approxDataItemsSize = LoadExecutor.BUFF_SIZE_LO;
	//
	private Consumer<T> consumer = null;
	//
	@SuppressWarnings("unchecked")
	public FileProducer(final long maxCount, final String fPathStr, final Class<T> dataItemsImplCls)
	throws NoSuchMethodException, IOException {
		this(maxCount, fPathStr, dataItemsImplCls, false);
	}
	//
	private FileProducer(
		final long maxCount, final String fPathStr, final Class<T> dataItemsImplCls,
		final boolean nested
	) throws IOException, NoSuchMethodException {
		super(fPathStr);
		fPath = FileSystems.getDefault().getPath(fPathStr);
		if(!Files.exists(fPath)) {
			throw new IOException("File \""+fPathStr+"\" doesn't exist");
		}
		if(!Files.isReadable(fPath)) {
			throw new IOException("File \""+fPathStr+"\" is not readable");
		}
		dataItemConstructor = dataItemsImplCls.getConstructor(String.class);
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		//
		if(!nested) {
			// try to read 1st max 100 data items to determine the
			new FileProducer<T>(MAX_COUNT_TO_ESTIMATE_SIZES, fPathStr, dataItemsImplCls, true) {
				{
					setConsumer(
						new Consumer<T>() {
							//
							private long sizeSum = 0, count = 0;
							//
							@Override
							public final void submit(final T data) {
								sizeSum += data.getSize();
								count ++;
								approxDataItemsSize = sizeSum / count;
							}
							//
							@Override
							public final void shutdown() {
							}
							//
							@Override
							public final long getMaxCount() {
								return MAX_COUNT_TO_ESTIMATE_SIZES;
							}
							//
							@Override
							public final void close() {
							}
						}
					);
					//
					start();
					try {
						join(MAX_WAIT_TO_ESTIMATE_MILLIS);
					} catch(final InterruptedException e) {
						LogUtil.exception(LOG, Level.WARN, e, "Interrupted");
					} finally {
						interrupt();
					}
				}
			};
			//
			// TODO randomize the lines if necessary
		}
	}
	//
	public final String getPath() {
		return fPath.toString();
	}
	//
	public final long getApproxDataItemsSize() {
		return approxDataItemsSize;
	}
	//
	@Override
	public final void run() {
		long dataItemsCount = 0;
		int batchSize = 0;
		//
		final Charset charset =  StandardCharsets.UTF_8;
		final CharsetDecoder decoder = charset.newDecoder();
		//
		if(RunTimeConfig.getContext().isEnabledDataRandom()) {
			batchSize = RunTimeConfig.getContext().getDataRandomBatchSize();
		}
		try(
			BufferedReader fReader = new RandomFileReader(
				new InputStreamReader(Files.newInputStream(fPath), decoder),
				batchSize, maxCount
			)
		) {
			String nextLine;
			T nextData;
			LOG.debug(
				LogUtil.MSG, "Going to produce up to {} data items for consumer \"{}\"",
				consumer.getMaxCount(), consumer.toString()
			);
			do {
				//
				nextLine = fReader.readLine();
				LOG.trace(LogUtil.MSG, "Got next line #{}: \"{}\"", dataItemsCount, nextLine);
				//
				if(nextLine == null || nextLine.isEmpty()) {
					LOG.debug(LogUtil.MSG, "No next line, exiting");
					break;
				} else {
					nextData = dataItemConstructor.newInstance(nextLine);
					try {
						consumer.submit(nextData);
						dataItemsCount ++;
					} catch(final RejectedExecutionException e) {
						LogUtil.exception(LOG, Level.DEBUG, e, "Consumer rejected the data item");
					} catch(final Exception e) {
						LogUtil.exception(LOG, Level.WARN, e, "Failed to submit the data item");
					}
				}
			} while(!isInterrupted() && dataItemsCount < maxCount);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to read line from the file");
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
		} finally {
			LOG.debug(LogUtil.MSG, "Produced {} data items", dataItemsCount);
			try {
				LOG.debug(LogUtil.MSG, "Feeding poison to consumer \"{}\"", consumer.toString());
				consumer.shutdown();
			} catch(final Exception e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to shut down the consumer");
			}
			LOG.debug(LogUtil.MSG, "Exiting");
		}
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		LOG.debug(LogUtil.MSG, "Set consumer to \"{}\"", consumer.toString());
		this.consumer = consumer;
	}
	//
	@Override
	public final Consumer<T> getConsumer()
	throws RemoteException {
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
}
