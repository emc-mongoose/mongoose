package com.emc.mongoose.client.impl.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.client.impl.load.executor.BasicMixedHttpDataLoadClient;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
// mongoose-server-api.jar
import com.emc.mongoose.core.impl.item.data.CsvFileDataItemInput;
import com.emc.mongoose.server.api.load.builder.HttpDataLoadBuilderSvc;
// mongoose-common.jar
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
// mongoose-client.jar
import com.emc.mongoose.client.impl.load.executor.BasicHttpDataLoadClient;
import com.emc.mongoose.client.api.load.builder.HttpDataLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.HttpDataLoadClient;
//
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
import com.emc.mongoose.server.api.load.executor.MixedHttpDataLoadSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 Created by kurila on 08.05.14.
 */
public final class BasicHttpDataLoadBuilderClient<
	T extends HttpDataItem, W extends HttpDataLoadSvc<T>, U extends HttpDataLoadClient<T, W>
> extends DataLoadBuilderClientBase<T, W, U, HttpDataLoadBuilderSvc<T, W>>
implements HttpDataLoadBuilderClient<T, W, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicHttpDataLoadBuilderClient()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	public BasicHttpDataLoadBuilderClient(final AppConfig appConfig)
	throws IOException {
		super(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected HttpRequestConfig<T, ? extends Container<T>> getDefaultIoConfig() {
		return HttpRequestConfigBase.getInstance();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected HttpDataLoadBuilderSvc<T, W> resolve(final String serverAddr)
	throws IOException {
		HttpDataLoadBuilderSvc<T, W> rlb;
		final String svcUri = "//" + serverAddr + '/' +
			getClass().getName().replace("client", "server").replace("Client", "Svc");
		rlb = (HttpDataLoadBuilderSvc<T, W>) ServiceUtil.getRemoteSvc(svcUri);
		rlb = (HttpDataLoadBuilderSvc<T, W>) ServiceUtil.getRemoteSvc(svcUri + rlb.fork());
		return rlb;
	}
	//
	@Override
	public final void invokePreConditions()
	throws IllegalStateException {
		((HttpRequestConfig) ioConfig).configureStorage(storageNodeAddrs);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws RemoteException {
		//
		final LoadType loadType = ioConfig.getLoadType();
		itemInput = selectItemInput(); // affects load service builders
		final Map<String, W> remoteLoadMap = new HashMap<>();
		HttpDataLoadBuilderSvc<T, W> nextBuilder;
		W nextLoad;
		for(final String addr : loadSvcMap.keySet()) {
			nextBuilder = loadSvcMap.get(addr);
			nextBuilder.setIoConfig(ioConfig); // should upload req conf right before instancing
			nextLoad = (W) ServiceUtil.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		if(LoadType.MIXED.equals(loadType)) {
			final List<String> inputFiles = (List<String>) appConfig
				.getProperty(AppConfig.KEY_ITEM_SRC_FILE);
			final List<String> loadPatterns = (List<String>) appConfig
				.getProperty(AppConfig.KEY_LOAD_TYPE);
			final Map<LoadType, Input<T>> itemInputMap = new HashMap<>();
			final Map<LoadType, Integer> loadTypeWeightMap = LoadType
				.getMixedLoadWeights(loadPatterns);
			if(inputFiles.size()==1) {
				final Path singleInputPath = Paths.get(inputFiles.get(0));
				for(final LoadType nextLoadType : loadTypeWeightMap.keySet()) {
					try {
						itemInputMap.put(
							nextLoadType,
							LoadType.WRITE.equals(nextLoadType) ?
								getNewItemInput() :
								new CsvFileDataItemInput<>(
									singleInputPath, (Class<T>) ioConfig.getItemClass(),
									ioConfig.getContentSource()
								)
						);
					} catch(final NoSuchMethodException | IOException e) {
						LogUtil.exception(LOG, Level.ERROR, e, "Failed to build new item src");
					}
				}
			} else if(inputFiles.size() == loadPatterns.size()) {
				final Iterator<String> inputFilesIterator = inputFiles.iterator();
				String nextInputFile;
				for(final LoadType nextLoadType : loadTypeWeightMap.keySet()) {
					nextInputFile = inputFilesIterator.next();
					try {
						itemInputMap.put(
							nextLoadType,
							LoadType.WRITE.equals(nextLoadType) && nextInputFile == null ?
								getNewItemInput() :
								new CsvFileDataItemInput<>(
									Paths.get(nextInputFile), (Class<T>) ioConfig.getItemClass(),
									ioConfig.getContentSource()
								)
						);
					} catch(final NoSuchMethodException | IOException e) {
						LogUtil.exception(LOG, Level.ERROR, e, "Failed to build new item src");
					}
				}
			} else {
				throw new IllegalStateException(
					"Unable to map the list of " + inputFiles.size() + " input files to " +
						loadPatterns.size() + " load jobs"
				);
			}
			//
			return (U) new BasicMixedHttpDataLoadClient<>(
				appConfig, (HttpRequestConfig) ioConfig, storageNodeAddrs, threadCount, countLimit,
				sizeLimit, rateLimit, (Map<String, MixedHttpDataLoadSvc<T>>) remoteLoadMap,
				itemInputMap, loadTypeWeightMap
			);
		} else {
			//
			return (U) new BasicHttpDataLoadClient<>(
				appConfig, (HttpRequestConfig) ioConfig, storageNodeAddrs, threadCount, itemInput,
				countLimit, sizeLimit, rateLimit, remoteLoadMap
			);
		}
	}
}
