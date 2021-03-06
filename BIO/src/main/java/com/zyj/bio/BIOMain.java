package com.zyj.bio;

import com.zyj.bio.server.BIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author : zhang yijun
 * @date : 2021/2/7 9:00
 * @description : TODO
 */

public class BIOMain {
    private static final Logger logger = LoggerFactory.getLogger(BIOServer.class);
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(6666));
            logger.info("服务器已启动，端口号：6666");
            while (true){
                logger.info("等待客户端连接...");
                // 等待客户端连接，当没有客户端连接时，会阻塞
                Socket socket = serverSocket.accept();
                System.out.println("客户端：" + socket.getLocalAddress() + "连接成功");
                // 每当有客户端连接进来，就启动一个线程进行处理
                new BIOServer(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(serverSocket !=null) {
                logger.info("服务器关闭了");
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
