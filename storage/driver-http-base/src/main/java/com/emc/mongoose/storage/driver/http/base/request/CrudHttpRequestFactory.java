package com.emc.mongoose.storage.driver.http.base.request;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.impl.io.AsyncCurrentDateInput;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.storage.driver.http.base.HttpDriver;
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

	protected final HttpDriver<I, O> httpDriver;
	protected final String srcContainer;

	protected CrudHttpRequestFactory(final HttpDriver<I, O> httpDriver, final String srcContainer) {
		this.httpDriver = httpDriver;
		this.srcContainer = srcContainer;
	}

	public static <I extends Item, O extends IoTask<I>> HttpRequestFactory getInstance(
		final LoadType ioType, final HttpDriver<I, O> httpDriver, final String srcContainer
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

	protected final HttpHeaders initHeaders(final String nodeAddr) {
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		return httpHeaders;
	}

	protected final HttpRequest initRequest(
		final I item, final O ioTask, final HttpHeaders httpHeaders
	) {
		final LoadType ioType = ioTask.getLoadType();
		final HttpMethod httpMethod = httpDriver.getHttpMethod(ioType);
		final String dstUriPath = httpDriver.getDstUriPath(item, ioTask);
		return new DefaultHttpRequest(
			HTTP_1_1, httpMethod, dstUriPath, httpHeaders
		);
	}

	private final static class CreateRequestFactory<I extends Item, O extends IoTask<I>>
	extends CrudHttpRequestFactory<I, O> {

		protected CreateRequestFactory(
			final HttpDriver<I, O> httpDriver, final String srcContainer
		) {
			super(httpDriver, srcContainer);
		}

		@Override
		public final HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
		throws URISyntaxException {
			final I item = ioTask.getItem();
			final HttpHeaders httpHeaders = initHeaders(nodeAddr);
			final HttpRequest httpRequest = initRequest(item, ioTask, httpHeaders);
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
			return httpRequest;
		}
	}

	private final static class ReadRequestFactory<I extends Item, O extends IoTask<I>>
	extends CrudHttpRequestFactory<I, O> {

		protected ReadRequestFactory(
			final HttpDriver<I, O> httpDriver, final String srcContainer
		) {
			super(httpDriver, srcContainer);
		}

		@Override
		public final HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
		throws URISyntaxException {
			final I item = ioTask.getItem();
			final HttpHeaders httpHeaders = initHeaders(nodeAddr);
			final HttpRequest httpRequest = initRequest(item, ioTask, httpHeaders);
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			return httpRequest;
		}
	}

	private final static class UpdateRequestFactory<I extends Item, O extends IoTask<I>>
	extends CrudHttpRequestFactory<I, O> {

		protected UpdateRequestFactory(
			final HttpDriver<I, O> httpDriver, final String srcContainer
		) {
			super(httpDriver, srcContainer);
		}

		@Override
		public final HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
		throws URISyntaxException {
			final I item = ioTask.getItem();
			final HttpHeaders httpHeaders = initHeaders(nodeAddr);
			final HttpRequest httpRequest = initRequest(item, ioTask, httpHeaders);
			// TODO cast to MutableDataItem, set ranges headers conditionally
			return httpRequest;
		}
	}

	private final static class DeleteRequestFactory<I extends Item, O extends IoTask<I>>
	extends CrudHttpRequestFactory<I, O> {

		protected DeleteRequestFactory(
			final HttpDriver<I, O> httpDriver, final String srcContainer
		) {
			super(httpDriver, srcContainer);
		}

		@Override
		public final HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
		throws URISyntaxException {
			final I item = ioTask.getItem();
			final HttpHeaders httpHeaders = initHeaders(nodeAddr);
			final HttpRequest httpRequest = initRequest(item, ioTask, httpHeaders);
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			return httpRequest;
		}
	}
}
