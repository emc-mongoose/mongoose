package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.impl.persist.TraceLogger;
import com.emc.mongoose.core.api.persist.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
/**
 Created by kurila on 12.05.14.
 A data item producer which constructs data items while reading the special input file.
 */
public class FileProducer<T extends DataItem>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Path fPath;
	private final Constructor<T> dataItemConstructor;
	private final long maxCount;
	//
	private Consumer<T> consumer = null;
	//
	@SuppressWarnings("unchecked")
	public FileProducer(final long maxCount, final String fPathStr, final Class<T> dataItemsImplCls)
	throws NoSuchMethodException, IOException {
		super(fPathStr);
		//
		fPath = FileSystems.getDefault().getPath(fPathStr);
		if(!Files.exists(fPath)) {
			throw new IOException("File \""+fPathStr+"\" doesn't exist");
		}
		if(!Files.isReadable(fPath)) {
			throw new IOException("File \""+fPathStr+"\" is not readable");
		}
		dataItemConstructor = dataItemsImplCls.getConstructor(String.class);
		//
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
	}
	//
	public final String getPath() {
		return fPath.toString();
	}
	//
	@Override
	public final void run() {
		long dataItemsCount = 0;
		try(BufferedReader fReader = Files.newBufferedReader(fPath, StandardCharsets.UTF_8)) {
			String nextLine;
			T nextData;
			LOG.debug(
				Markers.MSG, "Going to produce up to {} data items for consumer \"{}\"",
				consumer.getMaxCount(), consumer.toString()
			);
			do {
				//
				nextLine = fReader.readLine();
				LOG.trace(Markers.MSG, "Got next line #{}: \"{}\"", dataItemsCount, nextLine);
				//
				if(nextLine==null || nextLine.isEmpty()) {
					LOG.debug(Markers.MSG, "No next line, exiting");
					break;
				} else {
					nextData = dataItemConstructor.newInstance(nextLine);
					try {
						consumer.submit(nextData);
						dataItemsCount ++;
					} catch(final Exception e) {
						TraceLogger.failure(LOG, Level.WARN, e, "Failed to submit data item");
					}
				}
			} while(!isInterrupted() && dataItemsCount < maxCount);
		} catch(final IOException e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "Failed to read line from the file");
		} catch(final Exception e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "Unexpected failure");
		} finally {
			LOG.debug(Markers.MSG, "Produced {} data items", dataItemsCount);
			try {
				LOG.debug(Markers.MSG, "Feeding poison to consumer \"{}\"", consumer.toString());
				consumer.submit(null); // or: consumer.setMaxCount(dataItemsCount);
			} catch(final InterruptedException e) {
				TraceLogger.trace(LOG, Level.DEBUG, Markers.MSG, "Consumer is already shutdown");
			} catch(final Exception e) {
				TraceLogger.failure(LOG, Level.WARN, e, "Failed to submit the poison to remote consumer");
			}
			LOG.debug(Markers.MSG, "Exiting");
		}
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		LOG.debug(Markers.MSG, "Set consumer to \"{}\"", consumer.toString());
		this.consumer = consumer;
	}
	//
	@Override
	public final Consumer<T> getConsumer()
	throws RemoteException {
		return consumer;
	}
	//
}
