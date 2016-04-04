package com.emc.mongoose.common.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 04.04.16.
 */
public class JsonUtil {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	/**
	 *
	 * @param pathString - the string of the path to a file
	 * @return json that contains the file tree if the file is a directory,
	 * or the name of the file otherwise
	 * @throws JsonProcessingException - an exception of the json building
	 */
	public static String jsonPathContent(String pathString) throws JsonProcessingException {
		List<Object> dirContent = new ArrayList<>();
		File file = new File(pathString);
		if (file.isFile()) {
			return file.getName();
		} else {
			listDirectoryContents(file.toPath(), dirContent);
			Map<String, List<Object>> fileTree = new HashMap<>();
			fileTree.put(file.getName(), dirContent);
			return JSON_MAPPER.writeValueAsString(fileTree);
		}

	}

	private static void listDirectoryContents(Path dirPath, List<Object> rootList) {
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

}
