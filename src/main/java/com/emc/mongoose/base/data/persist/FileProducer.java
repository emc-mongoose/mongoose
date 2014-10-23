package com.emc.mongoose.base.data.persist;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
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
	//
	private Consumer<T> consumer = null;
	//
	@SuppressWarnings("unchecked")
	public FileProducer(final String fPathStr, final Class<T> dataItemsImplCls)
	throws NoSuchMethodException, IOException {
		super(
			String.format(
				"producer<%s>-file<%s>", dataItemsImplCls.getName(), fPathStr
			)
		);
		//
		fPath = FileSystems.getDefault().getPath(fPathStr);
		if(!Files.exists(fPath)) {
			throw new IOException("File \""+fPathStr+"\" doesn't exist");
		}
		if(!Files.isReadable(fPath)) {
			throw new IOException("File \""+fPathStr+"\" is not readable");
		}
		dataItemConstructor = dataItemsImplCls.getConstructor(String.class);
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
				if(nextLine==null) {
					LOG.debug(Markers.MSG, "No next line, exiting");
					break;
				} else {
					nextData = dataItemConstructor.newInstance(nextLine);
					try {
						consumer.submit(nextData);
					} catch(final RemoteException e) {
						LOG.warn(Markers.ERR, "Failed to submit data item to remote consumer");
					}
					dataItemsCount ++;
				}
			} while(!isInterrupted());
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to read line from the file");
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Unexpected failure");
		} finally {
			LOG.debug(Markers.MSG, "Produced {} data items", dataItemsCount);
			try {
				LOG.debug(Markers.MSG, "Feeding poison to consumer \"{}\"", consumer.toString());
				consumer.submit(null); // or: consumer.setMaxCount(dataItemsCount);
			} catch(final RemoteException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to submit the poison to remote consumer");
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
