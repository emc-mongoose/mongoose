package com.emc.mongoose.storage.driver.coop.jep321;

import com.emc.mongoose.base.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.exception.InterruptRunException;
import com.emc.mongoose.base.exception.OmgShootMyFootException;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.base.item.op.path.PathOperation;
import com.emc.mongoose.base.item.op.token.TokenOperation;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.storage.Credential;
import com.emc.mongoose.base.supply.BatchSupplier;
import com.emc.mongoose.base.supply.ConstantStringSupplier;
import com.emc.mongoose.base.supply.async.AsyncPatternDefinedSupplier;
import com.emc.mongoose.storage.driver.coop.CoopStorageDriverBase;

import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.emc.mongoose.base.item.op.Operation.SLASH;
import static com.emc.mongoose.base.supply.PatternDefinedSupplier.PATTERN_CHAR;

public class Jep321StorageDriverBase<I extends Item, O extends Operation<I>>
extends CoopStorageDriverBase<I, O>
implements Jep321StorageDriver {

	private static final Function<String, BatchSupplier<String>> ASYNC_PATTERN_SUPPLIER_FUNC = pattern -> {
		try {
			return new AsyncPatternDefinedSupplier(ServiceTaskExecutor.INSTANCE, pattern);
		} catch(final OmgShootMyFootException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to create the pattern defined input");
			return null;
		}
	};
	protected final AsyncCurrentDateSupplier dateSupplier = new AsyncCurrentDateSupplier(ServiceTaskExecutor.INSTANCE);

	protected final Map<String, String> headerPatterns = new HashMap<>();
	private final Map<String, BatchSupplier<String>> headerNameInputs = new ConcurrentHashMap<>();
	private final Map<String, BatchSupplier<String>> headerValueInputs = new ConcurrentHashMap<>();
	private final BatchSupplier<String> uriQueryInput;
	private final boolean sslFlag;

	protected final HttpClient client;
	protected final String storageNodeAddrs[];
	protected final int storageNodePort;
	protected final int connAttemptsLimit;
	protected final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder();
	private final HttpRequest.BodyPublisher emptyPublisher = HttpRequest.BodyPublishers.noBody();

	protected Jep321StorageDriverBase(
		final String stepId, final DataInput dataInput, final Config storageConfig, final boolean verifyFlag,
		final int batchSize
	) throws OmgShootMyFootException {

		super(stepId, dataInput, storageConfig, verifyFlag, batchSize);

		final var netConfig = storageConfig.configVal("net");

		sslFlag = netConfig.boolVal("ssl");
		final var timeoutMillis = netConfig.intVal("timeoutMilliSec");
		final var timeoutDuration = Duration.ofMillis(timeoutMillis > 0 ? timeoutMillis : Long.MAX_VALUE);

		final var httpConfig = netConfig.configVal("http");
		final var httpVersion = HttpClient.Version.valueOf(httpConfig.stringVal("version"));
		client = HttpClient
			.newBuilder()
			.connectTimeout(timeoutDuration)
			.version(httpVersion)
			.build();
		final var headersMap = httpConfig.<String>mapVal("headers");
		for(final var header : headersMap.entrySet()) {
			final var headerKey = header.getKey();
			final var headerValue = header.getValue();
			if(-1 < (headerKey).indexOf(PATTERN_CHAR) || -1 < headerValue.indexOf(PATTERN_CHAR)) {
				headerPatterns.put(headerKey, headerValue);
				headerNameInputs.computeIfAbsent(headerKey, ASYNC_PATTERN_SUPPLIER_FUNC);
				headerValueInputs.computeIfAbsent(headerValue, ASYNC_PATTERN_SUPPLIER_FUNC);
			} else {
				reqBuilder.header(headerKey, headerValue);
			}
		}
		final var uriArgs = httpConfig.<String>mapVal("uri-args");
		final var uriQueryPattern = uriArgs
			.entrySet()
			.stream()
			.map(entry -> entry.getKey() + '=' + entry.getValue())
			.collect(Collectors.joining("&"));
		if(uriQueryPattern.length() > 0) {
			uriQueryInput = new ConstantStringSupplier("");
		} else {
			uriQueryInput = ASYNC_PATTERN_SUPPLIER_FUNC.apply("?" + uriQueryPattern);
		}

		final var nodeConfig = netConfig.configVal("node");
		storageNodePort = nodeConfig.intVal("port");
		connAttemptsLimit = nodeConfig.intVal("connAttemptsLimit");
		final var t = nodeConfig.<String>listVal("addrs").toArray(new String[]{});
		storageNodeAddrs = new String[t.length];
		String n;
		for(var i = 0; i < t.length; i ++) {
			n = t[i];
			storageNodeAddrs[i] = n + (n.contains(":") ? "" : ":" + storageNodePort);
		}
	}

	@Override
	protected final boolean submit(final O op)
	throws InterruptRunException, IllegalStateException {
		final var req = httpRequest(op);
		client.sendAsync(req, this).th
		return false;
	}

	@Override
	protected final int submit(final List<O> ops, final int from, final int to)
	throws InterruptRunException, IllegalStateException {
		return 0;
	}

	@Override
	protected final int submit(final List<O> ops)
	throws InterruptRunException, IllegalStateException {
		return 0;
	}

	@Override
	protected String requestNewPath(final String path)
	throws InterruptRunException {
		return null;
	}

	@Override
	protected String requestNewAuthToken(final Credential credential)
	throws InterruptRunException {
		return null;
	}

	@Override
	public List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws InterruptRunException, IOException {
		return null;
	}

	@Override
	public final void adjustIoBuffers(final long avgTransferSize, final OpType opType) {
	}

	private HttpRequest.Builder httpRequestBuilder() {
		return reqBuilder.copy();
	}

	private final AtomicInteger nodeRoundRobinCounter = new AtomicInteger(0);

	protected HttpRequest httpRequest(final O op) {
		final var reqBuilder = httpRequestBuilder();
		// method
		final var method = httpMethod(op);
		final var publisher = bodyPublisher(op);
		reqBuilder.method(method.name(), publisher);
		// URI
		final var uri = uri(op);
		reqBuilder.uri(uri);
		// headers
		String name;
		String value;
		for(final var headerPattern: headerPatterns.entrySet()) {
			name = headerNameInputs.get(headerPattern.getKey()).get();
			value = headerValueInputs.get(headerPattern.getValue()).get();
			reqBuilder.header(name, value);
		}
		return reqBuilder.build();
	}

	protected HttpMethod httpMethod(final O op) {
		final var opType = op.type();
		if(op instanceof DataOperation) {
			return dataHttpMethod(opType);
		} else if(op instanceof PathOperation) {
			return pathHttpMethod(opType);
		} else if(op instanceof TokenOperation) {
			return tokenHttpMethod(opType);
		} else {
			throw new AssertionError("Unexpected item type: " + op.item().getClass());
		}
	}

	protected HttpMethod dataHttpMethod(final OpType opType) {
		switch(opType) {
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				return HttpMethod.PUT;
		}
	}

	protected HttpMethod tokenHttpMethod(final OpType opType) {
		switch(opType) {
			case NOOP:
			case CREATE:
				return HttpMethod.GET;
			default:
				throw new AssertionError("Not implemented yet");
		}
	}

	protected HttpMethod pathHttpMethod(final OpType opType) {
		switch(opType) {
			case NOOP:
			case CREATE:
				return HttpMethod.PUT;
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				throw new AssertionError("Not implemented yet");
		}
	}

	protected HttpRequest.BodyPublisher bodyPublisher(final O op) {
		final var opType = op.type();
		switch(opType) {
			case NOOP:
				return emptyPublisher;
			case CREATE:
				throw new AssertionError("Not implemented yet");
			case READ:
				return emptyPublisher;
			case UPDATE:
				throw new AssertionError("Not implemented yet");
			case DELETE:
				return emptyPublisher;
			case LIST:
				throw new AssertionError("Not implemented yet");
			default:
				throw new AssertionError("Not implemented yet");
		}
	}

	protected URI uri(final O op) {
		final var proto = sslFlag ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
		final var host = storageNodeAddrs[nodeRoundRobinCounter.incrementAndGet() % storageNodeAddrs.length];
		final var path = uriPath(op);
		final var query = uriQueryInput.get();
		return new URI(
			proto + "://" + (host.contains(":") ? host : host + ':' + storageNodePort)  + SLASH + path + query
		);
	}

	protected String uriPath(final O op) {
		if(op instanceof DataOperation) {
			return
		} else if(op instanceof PathOperation) {

		} else if(op instanceof TokenOperation) {

		} else {
			throw new AssertionError("Unexpected item type: " + op.item().getClass());
		}
	}

	@Override
	protected void doClose()
	throws IOException {
		super.doClose();
		uriQueryInput.close();
		dateSupplier.close();
		headerPatterns.clear();
		headerNameInputs.values().forEach(headerNameInput -> {
			try {
				headerNameInput.close();
			} catch(final IOException ignored) {
			}
		});
		headerNameInputs.clear();
		headerValueInputs.values().forEach(headerValueInput -> {
			try {
				headerValueInput.close();
			} catch(final IOException ignored) {
			}
		});
		headerValueInputs.clear();
	}
}
