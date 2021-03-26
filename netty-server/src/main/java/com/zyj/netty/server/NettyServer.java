package com.zyj.netty.server;

import com.zyj.netty.handler.MyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * @author : zhang yijun
 * @date : 2021/2/21 10:44
 * @description : TODO
 */
public class NettyServer {

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // 设置服务端通道实现类型
                    .option(ChannelOption.SO_BACKLOG, 128) // 服务端用于接收进来的连接，也就是bossGroup线程
                    .childOption(ChannelOption.SO_KEEPALIVE, true)// 设置保持活动连接状态，// 提供给父管道接收到的连接，也就是workerGroup线程
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new MyServerHandler());
                        }
                    });
            System.out.println("服务端已经准备就绪");
            // 绑定端口号，启动服务端
            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(6666)).sync();

            //对关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            System.out.println("服务端错误");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
