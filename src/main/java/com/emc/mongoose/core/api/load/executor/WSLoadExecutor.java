package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.IOException;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadExecutor<T extends WSObject>
extends ObjectLoadExecutor<T> {
	HttpResponse execute(final HttpRequest request)
	throws IOException;
}
