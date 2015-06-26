package com.emc.mongoose.storage.mock.impl;
// mongoose-common.jar
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.date.LowPrecisionDateGenerator;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.AsyncConsumerBase;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.api.Storage;
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.api.net.SocketEventDispatcher;
import com.emc.mongoose.storage.mock.api.stats.IOStats;
import com.emc.mongoose.storage.mock.impl.net.BasicSocketEventDispatcher;
import com.emc.mongoose.storage.mock.impl.data.BasicWSObjectMock;
import com.emc.mongoose.storage.mock.impl.request.APIRequestHandlerMapper;
import com.emc.mongoose.storage.mock.impl.net.BasicWSMockConnFactory;
import com.emc.mongoose.storage.mock.impl.stats.BasicStorageIOStats;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
/**
 * Created by olga on 28.01.15.
 */
public final class Cinderella<T extends WSObjectMock>
extends LRUMap<String, T>
implements Storage<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final SocketEventDispatcher sockEvtDispatchers[] ;
	private final HttpAsyncService protocolHandler;
	private final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
	private final int portStart;
	private final RunTimeConfig runTimeConfig;
	private final IOStats ioStats;
	//
	private final AsyncConsumer<T> createConsumer, deleteConsumer;
	//
	public Cinderella(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig.getStorageMockCapacity());
		this.runTimeConfig = runTimeConfig;
		final int
			maxQueueSize = runTimeConfig.getRunRequestQueueSize(),
			submTimeOutMilliSec = runTimeConfig.getRunSubmitTimeOutMilliSec();
		createConsumer = new AsyncConsumerBase<T>(Long.MAX_VALUE, maxQueueSize, submTimeOutMilliSec) {
			{ setDaemon(true); setName("asyncCreateWorker"); start(); }
			@Override
			protected final void submitSync(final T dataItem)
			throws InterruptedException, RemoteException {
				put(dataItem.getId(), dataItem);
			}
		};
		deleteConsumer = new AsyncConsumerBase<T>(Long.MAX_VALUE, maxQueueSize, submTimeOutMilliSec) {
			{ setDaemon(true); setName("asyncDeleteWorker"); start(); }
			@Override
			protected final void submitSync(final T dataItem)
			throws InterruptedException, RemoteException {
				remove(dataItem.getId());
			}
		};
		ioStats = new BasicStorageIOStats(runTimeConfig, this);
		sockEvtDispatchers = new SocketEventDispatcher[runTimeConfig.getStorageMockHeadCount()];
		portStart = runTimeConfig.getApiTypePort(runTimeConfig.getApiName());
		LOG.info(
			Markers.MSG, "Starting with {} heads and capacity of {}",
			sockEvtDispatchers.length, runTimeConfig.getStorageMockCapacity()
		);
		// connection config
		final ConnectionConfig connConfig = ConnectionConfig
			.custom()
			.setBufferSize(BUFF_SIZE_LO)
			//.setFragmentSizeHint(BUFF_SIZE_LO)
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
					bufferReader = Files.newBufferedReader(
						Paths.get(dataFilePath), StandardCharsets.UTF_8
					)
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
		int nextPort;
		for(int i = 0; i < sockEvtDispatchers.length; i ++) {
			nextPort = portStart + i;
			try {
				sockEvtDispatchers[i] = new BasicSocketEventDispatcher(
					runTimeConfig, protocolHandler, nextPort, connFactory, ioStats
				);
				sockEvtDispatchers[i].start();
			} catch(final IOReactorException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the head at port #{}", nextPort
				);
			}
		}
		if(sockEvtDispatchers.length > 1) {
			LOG.info(Markers.MSG,"Listening the ports {} .. {}",
				portStart, portStart + sockEvtDispatchers.length - 1);
		} else {
			LOG.info(Markers.MSG,"Listening the port {}", portStart);
		}
		//
		try {
			for(final SocketEventDispatcher sockEvtDispatcher : sockEvtDispatchers) {
				if(sockEvtDispatcher != null) {
					sockEvtDispatcher.join();
				}
			}
		} catch (final InterruptedException e) {
			LOG.info(Markers.MSG, "Interrupting the Cinderella");
		} finally {
			//
			try {
				createConsumer.close();
				LOG.debug(Markers.MSG, "Create consumer closed successfully");
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "I/O failure on close");
			}
			//
			try {
				deleteConsumer.close();
				LOG.debug(Markers.MSG, "Delete consumer closed successfully");
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "I/O failure on close");
			}
			//
			for(final SocketEventDispatcher sockEventDispatcher : sockEvtDispatchers) {
				if(sockEventDispatcher != null) {
					try {
						sockEventDispatcher.close();
						LOG.debug(
							Markers.MSG, "Socket event dispatcher \"{}\" closed successfully",
							sockEventDispatcher
						);
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Closing socket event dispatcher \"{}\" failure",
							sockEventDispatcher
						);
					}
				}
			}
			try {
				ioStats.close();
				LOG.debug(Markers.MSG, "Storage I/O stats daemon closed successfully");
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Closing storage I/O stats daemon failure");
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
