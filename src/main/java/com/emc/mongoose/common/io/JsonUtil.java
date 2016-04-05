package com.emc.mongoose.common.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created on 04.04.16.
 */
public class JsonUtil {

	private static final int BUFFER_SIZE = 8192;
	private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	/**
	 *
	 * @param pathString - the string of the path to a file
	 * @return json that contains the file tree if the file is a directory,
	 * or the name of the file otherwise
	 * @throws JsonProcessingException - an exception of the json building
	 */
	public static String jsonPathContent(final String pathString) throws JsonProcessingException {
		return jsonPathContent(new File(pathString));
	}

	/**
	 *
	 * @param file - some file
	 * @return json that contains the file tree if the file is a directory,
	 * or the name of the file otherwise
	 * @throws JsonProcessingException - an exception of the json building
	 */
	public static String jsonPathContent(final File file) throws JsonProcessingException {
		List<Object> dirContent = new ArrayList<>();
		if (file.isFile()) {
			return file.getName();
		} else {
			listDirectoryContents(file.toPath(), dirContent);
			Map<String, List<Object>> fileTree = new HashMap<>();
			fileTree.put(file.getName(), dirContent);
			return JSON_MAPPER.writeValueAsString(fileTree);
		}
	}

	public static String jsArrayPathContent(final String pathString) throws
			JsonProcessingException {
		return jsArrayPathContent(new File(pathString));
	}

	public static String jsArrayPathContent(final File file) throws JsonProcessingException {
		List<Object> dirContent = new ArrayList<>();
		if (file.isFile()) {
			return file.getName();
		} else {
			listDirectoryContents(file.toPath(), dirContent);
			return JSON_MAPPER.writeValueAsString(dirContent);
		}
	}

	public static String jsArrayPathContent(final Path path) throws JsonProcessingException {
		return jsArrayPathContent(path.toFile());
	}

	private static void listDirectoryContents(final Path dirPath, final List<Object> rootList) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
			for (Path file : stream) {
				if (file.toFile().isDirectory()) {
					Map<String, Object> dirMap = new HashMap<>();
					List<Object> subList = new ArrayList<>();
					dirMap.put(file.getFileName().toString(), subList);
					rootList.add(dirMap);
					listDirectoryContents(file.toAbsolutePath(), subList);
				} else {
					rootList.add(file.getFileName().toString());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static String readFileToString(Path path) throws IOException {
		return new String(readAllBytes(path));
	}

	/**
	 * see Java 8 java.nio.file.Files class
	 */
	private static byte[] readAllBytes(Path path) throws IOException {
		try (SeekableByteChannel sbc = Files.newByteChannel(path);
		     InputStream in = Channels.newInputStream(sbc)) {
			long size = sbc.size();
			if (size > (long)Integer.MAX_VALUE - 8)
				throw new OutOfMemoryError("Required array size too large");

			return read(in, (int)size);
		}
	}

	/**
	 * see Java 8 java.nio.file.Files class
	 */
	private static byte[] read(InputStream source, int initialSize) throws IOException {
		int capacity = initialSize;
		byte[] buf = new byte[capacity];
		int nread = 0;
		int n;
		for (;;) {
			while ((n = source.read(buf, nread, capacity - nread)) > 0)
				nread += n;

			if (n < 0 || (n = source.read()) < 0)
				break;

			if (capacity <= MAX_BUFFER_SIZE - capacity) {
				capacity = Math.max(capacity << 1, BUFFER_SIZE);
			} else {
				if (capacity == MAX_BUFFER_SIZE)
					throw new OutOfMemoryError("Required array size too large");
				capacity = MAX_BUFFER_SIZE;
			}
			buf = Arrays.copyOf(buf, capacity);
			buf[nread++] = (byte)n;
		}
		return (capacity == nread) ? buf : Arrays.copyOf(buf, nread);
	}

}
