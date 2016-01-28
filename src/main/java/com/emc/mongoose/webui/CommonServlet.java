package com.emc.mongoose.webui;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
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
public abstract class CommonServlet
extends HttpServlet {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static AppConfig DEFAULT_CFG;
	//
	private static volatile AppConfig LAST_RUN_TIME_CONFIG = null;
	public static Map<String, Thread> THREADS_MAP;
	public static Map<String, Boolean> STOPPED_RUN_MODES;
	public static Map<String, String> CHARTS_MAP;
	//
	protected AppConfig appConfig;
	//
	static {
		THREADS_MAP = new ConcurrentHashMap<>();
		STOPPED_RUN_MODES = new ConcurrentHashMap<>();
		CHARTS_MAP = new ConcurrentHashMap<>();
		DEFAULT_CFG = BasicConfig.THREAD_CONTEXT.get();
		try {
			LAST_RUN_TIME_CONFIG = (AppConfig) DEFAULT_CFG.clone();
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.FATAL, e, "Failed to clone the configuration");
		}
	}
	//
	@Override
	public void init() {
		try {
			super.init();
			appConfig = (AppConfig) (
				(AppConfig) getServletContext().getAttribute("appConfig")
			).clone();
		} catch(final ServletException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Interrupted servlet init method");
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the configuration");
		}
	}
	//
	protected void setupAppConfig(final HttpServletRequest request) {
		for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			if (entry.getValue()[0].trim().isEmpty()) {
				final String[] defaultPropValue = DEFAULT_CFG.getStringArray(entry.getKey());
				if (defaultPropValue.length > 0 && !entry.getKey().equals(AppConfig.KEY_RUN_ID)) {
					appConfig.setProperty(entry.getKey(), convertArrayToString(defaultPropValue));
				}
				continue;
			}
			if (entry.getValue().length > 1) {
				appConfig.setProperty(entry.getKey(), convertArrayToString(entry.getValue()));
				continue;
			}
			appConfig.setProperty(entry.getKey(), entry.getValue()[0].trim());
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
	public static void updateLastAppConfig(final AppConfig appConfig) {
		try {
			LAST_RUN_TIME_CONFIG = (AppConfig) appConfig.clone();
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the configuration");
		}
	}
	//
	public static AppConfig getLastAppConfig() {
		return LAST_RUN_TIME_CONFIG;
	}
}
