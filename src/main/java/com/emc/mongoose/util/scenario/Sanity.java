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
	public void run() {
		try {
			final StorageClientBuilder<WSObject, StorageClient<WSObject>>
				clientBuilder = new BasicWSClientBuilder<>();
			final StorageClient<WSObject> clientStandalone = clientBuilder
				.setNodes(new String[] {"127.0.0.1:9020"})
				.setLimitCount(1000000)
				.setLimitTime(100, TimeUnit.SECONDS)
				.setLimitRate(10000)
				.build();
			//
			final DataItemOutput<WSObject> dataDstW = new TxtFileItemOutput<>(
				Files.createTempFile(null, null), BasicWSObject.class
			);
			LOG.info(Markers.MSG, "Start writing");
			final long nWritten = clientStandalone.write(
				null, dataDstW, (short) 100, SizeUtil.toSize("1KB")
			);
			LOG.info(Markers.MSG, "Written successfully {} items", nWritten);
			//
			final DataItemInput<WSObject> dataSrcR = dataDstW.getInput();
			final DataItemOutput<WSObject> dataDstR = new BinFileItemOutput<>(
				Files.createTempFile(null, null)
			);
			final long nRead = clientStandalone.read(dataSrcR, dataDstR, (short) 100);
			LOG.info(Markers.MSG, "Read successfully {} items", nRead);
			//
			final DataItemInput<WSObject> dataSrcD = dataDstR.getInput();
			final long nDeleted = clientStandalone.delete(dataSrcD, null, (short) 100);
			LOG.info(Markers.MSG, "Deleted successfully {} items", nDeleted);
			//
			dataSrcR.reset();
			final DataItemOutput<WSObject> dataDstRW = new BinFileItemOutput<>(
				Files.createTempFile(null, null)
			);
			LOG.info(Markers.MSG, "Start rewriting");
			final long nReWritten = clientStandalone.write(dataSrcR, dataDstRW, (short) 100);
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
		RunTimeConfig.initContext();
		final Thread wsMockThread = new Thread(
			new Cinderella<>(RunTimeConfig.getContext()), "wsMock"
		);
		final Thread sanityThread = new Thread(new Sanity(), "sanity");
		wsMockThread.start();
		TimeUnit.SECONDS.sleep(1);
		sanityThread.start();
		TimeUnit.SECONDS.sleep(1);
		sanityThread.join();
		TimeUnit.SECONDS.sleep(1);
		wsMockThread.interrupt();
		LOG.info(Markers.MSG, "Sanity done");
	}
}
