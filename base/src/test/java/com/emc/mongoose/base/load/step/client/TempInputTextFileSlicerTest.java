package com.emc.mongoose.base.load.step.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.emc.mongoose.base.load.step.file.FileManager;
import com.emc.mongoose.base.load.step.file.FileManagerImpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;

public final class TempInputTextFileSlicerTest {

	private static final int BATCH_SIZE = 100;
	private static final String SRC_FILE_NAME;

	static {
		try {
			SRC_FILE_NAME = Files.createTempFile(TempInputTextFileSlicerTest.class.getSimpleName(), ".txt")
							.toString();
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
	}

	private static final int SLICE_COUNT = 10;
	private static final List<FileManager> FILE_MGRS = IntStream.range(0, 10).mapToObj(i -> new FileManagerImpl()).collect(Collectors.toList());
	private static final Map<FileManager, String> FILE_SLICES = FILE_MGRS.stream()
					.collect(
									Collectors.toMap(
													Function.identity(),
													fileMgr -> {
														try {
															return fileMgr.newTmpFileName();
														} catch (final IOException e) {
															throw new AssertionError(e);
														}
													}));
	private static final int SRC_LINE_COUNT = 10_000;

	@Before
	public final void beforeTest() throws Exception {
		try (final BufferedWriter srcFileWriter = Files.newBufferedWriter(Paths.get(SRC_FILE_NAME))) {
			for (int i = 0; i < SRC_LINE_COUNT; i++) {
				srcFileWriter.write(Long.toString(System.nanoTime()));
				srcFileWriter.newLine();
			}
		}
		TempInputTextFileSlicer.scatterLines(
						SRC_FILE_NAME, SLICE_COUNT, FILE_MGRS, FILE_SLICES, BATCH_SIZE);
	}

	@Test
	public final void test() throws Exception {
		final LongAdder slicedLineCount = new LongAdder();
		FILE_SLICES
						.values()
						.parallelStream()
						.forEach(
										dstFileName -> {
											try (final BufferedReader dstFileReader = Files.newBufferedReader(Paths.get(dstFileName))) {
												while (null != dstFileReader.readLine()) {
													slicedLineCount.increment();
												}
											} catch (final IOException e) {
												fail(e.getMessage());
											}
										});
		assertEquals(SRC_LINE_COUNT, slicedLineCount.sum());
	}
}
