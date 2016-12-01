package com.emc.mongoose.storage.driver.net.http.s3;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.net.http.base.HttpResponseHandlerBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import static com.emc.mongoose.storage.driver.net.http.s3.S3Constants.KEY_ATTR_UPLOAD_ID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 Created by andrey on 25.11.16.
 */
public final class S3ResponseHandler<I extends Item, O extends IoTask<I, R>, R extends IoTask.IoResult>
extends HttpResponseHandlerBase<I, O, R> {

	private static final Logger LOG = LogManager.getLogger();
	private static final AttributeKey<ByteBuf> contentAttrKey = AttributeKey.newInstance("content");
	private static final int MIN_CONTENT_SIZE = 0x100;
	private static final int MAX_CONTENT_SIZE = 0x400;
	private static final Pattern PATTERN_UPLOAD_ID = Pattern.compile(
		"<UploadId>([a-zA-Z\\d\\-_+=/]+)</UploadId>", Pattern.MULTILINE
	);

	public S3ResponseHandler(final S3StorageDriver<I, O, R> driver, final boolean verifyFlag) {
		super(driver, verifyFlag);
	}

	@Override
	protected final void handleResponseHeaders(final O ioTask, final HttpHeaders respHeaders) {
		if(ioTask instanceof PartialDataIoTask) {
			final PartialDataIoTask subTask = (PartialDataIoTask) ioTask;
			final String eTag = respHeaders.get(HttpHeaderNames.ETAG);
			final CompositeDataIoTask mpuTask = subTask.getParent();
			mpuTask.put(Integer.toString(subTask.getPartNumber() + 1), eTag);
		}
	}

	@Override
	protected final void handleResponseContentChunk(
		final Channel channel, final O ioTask, final ByteBuf contentChunk
	) {
		if(ioTask instanceof CompositeDataIoTask) {
			handleInitMultipartUploadResponseContentChunk(channel, contentChunk);
		} else {
			super.handleResponseContentChunk(channel, ioTask, contentChunk);
		}
	}

	private void handleInitMultipartUploadResponseContentChunk(
		final Channel channel, final ByteBuf contentChunk
	) {
		// expect the XML data which is not large (up to 1KB)
		final Attribute<ByteBuf> contentAttr = channel.attr(contentAttrKey);
		contentAttr.compareAndSet(null, Unpooled.buffer(MIN_CONTENT_SIZE));
		final ByteBuf content = contentAttr.get();
		try {
			content.writeBytes(contentChunk);
		} catch(final IndexOutOfBoundsException e) {
			LogUtil.exception(
				LOG, Level.WARN, e,
				"HTTP content input buffer overflow, expected no more than {} bytes",
				MAX_CONTENT_SIZE
			);
		}
	}

	@Override
	protected final void handleResponseContentFinish(final Channel channel, final O ioTask) {
		final Attribute<ByteBuf> contentAttr = channel.attr(contentAttrKey);
		final ByteBuf content = contentAttr.get();
		if(content != null && content.readableBytes() > 0) {
			final String contentStr = content.toString(UTF_8);
			final Matcher m = PATTERN_UPLOAD_ID.matcher(contentStr);
			if(m.find()) {
				channel.attr(KEY_ATTR_UPLOAD_ID).set(m.group(1));
			} else {
				LOG.warn(
					Markers.ERR, "Upload id not found in the following response content:\n{}",
					contentStr
				);
			}
		}
		super.handleResponseContentFinish(channel, ioTask);
	}
}
