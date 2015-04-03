package com.emc.mongoose.webui;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.TraceLogger;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
//
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Created by gusakk on 1/16/15.
 */
public abstract class CommonServlet extends HttpServlet {
	//
	private final static Logger LOG = LogManager.getLogger();
	private static volatile RunTimeConfig LAST_RUN_TIME_CONFIG;
	//
	public static ConcurrentHashMap<String, Thread> THREADS_MAP;
	public static ConcurrentHashMap<String, Boolean> STOPPED_RUN_MODES;
	public static ConcurrentHashMap<String, String> CHARTS_MAP;
	//
	protected RunTimeConfig runTimeConfig;
	//
	static {
		THREADS_MAP = new ConcurrentHashMap<>();
		STOPPED_RUN_MODES = new ConcurrentHashMap<>();
		CHARTS_MAP = new ConcurrentHashMap<>();
		LAST_RUN_TIME_CONFIG = RunTimeConfig.getContext().clone();
	}
	//
	@Override
	public void init() {
		try {
			super.init();
			runTimeConfig = ((RunTimeConfig) getServletContext().getAttribute("runTimeConfig")).clone();
		} catch (final ServletException e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "Interrupted servlet init method");
		}
	}
	//
	protected void setupRunTimeConfig(final HttpServletRequest request) {
		for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			if (entry.getValue()[0].trim().isEmpty()) {
				continue;
			}
			if (entry.getValue().length > 1) {
				runTimeConfig.set(entry.getKey(), convertArrayToString(entry.getValue()));
				continue;
			}
			runTimeConfig.set(entry.getKey(), entry.getValue()[0].trim());
		}
	}
	//
	protected String convertArrayToString(final String[] stringArray) {
		return Arrays.toString(stringArray)
				.replace("[", "")
				.replace("]", "")
				.replace(" ", "")
				.trim();
	}
	//
	public static void updateLastRunTimeConfig(final RunTimeConfig runTimeConfig) {
		LAST_RUN_TIME_CONFIG = runTimeConfig.clone();
	}
	//
	public static RunTimeConfig getLastRunTimeConfig() {
		return LAST_RUN_TIME_CONFIG;
	}
}
