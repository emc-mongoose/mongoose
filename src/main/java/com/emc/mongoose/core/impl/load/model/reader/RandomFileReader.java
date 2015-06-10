package com.emc.mongoose.core.impl.load.model.reader;
//mongoose-common.jar
//
import com.emc.mongoose.common.logging.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Random;
import java.util.Vector;

/**
 * Created by olga on 08.05.15.
 */
public final class RandomFileReader
extends BufferedReader {

	private final static Logger LOG = LogManager.getLogger();

	private final Vector<String> linesBuffer;
	private final Random random = new Random();
	private final long maxCount;
	private long count;
	private boolean isEOF;

	public RandomFileReader(final Reader in, final int batchSize, final long maxCount)
	throws IOException {
		super(in);

		LOG.trace(Markers.MSG, "Read data items randomly");

		this.maxCount = maxCount;
		this.linesBuffer = new Vector<>(batchSize);
		count = 0;
		isEOF = false;
	}

	@Override
	public final String readLine()
	throws IOException {
		if(linesBuffer.capacity() == 0) {
			return super.readLine();
		}
		//
		fillUp();
		//
		if(linesBuffer.isEmpty()) {
			return null;
		} else {
			final int i = random.nextInt(linesBuffer.size());
			return linesBuffer.remove(i);
		}
	}

	private void fillUp()
	throws IOException {
		while(!isEOF && (count < maxCount) && (linesBuffer.size() < linesBuffer.capacity())) {
			final String line = super.readLine();

			if((line == null) || line.isEmpty()) {
				isEOF = true;
				break;
			}

			count++;
			linesBuffer.add(line);
		}
	}
}
