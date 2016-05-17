package com.emc.mongoose.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 04.04.16.
 */
public class JsonUtil {

	private static final Logger LOG = LogManager.getLogger();

	public static final TypeReference PLAIN_JSON_TYPE = new PlainJsonType();
	public static final TypeReference COMMON_JSON_TYPE = new CommonJsonType();
	public static final TypeReference COMPLEX_JSON_TYPE = new ComplexJsonType();

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	static {
		JSON_MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
	}
	private static final Pattern COMMENT_PATTERN = Pattern.compile("(^[^\"]*)(//.*$)");


	/**
	 * @param pathString - the string of the path to a file
	 * @return json that contains the file tree if the file is a directory,
	 * or the name of the file otherwise
	 * @throws JsonProcessingException - an exception of the json building
	 */
	public static String jsonPathContent(final String pathString) throws JsonProcessingException {
		return jsonPathContent(new File(pathString));
	}

	/**
	 * @param file - some file
	 * @return json that contains the file tree if the file is a directory,
	 * or the name of the file otherwise
	 * @throws JsonProcessingException - an exception of the json building
	 */
	public static String jsonPathContent(final File file) throws JsonProcessingException {
		final List<Object> dirContent = new ArrayList<>();
		if (file.isFile()) {
			return file.getName();
		} else {
			listDirectoryContents(file.toPath(), dirContent);
			final Map<String, List<Object>> fileTree = new HashMap<>();
			fileTree.put(file.getName(), dirContent);
			return JSON_MAPPER.writeValueAsString(fileTree);
		}
	}

	public static String jsArrayPathContent(final String pathString) throws
			JsonProcessingException {
		return jsArrayPathContent(new File(pathString));
	}

	public static String jsArrayPathContent(final Path path) throws JsonProcessingException {
		return jsArrayPathContent(path.toFile());
	}

	public static String jsArrayPathContent(final File file) throws JsonProcessingException {
		final List<Object> dirContent = new ArrayList<>();
		if (file.isFile()) {
			return file.getName();
		} else {
			listDirectoryContents(file.toPath(), dirContent);
			return JSON_MAPPER.writeValueAsString(dirContent);
		}
	}

	private static void listDirectoryContents(final Path dirPath, final List<Object> rootList) {
		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
			Map<String, Object> dirMap;
			List<Object> subList;
			for (final Path file : stream) {
				if (file.toFile().isDirectory()) {
					dirMap = new HashMap<>();
					subList = new ArrayList<>();
					dirMap.put(file.getFileName().toString(), subList);
					rootList.add(dirMap);
					listDirectoryContents(file.toAbsolutePath(), subList);
				} else {
					rootList.add(file.getFileName().toString());
				}
			}
		} catch (final IOException e) {
			LOG.error("Failed to list the scenario directory");
		}
	}

	public static String readFileToString(final Path path) throws IOException {
		return readFileToString(path, false);
	}

	public static String readFileToString(final Path path, final boolean javaOneLineCommentRemove)
			throws IOException {
		String string;
		try (
				final BufferedReader reader =
						Files.newBufferedReader(path, StandardCharsets.US_ASCII)
		) {
			string = readString(reader, javaOneLineCommentRemove);
		}
		return string;
	}

	public static String readString(final BufferedReader reader) throws IOException {
		return readString(reader, false);
	}

	public static String readString(
			final BufferedReader reader, final boolean javaOneLineCommentRemove)
			throws IOException {
		final StringBuilder fileTextBuilder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			if (javaOneLineCommentRemove) {
				line = removeCommentariesFromLine(line);
			}
			fileTextBuilder.append(line).append('\n');
		}
		return fileTextBuilder.toString();
	}


	public static Map<String, String> readValue(final BufferedReader reader)
			throws IOException {
		final String plainJsonString = JsonUtil.readString(reader, false);
		return JSON_MAPPER.readValue(
				plainJsonString, PLAIN_JSON_TYPE
		);
	}

	private static String removeCommentariesFromLine(final String line) {
		final Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
		if (commentMatcher.find()) {
			return removeCommentariesFromLine(commentMatcher.group(1));
		} else {
			return line;
		}
	}

	private static final class PlainJsonType extends TypeReference<Map<String, String>> {
	}

	private static final class CommonJsonType extends TypeReference<Map<String, Object>> {
	}

	private static final class ComplexJsonType extends
			TypeReference<Map<String, Map<String, Object>>> {
	}

}
