package com.jiuxian.netty;

import com.jiuxian.netty.handler.CustomerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class NettyServer {
    private final static NettyServer INSTANCE = new NettyServer();

    public static NettyServer getInstance() {
        return INSTANCE;
    }

    private NettyServer() {
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            // 对写大数据流的支持
                            pipeline.addLast(new ChunkedWriteHandler());
                            // 对httpMessage进行聚合，聚合成FullHttpRequest或FullHttpResponse
                            // 几乎在netty中的编程，都会使用到此handler
                            pipeline.addLast(new HttpObjectAggregator(1024 * 64));

                            // ====================== 以上是用于支持http协议    ======================

                            // ====================== 以下是支持httpWebsocket ======================

                            /*
                             * websocket 服务器处理的协议，用于指定给客户端连接访问的路由 : /ws
                             * 本handler会帮你处理一些繁重的复杂的事
                             * 会帮你处理握手动作： handshaking（close, ping, pong） ping + pong = 心跳
                             * 对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
                             */
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                            pipeline.addLast(new CustomerHandler());
                        }
                    });
            ChannelFuture f = b.bind(8888).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}