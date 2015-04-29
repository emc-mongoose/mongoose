package com.emc.mongoose.persist;
//mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
//
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by olga on 24.04.15.
 */
public final class PersistProducer
implements Runnable{

	private final static Logger LOG = LogManager.getLogger();

	private final static DateFormat DATE_FORMAT =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss,SSS");

	public final static String
		MSG_FILE = "messages.log",
		ERR_FILE = "errors.log",
		DATA_ITEM_FILE = "data.items.csv",
		PERF_AVG_FILE = "perf.avg.log",
		PERF_SUM_FILE = "perf.sum.log",
		PERF_TRACE_FILE = "perf.trace.csv";
	//
	final RunHolder runHolder;
	//
	public PersistProducer(final RunHolder runHolder){
		this.runHolder = runHolder;
	}
	//
	@Override
	public void run(){
		System.out.println("producer run");
		File dirRunName = new File(Paths.get(LogUtil.PATH_LOG_DIR, runHolder.getRunName()).toUri());
		for(final File file: dirRunName.listFiles()){
			switch (file.getName()){
				case MSG_FILE:
					LOG.debug(LogUtil.MSG, "Start persist messages.log and errors.log files");
					readMessageFile(file);
					LOG.debug(LogUtil.MSG, "Finish persist messages.log and errors.log files");
					break;
				case ERR_FILE:
					break;
				case DATA_ITEM_FILE:
					//TODO
					break;
				case PERF_AVG_FILE:
					//TODO
					break;
				case PERF_SUM_FILE:
					//TODO
					break;
				case PERF_TRACE_FILE:
					//TODO
					break;
			}
		}
	}
	//
	private static Scanner getScanner(final File file, final String separator){
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
			scanner.useDelimiter(Pattern.compile(separator));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return scanner;
	}
	//parse String to Date
	private static Date getTimestamp(final String stringTimestamp){
		Date runTimestamp = null;
		try {
			runTimestamp = DATE_FORMAT.parse(stringTimestamp);
		} catch (final ParseException e) {
			LogUtil.failure(StatusLogger.getLogger(), Level.ERROR, e, "Parse run timestamp is failed");
		}catch (final NullPointerException e){
			LogUtil.failure(StatusLogger.getLogger(), Level.ERROR, e, "Parse run timestamp is failed. " +
				"Timestamp equals null.");
		}
		return runTimestamp;
	}

	private void readMessageFile(final File file){
		String message;
		final Scanner scanner = getScanner(file, "[\\r\\n\\|]+");
		String nextLine = scanner.next();
		PersistEvent event;
		while (scanner.hasNext()){
			event = new PersistEvent(LogUtil.MSG, runHolder.getRunName());
			message = "";
			event.setEventTstamp(getTimestamp(nextLine));
			event.setEventLevel(scanner.next());
			event.setEventClass(scanner.next());
			event.setEventThread(scanner.next());
			while (scanner.hasNext()){
				nextLine = scanner.next();
				if (isValidDate(nextLine)){
					break;
				}
				if (nextLine.contains(Constants.RUN_MODE_CINDERELLA) ||
					nextLine.contains(Constants.RUN_MODE_PERSIST) ||
					nextLine.contains(Constants.RUN_MODE_WSMOCK) ||
					nextLine.contains(Constants.RUN_MODE_WEBUI)
				) {
					//TODO
					System.out.println("Wow!");
				}
				if (nextLine.contains(Constants.RUN_MODE_PERSIST)){
					handleRun(Constants.RUN_MODE_PERSIST);
				}
				if (nextLine.contains(Constants.RUN_MODE_CLIENT)){
					handleRun(Constants.RUN_MODE_CLIENT);
				}
				if (nextLine.contains(Constants.RUN_MODE_COMPAT_CLIENT)) {
					handleRun(Constants.RUN_MODE_COMPAT_CLIENT);
				}
				if (nextLine.contains(Constants.RUN_MODE_COMPAT_SERVER)) {
					handleRun(Constants.RUN_MODE_COMPAT_SERVER);
				}
				if (nextLine.contains(Constants.RUN_MODE_SERVER)) {
					handleRun(Constants.RUN_MODE_SERVER);
				}
				if (nextLine.contains(Constants.RUN_MODE_STANDALONE)) {
					handleRun(Constants.RUN_MODE_STANDALONE);
				}
				message += nextLine;
			}
			event.setEventMessage(message);
			runHolder.addEvent(event);
		}
		scanner.close();
	}
	//
	private void handleRun(final String modeName){
		final PersistDAO persistDAO = new PersistDAO();
		persistDAO.persistRunEntiry(runHolder.getRunName(), modeName);
		persistDAO.closeEntityMF();
		runHolder.initRun();
	}
	//
	public static boolean isValidDate(String inDate) {
		DATE_FORMAT.setLenient(false);
		try {
			DATE_FORMAT.parse(inDate.trim());
		} catch (ParseException pe) {
			return false;
		}
		return true;
	}
	//
}
