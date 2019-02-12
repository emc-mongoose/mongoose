package com.emc.mongoose.storage.driver.coop.jep321;

import static com.emc.mongoose.base.item.op.Operation.SLASH;
import static com.emc.mongoose.base.supply.PatternDefinedSupplier.PATTERN_CHAR;
import static com.emc.mongoose.storage.driver.coop.jep321.SwiftApi.ContainerState.EXISTS;
import static com.emc.mongoose.storage.driver.coop.jep321.SwiftApi.ContainerState.NOT_EXISTS;
import static com.emc.mongoose.storage.driver.coop.jep321.SwiftApi.ContainerState.UNKNOWN;
import static com.emc.mongoose.storage.driver.coop.jep321.SwiftApi.ContainerState.VERSIONING_ENABLED;

import com.emc.mongoose.base.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.base.config.IllegalArgumentNameException;
import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.exception.InterruptRunException;
import com.emc.mongoose.base.exception.OmgShootMyFootException;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.PathItem;
import com.emc.mongoose.base.item.PathItemImpl;
import com.emc.mongoose.base.item.TokenItem;
import com.emc.mongoose.base.item.TokenItemImpl;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.base.item.op.path.PathOperation;
import com.emc.mongoose.base.item.op.token.TokenOperation;
import com.emc.mongoose.base.item.op.token.TokenOperationImpl;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.storage.Credential;
import com.emc.mongoose.base.supply.BatchSupplier;
import com.emc.mongoose.base.supply.ConstantStringSupplier;
import com.emc.mongoose.base.supply.async.AsyncPatternDefinedSupplier;
import com.emc.mongoose.storage.driver.coop.CoopStorageDriverBase;
import com.emc.mongoose.storage.driver.coop.jep321.data.BodyPublisherDataCreate;
import com.emc.mongoose.storage.driver.coop.jep321.data.ResponseBodyHandler;
import com.github.akurilov.confuse.Config;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;

