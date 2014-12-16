package com.emc.mongoose.web.load;
//
import com.emc.mongoose.object.load.ObjectLoadExecutor;
import com.emc.mongoose.web.data.WSObject;
//
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.IOException;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadExecutor<T extends WSObject>
extends ObjectLoadExecutor<T> {
	HttpResponse execute(final HttpHost tgtHost, final HttpRequest request)
	throws IOException;
}
