package com.emc.mongoose.util.docker;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import java.io.Closeable;
import java.io.IOException;

/** Created by andrey on 25.09.17. */
public final class ContainerOutputCallback implements ResultCallback<Frame> {

	private final StringBuilder stdOutBuff;
	private final StringBuilder stdErrBuff;

	private Closeable stream = null;

	public ContainerOutputCallback(final StringBuilder stdOutBuff, final StringBuilder stdErrBuff) {
		this.stdOutBuff = stdOutBuff;
		this.stdErrBuff = stdErrBuff;
	}

	@Override
	public final void onStart(final Closeable stream) {
		this.stream = stream;
	}

	@Override
	public final void onNext(final Frame object) {
		final StreamType streamType = object.getStreamType();
		final String payload = new String(object.getPayload());
		if (StreamType.STDOUT.equals(streamType)) {
			System.out.print(payload);
			if (stdOutBuff != null) {
				stdOutBuff.append(payload);
			}
		} else if (StreamType.STDERR.equals(streamType)) {
			System.err.print(payload);
			if (stdErrBuff != null) {
				stdErrBuff.append(payload);
			}
		} else {
			System.err.println("Unexpected stream type: " + object.getStreamType());
		}
	}

	@Override
	public final void onError(final Throwable throwable) {
		throwable.printStackTrace(System.err);
	}

	@Override
	public final void onComplete() {}

	@Override
	public final void close() throws IOException {
		if (stream != null) {
			stream.close();
			stream = null;
		}
	}
}
