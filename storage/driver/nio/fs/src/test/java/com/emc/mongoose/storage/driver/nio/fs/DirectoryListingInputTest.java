package com.emc.mongoose.storage.driver.nio.fs;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.math.Random;
import com.emc.mongoose.model.item.BasicItemFactory;
import com.emc.mongoose.model.item.Item;
import org.apache.commons.io.FileUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 Created by andrey on 01.12.16.
 */
public class DirectoryListingInputTest {

	private static Path TMP_DIR_PATH = null;

	@BeforeClass
	public static void setUpClass() {
		try {
			TMP_DIR_PATH = Files.createTempDirectory(null);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
	}

	@AfterClass
	public static void tearDownClass() {
		if(TMP_DIR_PATH != null) {
			try {
				FileUtils.deleteDirectory(TMP_DIR_PATH.toFile());
			} catch(final IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	@Test
	public final void testListingWithPrefix()
	throws Exception {

		final String prefix = "yohoho";
		final int count = 1000;

		for(int i = 0; i < count; i ++) {
			if(i < 10) {
				new File(TMP_DIR_PATH.toString() + File.separatorChar + prefix + "000" + Integer.toString(i))
					.createNewFile();
			} else if(i < 100) {
				new File(TMP_DIR_PATH.toString() + File.separatorChar + prefix + "00" + Integer.toString(i))
					.createNewFile();
			} else if(i < 1000) {
				new File(TMP_DIR_PATH.toString() + File.separatorChar + prefix + "0" + Integer.toString(i))
					.createNewFile();
			}
		}

		final Random rnd = new Random();
		for(int i = 0; i < count; i ++) {
			new File(TMP_DIR_PATH.toString() + File.separatorChar + Integer.toString(rnd.nextInt()))
				.createNewFile();
		}

		final Input<Item> in = new DirectoryListingInput<>(TMP_DIR_PATH.toString(), new BasicItemFactory<>(), 10, prefix);
		final List<Item> inBuff = new ArrayList<>(count);
		assertEquals(count, in.get(inBuff, count));
	}
}