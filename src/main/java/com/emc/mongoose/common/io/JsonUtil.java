package com.emc.mongoose.common.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 04.04.16.
 */
public class JsonUtil {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	public String jsonFileTree(String pathString) throws JsonProcessingException {
		List<Map<String, ?>> rootList = new ArrayList<>();
		listDirectoryContents(pathString, rootList);
		return JSON_MAPPER.writeValueAsString(rootList);
	}

	private static void listDirectoryContents(String pathString, List<Map<String, ?>> rootList) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(pathString))) {
			for (Path file : stream) {
				if (file.toFile().isDirectory()) {
					Map<String, Object> dirMap = new HashMap<>();
					dirMap.put("type", "dir");
					dirMap.put("name", file.getFileName().toString());
					List<Map<String, ?>> subList = new ArrayList<>();
					dirMap.put("children", subList);
					rootList.add(dirMap);
					listDirectoryContents(file.toAbsolutePath().toString(), subList);
				} else {
					Map<String, String> fileMap = new HashMap<>();
					fileMap.put("type", "file");
					fileMap.put("name", file.getFileName().toString());
					rootList.add(fileMap);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
