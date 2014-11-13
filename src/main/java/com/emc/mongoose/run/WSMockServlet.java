package com.emc.mongoose.run;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.web.data.impl.BasicWSObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
//
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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
	private final String dataSrcFPath;
	private final Map<String, BasicWSObject> mapDataObject = new HashMap<>();
	//
	public WSMockServlet(final RunTimeConfig runTimeConfig) {
		final String apiName = runTimeConfig.getStorageApi();
		dataSrcFPath = runTimeConfig.getDataSrcFPath();
		port = runTimeConfig.getInt("api." + apiName + ".port");
		LOG.debug(Markers.MSG, "Create map of BasicWSObject");
		createMapDataObject();
		LOG.debug(Markers.MSG, "Setup Jetty Server instance");
		server = new Server();
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		LOG.debug(Markers.MSG, "Setup Http Connector Setup");
		try(final ServerConnector httpConnector = new ServerConnector(server)) {
			httpConnector.setPort(port);
			server.addConnector(httpConnector);
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Creating of server connector failed");
		}
		LOG.debug(Markers.MSG, "Set up a new handler");
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		LOG.debug(Markers.MSG, "Add servlet");
		context.addServlet(new ServletHolder(this), "/*");
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private void createMapDataObject(){
		final Path pathDataItemCSV = Paths.get(dataSrcFPath);
		try {
			if (!pathDataItemCSV.toString().isEmpty()) {
				final BufferedReader fileReader = Files.newBufferedReader(pathDataItemCSV, StandardCharsets.UTF_8);
				String nextLine;
				do {
					nextLine = fileReader.readLine();
					if (nextLine == null || nextLine.isEmpty()) {
						break;
					} else {
						LOG.trace(Markers.MSG, "Got next line: \"{}\"", nextLine);
						final BasicWSObject nextData = new BasicWSObject(nextLine);
						mapDataObject.put(nextData.getId(), nextData);
					}
				} while (true);
			}
		} catch (final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to read line from the file");
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Runnable implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void run() {
		try {
			server.start();
			LOG.info(Markers.MSG, "Listening on port #{}", port);
			server.join();
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupting the WSMock servlet");
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to start WSMock servlet");
		} finally {
			try {
				server.stop();
			} catch (final Exception e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to stop jetty");
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Request handling methods ////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected final void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Get ");
		try(final ServletOutputStream servletOutputStream = response.getOutputStream()) {
			final String dataID = request.getRequestURI().split("/")[2];
			LOG.trace(Markers.MSG, "   Send data object ", dataID);
			// TODO set start nano timestamp
			mapDataObject.get(dataID).writeTo(servletOutputStream);
			// TODO get nano duration and update the histogram
			LOG.trace(Markers.MSG, "   Response: OK");
			response.setStatus(HttpServletResponse.SC_OK);
			// TODO ok
		} catch (final IOException e){
			// TODO fail
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Servlet output stream failed");
		} // TODO catch no id condition and response 404
	}
	@Override
	protected final void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Post ");
		response.setStatus(HttpServletResponse.SC_OK);
	}
	@Override
	protected final void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Put ");
		response.setStatus(HttpServletResponse.SC_OK);
		// TODO set start nano timestamp
		// TODO read request content as fast as possible
		// TODO get nano duration and update the histogram
	}
	@Override
	protected final void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Delete ");
		// 
		response.setStatus(HttpServletResponse.SC_OK);
	}
	@Override
	protected final void doHead(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		LOG.trace(Markers.MSG, " Request  method Head ");
		response.setStatus(HttpServletResponse.SC_OK);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
