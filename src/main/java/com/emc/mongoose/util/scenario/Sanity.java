package com.emc.mongoose.util.scenario;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.api.data.util.DataItemInput;
import com.emc.mongoose.core.impl.data.util.BinFileItemInput;
import com.emc.mongoose.storage.mock.impl.Cinderella;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 22.06.15.
 */
public class Sanity
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	static {
		LogUtil.init();
		RunTimeConfig.initContext();
	}
	//
	public void run() {
		final StorageClientBuilder<WSObject, StorageClient<WSObject>>
			clientBuilder = new BasicWSClientBuilder<>();
		final StorageClient<WSObject> client = clientBuilder
			.setNodes(new String[] {"127.0.0.1:9020"})
			.setLimitCount(10000000)
			.setLimitTime(10, TimeUnit.MINUTES)
			.build();
		//final DataItemInput<WSObject> dataSrcW = new BinFileItemInput<>("/tmp");
		//client.write(null, );

	}
	//
	public static void main(final String... args)
	throws IOException, InterruptedException {
		final ExecutorService scenarioExecutor = Executors.newFixedThreadPool(2);
		scenarioExecutor.submit(new Cinderella<>(RunTimeConfig.getContext()));
		scenarioExecutor.submit(new Sanity());
		scenarioExecutor.shutdown();
		scenarioExecutor.awaitTermination(1, TimeUnit.DAYS);
	}
}
