package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
//
import com.emc.mongoose.common.collections.Cache;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.data.BasicWSObjectMock;
//
import com.emc.mongoose.storage.mock.impl.request.APIRequestHandlerMapper;
//
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerMapper;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
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
	private final static NHttpConnectionFactory<DefaultNHttpServerConnection>
		CONNECTION_FACTORY = new FaultingConnectionFactory(ConnectionConfig.DEFAULT);
	private final static long METRICS_UPDATE_PERIOD_SEC = RunTimeConfig.getContext().getLoadMetricsPeriodSec();
	private final int portStart, countHeads;
	//
	public Cinderella(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig.getStorageMockCapacity());
		countHeads = runTimeConfig.getStorageMockHeadCount();
		portStart = runTimeConfig.getApiTypePort(runTimeConfig.getApiName());
		LOG.info(
			LogUtil.MSG, "Starting with {} heads and capacity of {}",
			countHeads, runTimeConfig.getStorageMockCapacity()
		);
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
		//if there is data src file path
		final String dataFilePath = RunTimeConfig.getContext().getDataSrcFPath();
		final int dataSizeRadix = RunTimeConfig.getContext().getDataRadixSize();
		if (!dataFilePath.isEmpty()){
			try {
				final FileReader reader = new FileReader(dataFilePath);
				final BufferedReader bufferReader = new BufferedReader(reader);
				String s;
				while((s = bufferReader.readLine()) != null) {
					final WSObjectMock dataObject = new BasicWSObjectMock(s) ;
					//if mongoose v.0.5.0
					if (dataSizeRadix == 0x10) {
						dataObject.setSize(Long.valueOf(String.valueOf(dataObject.getSize()), 0x10));
					}
					//
					LOG.trace(LogUtil.DATA_LIST, String.format("%s", dataObject));
					put(dataObject.getId(), dataObject);
				}
				reader.close();
			} catch (final FileNotFoundException e) {
				LogUtil.failure(LOG, Level.ERROR, e,
					"File not found.");
			} catch (final IOException e) {
				LogUtil.failure(LOG, Level.ERROR, e,
					"Read line is fault.");
			}
		}
		//
		for(int nextPort = portStart; nextPort < portStart + countHeads; nextPort ++){
			try {
				multiSocketSvc.submit(new SocketHandlerTask(protocolHandler, nextPort));
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
		try {
			//output metrics
			final long updatePeriodMilliSec = TimeUnit.SECONDS.toMillis(METRICS_UPDATE_PERIOD_SEC);
			while (METRICS_UPDATE_PERIOD_SEC > 0) {
				printMetrics();
				Thread.sleep(updatePeriodMilliSec);
			}
			//
			final long timeOutValue = RunTimeConfig.getContext().getLoadLimitTimeValue();
			final TimeUnit timeUnit = RunTimeConfig.getContext().getLoadLimitTimeUnit();
			if(timeOutValue > 0) {
				multiSocketSvc.awaitTermination(timeOutValue, timeUnit);
			} else {
				multiSocketSvc.awaitTermination(Long.MAX_VALUE, timeUnit);
			}
		} catch (final InterruptedException e) {
			LOG.info(LogUtil.MSG, "Interrupting the Cinderella");
		} finally {
			metricsReporter.close();
		}
	}
	//
	private final static String
		MSG_FMT_METRICS = "count(succ=(%d/%d/%d); fail=(%d/%d/%d)); " +
			"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	private void printMetrics() {
		LOG.info(
			LogUtil.PERF_AVG,
			String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				counterSuccCreate.getCount(), counterSuccRead.getCount(), counterSuccDelete.getCount(),
				counterFailCreate.getCount(), counterFailRead.getCount(), counterFailDelete.getCount(),
				//
				allTP.getMeanRate(),
				allTP.getOneMinuteRate(),
				allTP.getFiveMinuteRate(),
				allTP.getFifteenMinuteRate(),
				//
				allBW.getMeanRate() / LoadExecutor.MIB,
				allBW.getOneMinuteRate() / LoadExecutor.MIB,
				allBW.getFiveMinuteRate() / LoadExecutor.MIB,
				allBW.getFifteenMinuteRate() / LoadExecutor.MIB
			)
		);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// socket listening task ///////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static class SocketHandlerTask
	implements Runnable {
		//
		private final ListeningIOReactor ioReactor;
		private final IOEventDispatch ioEventDispatch;
		private final int port;
		//
		public SocketHandlerTask(final HttpAsyncService protocolHandler, final int port)
		throws IOReactorException {
			this.port = port;
			ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, CONNECTION_FACTORY);
			// Set I/O reactor defaults
			final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
			final IOReactorConfig config = IOReactorConfig.custom()
				.setIoThreadCount(localRunTimeConfig.getStorageMockIoThreadsPerSocket())
				.setSoTimeout(localRunTimeConfig.getSocketTimeOut())
				.setConnectTimeout(localRunTimeConfig.getConnTimeOut())
				.build();
			// Create server-side I/O reactor
			ioReactor = new DefaultListeningIOReactor(config);
		}
		//
		@Override
		public final void run() {
			try {
				// Listen of the given port
				ioReactor.listen(new InetSocketAddress(port));
				// Ready to go!
				ioReactor.execute(ioEventDispatch);
			} catch (final InterruptedIOException ex) {
				LogUtil.failure(LOG, Level.DEBUG, ex, "Interrupted");
			} catch (final IOReactorException ex) {
				LogUtil.failure(LOG, Level.ERROR, ex, "I/O reactor failure");
			} catch (final IOException ex) {
				LogUtil.failure(LOG, Level.ERROR, ex, "I/O failure");
			}
		}
	}
}
