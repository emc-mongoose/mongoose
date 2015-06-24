package com.emc.mongoose.util.scenario;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.util.DataItemInput;
import com.emc.mongoose.core.api.data.util.DataItemOutput;
//
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.util.BinFileItemOutput;
//
import com.emc.mongoose.core.impl.data.util.TxtFileItemOutput;
import com.emc.mongoose.storage.mock.impl.Cinderella;
//
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 22.06.15.
 */
public class Sanity
implements Runnable {
	//
	public final static Logger LOG;
	static {
		LogUtil.init();
		LOG = LogManager.getLogger();
	}
	//
	private final StorageClient<WSObject> client;
	//
	public Sanity(final StorageClient<WSObject> client) {
		this.client = client;
	}
	//
	public void run() {
		try {
			final DataItemOutput<WSObject> dataDstW = new BinFileItemOutput<>(
				Files.createTempFile(null, null)
			);
			LOG.info(Markers.MSG, "Start writing");
			final long nWritten = client.write(
				null, dataDstW, (short) 100, SizeUtil.toSize("16MB")
			);
			LOG.info(Markers.MSG, "Written successfully {} items", nWritten);
			//
			final DataItemInput<WSObject> dataSrcR = dataDstW.getInput();
			final DataItemOutput<WSObject> dataDstR = new BinFileItemOutput<>(
				Files.createTempFile(null, null)
			);
			final long nRead = client.read(dataSrcR, dataDstR, (short) 100);
			LOG.info(Markers.MSG, "Read successfully {} items", nRead);
			//
			final DataItemInput<WSObject> dataSrcD = dataDstR.getInput();
			final long nDeleted = client.delete(dataSrcD, null, (short) 100);
			LOG.info(Markers.MSG, "Deleted successfully {} items", nDeleted);
			//
			final DataItemInput<WSObject> dataSrcRW = dataDstR.getInput();
			final DataItemOutput<WSObject> dataDstRW = new BinFileItemOutput<>(
				Files.createTempFile(null, null)
			);
			LOG.info(Markers.MSG, "Start rewriting");
			final long nReWritten = client.write(dataSrcRW, dataDstRW, (short) 100);
			LOG.info(Markers.MSG, "Rewritten successfully {} items", nReWritten);
			//
		} catch(final Exception e) {
			e.printStackTrace(System.err);
			LogUtil.exception(LOG, Level.ERROR, e, "Sanity failure");
		}
	}
	//
	public static void main(final String... args)
	throws IOException, InterruptedException {
		//
		RunTimeConfig.initContext();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		//
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_CAPACITY, 1000000);
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_HEAD_COUNT, 10);
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_IO_THREADS_PER_SOCKET, 10);
		final Thread wsMockThread = new Thread(
			new Cinderella<>(RunTimeConfig.getContext()), "wsMock"
		);
		wsMockThread.start();
		LOG.info(Markers.MSG, "Storage mock started");
		//
		final StorageClientBuilder<WSObject, StorageClient<WSObject>>
			clientBuilder = new BasicWSClientBuilder<>();
		final String storageNodes[] = new String[] {
			"127.0.0.1:9020", "127.0.0.1:9021", "127.0.0.1:9022", "127.0.0.1:9023",
			"127.0.0.1:9024", "127.0.0.1:9025", "127.0.0.1:9026", "127.0.0.1:9027",
			"127.0.0.1:9028", "127.0.0.1:9029"
		};
		// standalone
		final StorageClient<WSObject> standaloneClient = clientBuilder
			.setNodes(storageNodes)
			.setLimitCount(1000000)
			.setLimitTime(100, TimeUnit.SECONDS)
			.setLimitRate(10000)
			.build();
		final Thread sanityThread = new Thread(new Sanity(standaloneClient), "sanityStandalone");
		TimeUnit.SECONDS.sleep(1);
		sanityThread.start();
		LOG.info(Markers.MSG, "Standalone sanity started");
		TimeUnit.SECONDS.sleep(1);
		sanityThread.join();
		LOG.info(Markers.MSG, "Standalone sanity finished");
		TimeUnit.SECONDS.sleep(1);
		// distributed mode
		final StorageClient<WSObject> distribuedClient = clientBuilder
			.setClientMode(new String[] { "127.0.0.1" })
			.build();

		// finish
		wsMockThread.interrupt();
		LOG.info(Markers.MSG, "Sanity done");
	}
}
