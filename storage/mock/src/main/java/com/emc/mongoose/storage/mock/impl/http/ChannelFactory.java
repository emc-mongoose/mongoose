package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.ui.log.Markers;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by reddy on 03.02.17.
 */
public class ChannelFactory {
    private static final int defaultPort = 9555;

    private static ConcurrentLinkedQueue<Pair<String, Channel>> openedChannels = new ConcurrentLinkedQueue<>();

    public static final int getDefaultPort() {
        return defaultPort;
    }

    /**
     * If there is a channel with this host, we just return it
     * @param host
     * @param port
     * @param usePlainText
     * @return
     */
    public static Channel newChannel(final String host, final int port, final boolean usePlainText) {
        if (openedChannels.stream().parallel().map(Pair::getKey).filter(x -> x.equals(host)).count() > 0) {
            return openedChannels.stream().parallel()
                    .filter(x -> x.getKey().equals(host))
                    .findFirst().get()
                    .getValue();
        }
        Channel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(usePlainText).build();
        openedChannels.add(new Pair<>(host, channel));
        return channel;
    }

    /**
     * by default we are using plain text
     * @param host
     * @param port
     * @return
     */
    public static Channel newChannel(final String host, final int port) {
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
    public static Channel newChannel(final JmDNS jmDns) {
        return newChannel(jmDns.getHostName(), defaultPort);
    }

    /**
     * if there is no any parameters it returns
     * a channel with default address localhost:8080
     * @return channel
     */
    public static Channel newChannel() {
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
