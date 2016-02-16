package com.emc.mongoose.common.net.http.request;
//
import com.emc.mongoose.common.net.http.request.format.HeaderFormatter;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HttpContext;
//
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.emc.mongoose.common.net.http.request.format.HeaderFormatter.PATTERN_SYMBOL;

/**
Created by kurila on 30.01.15.
*/
public final class SharedHeadersAdder
implements HttpRequestInterceptor {
	//
	private final HeaderGroup sharedHeaders;
	private Map<String, HeaderFormatter> headerFormatters;
	//
	public SharedHeadersAdder(final HeaderGroup sharedHeaders) {
		this.sharedHeaders = sharedHeaders;
		this.headerFormatters = new HashMap<>();
	}
	//
	@Override
	public final void process(final HttpRequest request, final HttpContext context)
	throws HttpException, IOException {
		for(final Header nextHeader : sharedHeaders.getAllHeaders()) {
			if(!request.containsHeader(nextHeader.getName())) {
				if (nextHeader.getValue().indexOf(PATTERN_SYMBOL) > 0) {
					if (!headerFormatters.containsKey(nextHeader.getName())) {
						headerFormatters.put(nextHeader.getName(), new HeaderFormatter(nextHeader.getValue()));
					}
					request.setHeader(
							new BasicHeader(nextHeader.getName(), headerFormatters.get(nextHeader.getName()).format()));
				} else {
					request.setHeader(nextHeader);
				}
			}
		}
	}
}
