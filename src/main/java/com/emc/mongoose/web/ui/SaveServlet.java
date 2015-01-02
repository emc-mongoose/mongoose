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
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.util.FileUtils;
import org.eclipse.jetty.util.Jetty;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by gusakk on 12/28/14.
 */
public class SaveServlet extends HttpServlet {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String FILENAME = "config.txt";
	private final static File FILE_PATH = Paths.get(Main.DIR_ROOT, JettyRunner.DIR_WEBAPP, JettyRunner.DIR_CONF).toFile();
	//	HTTP Headers
	private final static String CONTENT_TYPE = "Content-Type";
	private final static String CONTENT_LENGTH = "Content-Length";
	private final static String CONTENT_DISPOSITION = "Content-Disposition";
	//
	private RunTimeConfig runTimeConfig;
	//
	@Override
	public void init() throws ServletException {
		super.init();
		runTimeConfig = Main.RUN_TIME_CONFIG.get().clone();
	}
	@Override
	public void doGet(final HttpServletRequest request, final HttpServletResponse response) {
		//
		setupRunTimeConfig(request);
		saveConfigInSeparateFile();
		//
		final File fullFileName = new File(FILE_PATH.toString() + File.separator + FILENAME);
		try {
			final PrintWriter writer = response.getWriter();
			final Scanner scanner = new Scanner(fullFileName);
			while (scanner.hasNextLine()) {
				String s = scanner.nextLine();
				writer.write(s + "\n");
			}
		} catch (final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "IOException");
		}
		response.setHeader(CONTENT_TYPE, getServletContext().getMimeType(fullFileName.getName()));
		response.setHeader(CONTENT_LENGTH, String.valueOf(fullFileName.length()));
		response.setHeader(CONTENT_DISPOSITION, "attachment;filename=\"" + fullFileName.getName() + "\"");
	}
	//
	@Override
	public void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		setupRunTimeConfig(request);
		DirectoryLoader.updatePropertiesFromDir(Paths.get(Main.DIR_ROOT, Main.DIR_CONF, Main.DIR_PROPERTIES),
				runTimeConfig, true);
		//TODO fix this ugly code
		StartServlet.LAST_RUN_TIME_CONFIG = runTimeConfig;
		//
		response.setStatus(HttpServletResponse.SC_OK);
	}
	//
	private void saveConfigInSeparateFile() {
		if (!FILE_PATH.mkdirs()) {
			if (!FILE_PATH.exists()) {
				LOG.error(Markers.ERR, "Can't create folders for ui config");
				return;
			}
		}
		try (final FileWriter writer = new FileWriter(FILE_PATH + File.separator + FILENAME)) {
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
