package com.emc.mongoose.common.net.http.content;
//
import org.apache.http.nio.ContentEncoder;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
/**
 Created by kurila on 20.05.15.
 */
public final class OutputChannel
implements WritableByteChannel {
	//
	private final ContentEncoder contentEncoder;
	//
	public OutputChannel(final ContentEncoder contentEncoder) {
		this.contentEncoder = contentEncoder;
	}
	//
	@Override
	public final int write(final ByteBuffer src)
	throws IOException {
		return contentEncoder.write(src);
	}
	//
	@Override
	public final void close()
	throws IOException {
		if(!contentEncoder.isCompleted()) {
			contentEncoder.complete();
		}
	}
	//
	@Override
	public final boolean isOpen() {
		return !contentEncoder.isCompleted();
	}
}
