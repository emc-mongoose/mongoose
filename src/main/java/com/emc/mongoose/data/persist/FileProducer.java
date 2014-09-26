package com.emc.mongoose.data.persist;
//
import com.emc.mongoose.Consumer;
import com.emc.mongoose.Producer;
import com.emc.mongoose.data.UniformData;
import com.emc.mongoose.logging.Markers;
//
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
 */
public final class FileProducer<T extends UniformData>
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
	public FileProducer(final String fPathStr, final Class<T> cls)
	throws NoSuchMethodException, IOException {
		super(FileProducer.class.getSimpleName());
		//
		fPath = FileSystems.getDefault().getPath(fPathStr);
		if(!Files.exists(fPath)) {
			throw new IOException("File \""+fPathStr+"\" doesn't exist");
		}
		if(!Files.isReadable(fPath)) {
			throw new IOException("File \""+fPathStr+"\" is not readable");
		}
		//
		dataItemConstructor = cls.getConstructor(String.class);
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
			} while(dataItemsCount < consumer.getMaxCount());
		} catch(final IOException e) {
			LOG.error(Markers.ERR, "Failed to read line from the file", e);
		} catch(final Exception e) {
			LOG.warn(Markers.ERR, "Unexpected exception: ", e);
		} finally {
			LOG.debug(Markers.MSG, "Produced {} data items", dataItemsCount);
			try {
				LOG.debug(Markers.MSG, "Feeding poison to consumer \"{}\"", consumer.toString());
				consumer.submit(null); // or: consumer.setMaxCount(dataItemsCount);
			} catch(final RemoteException e) {
				LOG.debug(Markers.ERR, "Failed to submit poison to consumer due to {}", e.toString());
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
