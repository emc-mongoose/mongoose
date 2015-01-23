package com.emc.mongoose.web.storagemock;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.web.api.WSIOTask;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObject;
//
import org.apache.commons.codec.binary.Base64;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
//
import javax.management.MBeanServer;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
//
/**
 * Created by olga on 30.09.14.
 */
public final class MockServlet
extends HttpServlet
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final int port;
	private Server server;
	private final static Map<String, WSObject> MAP_DATA_OBJECT = new ConcurrentHashMap<>();
	//
	private final static String
		ALL_METHODS = "all",
		METRIC_COUNT = "count",
		//
		RANGE = "Range";
	private final Counter
		counterAllSucc, counterAllFail,
		counterGetSucc, counterGetFail,
		counterPostSucc, counterPostFail,
		counterPutSucc, counterPutFail,
		counterDeleteSucc, counterDeleteFail,
		counterHeadSucc;
	private final Histogram durAll, durGet, durPost, durPut, durDelete;
	private final Meter
		allBW, getBW, postBW, putBW, deleteBW,
		allTP, getTP, postTP, putTP, deleteTP;
	//
	private final JmxReporter metricsReporter;
	private static int MAX_PAGE_SIZE;
	private long metricsUpdatePeriodSec;
	//
	public MockServlet(final RunTimeConfig runTimeConfig) {
		//
		final MetricRegistry metrics = new MetricRegistry();
		final MBeanServer mBeanServer;
		metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
		MAX_PAGE_SIZE = (int) runTimeConfig.getDataPageSize();
		//Init bean server
		mBeanServer = ServiceUtils.getMBeanServer(runTimeConfig.getRemoteExportPort());
		metricsReporter = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		// init metrics
		counterAllSucc = metrics.counter(MetricRegistry.name(MockServlet.class,
			ALL_METHODS, METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterAllFail = metrics.counter(MetricRegistry.name(MockServlet.class,
			ALL_METHODS, METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durAll = metrics.histogram(MetricRegistry.name(MockServlet.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_DUR));
		allTP = metrics.meter(MetricRegistry.name(MockServlet.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_TP));
		allBW = metrics.meter(MetricRegistry.name(MockServlet.class,
			ALL_METHODS, LoadExecutor.METRIC_NAME_BW));
		//
		counterGetSucc = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.GET.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterGetFail = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.GET.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durGet = metrics.histogram(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.GET.name(), LoadExecutor.METRIC_NAME_DUR));
		getBW = metrics.meter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.GET.name(), LoadExecutor.METRIC_NAME_BW));
		getTP = metrics.meter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.GET.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterPostSucc = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.POST.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterPostFail = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.POST.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durPost = metrics.histogram(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.POST.name(), LoadExecutor.METRIC_NAME_DUR));
		postBW = metrics.meter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.POST.name(), LoadExecutor.METRIC_NAME_BW));
		postTP = metrics.meter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.POST.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterPutSucc = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.PUT.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterPutFail = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.PUT.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durPut = metrics.histogram(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.PUT.name(), LoadExecutor.METRIC_NAME_DUR));
		putBW = metrics.meter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.PUT.name(), LoadExecutor.METRIC_NAME_BW));
		putTP = metrics.meter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.PUT.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterDeleteSucc = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.DELETE.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterDeleteFail = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.DELETE.name(), METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durDelete = metrics.histogram(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.DELETE.name(), LoadExecutor.METRIC_NAME_DUR));
		deleteBW = metrics.meter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.DELETE.name(), LoadExecutor.METRIC_NAME_BW));
		deleteTP = metrics.meter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.DELETE.name(), LoadExecutor.METRIC_NAME_TP));
		//
		counterHeadSucc = metrics.counter(MetricRegistry.name(MockServlet.class,
			WSIOTask.HTTPMethod.HEAD.name(), LoadExecutor.METRIC_NAME_SUCC));
		//
		metricsReporter.start();
		//
		final String apiName = runTimeConfig.getStorageApi();
		port = runTimeConfig.getInt("api." + apiName + ".port");
		//for QueuedThreadPool
		final int size = runTimeConfig.getInt("wsmock.size");
		final int minThreads = runTimeConfig.getInt("wsmock.minThreads");
		final int maxThreads = runTimeConfig.getInt("wsmock.maxThreads");
		final int idleTimeout = runTimeConfig.getInt("wsmock.idleTimeout");
		final QueuedThreadPool queuedThreadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout, new ArrayBlockingQueue<Runnable>(size));
		//
		LOG.info(Markers.MSG, "Set up Jetty Server instance");
		server = new Server(queuedThreadPool);
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		LOG.info(Markers.MSG, "Set up Http Connector");
		try (final ServerConnector httpConnector = new ServerConnector(server)) {
			httpConnector.setPort(port);
			server.addConnector(httpConnector);
		} catch (final Exception e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "Creating of server connector failed");
		}
		LOG.debug(Markers.MSG, "Set up a new handler");
		final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		LOG.debug(Markers.MSG, "Add servlet");
		context.addServlet(new ServletHolder(this), "/*");
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Runnable implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void run() {
		try {
			server.start();
			LOG.info(Markers.MSG, "Listening on port #{}", port);
			//Output metrics
			final long updatePeriodMilliSec = TimeUnit.SECONDS.toMillis(metricsUpdatePeriodSec);
			while (metricsUpdatePeriodSec > 0) {
				printMetrics();
				Thread.sleep(updatePeriodMilliSec);
			}
			//
			server.join();
		} catch (final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupting the WSMock servlet");
		} catch (final Exception e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to start WSMock servlet");
		} finally {
			try {
				server.stop();
			} catch (final Exception e) {
				TraceLogger.failure(LOG, Level.WARN, e, "Failed to stop jetty");
			}
			metricsReporter.close();
		}
	}
	//
	private final static String
		MSG_FMT_METRICS = "count=(%d/%d); duration[us]=(%d/%d/%d/%d); " +
			"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	private void printMetrics() {
		final Snapshot allDurSnapshot = durAll.getSnapshot();
		LOG.info(
			Markers.PERF_AVG,
			String.format(
				Main.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				counterAllSucc.getCount(), counterAllFail.getCount(),
				//
				(int) (allDurSnapshot.getMean() / LoadExecutor.NANOSEC_SCALEDOWN),
				(int) (allDurSnapshot.getMin() / LoadExecutor.NANOSEC_SCALEDOWN),
				(int) (allDurSnapshot.getMedian() / LoadExecutor.NANOSEC_SCALEDOWN),
				(int) (allDurSnapshot.getMax() / LoadExecutor.NANOSEC_SCALEDOWN),
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
	// Request handling methods ////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected final void doGet(
		final HttpServletRequest request, final HttpServletResponse response
	) throws ServletException, IOException {
		LOG.trace(Markers.MSG, " Request  method Get ");
		response.setStatus(HttpServletResponse.SC_OK);
		try (final ServletOutputStream servletOutputStream = response.getOutputStream()) {
			final String dataID = request.getRequestURI().split("/")[2];
			if (MAP_DATA_OBJECT.containsKey(dataID)) {
				LOG.trace(Markers.MSG, "   Send data object ", dataID);
				final WSObject object = MAP_DATA_OBJECT.get(dataID);
				long nanoTime = System.nanoTime();
				object.writeTo(servletOutputStream);
				nanoTime = System.nanoTime() - nanoTime;
				durGet.update(nanoTime);
				durAll.update(nanoTime);
				counterAllSucc.inc();
				counterGetSucc.inc();
				getBW.mark(object.getSize());
				allBW.mark(object.getSize());
				getTP.mark();
				allTP.mark();
				LOG.trace(Markers.MSG, "   Response: OK");
			} else {
				counterAllFail.inc();
				counterGetFail.inc();
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				LOG.trace(Markers.ERR, String.format("No such object: \"%s\"", dataID));
			}
		} catch (final IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			counterAllFail.inc();
			counterGetFail.inc();
			TraceLogger.failure(LOG, Level.WARN, e, "Servlet output failed");
		} catch (final ArrayIndexOutOfBoundsException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			counterAllFail.inc();
			counterGetFail.inc();
			TraceLogger.failure(LOG, Level.WARN, e, "Request URI is not correct. " +
				"Data object ID doesn't exist in request URI");
		} catch(final Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			counterAllFail.inc();
			counterGetFail.inc();
			TraceLogger.failure(LOG, Level.ERROR, t, "Unexpected failure");
		}
	}
	//
	@Override
	protected final void doPost(
		final HttpServletRequest request, final HttpServletResponse response
	) throws ServletException, IOException {
		LOG.trace(Markers.MSG, " Request  method Post ");
		response.setStatus(HttpServletResponse.SC_OK);
		try (final ServletInputStream servletInputStream = request.getInputStream()) {
			//
			long nanoTime = System.nanoTime();
			final long bytes = calcInputByteCount(servletInputStream);
			nanoTime = System.nanoTime() - nanoTime;
			//
			durPost.update(nanoTime);
			durAll.update(nanoTime);
			counterAllSucc.inc();
			counterPostSucc.inc();
			postBW.mark(bytes);
			allBW.mark(bytes);
			postTP.mark();
			allTP.mark();
		} catch (final IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			counterAllFail.inc();
			counterPostFail.inc();
			TraceLogger.failure(LOG, Level.WARN, e, "Servlet input stream failed");
		}
	}
	//
	@Override
	protected final void doPut(
		final HttpServletRequest request, final HttpServletResponse response
	) throws ServletException, IOException {
		LOG.trace(Markers.MSG, " Request  method Put ");
		response.setStatus(HttpServletResponse.SC_OK);
		long offset;
		String dataID = "";
		WSObject dataObject = null;
		try (final ServletInputStream servletInputStream = request.getInputStream()) {
			//
			long nanoTime = System.nanoTime();
			long bytes = calcInputByteCount(servletInputStream);
			nanoTime = System.nanoTime() - nanoTime;
			//
			dataID = request.getRequestURI().split("/")[2];
			//
			if(Base64.isBase64(dataID) && dataID.length() < 12) { // v0.4x and 0.5x
				final byte dataIdBytes[] = Base64.decodeBase64(dataID);
				offset  = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(dataIdBytes).getLong(0);
			} else {
				try {
					offset = Long.valueOf(dataID, WSRequestConfigBase.RADIX); // versions since v0.6
				} catch (final NumberFormatException e) {
					offset = Long.valueOf(dataID, 0x10); // versions prior to 0.4
				}
			}
			//create data object or get it for append or update
			if(MAP_DATA_OBJECT.containsKey(dataID)) {
				dataObject = MAP_DATA_OBJECT.get(dataID);
			} else {
				dataObject = new WSObjectMock(dataID, offset, bytes);
			}
			//
			if (request.getHeader(RANGE) != null) {
				//Parse string of ranges information
				final String[] rangeStringArray = request.getHeader(RANGE).split("\\s*[=,-]\\s*");
				final List<Long> ranges = new ArrayList<>();
				for (int i = 1; i < rangeStringArray.length; i++){
					ranges.add(Long.valueOf(rangeStringArray[i]));
				}
				if (ranges.size() % 2 != 0){
					ranges.add(ranges.get(ranges.size()-1) + bytes);
				}
				// Switch append or update or exception
				//
				//if append
				if (ranges.get(0) == dataObject.getSize()) {
					//append data object
					dataObject.append(bytes);
					//resize data object
					dataObject.setSize(dataObject.getSize() + bytes);
					//end append
					//if update
				} else if (ranges.get(ranges.size() - 1) <= dataObject.getSize()){
					//update data object
					dataObject.updateRanges(ranges);
				} else {
					throw new Exception();
				}
			}
			//
			MAP_DATA_OBJECT.put(dataID, dataObject);
			//
			durPut.update(nanoTime);
			durAll.update(nanoTime);
			counterAllSucc.inc();
			counterPutSucc.inc();
			putBW.mark(bytes);
			allBW.mark(bytes);
			putTP.mark();
			allTP.mark();
		} catch (final IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			counterAllFail.inc();
			counterPutFail.inc();
			TraceLogger.failure(LOG, Level.WARN, e, "Servlet input stream failed");
		}catch (final NumberFormatException e){
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			counterAllFail.inc();
			counterPutFail.inc();
			e.printStackTrace();
			TraceLogger.failure(
				LOG, Level.WARN, e,
				String.format("Unexpected object id format: \"%s\"", dataID)
			);
		}catch (final ArrayIndexOutOfBoundsException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			counterAllFail.inc();
			counterPutFail.inc();
			TraceLogger.failure(LOG, Level.WARN, e, "Request URI is not correct. Data object ID doesn't exist in request URI");
		} catch (final Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			counterAllFail.inc();
			counterPutFail.inc();
			TraceLogger.failure(LOG, Level.WARN, e, "Ranges have not correct form.");
		}
	}
	//
	@Override
	protected final void doDelete(
		final HttpServletRequest request, final HttpServletResponse response
	) throws ServletException, IOException {
		LOG.trace(Markers.MSG, " Request  method Delete ");
		response.setStatus(HttpServletResponse.SC_OK);
		try (final ServletInputStream servletInputStream = request.getInputStream()) {
			//
			long nanoTime = System.nanoTime();
			final long bytes = calcInputByteCount(servletInputStream);
			nanoTime = System.nanoTime() - nanoTime;
			//
			durDelete.update(nanoTime);
			durAll.update(nanoTime);
			counterAllSucc.inc();
			counterDeleteSucc.inc();
			deleteBW.mark(bytes);
			allBW.mark(bytes);
			deleteTP.mark();
			allTP.mark();
		} catch (final IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			counterAllFail.inc();
			counterDeleteFail.inc();
			TraceLogger.failure(LOG, Level.WARN, e, "Servlet input stream failed");
		}
	}
	//
	@Override
	protected final void doHead(
		final HttpServletRequest request, final HttpServletResponse response
	) throws ServletException, IOException {
		LOG.trace(Markers.MSG, " Request  method Head ");
		response.setStatus(HttpServletResponse.SC_OK);
		counterAllSucc.inc();
		counterHeadSucc.inc();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private long calcInputByteCount(
		final ServletInputStream servletInputStream
	) throws ServletException, IOException {
		final byte buff[] = new byte[MAX_PAGE_SIZE];
		long doneByteCountSum = 0, doneByteCount;
		do {
			doneByteCount = servletInputStream.read(buff);
			doneByteCountSum += (doneByteCount < 0 ? 0 : doneByteCount);
		} while (doneByteCount >= 0);
		return doneByteCountSum;
	}
}
