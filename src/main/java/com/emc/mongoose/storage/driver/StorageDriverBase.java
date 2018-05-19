package com.emc.mongoose.storage.driver;

import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.concurrent.DaemonBase;
import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.io.task.data.DataIoTask;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.storage.Credential;
import com.github.akurilov.commons.concurrent.ThreadUtil;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.confuse.Config;
import org.apache.logging.log4j.CloseableThreadContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

/**
 Created by kurila on 11.07.16.
 */
public abstract class StorageDriverBase<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements StorageDriver<I,O> {

	private final DataInput itemDataInput;
	protected final String stepId;
	private final BlockingQueue<O> ioResultsQueue;
	protected final int concurrencyLevel;
	protected final int ioWorkerCount;
	protected final Credential credential;
	protected final boolean verifyFlag;

	protected final ConcurrentMap<String, Credential> pathToCredMap = new ConcurrentHashMap<>(1);

	private final ConcurrentMap<String, String> pathMap = new ConcurrentHashMap<>(1);
	protected abstract String requestNewPath(final String path);
	protected Function<String, String> requestNewPathFunc = this::requestNewPath;

	protected final ConcurrentMap<Credential, String> authTokens = new ConcurrentHashMap<>(1);
	protected abstract String requestNewAuthToken(final Credential credential);
	protected Function<Credential, String> requestAuthTokenFunc = this::requestNewAuthToken;

	protected StorageDriverBase(
		final String stepId, final DataInput itemDataInput, final Config loadConfig,
		final Config storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {

		this.itemDataInput = itemDataInput;
		final Config driverConfig = storageConfig.configVal("driver");
		final Config queueConfig = driverConfig.configVal("queue");
		final int outputQueueCapacity = queueConfig.intVal("output");
		this.ioResultsQueue = new ArrayBlockingQueue<>(outputQueueCapacity);
		this.stepId = stepId;
		final Config authConfig = storageConfig.configVal("auth");
		this.credential = Credential.getInstance(
			authConfig.stringVal("uid"), authConfig.stringVal("secret")
		);
		final String authToken = authConfig.stringVal("token");
		if(authToken != null) {
			if(this.credential == null) {
				this.authTokens.put(Credential.NONE, authToken);
			} else {
				this.authTokens.put(credential, authToken);
			}
		}
		this.concurrencyLevel = loadConfig.intVal("step-limit-concurrency");
		this.verifyFlag = verifyFlag;

		final int confWorkerCount = driverConfig.intVal("threads");
		if(confWorkerCount > 0) {
			ioWorkerCount = confWorkerCount;
		} else if(concurrencyLevel > 0) {
			ioWorkerCount = Math.min(concurrencyLevel, ThreadUtil.getHardwareThreadCount());
		} else {
			ioWorkerCount = ThreadUtil.getHardwareThreadCount();
		}
	}

	protected void prepareIoTask(final O ioTask) {
		ioTask.reset();
		if(ioTask instanceof DataIoTask) {
			((DataIoTask) ioTask).item().dataInput(itemDataInput);
		}
		final String dstPath = ioTask.dstPath();
		final Credential credential = ioTask.credential();
		if(credential != null) {
			pathToCredMap.putIfAbsent(dstPath == null ? "" : dstPath, credential);
			if(requestAuthTokenFunc != null) {
				authTokens.computeIfAbsent(credential, requestAuthTokenFunc);
			}
		}
		if(requestNewPathFunc != null) {
			// NOTE: in the distributed mode null dstPath becomes empty one
			if(dstPath != null && !dstPath.isEmpty()) {
				if(null == pathMap.computeIfAbsent(dstPath, requestNewPathFunc)) {
					Loggers.ERR.debug(
						"Failed to compute the destination path for the I/O task {}", ioTask
					);
					ioTask.status(IoTask.Status.FAIL_UNKNOWN);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void ioTaskCompleted(final O ioTask) {
		if(Loggers.MSG.isTraceEnabled()) {
			Loggers.MSG.trace("{}: I/O task completed", ioTask);
		}
		final O ioTaskResult = ioTask.result();
		if(!ioResultsQueue.offer(ioTaskResult)) {
			Loggers.ERR.warn(
				"{}: I/O task results queue overflow, dropping the result", toString()
			);
		}
	}

	@Override
	public final int getConcurrencyLevel() {
		return concurrencyLevel;
	}

	@Override
	public final O get() {
		return ioResultsQueue.poll();
	}

	@Override
	public final List<O> getAll() {
		final int n = ioResultsQueue.size();
		if(n == 0) {
			return Collections.emptyList();
		}
		final List<O> ioTaskResults = new ArrayList<>(n);
		ioResultsQueue.drainTo(ioTaskResults, n);
		return ioTaskResults;
	}

	@Override
	public final long skip(final long count) {
		int n = (int) Math.min(count, Integer.MAX_VALUE);
		final List<O> tmpBuff = new ArrayList<>(n);
		n = ioResultsQueue.drainTo(tmpBuff, n);
		tmpBuff.clear();
		return n;
	}

	@Override
	public Input<O> getInput() {
		return this;
	}

	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, StorageDriverBase.class.getSimpleName())
		) {
			itemDataInput.close();
			final int ioResultsQueueSize = ioResultsQueue.size();
			if(ioResultsQueueSize > 0) {
				Loggers.ERR.warn(
					"{}: I/O results queue contains {} unhandled elements", toString(),
					ioResultsQueueSize
				);
			}
			ioResultsQueue.clear();
			authTokens.clear();
			pathToCredMap.clear();
			pathMap.clear();
			Loggers.MSG.debug("{}: closed", toString());
		}
	}

	@Override
	public String toString() {
		return "storage/driver/" + concurrencyLevel + "/%s/" + hashCode();
	}
}
