package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.RemoteStorageMock;
import com.emc.mongoose.storage.mock.api.RemoteStorageMockListener;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.impl.base.RemoteStorageMockBase;
import com.emc.mongoose.storage.mock.impl.distribution.MDns;
import com.emc.mongoose.storage.mock.impl.distribution.BasicRemoteStorageMockListener;
import com.emc.mongoose.storage.mock.impl.http.request.AtmosRequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.S3RequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.SwiftRequestHandler;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.channel.ChannelInboundHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.storage.mock.impl.http.Nagaina.IDENTIFIER;
import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 Created on 31.08.16.
 */
public class RemoteNagaina
extends RemoteStorageMockBase<MutableDataItemMock> {

	private static final Logger LOG = LogManager.getLogger();

	private final Nagaina nagaina;
	private JmDNS jmDns;
	private RemoteStorageMockListener nodeListener;

	public RemoteNagaina(
		final Config.StorageConfig storageConfig, final Config.LoadConfig loadConfig,
		final Config.ItemConfig itemConfig
	) throws RemoteException {
		super();
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final Config.LoadConfig.LimitConfig limitConfig = loadConfig.getLimitConfig();
		final Config.ItemConfig.NamingConfig namingConfig = itemConfig.getNamingConfig();
		final Config.ItemConfig.DataConfig.ContentConfig contentConfig = itemConfig
			.getDataConfig()
			.getContentConfig();
		final String contentSourcePath = contentConfig.getFile();
		final ContentSource contentSource;
		try {
			contentSource = ContentSourceUtil.getInstance(
				contentSourcePath, contentConfig.getSeed(), contentConfig.getRingSize()
			);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to get content source on path {}", contentSourcePath
			);
			throw new IllegalStateException();
		}
		this.nagaina = new Nagaina(storageConfig, loadConfig, itemConfig, contentSource, handlers);
		handlers.add(new SwiftRequestHandler<>(limitConfig, namingConfig, this, contentSource));
		handlers.add(new AtmosRequestHandler<>(limitConfig, namingConfig, this, contentSource));
		handlers.add(new S3RequestHandler<>(limitConfig, namingConfig, this, contentSource));
		try {
//			System.setProperty("java.rmi.server.hostname", NetUtil.getHostAddrString()); workaround
			jmDns = JmDNS.create(NetUtil.getHostAddr());
			LOG.info("mDNS address: " + jmDns.getInetAddress());

		} catch(final IOException | OmgDoesNotPerformException | OmgLookAtMyConsoleException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to register Nagaina as service"
			);
		}
	}

	@Override
	public StorageMock<MutableDataItemMock> getLocalStorage()
	throws RemoteException {
		return nagaina;
	}

	@Override
	public void start()
	throws UserShootHisFootException, RemoteException {
		nagaina.start();
		try {
			LOG.info(Markers.MSG, "Register RMI method");
			Registry registry = null;
			try {
				registry = LocateRegistry.createRegistry(REGISTRY_PORT);
			} catch(final RemoteException e) {
				try {
					registry = LocateRegistry.getRegistry(REGISTRY_PORT);
				} catch(final RemoteException ie) {
					LogUtil.exception(
						LOG, Level.ERROR, ie, "Failed to obtain RMI registry"
					);
				}
			}
			if (registry != null) {
				registry.rebind(IDENTIFIER, this);
			}
			final ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", IDENTIFIER,
					MDns.DEFAULT_PORT, "storage mock");
			jmDns.registerService(serviceInfo);
			LOG.info("Nagaina registered as service");
			nodeListener = new BasicRemoteStorageMockListener(IDENTIFIER, jmDns, MDns.Type.HTTP);
			nodeListener.open();
			LOG.info(Markers.MSG, "Discover nodes");
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to start node discovering"
			);
		}
	}

	@Override
	public boolean isStarted()
	throws RemoteException {
		return nagaina.isStarted();
	}

	@Override
	public boolean await()
	throws InterruptedException, RemoteException {
		return nagaina.await();
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		return nagaina.await(timeout, timeUnit);
	}

	@Override
	public MutableDataItemMock getObjectRemotely(
		final String containerName, final String id, final long offset, final long size
	) throws RemoteException, ContainerMockException {
		return nagaina.getObject(containerName, id, offset, size);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<RemoteStorageMock<MutableDataItemMock>> getNodes()
	throws RemoteException {
		return nodeListener.getNodes();
	}

	@Override
	public void close()
	throws IOException {
		nodeListener.close();
		jmDns.unregisterAllServices();
		jmDns.close();
		nagaina.close();
	}
}
