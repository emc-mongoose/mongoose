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
import com.emc.mongoose.core.impl.data.util.CSVFileItemOutput;
//
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
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
	private final static short DEFAULT_NODE_COUNT = 5, DEFAULT_CONN_PER_NODE = 5;
	private final static long
		DEFAULT_DATA_SIZE = SizeUtil.toSize("768KB"), DEFAULT_DATA_COUNT_MAX = 1500000;
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
				null, dataDstW, DEFAULT_CONN_PER_NODE, DEFAULT_DATA_SIZE
			);
			LOG.info(Markers.MSG, "Written successfully {} items", nWritten);
			//
			final DataItemInput<WSObject> dataSrcU = dataDstW.getInput();
			final DataItemOutput dataDstU = new CSVFileItemOutput<>(
				Files.createTempFile(null, null), BasicWSObject.class
			);
			LOG.info(Markers.MSG, "Start updating");
			final long nUpdated = client.update(dataSrcU, dataDstU, DEFAULT_CONN_PER_NODE);
			LOG.info(Markers.MSG, "Updated successfully {} items", nUpdated);
			//
			final DataItemInput<WSObject> dataSrcR = dataDstU.getInput();
			final DataItemOutput<WSObject> dataDstR = new BinFileItemOutput<>(
				Files.createTempFile(null, null)
			);
			final long nRead = client.read(dataSrcR, dataDstR, DEFAULT_CONN_PER_NODE, false);
			LOG.info(Markers.MSG, "Read successfully {} items", nRead);
			//
			final DataItemInput<WSObject> dataSrcD = dataDstR.getInput();
			final long nDeleted = client.delete(dataSrcD, null, DEFAULT_CONN_PER_NODE);
			LOG.info(Markers.MSG, "Deleted successfully {} items", nDeleted);
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
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_CAPACITY, DEFAULT_DATA_COUNT_MAX);
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_HEAD_COUNT, DEFAULT_NODE_COUNT);
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_IO_THREADS_PER_SOCKET, DEFAULT_CONN_PER_NODE);
		rtConfig.set(RunTimeConfig.KEY_REMOTE_PORT_EXPORT, 1299);
		final Thread wsMockThread = new Thread(
			new Cinderella<>(RunTimeConfig.getContext()), "wsMock"
		);
		wsMockThread.setDaemon(true);
		wsMockThread.start();
		TimeUnit.SECONDS.sleep(1);
		//
		final StorageClientBuilder<WSObject, StorageClient<WSObject>>
			clientBuilder = new BasicWSClientBuilder<>();
		final String storageNodes[] = new String[DEFAULT_NODE_COUNT];
		for(int i = 0; i < DEFAULT_NODE_COUNT; i ++) {
			storageNodes[i] = "127.0.0.1:" + (9020 + i);
		}
		clientBuilder
			.setNodes(storageNodes)
			.setLimitCount(DEFAULT_DATA_COUNT_MAX)
			.setLimitTime(50, TimeUnit.SECONDS)
			.setLimitRate(15000);
		// standalone
		final Thread sanityThread1 = new Thread(
			new Sanity(clientBuilder.build()), "sanityStandalone"
		);
		sanityThread1.start();
		LOG.info(Markers.MSG, "Standalone sanity started");
		TimeUnit.SECONDS.sleep(1);
		sanityThread1.join();
		LOG.info(Markers.MSG, "Standalone sanity finished");
		TimeUnit.SECONDS.sleep(1);
		// distributed mode
		rtConfig.set(RunTimeConfig.KEY_REMOTE_PORT_EXPORT, 1399);
		final LoadBuilderSvc<WSObject, WSLoadSvc<WSObject>>
			loadSvcBuilder = new BasicWSLoadBuilderSvc<>(rtConfig);
		loadSvcBuilder.start();
		TimeUnit.SECONDS.sleep(1);
		rtConfig.set(RunTimeConfig.KEY_REMOTE_PORT_EXPORT, 1199);
		final StorageClient<WSObject> distributedClient = clientBuilder
			.setClientMode(new String[] {"127.0.0.1"})
			.build();
		final Thread sanityThread2 = new Thread(new Sanity(distributedClient), "sanityDistributed");
		sanityThread2.start();
		LOG.info(Markers.MSG, "Distributed sanity started");
		TimeUnit.SECONDS.sleep(1);
		sanityThread2.join();
		LOG.info(Markers.MSG, "Distributed sanity finished");
		TimeUnit.SECONDS.sleep(1);
		loadSvcBuilder.close();
		LOG.info(Markers.MSG, "Load service builder stopped");
		// finish
		wsMockThread.interrupt();
		LOG.info(Markers.MSG, "Storage mock stopped");
		LOG.info(Markers.MSG, "Sanity done");
	}
}
