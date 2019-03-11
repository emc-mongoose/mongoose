package com.emc.mongoose.storage.driver.coop.netty.http.s3;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.composite.data.CompositeDataOperation;
import com.emc.mongoose.base.item.op.partial.data.PartialDataOperation;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;

import com.emc.mongoose.storage.driver.coop.netty.http.HttpResponseHandlerBase;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
Created by andrey on 25.11.16.
*/
public final class AmzS3ResponseHandler<I extends Item, O extends Operation<I>>
				extends HttpResponseHandlerBase<I, O> {

	private static final AttributeKey<ByteBuf> CONTENT_ATTR_KEY = AttributeKey.newInstance(
					"content");
	private static final int MIN_CONTENT_SIZE = 0x100;
	private static final int MAX_CONTENT_SIZE = 0x400;
	private static final Pattern PATTERN_UPLOAD_ID = Pattern.compile(
					"<UploadId>([a-zA-Z\\d\\-_+=/]+)</UploadId>", Pattern.MULTILINE);

	public AmzS3ResponseHandler(final AmzS3StorageDriver<I, O> driver, final boolean verifyFlag) {
		super(driver, verifyFlag);
	}

	@Override
	protected final void handleResponseHeaders(final Channel channel, final O op, final HttpHeaders respHeaders) {
		if (op instanceof PartialDataOperation) {
			final PartialDataOperation subTask = (PartialDataOperation) op;
			final String eTag = respHeaders.get(HttpHeaderNames.ETAG);
			final CompositeDataOperation mpuTask = subTask.parent();
			mpuTask.put(Integer.toString(subTask.partNumber() + 1), eTag);
		}
	}

	@Override
	protected final void handleResponseContentChunk(final Channel channel, final O op, final ByteBuf contentChunk)
					throws IOException {
		if (op instanceof CompositeDataOperation) {
			handleInitMultipartUploadResponseContentChunk(channel, contentChunk);
		} else {
			super.handleResponseContentChunk(channel, op, contentChunk);
		}
	}

	private void handleInitMultipartUploadResponseContentChunk(
					final Channel channel, final ByteBuf contentChunk) {
		// expect the XML data which is not large (up to 1KB)
		final Attribute<ByteBuf> contentAttr = channel.attr(CONTENT_ATTR_KEY);
		contentAttr.compareAndSet(null, Unpooled.buffer(MIN_CONTENT_SIZE));
		final ByteBuf content = contentAttr.get();
		try {
			content.writeBytes(contentChunk);
		} catch (final IndexOutOfBoundsException e) {
			LogUtil.exception(
							Level.WARN, e, "HTTP content input buffer overflow, expected no more than {} bytes",
							MAX_CONTENT_SIZE);
		}
	}

	@Override
	protected final void handleResponseContentFinish(final Channel channel, final O op) {
		final Attribute<ByteBuf> contentAttr = channel.attr(CONTENT_ATTR_KEY);
		final ByteBuf content = contentAttr.get();
		if (content != null && content.readableBytes() > 0) {
			if (op instanceof CompositeDataOperation) {
				final CompositeDataOperation mpuOp = (CompositeDataOperation) op;
				if (!mpuOp.allSubOperationsDone()) {
					// this is an MPU init response
					final String contentStr = content.toString(UTF_8);
					final Matcher m = PATTERN_UPLOAD_ID.matcher(contentStr);
					if (m.find()) {
						channel.attr(AmzS3Api.KEY_ATTR_UPLOAD_ID).set(m.group(1));
					} else {
						Loggers.ERR.warn(
										"Upload id not found in the following response content:\n{}", contentStr);
					}
				}
			}
			content.clear();
		}
		super.handleResponseContentFinish(channel, op);
	}
}
