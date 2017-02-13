package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.ui.log.Markers;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by reddy on 03.02.17.
 */
public class ChannelFactory {
    private static final int defaultPort = 9555;

    public static final int getDefaultPort() {
        return defaultPort;
    }

    public Channel newChannel(final String host, final int port, final boolean usePlainText) {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext(usePlainText).build();
    }

    /**
     * by default we are using plain text
     * @param host
     * @param port
     * @return
     */
    public Channel newChannel(final String host, final int port) {
        return newChannel(host, port, true);
    }

    /**
     * get all important info from a jmDns and
     * constructs a channel, connection is opening on
     * a default port
     * @param jmDns
     * @return channel
     */
    //TODO see if we need to manage ports and decide what ports do we need to use
    public Channel newChannel(final JmDNS jmDns) {
        return newChannel(jmDns.getHostName(), defaultPort);
    }

    /**
     * if there is no any parameters it returns
     * a channel with default address localhost:8080
     * @return channel
     */
    public Channel newChannel() {
        Channel channel;
        try {
            channel = newChannel(InetAddress.getLocalHost().getHostName(), defaultPort);
        } catch (UnknownHostException e) {
            final Logger LOG = LogManager.getLogger();
            LOG.info(Markers.ERR, "Can't get localhost address " + e.fillInStackTrace());
            LOG.info(Markers.MSG, "setting localhost as \"localhost\"");
            channel = newChannel("localhost", defaultPort);
        }
        return channel;
    }
}
