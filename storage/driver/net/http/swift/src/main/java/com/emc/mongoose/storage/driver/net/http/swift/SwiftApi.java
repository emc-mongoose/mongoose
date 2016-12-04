package com.emc.mongoose.storage.driver.net.http.swift;

import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 Created by andrey on 07.10.16.
 */
public interface SwiftApi {

	String KEY_X_AUTH_KEY = "X-Auth-Key";
	String KEY_X_AUTH_TOKEN = "X-Auth-Token";
	String KEY_X_AUTH_USER = "X-Auth-User";
	String KEY_X_COPY_FROM = "X-Copy-From";
	String KEY_X_OBJECT_MANIFEST = "X-Object-Manifest";
	String KEY_X_VERSIONS_LOCATION = "X-Versions-Location";

	String URI_BASE = "/v1";
	String AUTH_URI = "/auth/v1.0";
	String DEFAULT_VERSIONS_LOCATION = "archive";

	String KEY_SIZE = "bytes";
	String KEY_ID = "name";

	Logger LOG = LogManager.getLogger();

	static <I extends Item> int parseContainerListingContent(
		final List<I> buff, final InputStream inStream, final ItemFactory<I> itemFactory,
		final int idRadix
	) throws IOException {

		int n = 0;
		boolean isInsideObjectToken = false;
		long lastSize = -1;
		long lastItemOffset;
		String lastItemId = null;
		I nextItem;

		try(final JsonParser jsonParser = new JsonFactory().createParser(inStream)) {
			final JsonToken rootToken = jsonParser.nextToken();
			JsonToken nextToken;
			if(JsonToken.START_ARRAY.equals(rootToken)) {
				do {
					nextToken = jsonParser.nextToken();
					switch(nextToken) {
						case START_OBJECT:
							if(isInsideObjectToken) {
								LOG.debug(Markers.ERR, "Looks like the json response is not plain");
							}
							isInsideObjectToken = true;
							break;
						case END_OBJECT:
							if(isInsideObjectToken) {
								if(lastItemId != null && lastSize > -1) {
									try {
										lastItemOffset = Long.parseLong(lastItemId, idRadix);
									} catch(final NumberFormatException e) {
										lastItemOffset = 0;
									}
									try {
										nextItem = itemFactory.getItem(
											lastItemId, lastItemOffset, lastSize
										);
										if(nextItem != null) {
											buff.add(nextItem);
											n ++;
										}
									} catch(final IllegalStateException e) {
										LogUtil.exception(
											LOG, Level.WARN, e,
											"Failed to create data item descriptor"
										);
									}
								} else {
									LOG.trace(
										Markers.ERR, "Invalid object id ({}) or size ({})",
										lastItemId, lastSize
									);
								}
							} else {
								LOG.debug(Markers.ERR, "End of json object is not inside object");
							}
							isInsideObjectToken = false;
							break;
						case FIELD_NAME:
							if(KEY_SIZE.equals(jsonParser.getCurrentName())) {
								lastSize = jsonParser.nextLongValue(-1);
							}
							if(KEY_ID.equals(jsonParser.getCurrentName())) {
								lastItemId = jsonParser.nextTextValue();
							}
							break;
						case VALUE_NUMBER_INT:
						case VALUE_STRING:
						case VALUE_NULL:
						case VALUE_FALSE:
						case VALUE_NUMBER_FLOAT:
						case VALUE_TRUE:
						case VALUE_EMBEDDED_OBJECT:
						case NOT_AVAILABLE:
						default:
							break;
					}
				} while(!JsonToken.END_ARRAY.equals(nextToken));
			} else {
				LOG.warn(
					Markers.ERR,
					"Response contains root JSON token \"{}\", but array token was expected"
				);
			}
		}

		return n;
	}
}
