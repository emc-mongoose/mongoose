package com.emc.mongoose.monitor.impl;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.monitor.api.TypedFactory;
import com.emc.mongoose.storage.driver.http.s3.HttpS3Driver;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by on 9/21/2016.
 */
public class HttpDriverFactory<I extends MutableDataItem, O extends DataIoTask<I>>
implements TypedFactory<Driver<I, O>, HttpDriverFactory.Protocol> {

    private final String runId;
    private final LoadConfig loadConfig;
    private final StorageConfig storageConfig;
    private final String srcContainer;
    private final SocketConfig socketConfig;

    public HttpDriverFactory(
            final String runId,
            final LoadConfig loadConfig, final StorageConfig storageConfig,
            final String srcContainer,
            final SocketConfig socketConfig
    ) {
        this.runId = runId;
        this.loadConfig = loadConfig;
        this.storageConfig = storageConfig;
        this.srcContainer = srcContainer;
        this.socketConfig = socketConfig;
    }

    @Override
    public Driver<I, O> create(final Protocol type) {
        final Logger log = LogManager.getLogger();
        switch(type) {
            case ATMOS:
                break;
            case SWIFT:
                break;
            case S3:
                try {
                    return new HttpS3Driver<>(
                            runId,
                            loadConfig, storageConfig,
                            srcContainer,
                            socketConfig
                    );
                } catch (final UserShootHisFootException e) {
                    log.error(e);
                }
        }
        return null;
    }

    public enum Protocol {
        S3, SWIFT, ATMOS
    }
}
