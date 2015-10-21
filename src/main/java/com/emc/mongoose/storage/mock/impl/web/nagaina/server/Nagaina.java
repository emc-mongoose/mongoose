package com.emc.mongoose.storage.mock.impl.web.nagaina.server;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.storage.mock.impl.web.nagaina.init.NagainaServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Created by ilya on 21.10.15.
 */
public class Nagaina {
    static final int PORT = 9020;

    public Nagaina(RunTimeConfig rtConfig) {
    }

    public void run() throws InterruptedException {
        EventLoopGroup dispatchGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(dispatchGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NagainaServerInitializer());
            Channel channel = bootstrap.bind(PORT).sync().channel();
            channel.closeFuture().sync();
        }
        finally {
            dispatchGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
