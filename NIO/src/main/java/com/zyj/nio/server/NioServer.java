package com.zyj.nio.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author : zhang yijun
 * @date : 2021/2/8 10:59
 * @description : NIO服务端
 */
public class NioServer {

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

            // 5. 将serverSocketChannel注册到Selector
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // 6. 循环等待客户端连接
            while (true) {
                // 当没有事件注册到selector时，继续下一次循环
                if (selector.select(1000) == 0) {
                    //System.out.println("当前没有事件发生，继续下一次循环");
                    continue;
                }
                // 获取相关的SelectionKey集合
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey selectionKey = it.next();
                    //根据key对应的通道发生的事件做相应的处理

                    // 如果是OP_ACCEPT事件，则表示有新的客户端连接
                    if (selectionKey.isAcceptable()) {
                        // 给客户端生成相应的Channel
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        // 将socketChannel设置为非阻塞
                        socketChannel.configureBlocking(false);
                        System.out.println("客户端连接成功...生成socketChannel");
                        // 将当前的socketChannel注册到selector上, 关注事件：读， 同时给socketChannel关联一个Buffer
                        socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                    }
                    // 如果是读取事件
                    if (selectionKey.isReadable()) {
                        // 通过key反向获取Channel
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        // 获取该channel关联的buffer
                        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();

                        // 把当前channel数据读到buffer里面去
                        socketChannel.read(buffer);
                        System.out.println("从客户端读取数据："+new String(buffer.array()));
                    }
                    it.remove();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
