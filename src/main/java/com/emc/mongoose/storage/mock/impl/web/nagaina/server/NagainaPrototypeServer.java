package com.emc.mongoose.storage.mock.impl.web.nagaina.server;

import com.emc.mongoose.storage.mock.impl.web.nagaina.init.NagainaServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Created by ilya on 21.10.15.
 */
public class NagainaPrototypeServer {
    static final int PORT = 9020;

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // default number of threads is Runtime.getRuntime().availableProcessors() * 2
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NagainaServerInitializer());
            Channel channel = bootstrap.bind(PORT).sync().channel();
            channel.closeFuture().sync();
        }
        finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
