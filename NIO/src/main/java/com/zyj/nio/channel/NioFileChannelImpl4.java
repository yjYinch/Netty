package com.zyj.nio.channel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author : zhang yijun
 * @date : 2021/2/7 15:13
 * @description : TODO
 */

public class NioFileChannelImpl4 {

    public static void main(String[] args) {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        FileChannel sourceChannel = null;
        FileChannel desChannel = null;
        try{
            fileInputStream = new FileInputStream("E:\\Netty\\test.txt");
            fileOutputStream =  new FileOutputStream("E:\\Netty\\test3.txt");
            sourceChannel = fileInputStream.getChannel();
            desChannel = fileOutputStream.getChannel();
            desChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                if (sourceChannel != null) {
                    sourceChannel.close();
                }
                if (desChannel != null) {
                    desChannel.close();
                }
                if (fileOutputStream != null){
                    fileOutputStream.close();
                }
                if (fileInputStream != null){
                    fileInputStream.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
