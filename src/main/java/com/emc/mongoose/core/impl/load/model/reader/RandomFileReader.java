package com.emc.mongoose.core.impl.load.model.reader;
//mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by olga on 08.05.15.
 */
public final class RandomFileReader
extends FileReader{

	private final static Logger LOG = LogManager.getLogger();
	private final List<String> dataItemsList = new ArrayList<>();
	private String nextLine;
	final Random random = new Random();


	public RandomFileReader(final Path fPath)
	throws IOException
	{
		super(fPath);
		final int batchSize = RunTimeConfig.getContext().getDataRandomBatchSize();
		LOG.trace(LogUtil.MSG, "Read data items randomly");
		while ((nextLine = fReader.readLine()) != null && dataItemsList.size() < batchSize){
			LOG.trace(LogUtil.MSG, "Got next line #{}", nextLine);
			dataItemsList.add(nextLine);
		}
	}

	@Override
	public final String getDataItemString()
	throws IOException
	{
		if (!dataItemsList.isEmpty()) {
			if (nextLine != null ) {
				LOG.trace(LogUtil.MSG, "Got next line #{}", nextLine);
				dataItemsList.add(nextLine);
				nextLine = fReader.readLine();

			}
			return dataItemsList.remove(random.nextInt(dataItemsList.size()));
		} else {
			LOG.debug(LogUtil.MSG, "Data items list is empty, exiting");
			return null;
		}
	}
}
