package com.emc.mongoose.client.impl.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.data.model.FileDataItemSrc;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-server-api.jar
import com.emc.mongoose.core.impl.data.model.CSVFileItemSrc;
import com.emc.mongoose.core.impl.load.builder.LoadBuilderBase;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
// mongoose-client.jar
import com.emc.mongoose.client.impl.load.executor.BasicWSLoadClient;
import com.emc.mongoose.client.api.load.builder.WSLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.WSLoadClient;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 08.05.14.
 */
public final class BasicWSLoadBuilderClient<T extends WSObject, U extends WSLoadClient<T>>
extends LoadBuilderClientBase<T, U>
implements WSLoadBuilderClient<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSLoadBuilderClient()
	throws IOException {
		super();
	}
	//
	public BasicWSLoadBuilderClient(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSRequestConfig<T> getDefaultRequestConfig() {
		return (WSRequestConfig<T>) WSRequestConfigBase.getInstance();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSLoadBuilderSvc<T, U> resolve(final String serverAddr)
	throws IOException {
		WSLoadBuilderSvc<T, U> rlb;
		final Service remoteSvc = ServiceUtils.getRemoteSvc(
			"//" + serverAddr + '/' + getClass().getPackage().getName().replace("client", "server")
		);
		if(remoteSvc == null) {
			throw new IOException("No remote load builder was resolved from " + serverAddr);
		} else if(remoteSvc instanceof WSLoadBuilderSvc) {
			rlb = (WSLoadBuilderSvc<T, U>) remoteSvc;
		} else {
			throw new IOException(
				"Illegal class " + remoteSvc.getClass().getCanonicalName() +
				" of the instance resolved from " + serverAddr
			);
		}
		return rlb;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final BasicWSLoadBuilderClient<T, U> setInputFile(final String listFile)
	throws RemoteException {
		this.listFile = listFile;
		if(listFile != null) {
			try {
				final FileDataItemSrc<T> fileInput = new CSVFileItemSrc<>(
					Paths.get(listFile), (Class<T>) BasicWSObject.class
				);
				final long approxDataItemsSize = fileInput.getApproxDataItemsSize(
					runTimeConfig.getBatchSize()
				);
				reqConf.setBuffSize(
					approxDataItemsSize < Constants.BUFF_SIZE_LO ?
						Constants.BUFF_SIZE_LO :
						approxDataItemsSize > Constants.BUFF_SIZE_HI ?
							Constants.BUFF_SIZE_HI : (int) approxDataItemsSize
				);
			} catch(final NoSuchMethodException | IOException e) {
				LOG.error(Markers.ERR, "Failure", e);
			}
		}
		return this;
	}
	//
	@Override
	protected final void invokePreConditions()
	throws IllegalStateException {
		reqConf.configureStorage(nodeAddrs);
	}
	//
	@Override  @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws RemoteException {
		//
		final Map<String, LoadSvc<T>> remoteLoadMap = new ConcurrentHashMap<>();
		//
		LoadBuilderSvc<T, U> nextBuilder;
		LoadSvc<T> nextLoad;
		//
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRequestConfig(reqConf); // should upload req conf right before instancing
			nextLoad = (LoadSvc<T>) ServiceUtils.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		//
		final DataItemSrc<T> itemSrc = LoadBuilderBase.buildItemInput(
			(Class<T>) BasicWSObject.class, reqConf, nodeAddrs, listFile, maxCount,
			minObjSize, maxObjSize, objSizeBias
		);
		//
		return (U) new BasicWSLoadClient<>(
			runTimeConfig, remoteLoadMap, (WSRequestConfig<T>) reqConf,
			runTimeConfig.getLoadLimitCount(), itemSrc
		);
	}
}
