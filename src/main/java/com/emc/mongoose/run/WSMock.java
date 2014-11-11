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
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
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
public final class WSMock
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final RunTimeConfig runTimeConfig;
	private final Map<String, BasicWSObject> mapDataObject = new HashMap<String,BasicWSObject>();
	//
	public WSMock(final RunTimeConfig runTimeConfig) {
		this.runTimeConfig = runTimeConfig;
	}
	//
	public final void run() {
		final String apiName = runTimeConfig.getStorageApi();
		final int port = runTimeConfig.getInt("api."+apiName+".port");
		LOG.debug(Markers.MSG, "Create map of BasicWSObject");
		createMapDataObject();
		LOG.debug(Markers.MSG, "Setup Jetty Server instance");
		final Server server = new Server();
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		LOG.debug(Markers.MSG, "Setup Http Connector Setup");
		try(final ServerConnector httpConnector = new ServerConnector(server)) {
			httpConnector.setPort(port);
			server.addConnector(httpConnector);
		}catch (final Exception e){
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Creating of server connector failed");
		}
		LOG.debug(Markers.MSG, "Set up a new handler");
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		LOG.debug(Markers.MSG, "Add servlet");
		context.addServlet(new ServletHolder(new SimpleWSMockServlet(mapDataObject)),"/*");
		//
		try {
            server.start();
            LOG.info(Markers.MSG, "Listening on port #{}", port);
            server.join();
        } catch (final Exception e) {
            ExceptionHandler.trace(LOG, Level.WARN, e, "WSMock was interrupted");
        } finally {
            try {
                server.stop();

            } catch (final Exception e) {
                ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to stop jetty");
            }
        }
	}
	//
	private void createMapDataObject(){
		final Path pathDataItemCSV = Paths.get(runTimeConfig.getDataSrcFPath());
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
	//
	@SuppressWarnings("serial")
	public final static class SimpleWSMockServlet extends HttpServlet {
		private Map<String, BasicWSObject> mapDataObject;
		//
		public SimpleWSMockServlet(final Map<String, BasicWSObject> map){
			this.mapDataObject = map;
		}
		//
		@Override
		protected final void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
		{
			System.out.println("get");
			LOG.trace(Markers.MSG, " Request  method Get ");
			final String dataID = request.getRequestURI().split("/")[2];
			try(final ServletOutputStream servletOutputStream = response.getOutputStream()) {
				LOG.trace(Markers.MSG, "   Send data object ", dataID);
				mapDataObject.get(dataID).writeTo(servletOutputStream);
				LOG.trace(Markers.MSG, "   Response: OK");
				response.setStatus(HttpServletResponse.SC_OK);
			}catch (final IOException e){
				ExceptionHandler.trace(LOG, Level.ERROR, e, "Servlet output stream failed");
			}
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
		}
		@Override
		protected final void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
		{
			LOG.trace(Markers.MSG, " Request  method Delete ");
			response.setStatus(HttpServletResponse.SC_OK);
		}
		@Override
		protected final void doHead(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
		{
			LOG.trace(Markers.MSG, " Request  method Head ");
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}
}
