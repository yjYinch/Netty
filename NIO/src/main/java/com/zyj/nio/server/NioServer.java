package com.zyj.nio.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * @author : zhang yijun
 * @date : 2021/2/8 10:59
 * @description : NIO服务端
 */
public class NioServer {
    private static final Logger logger = LoggerFactory.getLogger(NioServer.class);
    public static void main(String[] args) {
        try {
            // 1. 创建一个ServerSocketChannel
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

            // 2. 获取绑定端口
            serverSocketChannel.socket().bind(new InetSocketAddress(6666));

            // 3. 设置为非阻塞模式
            serverSocketChannel.configureBlocking(false);

            // 4. 获取Selector
            Selector selector = Selector.open();

            // 5. 将serverSocketChannel注册到selector上, 并且设置selector对客户端Accept事件感兴趣
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // 6. 循环等待客户端连接
            while (true) {
                // 当没有事件注册到selector时，继续下一次循环
                if (selector.select(5000) == 0) {
                    logger.info("当前没有事件发生，继续下一次循环");
                    continue;
                }
                // 获取相关的SelectionKey集合
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey selectionKey = it.next();
                    // 基于事件处理的handler
                    handler(selectionKey);
                    it.remove();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 基于事件处理的，根据key对应的通道发生的事件做相应的处理
     * @param selectionKey
     * @throws IOException
     */
    private static void handler(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isAcceptable()) {  // 如果是OP_ACCEPT事件，则表示有新的客户端连接
            ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
            // 给客户端生成相应的Channel
            SocketChannel socketChannel = channel.accept();
            // 将socketChannel设置为非阻塞
            socketChannel.configureBlocking(false);
            logger.info("客户端连接成功...socketChannel:{}", socketChannel.toString());
            // 将当前的socketChannel注册到selector上, 关注事件：读， 同时给socketChannel关联一个Buffer
            socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        } else if (selectionKey.isReadable()) { // 如果是读取事件
            // 通过key反向获取Channel
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            // 获取该channel关联的buffer
            ByteBuffer buffer = ByteBuffer.allocate(512);

            // 把当前channel数据读到buffer里面去
            socketChannel.read(buffer);
            logger.info("从客户端读取数据：{}", new String(buffer.array()));
            //
            ByteBuffer buffer1 = ByteBuffer.wrap("hello client".getBytes());
            socketChannel.write(buffer1);
            selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } else if (selectionKey.isWritable()){ // 如果是写事件
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            logger.info("当前为写事件...");
            socketChannel.write(ByteBuffer.wrap("即将发送消息给客户端".getBytes(StandardCharsets.UTF_8)));
            selectionKey.interestOps(SelectionKey.OP_READ);
        }
    }
}
