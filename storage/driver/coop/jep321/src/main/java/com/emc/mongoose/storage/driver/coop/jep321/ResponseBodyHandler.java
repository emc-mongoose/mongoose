package com.emc.mongoose.storage.driver.coop.jep321;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.logging.Loggers;

import static java.net.http.HttpResponse.BodyHandler;
import static java.net.http.HttpResponse.BodySubscriber;
import static java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ResponseBodyHandler<I extends Item, O extends Operation<I>>
extends CompletableFuture<Object>
implements BodyHandler<Object>, BodySubscriber<Object> {

	protected final O op;

	public ResponseBodyHandler(final O op) {
		this.op = op;
	}

	@Override
	public BodySubscriber<Object> apply(final ResponseInfo responseInfo) {
		Loggers.MSG.warn("ResponseBodyHandler::apply({})", responseInfo);
		return this;
	}

	@Override
	public CompletionStage<Object> getBody() {
		Loggers.MSG.warn("ResponseBodyHandler::getBody()");
		return this;
	}

	@Override
	public void onSubscribe(final Flow.Subscription subscription) {
		Loggers.MSG.warn("ResponseBodyHandler::onSubscribe({})", subscription);
	}

	@Override
	public void onNext(final List<ByteBuffer> item) {
		Loggers.MSG.warn("ResponseBodyHandler::onNext({})", item);
	}

	@Override
	public void onError(final Throwable throwable) {
		Loggers.MSG.warn("ResponseBodyHandler::onError({})", throwable);
	}

	@Override
	public void onComplete() {
		Loggers.MSG.warn("ResponseBodyHandler::onComplete()");
	}
}
