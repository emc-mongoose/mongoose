package com.emc.mongoose.run;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.web.data.impl.BasicWSObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
//
/**
 * Created by olga on 30.09.14.
 */
public final class WSMockServlet
		extends HttpServlet
		implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final int port;
	private Server server;
	private final Map<String, BasicWSObject> mapDataObject = new ConcurrentHashMap<>();
	// METRICS section BEGIN
	protected final MetricRegistry metrics = new MetricRegistry();
	private final static String
			ALL_METHODS = "all",
			METRIC_COUNT = "count";
	protected final Counter
			counterAllSucc, counterAllFail,
			counterGetSucc, counterGetFail,
			counterPostSucc, counterPostFail,
			counterPutSucc, counterPutFail,
			counterDeleteSucc, counterDeleteFail,
			counterHeadSucc, counterHeadFail;
	protected final Histogram durAll, durGet, durPost, durPut, durDelete;
	protected final Meter
			allBW, getBW, postBW, putBW, deleteBW,
			allTP, getTP, postTP, putTP, deleteTP;
	//
	protected final MBeanServer mBeanServer;
	protected final JmxReporter metricsReporter;
	private static int MAX_PAGE_SIZE;
	// METRICS section END
	private long metricsUpdatePeriodSec;
	public WSMockServlet(final RunTimeConfig runTimeConfig) {
		//
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
		counterAllSucc = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				ALL_METHODS, METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterAllFail = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				ALL_METHODS, METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durAll = metrics.histogram(MetricRegistry.name(WSMockServlet.class,
				ALL_METHODS, LoadExecutor.METRIC_NAME_DUR));
		allTP = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				ALL_METHODS, LoadExecutor.METRIC_NAME_TP));
		allBW = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				ALL_METHODS, LoadExecutor.METRIC_NAME_BW));
		//
		counterGetSucc = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpGet.METHOD_NAME, METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterGetFail = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpGet.METHOD_NAME, METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durGet = metrics.histogram(MetricRegistry.name(WSMockServlet.class,
				HttpGet.METHOD_NAME, LoadExecutor.METRIC_NAME_DUR));
		getBW = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				HttpGet.METHOD_NAME, LoadExecutor.METRIC_NAME_BW));
		getTP = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				HttpGet.METHOD_NAME, LoadExecutor.METRIC_NAME_TP));
		//
		counterPostSucc = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpPost.METHOD_NAME, METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterPostFail = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpPost.METHOD_NAME, METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durPost = metrics.histogram(MetricRegistry.name(WSMockServlet.class,
				HttpPost.METHOD_NAME, LoadExecutor.METRIC_NAME_DUR));
		postBW = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				HttpPost.METHOD_NAME, LoadExecutor.METRIC_NAME_BW));
		postTP = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				HttpPost.METHOD_NAME, LoadExecutor.METRIC_NAME_TP));
		//
		counterPutSucc = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpPut.METHOD_NAME, METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterPutFail = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpPut.METHOD_NAME, METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durPut = metrics.histogram(MetricRegistry.name(WSMockServlet.class,
				HttpPut.METHOD_NAME, LoadExecutor.METRIC_NAME_DUR));
		putBW = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				HttpPut.METHOD_NAME, LoadExecutor.METRIC_NAME_BW));
		putTP = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				HttpPut.METHOD_NAME, LoadExecutor.METRIC_NAME_TP));
		//
		counterDeleteSucc = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpDelete.METHOD_NAME, METRIC_COUNT, LoadExecutor.METRIC_NAME_SUCC));
		counterDeleteFail = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpDelete.METHOD_NAME, METRIC_COUNT, LoadExecutor.METRIC_NAME_FAIL));
		durDelete = metrics.histogram(MetricRegistry.name(WSMockServlet.class,
				HttpDelete.METHOD_NAME, LoadExecutor.METRIC_NAME_DUR));
		deleteBW = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				HttpDelete.METHOD_NAME, LoadExecutor.METRIC_NAME_BW));
		deleteTP = metrics.meter(MetricRegistry.name(WSMockServlet.class,
				HttpDelete.METHOD_NAME, LoadExecutor.METRIC_NAME_TP));
		//
		counterHeadSucc = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpHead.METHOD_NAME, LoadExecutor.METRIC_NAME_SUCC));
		counterHeadFail = metrics.counter(MetricRegistry.name(WSMockServlet.class,
				HttpHead.METHOD_NAME, LoadExecutor.METRIC_NAME_FAIL));
		//
		metricsReporter.start();
		//
		final String apiName = runTimeConfig.getStorageApi();
		port = runTimeConfig.getInt("api." + apiName + ".port");
		LOG.info(Markers.MSG, "Setup Jetty Server instance");
		server = new Server();
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		LOG.info(Markers.MSG, "Setup Http Connector Setup");
		try (final ServerConnector httpConnector = new ServerConnector(server)) {
			httpConnector.setPort(port);
			server.addConnector(httpConnector);
		} catch (final Exception e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Creating of server connector failed");
		}
		LOG.info(Markers.MSG, "Set up a new handler");
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		LOG.info(Markers.MSG, "Add servlet");
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
			printMetrics();
			//
			server.join();
		} catch (final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupting the WSMock servlet");
		} catch (final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to start WSMock servlet");
		} finally {
			try {
				server.stop();
			} catch (final Exception e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to stop jetty");
			}
			metricsReporter.close();
		}
	}
	//
	private void printMetrics(){
		try {
			final long updatePeriodMilliSec = TimeUnit.SECONDS.toMillis(metricsUpdatePeriodSec);
			while (metricsUpdatePeriodSec > 0) {
				final Snapshot allDurSnapshot = durAll.getSnapshot();
				LOG.info(
						Markers.PERF_AVG,
						String.format(Locale.ROOT, LoadExecutor.MSG_FMT_SUM_METRICS,
								//
								WSMockServlet.class.getSimpleName(),
								counterAllSucc.getCount(), counterAllFail.getCount(),
								//
								(float) allDurSnapshot.getMin() / LoadExecutor.BILLION,
								(float) allDurSnapshot.getMedian() / LoadExecutor.BILLION,
								(float) allDurSnapshot.getMean() / LoadExecutor.BILLION,
								(float) allDurSnapshot.getMax() / LoadExecutor.BILLION,
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
				Thread.sleep(updatePeriodMilliSec);
			}
		} catch (final InterruptedException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted");
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Request handling methods ////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected final void doGet(
			final HttpServletRequest request, final HttpServletResponse response
	) throws ServletException, IOException {
		LOG.trace(Markers.MSG, " Request  method Get ");
		try (final ServletOutputStream servletOutputStream = response.getOutputStream()) {
			final String dataID = request.getRequestURI().split("/")[2];
			if (mapDataObject.containsKey(dataID)) {
				LOG.trace(Markers.MSG, "   Send data object ", dataID);
				final BasicWSObject object = mapDataObject.get(dataID);
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
				response.setStatus(HttpServletResponse.SC_OK);
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
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Servlet output stream failed");
		} catch (final ArrayIndexOutOfBoundsException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			counterAllFail.inc();
			counterGetFail.inc();
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Request URI is not correct. Data object ID doesn't exist in request URI");
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
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Servlet output stream failed");
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
		try (final ServletInputStream servletInputStream = request.getInputStream()) {
			//
			long nanoTime = System.nanoTime();
			final long bytes = calcInputByteCount(servletInputStream);
			nanoTime = System.nanoTime() - nanoTime;
			//
			dataID = request.getRequestURI().split("/")[2];
			if(Base64.isBase64(dataID) && dataID.length() < 12) {
				final byte dataIdBytes[] = Base64.decodeBase64(dataID);
				offset  = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(dataIdBytes).getLong(0);
			} else {
				offset = Long.valueOf(dataID, 0x10);
			}
			final BasicWSObject dataObject = new BasicWSObject(dataID, offset, bytes);
			mapDataObject.put(dataID,dataObject);
			//
			durPut.update(nanoTime);
			durAll.update(nanoTime);
			counterAllSucc.inc();
			counterPutSucc.inc();
			putBW.mark(bytes);
			allBW.mark(bytes);
			putTP.mark();
			allTP.mark();
			//
		} catch (final IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			counterAllFail.inc();
			counterPutFail.inc();
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Servlet output stream failed");
		}catch (final NumberFormatException e){
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			counterAllFail.inc();
			counterPutFail.inc();
			ExceptionHandler.trace(
					LOG, Level.ERROR, e,
					String.format("Unexpected object id format: \"%s\"", dataID)
			);
		}catch (final ArrayIndexOutOfBoundsException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			counterAllFail.inc();
			counterPutFail.inc();
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Request URI is not correct. Data object ID doesn't exist in request URI");
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
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Servlet output stream failed");
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
	//
}
