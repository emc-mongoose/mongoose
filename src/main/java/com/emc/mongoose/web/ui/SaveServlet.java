package com.emc.mongoose.web.ui;

import com.emc.mongoose.run.JettyRunner;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.DirectoryLoader;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by gusakk on 12/28/14.
 */
public class SaveServlet extends HttpServlet {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private RunTimeConfig runTimeConfig;
	//
	@Override
	public void init() throws ServletException {
		super.init();
		runTimeConfig = Main.RUN_TIME_CONFIG.get().clone();
	}
	//
	@Override
	public void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		System.out.println(request.getParameter("operation"));
		/*setupRunTimeConfig(request);
		DirectoryLoader.updatePropertiesFromDir(Paths.get(Main.DIR_ROOT, Main.DIR_CONF, Main.DIR_PROPERTIES),
			runTimeConfig, true);
		response.setStatus(HttpServletResponse.SC_OK);*/
	}
	//
	@Override
	public void doGet(final HttpServletRequest request, final HttpServletResponse response) {
		setupRunTimeConfig(request);
		final File file = Paths.get(Main.DIR_ROOT, JettyRunner.DIR_WEBAPP, JettyRunner.DIR_CONF).toFile();
		if (!file.mkdirs()) {
			if (!file.exists()) {
				LOG.error(Markers.ERR, "Can't create folders for ui config");
				return;
			}
		}
		try (final FileWriter writer = new FileWriter(file + "conf")) {
			final PropertiesConfiguration props = new PropertiesConfiguration();
			for (String key : runTimeConfig.getUserKeys()) {
				props.setProperty(key, runTimeConfig.getProperty(key));
			}
			try {
				props.save(writer);
			} catch (final ConfigurationException e) {
				ExceptionHandler.trace(LOG, Level.ERROR, e, "Configuration exception");
			}
		} catch (final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Can't write properties to ui config file");
		}
	}
	//
	private void setupRunTimeConfig(final HttpServletRequest request) {
		for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			if (entry.getValue()[0].trim().isEmpty()) {
				continue;
			}
			if (entry.getValue().length > 1) {
				runTimeConfig.set(entry.getKey(), convertArrayToString(entry.getKey(), entry.getValue()));
				continue;
			}
			runTimeConfig.set(entry.getKey(), entry.getValue()[0].trim());
		}
	}
	//
	private String convertArrayToString(final String key, final String[] stringArray) {
		final String resultString = Arrays.toString(stringArray)
				.replace("[", "")
				.replace("]", "")
				.replace(" ", "")
				.trim();
		if (key.equals("run.time"))
			return resultString.replace(",", ".");
		return resultString;
	}

}
