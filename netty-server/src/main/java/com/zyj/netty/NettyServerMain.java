package com.zyj.netty;

import com.zyj.netty.server.NettyServer;
import io.netty.util.NettyRuntime;

/**
 * @author : zhang yijun
 * @date : 2021/2/21 10:44
 * @description : TODO
 */

public class NettyServerMain {
    public static void main(String[] args) {
        System.out.println(NettyRuntime.availableProcessors());
        NettyServer server = new NettyServer();
        server.run();


    }
}
