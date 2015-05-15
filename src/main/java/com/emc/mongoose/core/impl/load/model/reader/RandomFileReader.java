package com.emc.mongoose.core.impl.load.model.reader;
//mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by olga on 08.05.15.
 */
public final class RandomFileReader
extends BufferedReader {

	private final static Logger LOG = LogManager.getLogger();

	private final List<String> lines;
	private final Random random = new Random();
	private final long batchSize;
	private final long maxCount;
	private long count;
	private boolean eof;

	public RandomFileReader(final Reader in, final long batchSize, final long maxCount)
	throws IOException {
		super(in);

		LOG.trace(LogUtil.MSG, "Read data items randomly");

		this.batchSize = batchSize;
		this.maxCount = maxCount;
		this.lines = new ArrayList<>();
		count = 0;
		eof = false;
	}

	@Override
	public final String readLine()
	throws IOException {
		fillUp();
		//
		if(lines.isEmpty()) {
			return null;
		} else {
			final int i = random.nextInt(lines.size());
			return lines.remove(i);
		}
	}

	private void fillUp()
	throws IOException {
		while(!eof && (count < maxCount) && (lines.size() < batchSize)) {
			final String line = super.readLine();

			if((line == null) || line.isEmpty()) {
				eof = true;
				break;
			}

			count++;
			lines.add(line);
		}
	}
}
