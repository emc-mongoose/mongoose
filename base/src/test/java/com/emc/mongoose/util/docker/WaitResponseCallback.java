package com.emc.mongoose.util.docker;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.WaitResponse;
import java.io.Closeable;
import java.io.IOException;

/** Created by andrey on 25.09.17. */
public final class WaitResponseCallback implements ResultCallback<WaitResponse> {

	private long tsStart;
	private long tsComplete;

	@Override
	public final void onStart(final Closeable closeable) {
		tsStart = System.currentTimeMillis();
	}

	@Override
	public final void onNext(final WaitResponse object) {}

	@Override
	public final void onError(final Throwable throwable) {
		throwable.printStackTrace(System.err);
	}

	@Override
	public final void onComplete() {
		tsComplete = System.currentTimeMillis();
	}

	@Override
	public final void close() throws IOException {}
}
