package com.emc.mongoose.system.feature.content;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.core.impl.item.base.ListItemDst;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 16.10.15.
 */
public class WriteRikkiTikkiTaviTest
extends StandaloneClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 100, OBJ_SIZE = (int) SizeInBytes.toFixedSize("64KB");
	private final static String
		RUN_ID = WriteRikkiTikkiTaviTest.class.getCanonicalName(),
		BASE_URL = "http://127.0.0.1:9020/" + WriteRikkiTikkiTaviTest.class.getSimpleName() + "/";
	private final static List<WSObject> OBJ_BUFF = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(AppConfig.KEY_DATA_CONTENT_FPATH, "conf/content/textexample");
		StandaloneClientTestBase.setUpClass();
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setS3Bucket(WriteRikkiTikkiTaviTest.class.getSimpleName())
				.build()
		) {
			countWritten = client.write(
				null, new ListItemDst<>(OBJ_BUFF), COUNT_TO_WRITE, 10, OBJ_SIZE
			);
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countWritten);
	}
	//
	@Test
	public void checkAllObjectsContainTheTaleText()
		throws Exception {
		Assert.assertEquals(COUNT_TO_WRITE, OBJ_BUFF.size());
		URL nextObjURL;
		final byte buff[] = new byte[OBJ_SIZE];
		for(int i = 0; i < OBJ_BUFF.size(); i ++) {
			nextObjURL = new URL(BASE_URL + OBJ_BUFF.get(i).getName());
			try(final BufferedInputStream in = new BufferedInputStream(nextObjURL.openStream())) {
				int n = 0, m;
				do {
					m = in.read(buff, n, OBJ_SIZE - n);
					if(m < 0) {
						try(
							final BufferedInputStream listInput = new BufferedInputStream(
								new URL("http://127.0.0.1:9020/" + WriteRikkiTikkiTaviTest.class.getSimpleName())
									.openStream()
							)
						) {
							final ByteArrayOutputStream baos = new ByteArrayOutputStream();
							do {
								m = listInput.read(buff);
								if(m < 0) {
									break;
								} else {
									baos.write(buff, 0, m);
								}
							} while(true);
							baos.writeTo(System.out);
						}
						throw new EOFException(
							"#" + i + ": unexpected end of stream @ offset " + n +
								" while reading the content from " + nextObjURL
						);
					} else {
						n += m;
					}
				} while(n < OBJ_SIZE);
			}
			final String text = new String(buff, StandardCharsets.UTF_8);
			Assert.assertTrue(
				"No word \"mongoose\" in the text of the object #" + i + " @ " + nextObjURL,
				text.contains("mongoose")
			);
			Assert.assertTrue(text, text.contains("Nag is dead"));
			Assert.assertTrue(text, text.contains("Chuchundra"));
		}
	}
}
