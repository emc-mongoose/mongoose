package com.emc.mongoose.webui;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.object.load.WSLoadBuilder;
import com.emc.mongoose.object.load.WSLoadBuilderImpl;
import com.emc.mongoose.object.load.WSLoadExecutor;
import com.emc.mongoose.object.load.client.WSLoadBuilderClientImpl;
import com.emc.mongoose.object.load.client.WSLoadClient;
import com.emc.mongoose.object.load.server.WSLoadBuilderSvc;
import com.emc.mongoose.object.load.server.WSLoadBuilderSvcImpl;
import com.emc.mongoose.run.WSMock;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;

import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
import org.apache.commons.configuration.ConversionException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 01/10/14.
 */
public class StartServlet extends HttpServlet {

    private final static Logger LOG = LogManager.getLogger();
    private static List<Thread> threads;
    public static long threadId;

    @Override
    public void init() throws ServletException {
        super.init();
        threads = new CopyOnWriteArrayList<>();
    }

    //
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
		//
        final String runMode = request.getParameter("runmode");
        //
        LOG.info(Markers.MSG, "Launch mode is \"{}\"", RunModes.valueOf(runMode).getValue());
        //
        switch (RunModes.valueOf(runMode)) {
            case VALUE_RUN_MODE_SERVER:
                LOG.debug(Markers.MSG, "Starting the server");
                threadId = runServer();
                break;
            case VALUE_RUN_MODE_STANDALONE:
                LOG.debug(Markers.MSG, "Starting the standalone");
                threadId = runStandalone();
                break;
            case VALUE_RUN_MODE_WSMOCK:
                LOG.debug(Markers.MSG, "Starting the web storage mock");
                threadId = runWSMock();
                break;
            case VALUE_RUN_MODE_CLIENT:
                LOG.debug(Markers.MSG, "Starting the client");
                threadId = runClient();
                break;
            default:
                LOG.debug(Markers.MSG, "Starting the standalone");
                threadId = runStandalone();
                break;
        }
    }
	//

    private long runServer() {
        Thread thread = new Thread() {
            final WSLoadBuilderSvc loadBuilderSvc = new WSLoadBuilderSvcImpl();
            @Override
            public void run() {
                try {
                    loadBuilderSvc.start();
                } catch (final RemoteException e) {
                    ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to start load builder service");
                }
            }
            @Override
            public void interrupt() {
                ServiceUtils.close(loadBuilderSvc);
                super.interrupt();
            }
        };
        thread.start();
        threads.add(thread);
        return thread.getId();
    }

    private long runClient() {
        //
        Thread thread = new Thread() {
            WSLoadClient loadClient;
            @Override
            public void run() {
                try {
                    final WSLoadBuilderClientImpl<WSObjectImpl, WSLoadClient<WSObjectImpl>> loadBuilderClient = new WSLoadBuilderClientImpl<>();
                    loadBuilderClient.setProperties(new RunTimeConfig());
                    //
                    try {
                        final Request.Type loadType = Request.Type.valueOf(
                                RunTimeConfig.getString("scenario.single.load").toUpperCase()
                        );
                        loadBuilderClient.setLoadType(loadType);
                    } catch (NoSuchElementException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "No load type specified, try arg -Dscenario.single.load=<VALUE> to override");
                    } catch (IllegalArgumentException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "No such load type, it should be a constant from Load.Type enumeration");
                    }
                    //
                    //final WSLoadExecutor loadExecutor = loadBuilder.build();
                    loadClient = loadBuilderClient.build();
                    //
                    final String timeOutString;
                    final String[] timeOutArray;
                    //
                    try {
                        timeOutString = RunTimeConfig.getString("run.time");
                        timeOutArray = timeOutString.split("\\.");
                    } catch (NoSuchElementException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "No timeout specified, try arg -Drun.time=<INTEGER>.<UNIT> to override");
                        return;
                    } catch (IllegalArgumentException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "Timeout unit should be a name of a constant from TimeUnit enumeration");
                        return;
                    } catch (IllegalStateException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "Time unit should be specified with timeout value (following after \".\" separator)");
                        return;
                    }
                    //
                    loadClient.start();
                    //
                    try {
                        loadClient.join(TimeUnit.valueOf(timeOutArray[1].toUpperCase()).toMillis(Integer.valueOf(timeOutArray[0])));
                    } catch (InterruptedException e) {
                        ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted");
                    }
                    //
                    LOG.info(Markers.MSG, "Scenario end");
                    loadClient.close();
                } catch (final ConversionException e) {
                    ExceptionHandler.trace(LOG, Level.FATAL, e, "Servers address list should be comma delimited");
                } catch (final NoSuchElementException e) {
                    ExceptionHandler.trace(LOG, Level.FATAL, e, "Servers address list not specified, try  arg -Dremote.servers=<LIST> to override");
                } catch (final IOException e) {
                    ExceptionHandler.trace(LOG, Level.FATAL, e, "Failed to create load builder client");
                }
            }

            @Override
            public void interrupt() {
                try {
                    if (loadClient != null) {
                        loadClient.close();
                    }
                } catch (IOException e) {
                    ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to start client mode");
                }
                super.interrupt();
            }
        };
        thread.start();
        threads.add(thread);
        return thread.getId();

    }

    private long runStandalone()
    throws IOException {
        Thread thread = new Thread() {
            WSLoadExecutor loadExecutor;
            @Override
            public void run() {
                try {
                    //
                    final WSLoadBuilder<WSObjectImpl, WSLoadExecutor<WSObjectImpl>>
                            loadBuilder = new WSLoadBuilderImpl<>();
                    //
                    try {
                        final Request.Type loadType = Request.Type.valueOf(
                                RunTimeConfig.getString("scenario.single.load").toUpperCase()
                        );
                        loadBuilder.setLoadType(loadType);
                    } catch (NoSuchElementException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "No load type specified, try arg -Dscenario.single.load=<VALUE> to override");
                    } catch (IllegalArgumentException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "No such load type, it should be a constant from Load.Type enumeration");
                    }
                    //
                    //final WSLoadExecutor loadExecutor = loadBuilder.build();
                    loadExecutor = loadBuilder.build();
                    //
                    final String timeOutString;
                    final String[] timeOutArray;
                    //
                    try {
                        timeOutString = RunTimeConfig.getString("run.time");
                        timeOutArray = timeOutString.split("\\.");
                    } catch (NoSuchElementException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "No timeout specified, try arg -Drun.time=<INTEGER>.<UNIT> to override");
                        return;
                    } catch (IllegalArgumentException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "Timeout unit should be a name of a constant from TimeUnit enumeration");
                        return;
                    } catch (IllegalStateException e) {
                        ExceptionHandler.trace(LOG, Level.ERROR, e, "Time unit should be specified with timeout value (following after \".\" separator)");
                        return;
                    }
                    //
                    loadExecutor.start();
                    //
                    try {
                        loadExecutor.join(TimeUnit.valueOf(timeOutArray[1].toUpperCase()).toMillis(Integer.valueOf(timeOutArray[0])));
                    } catch (InterruptedException e) {
                        ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted");
                    }
                    //
                    LOG.info(Markers.MSG, "Scenario end");
                    loadExecutor.close();
                } catch (IOException e) {
                    ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to start standalone mode");
                }
            }

            @Override
            public void interrupt() {
               try {
                   loadExecutor.interrupt();
               } catch (RemoteException e) {
                   ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to interrupt the load executor");
               }
               //
               super.interrupt();
            }
        };

        thread.start();
        threads.add(thread);
        return thread.getId();
    }

    private long runWSMock() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                WSMock.run();
            }
        };
        thread.start();
        threads.add(thread);
        return thread.getId();
    }

    public static void interruptMongoose() {
        threads.get(threads.size() - 1).interrupt();
    }

}
