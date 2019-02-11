package com.emc.mongoose.storage.driver.coop.jep321.data;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import static com.emc.mongoose.base.item.op.Operation.Status.FAIL_TIMEOUT;
import static com.emc.mongoose.base.item.op.Operation.Status.FAIL_UNKNOWN;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_AUTH;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_CLIENT;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_NOT_FOUND;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_SPACE;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_SVC;
import static com.emc.mongoose.base.item.op.Operation.Status.SUCC;

import org.apache.logging.log4j.Level;

import static java.net.http.HttpResponse.BodyHandler;
import static java.net.http.HttpResponse.BodySubscriber;
import static java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class ResponseBodyHandler<I extends Item, O extends Operation<I>>
extends CompletableFuture<Object>
implements BodyHandler<Object>, BodySubscriber<Object> {

	protected final O op;
	protected final Consumer<O> completionHandler;

	public ResponseBodyHandler(final O op, final Consumer<O> completionHandler) {
		this.op = op;
		this.completionHandler = completionHandler;
	}

	@Override
	public BodySubscriber<Object> apply(final ResponseInfo responseInfo) {
		op.startResponse();
		final var statusCode = responseInfo.statusCode();
		if(statusCode == 401) {
			Loggers.ERR.warn("{}: {}/Unauthorized", op.toString(), statusCode);
			op.status(RESP_FAIL_AUTH);
		} else if(statusCode == 403) {
			Loggers.ERR.warn("{}: {}/Forbidden", op.toString(), statusCode);
			op.status(RESP_FAIL_AUTH);
		} else if(statusCode == 404) {
			Loggers.ERR.warn("{}: {}/Not Found", op.toString(), statusCode);
			op.status(RESP_FAIL_NOT_FOUND);
		} else if(statusCode == 413) {
			Loggers.ERR.warn("{}: request entity too large {}", op.toString(), statusCode);
			op.status(RESP_FAIL_SVC);
		} else if(statusCode == 414) {
			Loggers.ERR.warn("{}: request URI too long {}", op.toString(), statusCode);
			op.status(RESP_FAIL_SVC);
		} else if(statusCode == 503) {
			Loggers.ERR.warn("{}: service unavailable {}", op.toString(), statusCode);
			op.status(RESP_FAIL_SVC);
		} else if(statusCode == 504) {
			Loggers.ERR.warn("{}: gateway timeout {}", op.toString(), statusCode);
			op.status(FAIL_TIMEOUT);
		} else if(statusCode == 507) {
			Loggers.ERR.warn("{}: insufficient space {}", op.toString(), statusCode);
			op.status(RESP_FAIL_SPACE);
		} else if(statusCode >= 100 && statusCode < 200) {
			Loggers.ERR.warn("{}: response status {}", op.toString(), statusCode);
			op.status(RESP_FAIL_CLIENT);
		} else if(statusCode < 300) {
			op.status(SUCC);
		} else if(statusCode < 500) {
			Loggers.ERR.warn("{}: response status {}", op.toString(), statusCode);
			op.status(RESP_FAIL_CLIENT);
		} else {
			Loggers.ERR.warn("{}: response status {}", op.toString(), statusCode);
			op.status(RESP_FAIL_SVC);
		}
		return this;
	}

	@Override
	public CompletionStage<Object> getBody() {
		//Loggers.MSG.warn("ResponseBodyHandler::getBody()");
		return this;
	}

	@Override
	public void onSubscribe(final Flow.Subscription subscription) {
		//Loggers.MSG.warn("ResponseBodyHandler::onSubscribe({})", subscription);
	}

	@Override
	public void onNext(final List<ByteBuffer> item) {
		Loggers.MSG.warn("ResponseBodyHandler::onNext({})", item);
	}

	@Override
	public void onError(final Throwable throwable) {
		op.finishResponse();
		LogUtil.exception(Level.ERROR, throwable, "{}: load operation failed", op);
		op.status(FAIL_UNKNOWN);
		completionHandler.accept(op);
	}

	@Override
	public void onComplete() {
		op.finishResponse();
		completionHandler.accept(op);
	}
}
