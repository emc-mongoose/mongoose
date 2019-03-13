package com.emc.mongoose.storage.driver.coop.netty.http.swift;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author veronika K. on 28.02.19
 */
public class SwiftResponseHandlerTest {

	private static final String HTTP_RESPONSE =
		""
			+ "\r\n--3d07fbbddf4041880c931c29e43cb6c4"
			+ "\r\nContent-Type: application/octet-stream"
			+ "\r\nContent-Range: bytes 0-4/10\n\r\n\r"
			+ "\naaa\naa"
			+ "\r\n--3d07fbbddf4041880c931c29e43cb6c4"
			+ "\r\nContent-Type: application/octet-stream"
			+ "\r\nContent-Range: bytes 5-9/10\n\r\n\r"
			+ "aaaaa"
			+ "\r\n--3d07fbbddf4041880c931c29e43cb6c4--\r\n";

	private static final String PART_1_HTTP_RESPONSE =
		""
			+ "\r\n--3d07fbbddf4041880c931c29e43cb6c4"
			+ "\r\nContent-Type: application/octet-stream"
			+ "\r\nContent-Range: bytes 0-4/10\n\r\n\r"
			+ "\naaa\naa"
			+ "\r\n--3d07fbbddf4041880c931c29e43cb6c4"
			+ "\r\nContent-Type: appli";

	private static final String PART_2_HTTP_RESPONSE =
		""
			+ "cation/octet-stream"
			+ "\r\nContent-Range: bytes 5-9/10\n\r\n\r"
			+ "aaaaa"
			+ "\r\n--3d07fbbddf4041880c931c29e43cb6c4--\r\n";

	private static final String EXPECTED_CONTENT = "\naaa\naaaaaaa";
	private static final String BOUNDARY = "--3d07fbbddf4041880c931c29e43cb6c4";
	private static final EmbeddedChannel channel = new EmbeddedChannel(); // channel mock
	private static final AttributeKey<String> ATTR_KEY_BOUNDARY_MARKER =
		AttributeKey.valueOf("boundary_marker");

	static {
		channel.attr(ATTR_KEY_BOUNDARY_MARKER).set(BOUNDARY);
	}

	@Test
	public void fullContentTest() throws IOException {
		channel.writeOutbound(HTTP_RESPONSE);
		final ByteBuf expectedContent = Unpooled.copiedBuffer(EXPECTED_CONTENT.getBytes());
		final ByteBuf contentChunk =
			Unpooled.copiedBuffer(channel.readOutbound().toString().getBytes());
		final SwiftResponseHandler responseHandler = new SwiftResponseHandler(null, true);
		final ByteBuf newContentChunk = responseHandler.removeHeaders(channel, null, contentChunk);

		Assert.assertEquals(expectedContent.array().length, newContentChunk.array().length);
		while (expectedContent.isReadable()) {
			final byte a = expectedContent.readByte();
			final byte b = newContentChunk.readByte();
			Assert.assertEquals(a, b);
		}
	}

	@Test
	public void partContentTest() throws IOException {
		final SwiftResponseHandler responseHandler = new SwiftResponseHandler(null, true);
		final ByteBuf expectedContent = Unpooled.copiedBuffer(EXPECTED_CONTENT.getBytes());

		channel.writeOutbound(PART_1_HTTP_RESPONSE);
		final ByteBuf contentChunk1 =
			Unpooled.copiedBuffer(channel.readOutbound().toString().getBytes());
		final ByteBuf newContentChunk1 = responseHandler
			.removeHeaders(channel, null, contentChunk1);

		channel.writeOutbound(PART_2_HTTP_RESPONSE);
		final ByteBuf contentChunk2 =
			Unpooled.copiedBuffer(channel.readOutbound().toString().getBytes());
		final ByteBuf newContentChunk2 = responseHandler
			.removeHeaders(channel, null, contentChunk2);

		final ByteBuf fullContentChunk = Unpooled.copiedBuffer(newContentChunk1, newContentChunk2);

		Assert.assertEquals(expectedContent.array().length, fullContentChunk.array().length);
		while (expectedContent.isReadable()) {
			final byte a = expectedContent.readByte();
			final byte b = fullContentChunk.readByte();
			Assert.assertEquals(a, b);
		}
	}
}
