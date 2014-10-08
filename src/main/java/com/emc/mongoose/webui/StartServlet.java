package com.emc.mongoose.webui;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.object.load.WSLoadBuilder;
import com.emc.mongoose.object.load.WSLoadBuilderImpl;
import com.emc.mongoose.object.load.WSLoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 01/10/14.
 */
public class StartServlet extends HttpServlet {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
		//
        final Request.Type loadType = Request.Type.valueOf(
			RunTimeConfig.getString("scenario.single.load").toUpperCase()
		);
		//
        final WSLoadBuilder<WSObjectImpl, WSLoadExecutor<WSObjectImpl>>
			loadBuilder = new WSLoadBuilderImpl<>();
        loadBuilder.setLoadType(loadType);
		//
        final WSLoadExecutor loadExecutor = loadBuilder.build();
		//
		loadExecutor.start();
		//
        final String timeOutString = RunTimeConfig.getString("run.time");
        final String[] timeOutArray = timeOutString.split("\\.");
		//
        try {
            loadExecutor.join(TimeUnit.valueOf(timeOutArray[1].toUpperCase()).toMillis(Integer.valueOf(timeOutArray[0])));
        } catch (InterruptedException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Interrupted");
        }
		//
        loadExecutor.close();
    }
	//
}
