package com.emc.mongoose.core.impl.data.content;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.content.ContentSource;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
/**
 Created by kurila on 16.10.15.
 */
public final class FileContentSource
extends ContentSourceBase
implements ContentSource {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public FileContentSource()
	throws IOException, InvalidPathException {
		this(RunTimeConfig.getContext().getDataContentFPath());
	}
	//
	public FileContentSource(final String fPath)
	throws IOException, InvalidPathException {
		this(Paths.get(fPath));
	}
	//
	private FileContentSource(final Path fPath)
	throws IOException {
		this(Files.newByteChannel(fPath, StandardOpenOption.READ), Files.size(fPath));
	}
	//
	private FileContentSource(final ReadableByteChannel contentSrcChan, final long size)
	throws IOException {
		super(contentSrcChan, size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size);
	}
	//
	@Override
	public final ByteBuffer getLayer(final int layerIndex) {
		// zero layer always exists so it may be useful to do it very simply and fast
		if(layerIndex == 0) {
			return zeroByteLayer;
		}
		// else
		ByteBuffer lastLayerBytes, nextLayerBytes;
		final int
			size = zeroByteLayer.capacity(),
			wordCount = size / WORD_SIZE,
			tailByteCount = size % WORD_SIZE;
		synchronized(byteLayers) {
			for(int i = byteLayers.size(); i <= layerIndex; i ++) {
				lastLayerBytes = byteLayers.get(i - 1);
				lastLayerBytes.clear();
				nextLayerBytes = ByteBuffer.allocate/*Direct*/(size);
				// apply xorshift to each word
				for(int j = 0; j < wordCount; j ++) {
					nextLayerBytes.putLong(nextWord(lastLayerBytes.getLong()));
				}
				// append the tail bytes
				if(tailByteCount > 0) {
					final ByteBuffer tailBytes = ByteBuffer.allocate(WORD_SIZE);
					for(int j = 0; j < tailByteCount; j++) {
						tailBytes.put(lastLayerBytes.get());
					}
					for(int j = tailByteCount; j < WORD_SIZE; j ++) {
						tailBytes.put((byte) 0);
					}
					tailBytes.clear(); // reset the position
					final long tailWord = tailBytes.asLongBuffer().get();
					tailBytes.clear(); // reset the position
					tailBytes.asLongBuffer().put(nextWord(tailWord));
					tailBytes.clear(); // reset the position
					for(int j = 0; j < tailByteCount; j ++) {
						nextLayerBytes.put(tailBytes.get());
					}
				}
				byteLayers.add(nextLayerBytes);
			}
		}
		return byteLayers.get(layerIndex);
	}
}
