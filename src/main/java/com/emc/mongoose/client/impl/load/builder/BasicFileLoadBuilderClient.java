package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.builder.FileLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.FileLoadClient;
//
import com.emc.mongoose.client.impl.load.executor.BasicFileLoadClient;
import com.emc.mongoose.client.impl.load.executor.BasicMixedFileLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
//
import com.emc.mongoose.core.impl.io.conf.BasicFileIoConfig;
//
import com.emc.mongoose.core.impl.item.data.CsvFileDataItemInput;
import com.emc.mongoose.server.api.load.builder.FileLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
import com.emc.mongoose.server.api.load.executor.MixedFileLoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 Created by kurila on 26.11.15.
 */
public class BasicFileLoadBuilderClient<
	T extends FileItem, W extends FileLoadSvc<T>, U extends FileLoadClient<T, W>
> extends DataLoadBuilderClientBase<T, W, U, FileLoadBuilderSvc<T, W>>
implements FileLoadBuilderClient<T, W, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicFileLoadBuilderClient()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	public BasicFileLoadBuilderClient(final AppConfig appConfig)
	throws IOException {
		super(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected FileIoConfig<T, ? extends Directory<T>> getIoConfig(final AppConfig appConfig) {
		return new BasicFileIoConfig<>(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected FileLoadBuilderSvc<T, W> resolve(final String serverAddr)
	throws IOException {
		FileLoadBuilderSvc<T, W> rlb;
		final String svcUri = "//" + serverAddr + '/' +
			getClass().getName().replace("client", "server").replace("Client", "Svc");
		rlb = (FileLoadBuilderSvc<T, W>) ServiceUtil.getRemoteSvc(svcUri);
		rlb = (FileLoadBuilderSvc<T, W>) ServiceUtil.getRemoteSvc(svcUri + rlb.fork());
		return rlb;
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException, RemoteException {
		FileLoadBuilderSvc<T, W> nextBuilder;
		for(final String addr : loadSvcMap.keySet()) {
			nextBuilder = loadSvcMap.get(addr);
			nextBuilder.invokePreConditions();
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually()
	throws RemoteException, DuplicateSvcNameException {
		final LoadType loadType = ioConfig.getLoadType();
		itemInput = selectItemInput(); // affects load service builders
		final Map<String, W> remoteLoadMap = new HashMap<>();
		FileLoadBuilderSvc<T, W> nextBuilder;
		W nextLoad;
		for(final String addr : loadSvcMap.keySet()) {
			nextBuilder = loadSvcMap.get(addr);
			nextBuilder.setIoConfig(ioConfig); // should upload req conf right before instancing
			nextLoad = (W) ServiceUtil.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		//
		if(LoadType.MIXED.equals(loadType)) {
			final Object inputFilesRaw = appConfig.getProperty(AppConfig.KEY_ITEM_SRC_FILE);
			final List<String> inputFiles;
			if(inputFilesRaw instanceof List) {
				inputFiles = (List<String>) inputFilesRaw;
			} else if(inputFilesRaw instanceof String){
				inputFiles = new ArrayList<>();
				inputFiles.add((String) inputFilesRaw);
			} else {
				throw new IllegalStateException(
					"Invalid configuration parameter type for " + AppConfig.KEY_ITEM_SRC_FILE +
						": \"" + inputFilesRaw + "\""
				);
			}
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
							LoadType.CREATE.equals(nextLoadType) ?
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
				Path nextInputPath;
				for(final LoadType nextLoadType : loadTypeWeightMap.keySet()) {
					nextInputPath = Paths.get(inputFilesIterator.next());
					try {
						itemInputMap.put(
							nextLoadType,
							nextInputPath == null && LoadType.CREATE.equals(nextLoadType) ?
								getNewItemInput() :
								new CsvFileDataItemInput<>(
									nextInputPath, (Class<T>) ioConfig.getItemClass(),
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
			return (U) new BasicMixedFileLoadClient<>(
				appConfig, (FileIoConfig<T, ? extends Directory<T>>) ioConfig, threadCount,
				countLimit, sizeLimit, rateLimit,
				(Map<String, MixedFileLoadSvc<T>>) remoteLoadMap, itemInputMap, loadTypeWeightMap
			);
		} else {
			return (U) new BasicFileLoadClient<>(
				appConfig, (FileIoConfig<T, ? extends Directory<T>>) ioConfig,
				appConfig.getLoadThreads(), itemInput, countLimit, sizeLimit, rateLimit,
				remoteLoadMap
			);
		}
	}
}
