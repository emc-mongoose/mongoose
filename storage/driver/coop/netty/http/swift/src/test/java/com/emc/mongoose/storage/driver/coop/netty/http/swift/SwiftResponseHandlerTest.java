package com.emc.mongoose.storage.driver.coop.netty.http.swift;

import com.emc.mongoose.storage.driver.coop.netty.http.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpStorageDriverBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import java.net.SocketAddress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author veronika K. on 28.02.19
 */
public class SwiftResponseHandlerTest {

	private static final String HTTP_RESPONSE = "\n"
		+ "--3d07fbbddf4041880c931c29e43cb6c4\n"
		+ "Content-Type: application/octet-stream\n"
		+ "Content-Range: bytes 0-4/10\n"
		+ "aaaaa\n"
		+ "--3d07fbbddf4041880c931c29e43cb6c4\n"
		+ "Content-Type: application/octet-stream\n"
		+ "Content-Range: bytes 5-9/10\n"
		+ "aaaaa\n"
		+ "--3d07fbbddf4041880c931c29e43cb6c4--\n";

	private static final String EXPECTED_CONTENT = "aaaaaaaaaa";

	private static final String BOUNDARY = "--3d07fbbddf4041880c931c29e43cb6c4";
	private static final EmbeddedChannel channel = new EmbeddedChannel(); //channel mock
	private static final AttributeKey<String> ATTR_KEY_BOUNDARY_MARKER = AttributeKey.valueOf("boundary_marker");
	private static HttpStorageDriverBase storageDriver;
	private static final SocketAddress ADDRESS = new LocalAddress("127.0.0.1");

	@Before
	public void setUp() throws Exception {

//		Map<String, Object> configSchema = SchemaProvider
//			.resolveAndReduce("", Thread.currentThread().getContextClassLoader());
//		final Config config = new BasicConfig("-", configSchema);
//		storageDriver = new SwiftStorageDriver("", null, config.configVal("storage"), false, 1);
		channel.pipeline().addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
		channel.writeOutbound(HTTP_RESPONSE);
		channel.attr(ATTR_KEY_BOUNDARY_MARKER).set(BOUNDARY);
	}

	@Test
	public void test() {
//		HttpResponseHandlerBase responseHandler = new SwiftResponseHandler(storageDriver, true);
//		((SwiftResponseHandler) responseHandler).handleResponseContentChunk();
		final ByteBuf expectedContent = Unpooled.copiedBuffer(EXPECTED_CONTENT.getBytes());
		final ByteBuf contentChunk = Unpooled
			.copiedBuffer(channel.readOutbound().toString().getBytes());
		HttpResponseHandlerBase responseHandler = new SwiftResponseHandler(null, true);
		final ByteBuf newContentChunk = ((SwiftResponseHandler) responseHandler)
			.removeHeaders(channel, null, contentChunk);
		while (expectedContent.isReadable()) {
			final byte a = expectedContent.readByte();
			final byte b = newContentChunk.readByte();
			Assert.assertEquals(a, b);
			System.out.println(a + " " + b);
		}

//		((SwiftResponseHandler) responseHandler).handleResponseContentChunk();
	}


}
