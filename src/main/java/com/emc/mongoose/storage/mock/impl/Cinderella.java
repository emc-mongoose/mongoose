package com.emc.mongoose.storage.mock.impl;
// mongoose-common.jar
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.date.LowPrecisionDateGenerator;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.AsyncConsumerBase;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.api.Storage;
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.api.stats.IOStats;
import com.emc.mongoose.storage.mock.impl.net.BasicSocketEventDispatcher;
import com.emc.mongoose.storage.mock.impl.data.BasicWSObjectMock;
import com.emc.mongoose.storage.mock.impl.request.APIRequestHandlerMapper;
import com.emc.mongoose.storage.mock.impl.net.BasicWSMockConnFactory;
import com.emc.mongoose.storage.mock.impl.stats.BasicIOStats;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
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
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 * Created by olga on 28.01.15.
 */
public final class Cinderella<T extends WSObjectMock>
extends LRUMap<String, T>
implements Storage<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final ExecutorService multiSocketSvc;
	private final HttpAsyncService protocolHandler;
	private final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
	private final int portStart, countHeads;
	private final RunTimeConfig runTimeConfig;
	private final IOStats ioStats;
	//
	private final AsyncConsumer<T> createConsumer, deleteConsumer;
	//
	public Cinderella(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig.getStorageMockCapacity());
		this.runTimeConfig = runTimeConfig;
		createConsumer = new AsyncConsumerBase<T>(
			(Class<T>) BasicWSObjectMock.class, runTimeConfig, Long.MAX_VALUE, true
		) {
			{ setDaemon(true); setName("createQueueWorker"); start(); }
			@Override
			protected final void submitSync(final T dataItem)
			throws InterruptedException, RemoteException {
				put(dataItem.getId(), dataItem);
			}
		};
		deleteConsumer = new AsyncConsumerBase<T>(
			(Class<T>) BasicWSObjectMock.class, runTimeConfig, Long.MAX_VALUE, true
		) {
			{ setDaemon(true); setName("deleteQueueWorker"); start(); }
			@Override
			protected final void submitSync(final T dataItem)
			throws InterruptedException, RemoteException {
				remove(dataItem.getId());
			}
		};
		ioStats = new BasicIOStats(runTimeConfig, this);
		countHeads = runTimeConfig.getStorageMockHeadCount();
		portStart = runTimeConfig.getApiTypePort(runTimeConfig.getApiName());
		LOG.info(
			Markers.MSG, "Starting with {} heads and capacity of {}",
			countHeads, runTimeConfig.getStorageMockCapacity()
		);
		// connection config
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(2 * BUFF_SIZE_LO)
			.setFragmentSizeHint(0)
			.build();
		connFactory = new BasicWSMockConnFactory(runTimeConfig, connConfig);
		// Set up the HTTP protocol processor
		final HttpProcessor httpProc = HttpProcessorBuilder.create()
			.add( // this is a date header generator below
				new HttpResponseInterceptor() {
					@Override
					public void process(
						final HttpResponse response, final HttpContext context
					) throws HttpException, IOException {
						response.setHeader(
							HTTP.DATE_HEADER, LowPrecisionDateGenerator.getDateText()
						);
					}
				}
			)
			.add( // user-agent header
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
		final HttpAsyncRequestHandlerMapper apiReqHandlerMapper = new APIRequestHandlerMapper<>(
			runTimeConfig, this
		);
		// Register the default handler for all URIs
		protocolHandler = new HttpAsyncService(httpProc, apiReqHandlerMapper);
		multiSocketSvc = Executors.newFixedThreadPool(
			countHeads, new GroupThreadFactory("cinderellaHead")
		);
	}
	//
	@Override
	public void run() {
		ioStats.start();
		try {
			createConsumer.start();
			deleteConsumer.start();
		} catch(final RemoteException ignored) {
		}
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
					final T dataObject = (T) new BasicWSObjectMock(s) ;
					// if mongoose is v0.5.0
					if(dataSizeRadix == 0x10) {
						dataObject.setSize(Long.valueOf(String.valueOf(dataObject.getSize()), 0x10));
					}
					//
					LOG.trace(Markers.DATA_LIST, dataObject);
					put(dataObject.getId(), dataObject);
				}
			} catch(final FileNotFoundException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "File \"{}\" not found", dataFilePath
				);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to read from file \"{}\"", dataFilePath
				);
			}
		}
		//
		for(int nextPort = portStart; nextPort < portStart + countHeads; nextPort ++){
			try {
				multiSocketSvc.submit(
					new BasicSocketEventDispatcher(
						runTimeConfig, protocolHandler, nextPort, connFactory, ioStats
					)
				);
			} catch(final IOReactorException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the head at port #{}", nextPort
				);
			}
		}
		if(countHeads > 1) {
			LOG.info(Markers.MSG,"Listening the ports {} .. {}",
				portStart, portStart + countHeads - 1);
		} else {
			LOG.info(Markers.MSG,"Listening the port {}", portStart);
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
			LOG.info(Markers.MSG, "Interrupting the Cinderella");
		} finally {
			try {
				createConsumer.close();
				deleteConsumer.close();
				ioStats.close();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Closing I/O stats failure");
			}
		}
	}
	//
	@Override
	public final synchronized T get(final String id) {
		return super.get(id);
	}
	//
	@Override
	public final synchronized T put(final String id, final T object) {
		return super.put(id, object);
	}
	//
	@Override
	public final synchronized T remove(final Object id) {
		return super.remove(id);
	}
	//
	@Override
	public long getSize() {
		return size();
	}
	@Override
	public long getCapacity() {
		return maxSize();
	}
	//
	@Override
	public final IOStats getStats() {
		return ioStats;
	}
	//
	@Override
	public final void create(final T object) {
		try {
			createConsumer.submit(object);
		} catch(final InterruptedException | RejectedExecutionException | RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Create submission failure");
		}
	}
	//
	@Override
	public final void delete(final T object) {
		try {
			deleteConsumer.submit(object);
		} catch(final InterruptedException | RejectedExecutionException | RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Delete submission failure");
		}
	}
}
