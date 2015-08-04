package com.emc.mongoose.core.impl.load.builder;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.WSObject;

import com.emc.mongoose.core.api.io.req.conf.ObjectRequestConfig;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.load.builder.ObjectLoadBuilder;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import com.emc.mongoose.core.impl.io.req.conf.ObjectRequestConfigBase;
import com.emc.mongoose.core.impl.io.req.conf.RequestConfigBase;
import com.emc.mongoose.core.impl.io.req.conf.WSRequestConfigBase;
import com.emc.mongoose.core.impl.load.builder.LoadBuilderBase;
import com.emc.mongoose.core.impl.load.executor.BasicWSLoadExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.crypto.Data;
import java.util.NoSuchElementException;

/**
 * Created by Brandon on 7/28/15.
 */
public class BasicObjectLoadBuilder<T extends DataObject, U extends ObjectLoadExecutor<T>> extends LoadBuilderBase {

    private final static Logger LOG = LogManager.getLogger();
    //
    public BasicObjectLoadBuilder(final RunTimeConfig runTimeConfig, WSRequestConfig wsReqConf, String[] dataNodeAddrs, Object o, String listFile, long maxCount, long minObjSize, long maxObjSize, float objSizeBias, float rateLimit, int updatesPerItem) {
        super(runTimeConfig);
        setProperties(runTimeConfig);
    }
    //
    @Override @SuppressWarnings("unchecked")
    protected ObjectRequestConfig<T> getDefaultRequestConfig(){
        return (ObjectRequestConfig<T>) WSRequestConfigBase.getInstance();
    }


    //
    @Override
    public ObjectLoadBuilder<T, U> setProperties(final RunTimeConfig runTimeConfig) {
        //
        super.setProperties(runTimeConfig);
        //
        final String paramName = RunTimeConfig.KEY_STORAGE_SCHEME;
        try {
            RequestConfig.class.cast(reqConf).setScheme(runTimeConfig.getStorageProto());
        } catch(final NoSuchElementException e) {
            LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
        } catch(final IllegalArgumentException e) {
            LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
        }
        //
        return (ObjectLoadBuilder<T, U>) this;
    }
    //
    @Override @SuppressWarnings("CloneDoesntCallSuperClone")
    public final BasicObjectLoadBuilder<T, U> clone()
            throws CloneNotSupportedException {
        final BasicObjectLoadBuilder<T, U> lb = (BasicObjectLoadBuilder<T, U>) super.clone();
        LOG.debug(Markers.MSG, "Cloning request config for {}", reqConf.toString());
        return lb;
    }
    //
    @Override
    protected void invokePreConditions()
            throws IllegalStateException {
        reqConf.configureStorage(dataNodeAddrs);
    }
    //
    @Override @SuppressWarnings("unchecked")
    protected U buildActually() {
        if(reqConf == null) {
            throw new IllegalStateException("Should specify request builder instance");
        }
        //
        final WSRequestConfig wsReqConf = WSRequestConfig.class.cast(reqConf);
        final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
        if(minObjSize > maxObjSize) {
            throw new IllegalStateException(
                    String.format(
                            "Min object size (%s) shouldn't be more than max (%s)",
                            SizeUtil.formatSize(minObjSize), SizeUtil.formatSize(maxObjSize)
                    )
            );
        }
        //
        return (U) new BasicObjectLoadBuilder<>(
               localRunTimeConfig, wsReqConf, dataNodeAddrs, threadsPerNodeMap.get(loadType),
               listFile, maxCount, minObjSize, maxObjSize, objSizeBias, rateLimit, updatesPerItem)
        ;
    }
}