public class Jep321StorageDriverBase<I extends Item, O extends Operation<I>>
    extends CoopStorageDriverBase<I, O> implements Jep321StorageDriver, SwiftApi {

  private static final Function<String, BatchSupplier<String>> ASYNC_PATTERN_SUPPLIER_FUNC =
      pattern -> {
        try {
          return new AsyncPatternDefinedSupplier(ServiceTaskExecutor.INSTANCE, pattern);
        } catch (final OmgShootMyFootException e) {
          LogUtil.exception(Level.ERROR, e, "Failed to create the pattern defined input");
          return null;
        }
      };
  private static final TokenOperation<TokenItem> CREATE_AUTH_TOKEN_OP =
      new TokenOperationImpl<>(0, OpType.CREATE, new TokenItemImpl(" "), null);
  protected static final HttpRequest.BodyPublisher EMPTY_PUBLISHER =
      HttpRequest.BodyPublishers.noBody();
  protected static final HttpResponse.BodySubscriber<Void> DISCARDING_RESPONSE_BODY_SUBSCRIBER =
      HttpResponse.BodySubscribers.discarding();
  protected static final HttpResponse.BodyHandler<Void> DISCARDING_RESPONSE_BODY_HANDLER =
      (responseInfo) -> DISCARDING_RESPONSE_BODY_SUBSCRIBER;

  protected final AsyncCurrentDateSupplier dateSupplier =
      new AsyncCurrentDateSupplier(ServiceTaskExecutor.INSTANCE);
  protected final Map<String, String> headerPatterns = new HashMap<>();
  private final Map<String, BatchSupplier<String>> headerNameInputs = new ConcurrentHashMap<>();
  private final Map<String, BatchSupplier<String>> headerValueInputs = new ConcurrentHashMap<>();
  private final BatchSupplier<String> uriQueryInput;
  private final boolean sslFlag;
  private final String protocol;
  private final boolean versioning;
  private final String namespacePath;

  protected final HttpClient client;
  protected final String[] storageNodeAddrs;
  protected final int storageNodePort;
  protected final int connAttemptsLimit;
  protected final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder();

  protected Jep321StorageDriverBase(
      final String stepId,
      final DataInput dataInput,
      final Config storageConfig,
      final boolean verifyFlag,
      final int batchSize)
      throws OmgShootMyFootException {

    super(stepId, dataInput, storageConfig, verifyFlag, batchSize);

    final var driverConfig = storageConfig.configVal("driver");
    final var threads = driverConfig.intVal("threads");

    final var netConfig = storageConfig.configVal("net");

    sslFlag = netConfig.boolVal("ssl");
    protocol = sslFlag ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
    final var timeoutMillis = netConfig.intVal("timeoutMilliSec");
    final var timeoutDuration =
        Duration.ofMillis(timeoutMillis > 0 ? timeoutMillis : Long.MAX_VALUE);

    final var httpConfig = netConfig.configVal("http");
    final var httpVersion = Version.valueOf(httpConfig.stringVal("version"));
    client = HttpClient.newBuilder().version(httpVersion).build();
    reqBuilder.version(httpVersion);
    final var headersMap = httpConfig.<String>mapVal("headers");
    for (final var header : headersMap.entrySet()) {
      final var headerKey = header.getKey();
      final var headerValue = header.getValue();
      if (-1 < (headerKey).indexOf(PATTERN_CHAR) || -1 < headerValue.indexOf(PATTERN_CHAR)) {
        headerPatterns.put(headerKey, headerValue);
        headerNameInputs.computeIfAbsent(headerKey, ASYNC_PATTERN_SUPPLIER_FUNC);
        headerValueInputs.computeIfAbsent(headerValue, ASYNC_PATTERN_SUPPLIER_FUNC);
      } else {
        reqBuilder.header(headerKey, headerValue);
      }
    }
    final var uriArgs = httpConfig.<String>mapVal("uri-args");
    final var uriQueryPattern =
        uriArgs.entrySet().stream()
            .map(entry -> entry.getKey() + '=' + entry.getValue())
            .collect(Collectors.joining("&"));
    if (uriQueryPattern.length() == 0) {
      uriQueryInput = new ConstantStringSupplier("");
    } else {
      uriQueryInput = ASYNC_PATTERN_SUPPLIER_FUNC.apply("?" + uriQueryPattern);
    }

    // swift-specific
    versioning = httpConfig.boolVal("versioning");
    if (namespace == null) {
      throw new IllegalArgumentNameException("Namespace is not set");
    }
    namespacePath = URI_BASE + SLASH + namespace;

    final var nodeConfig = netConfig.configVal("node");
    storageNodePort = nodeConfig.intVal("port");
    connAttemptsLimit = nodeConfig.intVal("connAttemptsLimit");
    final var t = nodeConfig.<String>listVal("addrs").toArray(new String[] {});
    storageNodeAddrs = new String[t.length];
    String n;
    for (var i = 0; i < t.length; i++) {
      n = t[i];
      storageNodeAddrs[i] = n + (n.contains(":") ? "" : ":" + storageNodePort);
    }
  }

  @Override
  protected final boolean submit(final O op) throws InterruptRunException, IllegalStateException {
    if (concurrencyThrottle.tryAcquire()) {
      try {
        final var req = httpRequest(op);
        final var respBodyHandler = new ResponseBodyHandler<>(op, this::releaseCompleted);
        op.startRequest();
        op.finishRequest();
        client.sendAsync(req, respBodyHandler, null).handle(this::handleResponse);
        return true;
      } catch (final URISyntaxException e) {
        LogUtil.exception(Level.ERROR, e, "{}: failed to build the request URI", stepId);
      }
    }
    return false;
  }

  @Override
  protected final int submit(final List<O> ops, final int from, final int to)
      throws InterruptRunException, IllegalStateException {
    for (var i = from; i < to; i++) {
      submit(ops.get(i));
    }
    return to - from;
  }

  @Override
  protected final int submit(final List<O> ops)
      throws InterruptRunException, IllegalStateException {
    for (final var op : ops) {
      submit(op);
    }
    return ops.size();
  }

  private void releaseCompleted(final O op) {
    concurrencyThrottle.release();
    super.handleCompleted(op);
  }

  @Override
  protected String requestNewPath(final String path) throws InterruptRunException {
    final var container = new PathItemImpl(path);
    final var containerState = requestContainerState(container);
    // create or update the destination container if it doesn't exists
    if (NOT_EXISTS.equals(containerState) || (EXISTS.equals(containerState) && versioning)) {
      final var reqBuilder = httpRequestBuilder();
      applySharedHeaders(reqBuilder);
      if (versioning) {
        reqBuilder.header(KEY_X_VERSIONS_LOCATION, DEFAULT_VERSIONS_LOCATION);
      }
      final var containerPath = pathUriPath(container, null, null, null);
      final var method = pathHttpMethod(OpType.CREATE);
      applyAuthHeaders(reqBuilder, method, containerPath, credential);
      reqBuilder.method(method.name(), EMPTY_PUBLISHER);
      final var host = host();
      try {
        final var uri = new URI(protocol + "://" + host + containerPath);
        final var req = reqBuilder.uri(uri).build();
        try {
          final var respStatisCode =
              client.send(req, DISCARDING_RESPONSE_BODY_HANDLER).statusCode();
          if (respStatisCode < 200 || respStatisCode >= 300) {
            Loggers.ERR.warn(
                "{}: create/update container \"{}\" response status: {}",
                stepId,
                path,
                respStatisCode);
            return null;
          }
        } catch (final InterruptedException e) {
          throw new InterruptRunException(e);
        } catch (final IOException e) {
          LogUtil.exception(
              Level.WARN, e, "{}: failed to create the container {}", stepId, container);
        }
      } catch (final URISyntaxException e) {
        LogUtil.exception(
            Level.WARN, e, "{}: failed to create the container {}", stepId, container);
      }
    }
    return path;
  }

  private ContainerState requestContainerState(final PathItem container) {
    // check the destination container if it exists w/ HEAD request
    final var reqBuilder = httpRequestBuilder();
    final var method = HttpMethod.HEAD;
    reqBuilder.method(method.name(), EMPTY_PUBLISHER);
    applySharedHeaders(reqBuilder);
    final var containerPath = pathUriPath(container, null, null, null);
    final var credential = pathToCredMap.getOrDefault(container.name(), this.credential);
    applyAuthHeaders(reqBuilder, method, containerPath, credential);
    final var host = host();
    var containerState = UNKNOWN; // assume
    try {
      final var uri = new URI(protocol + "://" + host + containerPath);
      final var req = reqBuilder.uri(uri).build();
      try {
        final var resp = client.send(req, HttpResponse.BodyHandlers.discarding());
        final var respStatusCode = resp.statusCode();
        if (respStatusCode >= 200 && respStatusCode < 300) {
          Loggers.MSG.info("Container \"{}\" already exists", container);
          final var versionsLocation = resp.headers().firstValue(KEY_X_VERSIONS_LOCATION);
          if (versionsLocation.isPresent() && !versionsLocation.get().isEmpty()) {
            containerState = VERSIONING_ENABLED;
          } else {
            containerState = EXISTS;
          }
        } else if (respStatusCode == 404) {
          containerState = NOT_EXISTS;
        } else {
          Loggers.ERR.warn("Unexpected container checking response: {}", respStatusCode);
        }
      } catch (final InterruptedException e) {
        throw new InterruptRunException(e);
      } catch (final IOException e) {
        LogUtil.exception(
            Level.WARN, e, "{}: failed to check the container {} state", stepId, container);
      }
    } catch (final URISyntaxException e) {
      LogUtil.exception(
          Level.WARN, e, "{}: failed to check the container {} state", stepId, container);
    }
    return containerState;
  }

  @Override
  protected String requestNewAuthToken(final Credential credential) throws InterruptRunException {
    var authTokenVal = (String) null;
    final var reqBuilder = httpRequestBuilder();
    final var method = tokenHttpMethod(OpType.CREATE);
    reqBuilder.method(method.name(), EMPTY_PUBLISHER);
    applyAuthHeaders(reqBuilder, method, AUTH_URI, credential);
    reqBuilder.header("Accept", "*/*");
    try {
      final var uri = uri(CREATE_AUTH_TOKEN_OP);
      reqBuilder.uri(uri);
      final var req = reqBuilder.build();
      try {
        authTokenVal =
            client
                .send(req, DISCARDING_RESPONSE_BODY_HANDLER)
                .headers()
                .firstValue(KEY_X_AUTH_TOKEN)
                .orElse(null);
      } catch (final InterruptedException e) {
        throw new InterruptRunException(e);
      } catch (final IOException e) {
        LogUtil.exception(Level.WARN, e, "{}: failed to create new auth token", stepId);
      }
    } catch (final URISyntaxException e) {
      LogUtil.exception(Level.WARN, e, "{}: failed to create new auth token", stepId);
    }
    return authTokenVal;
  }

  @Override
  public List<I> list(
      final ItemFactory<I> itemFactory,
      final String path,
      final String prefix,
      final int idRadix,
      final I lastPrevItem,
      final int count)
      throws InterruptRunException, IOException {
    return null;
  }

  @Override
  public final void adjustIoBuffers(final long avgTransferSize, final OpType opType) {}

  private HttpRequest.Builder httpRequestBuilder() {
    return reqBuilder.copy();
  }

  private final AtomicInteger nodeRoundRobinCounter = new AtomicInteger(0);

  protected HttpRequest httpRequest(final O op) throws URISyntaxException {
    final var reqBuilder = httpRequestBuilder();
    // method
    final var method = httpMethod(op);
    final var publisher = bodyPublisher(op);
    reqBuilder.method(method.name(), publisher);
    // URI
    final var uri = uri(op);
    reqBuilder.uri(uri);
    // headers
    applySharedHeaders(reqBuilder);
    applyAuthHeaders(reqBuilder, method, op.dstPath(), credential);
    return reqBuilder.build();
  }

  protected HttpMethod httpMethod(final O op) {
    final var opType = op.type();
    if (op instanceof DataOperation) {
      return dataHttpMethod(opType);
    } else if (op instanceof PathOperation) {
      return pathHttpMethod(opType);
    } else if (op instanceof TokenOperation) {
      return tokenHttpMethod(opType);
    } else {
      throw new AssertionError("Unexpected item type: " + op.item().getClass());
    }
  }

  protected HttpMethod dataHttpMethod(final OpType opType) {
    switch (opType) {
      case READ:
        return HttpMethod.GET;
      case DELETE:
        return HttpMethod.DELETE;
      default:
        return HttpMethod.PUT;
    }
  }

  protected HttpMethod tokenHttpMethod(final OpType opType) {
    switch (opType) {
      case NOOP:
      case CREATE:
        return HttpMethod.GET;
      default:
        throw new AssertionError("Not implemented yet");
    }
  }

  protected HttpMethod pathHttpMethod(final OpType opType) {
    switch (opType) {
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

  protected BodyPublisher bodyPublisher(final O op) {
    final var opType = op.type();
    switch (opType) {
      case NOOP:
        return EMPTY_PUBLISHER;
      case CREATE:
        if (op instanceof DataOperation) {
          return new BodyPublisherDataCreate((DataOperation) op);
        } else if (op instanceof PathOperation) {
          throw new AssertionError("Not implemented yet");
        } else if (op instanceof TokenOperation) {
          throw new AssertionError("Not implemented yet");
        }
      case READ:
        return EMPTY_PUBLISHER;
      case UPDATE:
        throw new AssertionError("Not implemented yet");
      case DELETE:
        return EMPTY_PUBLISHER;
      case LIST:
        throw new AssertionError("Not implemented yet");
      default:
        throw new AssertionError("Not implemented yet");
    }
  }

  protected URI uri(final Operation<? extends Item> op) throws URISyntaxException {
    final var host = host();
    final var path = uriPath(op);
    final var query = uriQueryInput.get();
    return new URI(protocol + "://" + host + path + query);
  }

  protected String host() {
    return storageNodeAddrs[nodeRoundRobinCounter.incrementAndGet() % storageNodeAddrs.length];
  }

  protected String uriPath(final Operation<? extends Item> op) {
    final var item = op.item();
    final var dstPath = op.dstPath();
    final var srcPath = op.srcPath();
    final var opType = op.type();
    if (op instanceof DataOperation) {
      return dataUriPath((DataItem) item, srcPath, dstPath, opType);
    } else if (op instanceof PathOperation) {
      return pathUriPath((PathItem) item, srcPath, dstPath, opType);
    } else if (op instanceof TokenOperation) {
      return tokenUriPath((TokenItem) item, srcPath, dstPath, opType);
    } else {
      throw new AssertionError("Unexpected item type: " + op.item().getClass());
    }
  }

  protected final String dataUriPath(
      final DataItem item, final String srcPath, final String dstPath, final OpType opType) {
    final String itemPath;
    if (dstPath != null) {
      itemPath = dstPath.startsWith(SLASH) ? dstPath : SLASH + dstPath;
    } else if (srcPath != null) {
      itemPath = srcPath.startsWith(SLASH) ? srcPath : SLASH + srcPath;
    } else {
      itemPath = null;
    }
    final var itemNameRaw = item.name();
    final var itemName = itemNameRaw.startsWith(SLASH) ? itemNameRaw : SLASH + itemNameRaw;
    return namespacePath
        + ((itemPath == null || itemName.startsWith(itemPath)) ? itemName : (itemPath + itemName));
  }

  protected final String pathUriPath(
      final PathItem item, final String srcPath, final String dstPath, final OpType opType) {
    final var itemName = item.name();
    if (itemName.startsWith(SLASH)) {
      return namespacePath + itemName;
    } else {
      return namespacePath + SLASH + itemName;
    }
  }

  protected final String tokenUriPath(
      final TokenItem item, final String srcPath, final String dstPath, final OpType opType) {
    return AUTH_URI;
  }

  protected void applySharedHeaders(final HttpRequest.Builder reqBuilder) {
    // reqBuilder.header("Date", dateSupplier.get());
    String name;
    String value;
    for (final var headerPattern : headerPatterns.entrySet()) {
      name = headerNameInputs.get(headerPattern.getKey()).get();
      value = headerValueInputs.get(headerPattern.getValue()).get();
      reqBuilder.header(name, value);
    }
  }

  protected void applyMetaDataHeaders(final HttpRequest.Builder reqBuilder) {}

  protected void applyAuthHeaders(
      final HttpRequest.Builder reqBuilder,
      final HttpMethod httpMethod,
      final String dstUriPath,
      final Credential credential) {
    final String authToken;
    final String uid;
    final String secret;
    if (credential != null) {
      authToken = authTokens.get(credential);
      uid = credential.getUid();
      secret = credential.getSecret();
    } else if (this.credential != null) {
      authToken = authTokens.get(this.credential);
      uid = this.credential.getUid();
      secret = this.credential.getSecret();
    } else {
      authToken = authTokens.get(Credential.NONE);
      uid = null;
      secret = null;
    }
    if (dstUriPath.equals(AUTH_URI)) {
      if (uid != null && !uid.isEmpty()) {
        reqBuilder.header(KEY_X_AUTH_USER, uid);
      }
      if (secret != null && !secret.isEmpty()) {
        reqBuilder.header(KEY_X_AUTH_KEY, secret);
      }
    } else if (authToken != null && !authToken.isEmpty()) {
      reqBuilder.header(KEY_X_AUTH_TOKEN, authToken);
    }
  }

  protected void applyCopyHeaders(final HttpRequest.Builder reqBuilder, final String srcPath)
      throws URISyntaxException {
    reqBuilder.header(
        KEY_X_COPY_FROM,
        srcPath != null && !srcPath.isEmpty() && srcPath.startsWith(namespacePath)
            ? srcPath.substring(namespacePath.length())
            : srcPath);
  }

  protected Object handleResponse(final HttpResponse resp, final Throwable thrown) {
    if (thrown == null) {
      throw new AssertionError("Unexpected behaviour");
    } else {
      LogUtil.trace(Loggers.ERR, Level.WARN, thrown, "{}: HTTP request failed", stepId);
    }
    return null;
  }

  @Override
  protected void doClose() throws IOException {
    super.doClose();
    uriQueryInput.close();
    dateSupplier.close();
    headerPatterns.clear();
    headerNameInputs
        .values()
        .forEach(
            headerNameInput -> {
              try {
                headerNameInput.close();
              } catch (final IOException ignored) {
              }
            });
    headerNameInputs.clear();
    headerValueInputs
        .values()
        .forEach(
            headerValueInput -> {
              try {
                headerValueInput.close();
              } catch (final IOException ignored) {
              }
            });
    headerValueInputs.clear();
  }
}
