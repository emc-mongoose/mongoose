package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.emc.mongoose.common.collections.Cache;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.data.BasicWSObjectMock;
import com.emc.mongoose.storage.mock.impl.cinderella.request.APIRequestHandlerMapper;
import com.emc.mongoose.storage.mock.impl.cinderella.request.WSRequestHandlerBase;
//
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
//
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerMapper;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOReactorException;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 * Created by olga on 28.01.15.
 */
public final class Cinderella
extends Cache<String, WSObjectMock>
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final ExecutorService multiSocketSvc;
	private final HttpAsyncService protocolHandler;
	private final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
	private final int portStart, countHeads;
	private final RunTimeConfig runTimeConfig;
	//
	public Cinderella(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig.getStorageMockCapacity());
		this.runTimeConfig = runTimeConfig;
		countHeads = runTimeConfig.getStorageMockHeadCount();
		portStart = runTimeConfig.getApiTypePort(runTimeConfig.getApiName());
		LOG.info(
			LogUtil.MSG, "Starting with {} heads and capacity of {}",
			countHeads, runTimeConfig.getStorageMockCapacity()
		);
		// connection config
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(LoadExecutor.BUFF_SIZE_LO > 0x1000 ? 0x2000 : 2 * LoadExecutor.BUFF_SIZE_LO)
			.setFragmentSizeHint(LoadExecutor.BUFF_SIZE_LO > 0x1000 ? 0x1000 : LoadExecutor.BUFF_SIZE_LO)
			.build();
		final int faultConnCacheSize = runTimeConfig.getStorageMockFaultConnCacheSize();
		if(faultConnCacheSize > 0) {
			connFactory = new FaultingConnectionFactory(runTimeConfig, connConfig);
		} else {
			connFactory = new DefaultNHttpServerConnectionFactory(connConfig);
		}
		// Set up the HTTP protocol processor
		final HttpProcessor httpproc = HttpProcessorBuilder.create()
			.add(new ResponseDate())
			.add(
				new ResponseServer(
					String.format(
						"%s/%s", Cinderella.class.getSimpleName(), runTimeConfig.getRunVersion()
					)
				)
			)
			.add(new ResponseContent())
			.add(new ResponseConnControl())
			.build();
		// Create request handler registry
		final HttpAsyncRequestHandlerMapper apiReqHandlerMapper = new APIRequestHandlerMapper(
			runTimeConfig, this
		);
		// Register the default handler for all URIs
		protocolHandler = new HttpAsyncService(httpproc, apiReqHandlerMapper);
		multiSocketSvc = Executors.newFixedThreadPool(
			countHeads, new NamingWorkerFactory("cinderellaWorker")
		);
	}

	@Override
	public void run() {
		// if there is data src file path
		final String dataFilePath = runTimeConfig.getDataSrcFPath();
		final int dataSizeRadix = runTimeConfig.getDataRadixSize();
		if(!dataFilePath.isEmpty()) {
			try(
				final BufferedReader
					bufferReader = new BufferedReader(new FileReader(dataFilePath))
			) {
				String s;
				while((s = bufferReader.readLine()) != null) {
					final WSObjectMock dataObject = new BasicWSObjectMock(s) ;
					// if mongoose is v0.5.0
					if(dataSizeRadix == 0x10) {
						dataObject.setSize(Long.valueOf(String.valueOf(dataObject.getSize()), 0x10));
					}
					//
					LOG.trace(LogUtil.DATA_LIST, String.format("%s", dataObject));
					put(dataObject.getId(), dataObject);
				}
			} catch(final FileNotFoundException e) {
				LogUtil.failure(
					LOG, Level.ERROR, e, String.format("File \"%s\" not found", dataFilePath)
				);
			} catch(final IOException e) {
				LogUtil.failure(
					LOG, Level.ERROR, e,
					String.format("Failed to read from file \"%s\"", dataFilePath)
				);
			}
		}
		//
		for(int nextPort = portStart; nextPort < portStart + countHeads; nextPort ++){
			try {
				multiSocketSvc.submit(
					new WSSocketIOEventDispatcher(
						runTimeConfig, protocolHandler, nextPort, connFactory
					)
				);
			} catch(final IOReactorException e) {
				LogUtil.failure(
					LOG, Level.ERROR, e,
					String.format("Failed to start the head at port #%d", nextPort)
				);
			}
		}
		if(countHeads > 1) {
			LOG.info(LogUtil.MSG,"Listening the ports {} .. {}",
				portStart, portStart + countHeads - 1);
		} else {
			LOG.info(LogUtil.MSG,"Listening the port {}", portStart);
		}
		multiSocketSvc.shutdown();
		//
		try {
			final long timeOutValue = runTimeConfig.getLoadLimitTimeValue();
			final TimeUnit timeUnit = runTimeConfig.getLoadLimitTimeUnit();
			if(timeOutValue > 0) {
				multiSocketSvc.awaitTermination(timeOutValue, timeUnit);
			} else {
				multiSocketSvc.awaitTermination(Long.MAX_VALUE, timeUnit);
			}
		} catch (final InterruptedException e) {
			LOG.info(LogUtil.MSG, "Interrupting the Cinderella");
		} finally {
			WSRequestHandlerBase.METRICS.close();
		}
	}
}
