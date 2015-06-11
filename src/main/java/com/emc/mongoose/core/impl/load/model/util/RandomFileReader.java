package com.emc.mongoose.core.impl.load.model.util;
// mongoose-common.jar
import com.emc.mongoose.common.log.Markers;
// mongoose-core-impl.jar
import com.emc.mongoose.core.api.load.model.util.LineReader;
import com.emc.mongoose.core.api.load.model.util.Randomizer;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Vector;
/**
 * Created by olga on 08.05.15.
 */
public final class RandomFileReader
implements AutoCloseable{

	private static final Logger LOG = LogManager.getLogger();

	private final LineReader reader;
	private final Vector<String> linesBuffer;
	private final long maxCount;
	private final Randomizer random;

	private long count;
	private boolean isEOF;

	public RandomFileReader(final LineReader reader, final int batchSize, final long maxCount,
		final Randomizer random)
	throws IOException {
		LOG.trace(Markers.MSG, "Read data items randomly");
		this.reader = reader;
		this.linesBuffer = new Vector<>(batchSize);
		this.maxCount = maxCount;
		this.random = random;
		count = 0;
		isEOF = false;
	}
	//
	public final String readLine()
	throws IOException {
		if(linesBuffer.capacity() == 0) {
			return reader.readLine();
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
			final String line = reader.readLine();

			if((line == null) || line.isEmpty()) {
				isEOF = true;
				break;
			}

			count++;
			linesBuffer.add(line);
		}
	}

	@Override
	public void close() throws Exception {
		reader.close();
	}
}
