package com.emc.mongoose.web.api;
//
import com.emc.mongoose.object.api.ObjectIOClient;
import com.emc.mongoose.web.data.WSObject;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
/**
 Created by kurila on 02.12.14.
 */
public interface WSClient<T extends WSObject>
extends ObjectIOClient<T> {
	HttpResponse execute(final HttpHost tgtHost, final HttpRequest request);
}
