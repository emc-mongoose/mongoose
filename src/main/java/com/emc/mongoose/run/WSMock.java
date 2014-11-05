package com.emc.mongoose.run;
//
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.data.impl.BasicObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
//
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletHandler;
//
import javax.servlet.ServletException;
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

	private final static Logger LOG = LogManager.getLogger();
	private final RunTimeConfig runTimeConfig;
	private final Map<String, DataObject> mapDataObject = new HashMap<String,DataObject>();
	//
	public WSMock(final RunTimeConfig runTimeConfig) {
		this.runTimeConfig = runTimeConfig;
	}
	//
	public void run() {
		final String apiName = runTimeConfig.getStorageApi();
		final int port = runTimeConfig.getInt("api."+apiName+".port");
		//Create Map
		createDataObjectMap();
		// Setup Jetty Server instance
		final Server server = new Server();
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		// Http Connector Setup
		final ServerConnector httpConnector = new ServerConnector(server);
		httpConnector.setPort(port);
		server.addConnector(httpConnector);
		//Set a new handler
		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);
		//server.setHandler(new SimpleHandler());
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
	private void createDataObjectMap(){
		final Path pathDtaItemCSV = Paths.get(runTimeConfig.getDataSrcFPath());
		try {
			final BufferedReader fReader = Files.newBufferedReader(pathDtaItemCSV, StandardCharsets.UTF_8);
			String nextLine = fReader.readLine();
			while(nextLine!=null){
				//
				LOG.trace(Markers.MSG, "Got next line: \"{}\"", nextLine);
				//
				final BasicObject nextData = new BasicObject(nextLine);
				System.out.println(nextData.getId()+" "+ nextData.getOffset());
				mapDataObject.put(nextData.getId(),nextData);
				nextLine = fReader.readLine();
			}
		} catch (IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to read line from the file");
		}
	}

	@SuppressWarnings("serial")
	private final static class SimpleHandler
	extends AbstractHandler {

		@Override
		public final void handle(
			final String target, final Request baseRequest, final HttpServletRequest request,
			final HttpServletResponse response
		) throws IOException, ServletException {
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
		}
	}
	//
	@SuppressWarnings("serial")
	public static class HelloServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
		{
			LOG.info(Markers.MSG, "   doGet called with URI: ", request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_OK);
			//response.getWriter().println();
		}
		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
		{
			LOG.info(Markers.MSG, "   doPost called with URI: ", request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_OK);
		}
		@Override
		protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
		{
			LOG.info(Markers.MSG, "   doPost called with URI: ", request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_OK);
		}
		@Override
		protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
		{
			LOG.info(Markers.MSG, "   doPost called with URI: ", request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}
}
