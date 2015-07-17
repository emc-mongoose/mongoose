package com.emc.mongoose.util.scenario;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.data.model.DataItemOutput;
//
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.model.BinFileItemOutput;
import com.emc.mongoose.core.impl.data.model.CSVFileItemOutput;
import com.emc.mongoose.core.impl.data.model.CircularListItemOutput;
//
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
//
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
//
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
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
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 22.06.15.
 */
public class Sanity
implements Runnable {
	//
	private final static short DEFAULT_NODE_COUNT = 5, DEFAULT_CONN_PER_NODE = 5;
	private final static long
		DEFAULT_DATA_SIZE = SizeUtil.toSize("256KB"), DEFAULT_DATA_COUNT_MAX = 1000000;
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
			// create new items
			final DataItemOutput<WSObject> dataDstW = new BinFileItemOutput<>();
			LOG.info(Markers.MSG, "Start writing");
			final long nWritten = client.write(
				null, dataDstW, DEFAULT_CONN_PER_NODE, DEFAULT_DATA_SIZE
			);
			LOG.info(Markers.MSG, "Written successfully {} items", nWritten);
			// read and verify the written items
			final DataItemInput<WSObject> dataSrcR = dataDstW.getInput();
			final DataItemOutput<WSObject> dataDstR = new BinFileItemOutput<>();
			final long nRead = client.read(dataSrcR, dataDstR, DEFAULT_CONN_PER_NODE);
			LOG.info(Markers.MSG, "Read successfully {} items", nRead);
			// update the appended items
			final DataItemInput<WSObject> dataSrcU = dataDstW.getInput();
			final DataItemOutput dataDstU = new CSVFileItemOutput<>(BasicWSObject.class);
			LOG.info(Markers.MSG, "Start updating");
			final long nUpdated = client.update(dataSrcU, dataDstU, DEFAULT_CONN_PER_NODE);
			LOG.info(Markers.MSG, "Updated successfully {} items", nUpdated);
			// reread the updated items
			final DataItemInput<WSObject> dataSrcR2 = dataDstW.getInput();
			final DataItemOutput<WSObject> dataDstR2 = new CircularListItemOutput<>(
				new ArrayList<WSObject>(1000), 1000
			);
			LOG.info(Markers.MSG, "Start rereading");
			final long nRead2 = client.read(dataSrcR2, dataDstR2, DEFAULT_CONN_PER_NODE, false);
			LOG.info(Markers.MSG, "Reread successfully {} items", nRead2);
			// rewrite the read data items in a circle
			final DataItemInput<WSObject> dataSrcW2 = dataDstR2.getInput();
			LOG.info(Markers.MSG, "Start circular rewriting");
			final long nRewritten = client.write(dataSrcW2, null, DEFAULT_CONN_PER_NODE);
			LOG.info(Markers.MSG, "Rewritten successfully {} times", nRewritten);
			// append the written items
			final DataItemInput<WSObject> dataSrcA = dataDstW.getInput();
			LOG.info(Markers.MSG, "Start updating");
			final long nAppended = client.append(dataSrcA, null, DEFAULT_CONN_PER_NODE);
			LOG.info(Markers.MSG, "Appended successfully {} items", nAppended);
			// delete all written data items
			final DataItemInput<WSObject> dataSrcD = dataDstW.getInput();
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
		rtConfig.set(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, 0);
		final Thread wsMockThread = new Thread(
			new Cinderella(RunTimeConfig.getContext()), "wsMock"
		);
		wsMockThread.setDaemon(true);
		wsMockThread.start();
		//
		rtConfig.set(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, 10);
		final StorageClientBuilder<WSObject, StorageClient<WSObject>>
			clientBuilder = new BasicWSClientBuilder<>();
		final String storageNodes[] = new String[DEFAULT_NODE_COUNT];
		for(int i = 0; i < DEFAULT_NODE_COUNT; i++) {
			storageNodes[i] = "127.0.0.1:" + (9020 + i);
		}
		clientBuilder
			.setNodes(storageNodes)
			.setLimitCount(DEFAULT_DATA_COUNT_MAX)
			.setLimitTime(100, TimeUnit.SECONDS)
			.setLimitRate(10000);
		// standalone
		try(final StorageClient<WSObject> client = clientBuilder.build()) {
			final Thread sanityThread1 = new Thread(new Sanity(client), "sanityStandalone");
			sanityThread1.start();
			LOG.info(Markers.MSG, "Standalone sanity started");
			sanityThread1.join();
			LOG.info(Markers.MSG, "Standalone sanity finished");
		}
		// distributed mode
		rtConfig.set(RunTimeConfig.KEY_REMOTE_SERVE_IF_NOT_LOAD_SERVER, true);
		ServiceUtils.init();
		//
		try(
			final LoadBuilderSvc<WSObject, WSLoadSvc<WSObject>>
				loadSvcBuilder = new BasicWSLoadBuilderSvc<>(rtConfig);
		) {
			loadSvcBuilder.start();
			TimeUnit.SECONDS.sleep(1);
			rtConfig.set(RunTimeConfig.KEY_REMOTE_PORT_EXPORT, 1299);
			try(
				final StorageClient<WSObject> client = clientBuilder
					.setClientMode(new String[] {ServiceUtils.getHostAddr()})
					.build();
			) {
				final Thread sanityThread2 = new Thread(new Sanity(client), "sanityDistributed");
				sanityThread2.start();
				LOG.info(Markers.MSG, "Distributed sanity started");
				sanityThread2.join();
				LOG.info(Markers.MSG, "Distributed sanity finished");
			}
		}
		//
		ServiceUtils.shutdown();
		// finish
		wsMockThread.interrupt();
		LOG.info(Markers.MSG, "Storage mock stopped");
		LOG.info(Markers.MSG, "Sanity done");
	}
}
