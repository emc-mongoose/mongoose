package com.emc.mongoose.core.impl.load.model.reader;
//mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by olga on 08.05.15.
 */
public class SimpleFileReader
extends FileReader{

	private final static Logger LOG = LogManager.getLogger();

	public SimpleFileReader(final Path fPath)
	throws IOException{
		super(fPath);
	}

	@Override
	public String getDataItemString()
	throws IOException {
		String nextLine;
		if ((nextLine = fReader.readLine()) != null){
			LOG.trace(LogUtil.MSG, "Got next line #{}", nextLine);
			return nextLine;
		} else {
			LOG.debug(LogUtil.MSG, "No next line, exiting");
			return null;
		}
	}
}
