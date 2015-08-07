package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.http.RequestSharedHeaders;
import com.emc.mongoose.common.http.RequestTargetHost;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.conf.ObjectRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.task.WSIOTask;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
//
import com.emc.mongoose.core.impl.data.BasicObject;
import com.emc.mongoose.core.impl.io.task.BasicObjectIOTask;
import com.emc.mongoose.core.impl.io.task.BasicWSIOTask;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.load.tasks.HttpClientRunTask;
//
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestUserAgent;
//
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.xml.crypto.Data;
import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.12.14.
 */
public class BasicObjectLoadExecutor<T extends DataObject>
        extends ObjectLoadExecutorBase<T>
{//implements ObjectLoadExecutor<T> {
    //
    private final static Logger LOG = LogManager.getLogger();
    //


    //
    @SuppressWarnings("unchecked")
    public BasicObjectLoadExecutor(
            final RunTimeConfig runTimeConfig, final ObjectRequestConfig<T> reqConfig, final String[] addrs,
            final int connCountPerNode, final String listFile, final long maxCount,
            final long sizeMin, final long sizeMax, final float sizeBias, final float rateLimit,
            final int countUpdPerReq
    ) {
        super(
                (Class<T>) BasicObject.class,
                runTimeConfig, reqConfig, addrs, connCountPerNode, listFile, maxCount,
                sizeMin, sizeMax, sizeBias, rateLimit, countUpdPerReq
        );
    }
//      wsReqConfigCopy = (WSRequestConfig<T>) reqConfigCopy;
        //
        final int totalConnCount = connCountPerNode * storageNodeCount;
        final String userAgent = runTimeConfig.getRunName() + "/" + runTimeConfig.getRunVersion();
        // ....

        //




  //


        @Override @SuppressWarnings("unchecked")
        protected BasicObjectIOTask<T> getIOTask(final T dataItem, final String nextNodeAddr) {
            return BasicObjectIOTask.getInstance(this, dataItem, nextNodeAddr);
        }
        //
        @Override
        public void close()
        throws IOException {
            try {
                super.close();
            } finally {
                BasicObjectIOTask.INSTANCE_POOL_MAP.put(this, null); // dispose the I/O tasks pool
            }
        }
    }



