package com.zyj.netty.server;

import com.zyj.netty.channel.MyChannelInitializer;
import com.zyj.netty.handler.MySimpleChannelHander;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author : zhang yijun
 * @date : 2021/2/21 10:44
 */
public class NettyServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    public void run() {
        // bossGroup处理客户端连接，绑定线程数1
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 处理IO事件的事件循环组，线程数为cpu的核心数*2
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup) //初始化ServerBootstrap的线程组
                    .channel(NioServerSocketChannel.class) // 设置服务端通道实现类型
                    .option(ChannelOption.SO_BACKLOG, 128) // 服务端用于接收进来的连接，也就是bossGroup线程
                    .childOption(ChannelOption.SO_KEEPALIVE, true)// 设置workerGroup线程保持活动连接状态
                    .childHandler(new MyChannelInitializer());
            // 绑定端口号，启动服务端
            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(6666)).sync();
            if (channelFuture.isSuccess()){
                logger.info("==============服务端启动成功==================");
            }

            // 主线程执行到这里就 wait 子线程结束，子线程才是真正监听和接受请求的
            // closeFuture()是开启了一个channel的监听器，负责监听channel是否关闭的状态
            // 如果监听到channel关闭了，子线程才会释放，
            channelFuture.channel().closeFuture().sync();
            logger.info("channel已关闭");
        } catch (Exception e) {
            System.out.println("服务端错误");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
