package com.emc.mongoose.storage.driver.coop.netty.http.swift;

import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.VALUE_MULTIPART_BYTERANGES;

import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpStorageDriverBase;
import com.github.akurilov.commons.collection.Range;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by andrey on 26.11.16.
 */
public final class SwiftResponseHandler<I extends Item, O extends Operation<I>>
	extends HttpResponseHandlerBase<I, O> {

	private static final Pattern BOUNDARY_VALUE_PATTERN =
		Pattern.compile(
			VALUE_MULTIPART_BYTERANGES + ";" + HttpHeaderValues.BOUNDARY + "=([0-9a-f]+)");
	private static final String HEADER_PATTERN = "(Content-Type:).*[\\s]*(Content-Range:).*";

	private static final String HEADER_WITH_BOUNDARY_PATTERN =
		"[\\s]{2}((%1$s)[\\s]*(" + HEADER_PATTERN + ")[\\s]{4}|(%1$s--)[\\s]*)";
	private static final AttributeKey<String> ATTR_KEY_BOUNDARY_MARKER =
		AttributeKey.valueOf("boundary_marker");

	private byte[] cutChunck = new byte[]{};

	public SwiftResponseHandler(final HttpStorageDriverBase<I, O> driver,
		final boolean verifyFlag) {
		super(driver, verifyFlag);
	}

	@Override
	protected final void handleResponseHeaders(
		final Channel channel, final O op, final HttpHeaders respHeaders) {
		final String contentType = respHeaders.get(HttpHeaderNames.CONTENT_TYPE);
		if (contentType != null) {
			final Matcher boundaryMatcher = BOUNDARY_VALUE_PATTERN.matcher(contentType);
			if (boundaryMatcher.find()) {
				final String boundaryMarker = "--" + boundaryMatcher.group(1);
				channel.attr(ATTR_KEY_BOUNDARY_MARKER).set(boundaryMarker);
			}
		}
	}

	/*
      Boundary marker: ac9c12f841fa093d82ba80a402f6b62e
      Content:
    --ac9c12f841fa093d82ba80a402f6b62e
    Content-Type: application/octet-stream
    Content-Range: bytes 0-0/10240

    ?
    --ac9c12f841fa093d82ba80a402f6b62e
    Content-Type: application/octet-stream
    Content-Range: bytes 3-6/10240

    ????
    --ac9c12f841fa093d82ba80a402f6b62e--
       */
	protected final void handleResponseContentChunk(
		final Channel channel, final O op, final ByteBuf contentChunk) throws IOException {
		if (OpType.READ.equals(op.type())) {
			if (op instanceof DataOperation) {
				final DataOperation<? extends DataItem> dataOp = (DataOperation<? extends DataItem>) op;
				final BitSet[] markedRangesMaskPair = dataOp.markedRangesMaskPair();
				// if the count of marked byte ranges > 1
				if (1 < markedRangesMaskPair[0].cardinality() + markedRangesMaskPair[1]
					.cardinality()) {
					final ByteBuf newContentChunk = removeHeaders(channel, op, contentChunk);
					super.handleResponseContentChunk(channel, op, newContentChunk);
				} else {
					final List<Range> fixedRanges = dataOp.fixedRanges();
					if (fixedRanges != null && 1 < fixedRanges.size()) {
						final ByteBuf newContentChunk = removeHeaders(channel, op, contentChunk);
						super.handleResponseContentChunk(channel, op, newContentChunk);
					} else {
						super.handleResponseContentChunk(channel, op, contentChunk);
					}
				}
			} else {
				super.handleResponseContentChunk(channel, op, contentChunk);
			}
		} else {
			super.handleResponseContentChunk(channel, op, contentChunk);
		}
	}

	ByteBuf removeHeaders(final Channel channel, final O op, final ByteBuf contentChunk) {
		final String boundaryMarker = channel.attr(ATTR_KEY_BOUNDARY_MARKER).get();
		final int rawSize = contentChunk.readableBytes();
		final int cutChunkSize = cutChunck.length;
		final byte[] rawBytesChunk = new byte[cutChunkSize + rawSize];
		System.arraycopy(cutChunck, 0, rawBytesChunk, 0, cutChunkSize);
		while (contentChunk.readerIndex() < rawSize) {
			rawBytesChunk[cutChunkSize + contentChunk.readerIndex()] = contentChunk.readByte();
		}
		String s = new String(rawBytesChunk);
		final Pattern p = Pattern
			.compile(String.format(HEADER_WITH_BOUNDARY_PATTERN, boundaryMarker));
		final Matcher matcher = p.matcher(s);
		final List<int[]> contentRangeIdxs = new ArrayList();
		final List<MatchResult> results = matcher.results().collect(Collectors.toList());
		int startIndex = 0;
		int endIndex;
		for (final var result : results){
			endIndex = result.start();
			contentRangeIdxs.add(new int[]{startIndex, endIndex}); //TODO replace on {start,size}
			startIndex = result.end();
		}
		endIndex = rawBytesChunk.length;
		contentRangeIdxs.add(new int[]{startIndex, endIndex});
		//
		//
		int newContentSize = 0;
		for (final int[] range : contentRangeIdxs) { //TODO: replace on stream
			newContentSize += range[1] - range[0];
		}
		final byte[] bytesChunk = new byte[newContentSize];
		int lastIdx = 0; //index in bytesChunk
		for (final int[] range : contentRangeIdxs) { //TODO: replace on stream
			final int rangeSize = range[1] - range[0];
			System.arraycopy(rawBytesChunk, range[0], bytesChunk, lastIdx, rangeSize);
			lastIdx += rangeSize;
		}

		final byte[] bytesChunkWithoutEnd = cuteEnd(bytesChunk);
		return Unpooled.copiedBuffer(bytesChunkWithoutEnd);
	}

	private byte[] cuteEnd(final byte[] bytesChunk) {
		String tmpString = new String(bytesChunk);
		String cutString = "";
		final byte[] newBytesChunk;
		if (tmpString.substring(tmpString.length() - 1) == "-") {
			cutString = "-";
		}
		if (tmpString.substring(tmpString.length() - 2) == "--") {
			cutString = "--";
		}
		final Pattern pattern = Pattern.compile("[\\s]{2}--(.|\\s)*");
		final Matcher matcher = pattern.matcher(tmpString);
		if (matcher.find()) {
			// TODO kochuv: may be without count?
			final int count = matcher.groupCount();
			cutString = matcher.group(count - 1);
		}
		cutChunck = cutString.getBytes(); //cutString includes only writable chars
		final int newSize = bytesChunk.length - cutString.length();
		newBytesChunk = new byte[newSize];
		System.arraycopy(bytesChunk, 0, newBytesChunk, 0, newSize);
		return newBytesChunk;
	}

}
