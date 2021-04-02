package com.zyj.netty.channel;

import com.zyj.netty.handler.MyServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @author : zhang yijun
 * @date : 2021/4/2 10:23
 * @description : TODO
 */
public class MyChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // 获取pipeline
        ChannelPipeline pipeline = ch.pipeline();
        // 添加解码器，这里定义netty自带的String解码器
        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
        // 编码器，发送给客户端时将器编码
        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
        // 添加自定义的handler处理器
        pipeline.addLast(new MyServerHandler());
    }
}
