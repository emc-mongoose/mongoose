package com.emc.mongoose.common.net.http.request;
//
import com.emc.mongoose.common.generator.ValueGenerator;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.generator.AsyncFormattingGenerator;
//
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HttpContext;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import static com.emc.mongoose.common.generator.AsyncFormattingGenerator.PATTERN_SYMBOL;

/**
Created by kurila on 30.01.15.
*/
public final class SharedHeadersAdder
implements HttpRequestInterceptor {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final HeaderGroup sharedHeaders;
	private final Map<String, ValueGenerator<String>> headerFormatters;
	//
	public SharedHeadersAdder(final HeaderGroup sharedHeaders) {
		this.sharedHeaders = sharedHeaders;
		this.headerFormatters = new HashMap<>();
	}
	//
	@Override
	public final void process(final HttpRequest request, final HttpContext context)
	throws HttpException, IOException {
		String headerName, headerValue;
		for(final Header nextHeader : sharedHeaders.getAllHeaders()) {
			headerName = nextHeader.getName();
			headerValue = nextHeader.getValue();
			if(!request.containsHeader(headerName)) {
				if (headerValue != null && headerValue.indexOf(PATTERN_SYMBOL) > -1) {
					if (!headerFormatters.containsKey(headerName)) {
						try {
							final ValueGenerator<String>
								formatter = new AsyncFormattingGenerator(headerValue);
							/*while(null == formatter.get()) {
								LockSupport.parkNanos(1);
								Thread.yield();
							}*/
							headerFormatters.put(headerName, formatter);
						} catch(final ParseException e) {
							LogUtil.exception(
								LOG, Level.ERROR, e, "Failed to parse the pattern \"{}\"",
								headerValue
							);
						}
					}
					request.setHeader(
						new BasicHeader(headerName, headerFormatters.get(headerName).get())
					);
				} else {
					request.setHeader(nextHeader);
				}
			}
		}
	}
}
