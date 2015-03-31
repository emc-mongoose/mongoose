package com.emc.mongoose.webui;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.conf.JsonConfigLoader;
//
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Scanner;
/**
 * Created by gusakk on 12/28/14.
 */
public class SaveServlet extends CommonServlet {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String FILENAME = "config.txt";
	private final static File FILE_PATH = Paths.get(
		RunTimeConfig.DIR_ROOT, Constants.DIR_WEBAPP, Constants.DIR_CONF
	).toFile();
	//	HTTP Headers
	private final static String CONTENT_TYPE = "Content-Type";
	private final static String CONTENT_LENGTH = "Content-Length";
	private final static String CONTENT_DISPOSITION = "Content-Disposition";
	//
	@Override
	public void doGet(final HttpServletRequest request, final HttpServletResponse response) {
		if (!request.getParameterMap().isEmpty()) {
			setupRunTimeConfig(request);
			saveConfigInSeparateFile();
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}
		final File fullFileName = new File(FILE_PATH.toString() + File.separator + FILENAME);
		try {
			final PrintWriter writer = response.getWriter();
			final Scanner scanner = new Scanner(fullFileName);
			while (scanner.hasNextLine()) {
				String s = scanner.nextLine();
				writer.write(s + "\n");
			}
		} catch (final IOException e) {
			LogUtil.failure(LOG, Level.ERROR, e, "IOException");
		}
		response.setHeader(CONTENT_TYPE, getServletContext().getMimeType(fullFileName.getName()));
		response.setHeader(CONTENT_LENGTH, String.valueOf(fullFileName.length()));
		response.setHeader(CONTENT_DISPOSITION, "attachment;filename=\"" + fullFileName.getName() + "\"");
	}
	//
	@Override
	public void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		setupRunTimeConfig(request);
		JsonConfigLoader.updateProps(
			Paths.get(RunTimeConfig.DIR_ROOT, Constants.DIR_CONF).resolve(RunTimeConfig.FNAME_CONF),
			runTimeConfig, true
		);
		updateLastRunTimeConfig(runTimeConfig);
		response.setStatus(HttpServletResponse.SC_OK);
	}
	//
	private void saveConfigInSeparateFile() {
		if (!FILE_PATH.mkdirs()) {
			if (!FILE_PATH.exists()) {
				LOG.error(LogUtil.ERR, "Failed to create folders for ui config");
				return;
			}
		}
		try (final FileWriter writer = new FileWriter(FILE_PATH + File.separator + FILENAME)) {
			final PropertiesConfiguration props = new PropertiesConfiguration();
			for (String key : runTimeConfig.getMongooseKeys()) {
				props.setProperty(key, runTimeConfig.getProperty(key));
			}
			try {
				props.save(writer);
			} catch (final ConfigurationException e) {
				LogUtil.failure(LOG, Level.ERROR, e, "Configuration exception");
			}
		} catch (final IOException e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Failed to write properties to ui config file");
		}
	}
}
