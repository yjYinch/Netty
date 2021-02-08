package com.zyj.bio.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author : zhang yijun
 * @date : 2021/2/7 9:07
 * @description : TODO
 */

public class BIOServer extends Thread{

    private Socket socket;

    public BIOServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            while (true) {
                BufferedInputStream bufferedInputStream =
                        new BufferedInputStream(socket.getInputStream());
                byte[] bytes = new byte[1024];
                System.out.println("等待数据发送...");
                // 当没有数据的时候，这个地方会阻塞
                int read = bufferedInputStream.read(bytes, 0, 1024);
                String result = new String(bytes, 0, read);
                System.out.println(">>> " + result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
