package com.emc.mongoose.core.impl.load.model.reader;
//mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.Reader;

/**
 * Created by olga on 08.05.15.
 */
public final class SimpleFileReader
extends FileReader{

	private final static Logger LOG = LogManager.getLogger();

	public SimpleFileReader(final Reader in)
	throws IOException{
		super(in);
	}

	@Override
	public final String getLine()
	throws IOException {
		final String nextLine = readLine();
		if (nextLine != null && !nextLine.isEmpty()){
			LOG.trace(LogUtil.MSG, "Got next line #{}", nextLine);
			return nextLine;
		} else {
			LOG.debug(LogUtil.MSG, "No next line, exiting");
			return null;
		}
	}
}
