package com.emc.mongoose.storage.driver.http.base.request;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.impl.io.AsyncCurrentDateInput;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.storage.driver.http.base.HttpStorageDriver;
import com.emc.mongoose.ui.config.IllegalArgumentNameException;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 Created by andrey on 22.09.16.
 */
public abstract class CrudHttpRequestFactory<I extends Item, O extends IoTask<I>>
implements HttpRequestFactory<I, O> {

	protected final HttpStorageDriver<I, O> httpDriver;
	protected final String srcContainer;

	protected CrudHttpRequestFactory(final HttpStorageDriver<I, O> httpDriver, final String srcContainer) {
		this.httpDriver = httpDriver;
		this.srcContainer = srcContainer;
	}

	public static <I extends Item, O extends IoTask<I>> HttpRequestFactory getInstance(
		final LoadType ioType, final HttpStorageDriver<I, O> httpDriver, final String srcContainer
	) {
		switch(ioType) {
			case CREATE:
				return new CreateRequestFactory<>(httpDriver, srcContainer);
			case READ:
				return new ReadRequestFactory<>(httpDriver, srcContainer);
			case UPDATE:
				return new UpdateRequestFactory<>(httpDriver, srcContainer);
			case DELETE:
				return new DeleteRequestFactory<>(httpDriver, srcContainer);
			default:
				throw new IllegalArgumentNameException(
					"Not implemented for \"" + ioType + "\" load type"
				);
		}
	}

	@Override
	public final HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {
		final I item = ioTask.getItem();
		final LoadType ioType = ioTask.getLoadType();
		final HttpMethod httpMethod = httpDriver.getHttpMethod(ioType);
		final String dstUriPath = httpDriver.getDstUriPath(item, ioTask);
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, dstUriPath, httpHeaders
		);
		configureHttpRequest(item, httpHeaders);
		httpDriver.applyMetaDataHeaders(httpHeaders);
		httpDriver.applyAuthHeaders(httpMethod, dstUriPath, httpHeaders);
		httpDriver.applyDynamicHeaders(httpHeaders);
		httpDriver.applySharedHeaders(httpHeaders);
		return httpRequest;
	}

	protected abstract void configureHttpRequest(final I item, final HttpHeaders httpHeaders)
	throws URISyntaxException;

	private final static class CreateRequestFactory<I extends Item, O extends IoTask<I>>
	extends CrudHttpRequestFactory<I, O> {

		protected CreateRequestFactory(
			final HttpStorageDriver<I, O> httpDriver, final String srcContainer
		) {
			super(httpDriver, srcContainer);
		}

		@Override
		protected final void configureHttpRequest(final I item, final HttpHeaders httpHeaders)
		throws URISyntaxException {
			if(srcContainer == null) {
				if(item instanceof DataItem) {
					try {
						httpHeaders.set(
							HttpHeaderNames.CONTENT_LENGTH, ((DataItem) item).size()
						);
					} catch(final IOException ignored) {
					}
				} else {
					httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				}
			} else {
				httpDriver.applyCopyHeaders(httpHeaders, item);
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			}
		}
	}

	private final static class ReadRequestFactory<I extends Item, O extends IoTask<I>>
	extends CrudHttpRequestFactory<I, O> {

		protected ReadRequestFactory(
			final HttpStorageDriver<I, O> httpDriver, final String srcContainer
		) {
			super(httpDriver, srcContainer);
		}

		@Override
		protected final void configureHttpRequest(final I item, final HttpHeaders httpHeaders)
		throws URISyntaxException {
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		}
	}

	private final static class UpdateRequestFactory<I extends Item, O extends IoTask<I>>
	extends CrudHttpRequestFactory<I, O> {

		protected UpdateRequestFactory(
			final HttpStorageDriver<I, O> httpDriver, final String srcContainer
		) {
			super(httpDriver, srcContainer);
		}

		@Override
		protected final void configureHttpRequest(final I item, final HttpHeaders httpHeaders)
		throws URISyntaxException {
			// TODO cast to MutableDataItem, set ranges headers conditionally
		}
	}

	private final static class DeleteRequestFactory<I extends Item, O extends IoTask<I>>
	extends CrudHttpRequestFactory<I, O> {

		protected DeleteRequestFactory(
			final HttpStorageDriver<I, O> httpDriver, final String srcContainer
		) {
			super(httpDriver, srcContainer);
		}

		@Override
		protected final void configureHttpRequest(final I item, final HttpHeaders httpHeaders)
		throws URISyntaxException {
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		}
	}
}
