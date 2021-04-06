# Netty

**Netty是什么？**

* `Netty`是一个**异步的**、**基于事件驱动**的网络应用框架，用以快速开发高性能、高可可靠性的网络`IO`程序
* `Netty`主要针对在`TCP`协议下，面向Clients端的高并发应用，或者Peer-to-Peer场景下的大量数据持续传输的应用
* `Netty`本质是一个`NIO`框架，适用于服务器通讯相关的多种应用场景

**Netty的结构**

![netty组成结构](https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143047.png)

**Netty的应用场景**

* 分布式服务的远程服务调用`RPC`框架

* `Netty`作为高性能的基础通信组件，提供了`TCP/UDP`和`HTTP`协议栈，方便定制和开发私有协议栈

---



## 一、IO模型

### 1.1 前言

学习`Netty`之前，需要先理解`IO`模型是什么？以及在`Java`中支持的`IO`模式。

**什么是I/O模型？**

> 简单理解就是用什么样的通道进行数据的发送和接受，并且很大程序上决定了程序通信的性能。

**`Java`中支持的3种网络编程模型/IO模式**

* `BIO`同步并阻塞

  ```markdown
  # 实现方式
  	服务器实现模式为一个连接一个线程，即客户端有连接请求时服务端就需要启动一个线程进行处理
  # 应用场景
  	适用于连接数较小且固定的机构，对服务器资源要求比较高
  # 弊端
  	如果这个连接不做任何事情就回造成不必要的线程开销
  ```

* `NIO`同步非阻塞

  ```markdown
  # 实现方式
  	服务器实现模式为一个线程处理多个请求（连接），即客户端发送的连接请求都会注册到多路复用器上，多路复用器轮询到连接有I/O请求就进行处理
  # 特点
  	选择器Selector来维护连接通道channel。Netty框架基于NIO实现。
  ```

* `AIO`异步非阻塞

  ```markdown
  # 实现方式
  	AIO引入异步通道的概念，采用了Proactor模式，简化了编程，有效的请求才启动线程。
  # 特点
  	由操作系统完成后才通知服务端程序启动线程去处理，一般应用于连接数较多且连接时间较长的应用。
  ```



### 1.2 BIO模型

#### 1.2.1 工作原理图

> 每次读写请求都会创建一个线程去处理。

![image-20210206223302882](https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143708.png)

#### 1.2.2 BIO编程流程

1. 服务端启动一个`ServerSocket`
2. 客户端发送请求后，先咨询服务器是否有线程响应，如果没有则会等待，或者被拒绝
3. 如果有响应，客户端线程会等待请求结束后，再继续执行

#### 1.2.3 BIO服务端

```java
public class BIOMain {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(6666));
            System.out.println("服务器已启动，端口号：6666");
            while (true){
                System.out.println("等待客户端连接...");
                // 等待客户端连接，当没有客户端连接时，会阻塞
                Socket socket = serverSocket.accept();
                System.out.println("客户端：" + socket.getLocalAddress() + "连接成功");
                // 每当有客户端连接进来，就启动一个线程进行处理
                new BioServer(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(serverSocket !=null) {
                System.out.println("服务器关闭了");
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
```

#### 1.2.4 BIO客户端

将客户端的数据进行业务处理：

```java
public class BioServer extends Thread {

    private Socket socket;

    public BioServer(Socket socket) {
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
```

**缺点**

> <font color=red size=4px>每来一个连接都会创建一个线程，消耗CPU资源，即使加上线程池也效果不好，因为它在处理连接Accept和Read地方会造成线程阻塞，浪费资源。</font>

---

### 1.3 NIO模型

​	我们知道BIO模型主要问题就在线程阻塞的地方，因此，NIO就解决了线程阻塞的问题。

#### 1.3.1 NIO版本

服务端

```java
public class NioServerDemo {

    public static void main(String[] args) {
        // 存入SocketChannel
        List<SocketChannel> socketChannelList = new ArrayList<>();
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(8989));
            serverSocketChannel.configureBlocking(false);

            while (true) {
                // 这个时候accept设置为非阻塞
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel == null) {
                    // 读取数据
                    for (SocketChannel channel : socketChannelList) {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int readLength = channel.read(buffer);
                        if (readLength > 0){
                            System.out.println("读取数据，" + new String(buffer.array()));
                        }
                    }
                } else {
                    // 获取到了客户端连接
                    System.out.println("新的客户端连接："+ socketChannel);
                    socketChannelList.add(socketChannel);
                    // 将socketChannel的读取方法设置为非阻塞
                    socketChannel.configureBlocking(false);
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int readLength = socketChannel.read(buffer);
                    if (readLength > 0){
                        System.out.println("读取数据，" + new String(buffer.array()));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

```

**这样写的优点是什么？**

​	解决了非阻塞的问题，使得服务端在连接和读取的情况下都是非阻塞进行的。

**缺点是什么？**

​	从代码中可以看出，我们是用集合存放`SocketChannel`，假如集合数量很大，但是活跃的`SocketChannel`却很少，但是每次都要遍历所有的集合，导致效率很低。

为了解决上述这种情况，诞生出了**基于事件驱动**（`Selector`）的NIO。

#### 1.3.2 **NIO工作原理**

**服务端：**

```java
public class NioServer {

    public static void main(String[] args) {
        try {
            // 1. 创建一个ServerSocketChannel，用于服务端
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
                if (selector.select(1000) == 0) {
                    //System.out.println("当前没有事件发生，继续下一次循环");
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
            System.out.println("客户端连接成功...生成socketChannel");
            // 将当前的socketChannel注册到selector上, 关注事件：读， 同时给socketChannel关联一个Buffer
            socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        } else if (selectionKey.isReadable()) { // 如果是读取事件
            // 通过key反向获取Channel
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            // 获取该channel关联的buffer
            //ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
            ByteBuffer buffer = ByteBuffer.allocate(512);

            // 把当前channel数据读到buffer里面去
            socketChannel.read(buffer);
            System.out.println("从客户端读取数据："+new String(buffer.array()));

            //
            ByteBuffer buffer1 = ByteBuffer.wrap("hello client".getBytes());
            socketChannel.write(buffer1);
            selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } else if (selectionKey.isWritable()){ // 如果是写事件
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            System.out.println("写事件");
            selectionKey.interestOps(SelectionKey.OP_READ);
        }
    }
}
```

`SelectionKey`事件

```java
public class SelectionKey{
    // 读事件，值为1
    public static final int OP_READ = 1 << 0;
    
    // 写事件，值为4
    public static final int OP_WRITE = 1 << 2;
    
    // socket connect 值为8
    public static final int OP_CONNECT = 1 << 3;
    
    // 服务端处理客户端的连接请求，设置为accept事件，值为16
    public static final int OP_ACCEPT = 1 << 4;
}
```



架构图：

<img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143719.png" alt="image-20210206232947444" style="zoom: 67%;" />

**上图流程说明：**

```markdown
# 1. 当有客户端连接时，会通过服务端的ServerSocketChannel得到SocketChannel

# 2. selector开始监听...

# 2. 将SocketChannel注册到selector(一个Selector可以注册多个SocketChannel)，注册方法是register

# 3. 注册后返回一个SelectionKey，会和该Selector关联（集合）

# 4. Selector进行监听select方法，返回有事件发生的通道的个数total

# 5. 当total > 0（表示有事件发生） 时，获取各个SelectionKey

# 6. 根据SelectionKey反向获取注册的SocketChannel

# 7. 根据得到的SocketChannel完成读写任务
```

**`NIO`三大核心部分**：

* **Channel通道**

  客户端与服务端之间的双工连接通道。所以在请求的过程中，客户端与服务端中间的Channel就在不停的执行“连接、询问、断开”的过程。直到数据准备好，再通过Channel传回来。Channel主要有4个类型：**FileChannel**(从文件读取数据)、DatagramChannel(读写UDP网络协议数据)、SocketChannel(读写TCP网络协议数据)、ServerSocketChannel(可以监听TCP连接)

* **Buffer缓冲区** 

  客户端存放服务端信息的一个缓冲区容器，服务端如果把数据准备好了，就会通过Channel往Buffer缓冲区里面传。Buffer有7个类型：`ByteBuffer`、`CharBuffer`、`DoubleBuffer`、`FloatBuffer`、`IntBuffer`、`LongBuffer`、`ShortBuffer`。

* **Selector选择器**

  服务端选择Channel的一个复用器。Selector有两个核心任务：监控数据是否准备好，应答Channel。

**`NIO`工作原理**：

> NIO是面向缓冲区编程的。它是将数据读取到缓冲区中，需要时可在缓冲区前后移动。

**`NIO`工作模式——非阻塞模式**：

> Java NIO的非阻塞模式，使一个线程从某通道发送请求或者读取数据，但是它仅能获得目前可用的数据，如果目前没有数据可用，就什么都不会获取，而不是保持线程阻塞。

**`NIO`特点**：

> 一个线程维护一个Selector, Selector维护多个Channel, 当channel有事件时，则该线程进行处理。                                                                  

**`BIO`和`NIO`的对比**

* `BIO`以流的方式处理数据，`NIO`以块的方式处理数据，块的方式处理数据比流的效率高
* `BIO`是阻塞的，而`NIO`是非阻塞的
* `BIO`是基于字节流和字符流进行操作，而`NIO`是基于channel和buffer进行操作，数据从通道读到缓冲区或者从缓冲区写到通道中，selector用于监听多个通道的事件（比如：连接请求，数据到达等），因此使用单个线程就可以监听多个客户端通道

#### 1.3.2 缓冲区Buffer

`Buffer`关联多个类型的子类：

* `ByteBuffer`
* `CharBuffer`
* `DoubleBuffer`
* `FloatBuffer`
* `IntBuffer`
* `LongBUffer`

`Buffer`本身是一个内存块，底层是一个容器对象（包含数组），该对象提供了一系列方法。数据的读写都是通过`Buffer`操作的。

`Buffer`源码中的四个关键数据：

```java
public class Buffer{
// Invariants: mark <= position <= limit <= capacity
    private int mark = -1; 
    private int position = 0;
    private int limit;
    private int capacity;
}
```

| 属性     | 描述                                                         |
| -------- | ------------------------------------------------------------ |
| mark     | 标记，是为了reset时记录上一次position的位置                  |
| position | 下一个要被读/写的元素索引，每次读写都会改变这个值，为下次读写做准备 |
| limit    | 表示缓冲区的当前终点，不能对缓冲区超过的极限的位置进行修改。且这个极限的位置可以被修改 |
| capacity | 可以容纳的最大数据量，在缓冲区创建时设定并且不能改变         |

下面以`IntBuffer`为例：

```java
public class Buffer {

    public static void main(String[] args) {
        // 创建一个buffer, 大小为5，可以存放5个int
        IntBuffer intBuffer = IntBuffer.allocate(5);

        // 向buffer中存数据
        for (int i = 0; i < intBuffer.capacity(); i++) {
            intBuffer.put(i);
        }

        // 从buffer中读数据
        // 将buffer转换，读写切换
        intBuffer.flip();

        // 现在buffer已经切换到读了
        while(intBuffer.hasRemaining()){
            // 维护一个索引，每get一次，指针就后移一下
            int i = intBuffer.get();
            System.out.println("获取数据："+ i);
        }
    }
}
```

下面以debug形式来看`Buffer`中4个属性参数的变化：

1. 调用`allocate`方法初始化后

   <img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143808.jpeg" alt="buffer初始化" style="zoom:67%;" />

2. 写入数据后

   <img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143812.jpeg" alt="buffer写入数据后" style="zoom:67%;" />

3. flip切换后

   <img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143817.jpeg" alt="buffer flip切换后" style="zoom:67%;" />

4. 读完数据后

   <img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143824.jpeg" alt="buffer读完数据后" style="zoom:67%;" />

**`Buffer`常用方法：**

```java
public abstract class Buffer{
    public final int capacity(); // 返回此缓冲区的容量
    public final int position(); // 返回此缓冲区的位置
    public final Buffer position(int newPosition); // 设置此缓冲区的位置
    public final int limit(); // 返回此缓冲区的限制大小
    public final Buffer limit(int newLimit); // 设置此缓冲区的限制
    public final Buffer mark(); //在此缓冲区的位置设置标记
    public final Buffer reset(); //将此缓冲区的位置重置为以前标记的位置
    public final Buffer clear(); //清楚此缓冲区，将各个标记恢复到初始状态，但是数据并没有正在删除
    public final Buffer flip(); // 反转此缓冲区
    public final Buffer rewind(); // 重绕此缓冲区
    public final int remaining(); // 返回当前位置与限制之间的元素数
    public final boolean hasRemaning();// 判断当前位置与限制之间是否有元素
    public abstract boolean isReadOnly(); // 判断此缓冲区是否为只读缓冲区
    public final Buffer asReadOnlyBuffer(); // 将buffer转换为只读buffer
    
    public abstract boolean hasArray(); // 判断此缓冲区是否具有可访问的底层实现数组
    public abstract Object array(); // 返回底层实现数组, 也就是hb
    public abstract int arrayOffset(); // 返回此缓冲区的底层实现数组中第一个缓冲区元素的偏移量
    public abstract boolean isDirect(); // 判断此缓冲区是否为直接缓冲区
}
```



#### 1.3.3 通道Channel

`NIO`的通道类似于流，但是有些区别：

* 通道Channel可以同时进行读写，而流只能读或者只能写
* 通道Channel可以实现异步读写数据
* 通道Channel可以从缓冲区读数据，也可以从缓冲区写数据
* 通道Channel在`NIO`中是一个接口
  * 常用的实现类
    * `FileChannel` -- 用于文件的读写
    * `DatagramChannel` -- 用于UDP数据的读写
    * `ServerSocketChannel`(类似于`ServerSocket`) -- TCP数据的读写
    * `SocketChannel`(类似于`Socket`) -- TCP数据的读写

一个线程对应多个Channel（可以简单理解为连接），每个channel都会关联一个buffer。

![channel](https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143831.jpeg)

`FileChannel`类:用于对本地文件进行IO操作，常见的方法：

```java
public int read(ByteBuffer b); // 从通道中读取数据并放入到缓冲区
public int write(ByteBuffer b); // 把缓冲区的数据写入到channel中
```

> 输入--> 读； 输出--> 写

**示例1：本地文件写数据**

```java
public class NioFileChannelImpl {

    public static void main(String[] args) {

        String message = "Test file channel";
        FileOutputStream fileOutputStream = null;
        try {
            // 创建一个输出流，将message写入到本地路径位置文件
            fileOutputStream = new FileOutputStream("E:\\Netty\\test.txt");
            // 获取channel, 真实类型是FileChannelImpl
            FileChannel fileChannel = fileOutputStream.getChannel();
            // 创建一个缓冲区，ByteBuffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            // 把数据message写入到buffer
            byteBuffer.put(message.getBytes());

            // 反转指针，从byteBuffer读取数据并写入到fileOutputStream
            byteBuffer.flip();

            // 将缓冲区的数据写入到channel中
            fileChannel.write(byteBuffer);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null){
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

```

**示例2：本地文件读数据**

```java
public class NioFileChannelImpl2 {
    public static void main(String[] args) {
        FileInputStream fileInputStream = null;
        try {
            // 读取文件
            fileInputStream = new FileInputStream("E:\\Netty\\test.txt");
            // 获取channel
            FileChannel fileChannel = fileInputStream.getChannel();

            // 初始化ByteBuffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            // 从通道读取数据并将其将读到byteBuffer中
            fileChannel.read(byteBuffer);

            // 将byteBuffer转为String, byteBuffer.array() 获取字节数组hb
            String s = new String(byteBuffer.array());
            System.out.println(s);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

**示例3：使用一个Buffer完成文件的读写**

```java
public class NioFileChannelImpl3 {

    public static void main(String[] args) {
        FileInputStream fileInputStream = null;

        try {
            // 读取一个文件
            fileInputStream = new FileInputStream("E:\\Netty\\test.txt");
            FileChannel channel = fileInputStream.getChannel();

            FileOutputStream fileOutputStream = new FileOutputStream("E:\\Netty\\test1.txt");
            FileChannel channel2 = fileOutputStream.getChannel();

            ByteBuffer byteBuffer = ByteBuffer.allocate(5);
            while(true){
                // 清楚ByteBuffer缓冲区，类似于重新初始化
                byteBuffer.clear();
                // 将channel里面的内容一次性读取到byteBuffer，如果byteBuffer容量不够，则读到最大容量
                int read = channel.read(byteBuffer);
                if (read == -1){
                    break;
                }
                // 读转换为写
                byteBuffer.flip();
                // 将byteBuffer缓冲区的内容写入到channel中
                channel2.write(byteBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
```

**示例4：文件拷贝**

```java
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
            // channel拷贝
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

```



**MappedByteBuffer**

> 可以让文件直接在内存（堆外内存）修改，操作系统不需要拷贝一次。

**Buffer的分散和聚集**

* Scattering

  将数据写入到buffer时，可以采用buffer数组，依次写入 [分散]

* Gathering

  从buffer读取数据时，可以采用buffer数组，依次读取

示例：

```java
public class ScatteringGathering {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress socketAddress = new InetSocketAddress(7000);

        // 绑定端口到socket并启动
        serverSocketChannel.socket().bind(socketAddress);

        // 创建buffer数组
        ByteBuffer[] byteBuffer = new ByteBuffer[2];
        byteBuffer[0] = ByteBuffer.allocate(5);
        byteBuffer[1] = ByteBuffer.allocate(3);

        // 等待客户端连接
        System.out.println("等待客户端连接....");
        SocketChannel socketChannel = serverSocketChannel.accept();
        System.out.println("客户端:{" + socketChannel.getLocalAddress() + "}" + "已连接...");

        // 假设从客户端接收8个字节
        int messageLength = 8;

        while (true) {
            int byteRead = 0;
            while (byteRead < messageLength) {
                long l = socketChannel.read(byteBuffer);
                byteRead += l;
                System.out.println("byteRead = " + byteRead);

                for (ByteBuffer buffer : byteBuffer) {
                    System.out.println("position =" + buffer.position() + ", limit = " + buffer.limit());
                }
            }

            // 将所有的buffer进行反转
            for (ByteBuffer buffer : byteBuffer) {
                buffer.flip();
            }
            // 将数据读出显示给客户端
            long byteWrite = 0;
            while (byteWrite < messageLength) {
                long l = socketChannel.write(byteBuffer);
                byteWrite += l;
            }
            // 将所有的buffer进行clear操作
            for (ByteBuffer buffer : byteBuffer) {
                buffer.clear();
            }

            System.out.println("byteRead = " + byteRead + ", byteWrite" + byteWrite + ", messageLength = " + messageLength);
        }

    }
}

```



#### 1.3.4 选择器Selector

一个Selector对应一个线程，一个线程关联多个Channel，selector会根据不同的事件，在多个Channel上切换。

**特点：**

* Selector选择器，又称多路复用器
* 当线程从某客户端Socket通道处理数据时，如果没有数据传输，这个线程可以去做其它的事情
* 一个线程可以并发处理多个客户端的连接和读写操作

**处理流程：**

![Selector](https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143839.jpeg)

**`Api`:**

```java
public abstract class Selector implements Closeable {
    // 获取一个Selector选择器对象
    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }
    // 监控所有注册的通道，当其有IO操作可以进行时，将对应的SelectionKey加入到内部集合中并返回，参数用来设置超时时间
    public int select(long timeout);
    
    // 当注册的channel没有事件发生时，会阻塞
    public int select();
    
    //从内部集合中得到所有的SelectionKey
    public Set<SelectionKey> selectKeys();
}
```

**Selector相关说明：**

* `Selector.select()`
  * 阻塞
* `Selector.select(1000)`
  * 阻塞1000 ms，1000 ms后返回
* `Selector.wakeup()`
  * 唤醒selector
* `Selector.selectNow()`
  * 不阻塞，立马返回

#### 1.3.5 NIO 服务端

```java
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
```



#### 1.3.6 总结

* 每个Channel对应一个Buffer
* selector --> thread --> 多个 channel
* buffer是双向的，既可以读也可以写，但是需要flip切换，而BIO的读写只能是单向的，要么是input要么是output
* channel是双向的



## 二、线程模型

### 2.1 单Reactor单线程模型

> 连接请求和处理模式都是单一操作。

```markdown
# 优点
	1. 编程简单 
# 缺点
	1. 单线程操作，性能差
```



### 2.2 单Reactor多线程模型

```markdown
# 优点
	1. 多线程操作，性能比单线程模型好
# 缺点
	1. 多线程复杂
	2. 单个Reactor处理了所有事件的情况（连接、读写），高并发场景下容易出现性能瓶颈
```



### 2.3 主从Reactor多线程模型

```markdown
# 优点
	1. Reactor为多线程的
# 缺点
	1. 编程复杂
```



## 三、Netty

在引入Netty之前，我们来介绍一下为什么Netty能够产生，并且被广泛使用。

### 3.1 Netty的产生背景

在学习Netty之前，我们再来回顾一下传统HTTP服务器的处理流程：

**BIO：阻塞型I/O模型**

> 1. 创建一个ServerSocket，监听并绑定一个端口
>
> 2. 一系列客户端来请求这个端口
>
> 3. 服务器使用Accept，获得一个来自客户端的Socket连接对象 （当没有连接过来时，阻塞）
>
> 4. 启动一个新线程处理连接
>
>    (1) 读Socket，得到字节流（此步阻塞）
>
>    (2) 解码协议，得到Http请求对象
>
>    (3) 处理HTTP请求，得到一个结果，封装成一个HttpResponse对象
>
>    (4) 编码协议，将结果序列化字节流
>
> 5. 写Socket，将字节流发给客户端（阻塞）
>
> 6. 继续循环步骤3

<font color=red size=3px>现在，由于每来一个请求都要创建一个线程去处理请求内容，对操作系统的负载很高，任务调度压力大，因此基于非阻塞型的I/O就诞生了——NIO（Non-blocking I/O）</font>。

**NIO：非阻塞I/O模型：**

> 基于事件机制，用一个线程就可以把Accept、Read、Write全部完成了。如果什么事都没得做，它也不会死循环，它会将线程休眠起来，直到下一个事件来了再继续干活，这样的一个线程称之为NIO线程。

```java
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
         // 每读处理完一个事件后，就将
         it.remove();
	}
```

简化版：

```java
while(true){
    events = takeEvents(selectionKeys); // 获取事件
    // 遍历events
    while(events.hasNext()){
        if(accept){
            doAccept(); // 如果是新连接，就处理新连接
        } else if(read){
            doRead(); // 读取事件
        } else if(write){
            doWrite(); // 写事件
        }
    }
}
```

由于JDK NIO使用起来没有那么方便，并且有臭名昭著的bug（空轮询，容易导致CPU 100%）。而Netty把它封装之后，进行优化并提供了一个易于操作的使用模式和接口，因此Netty就被广泛使用于通信框架。



### 3.2 Netty的功能

可以实现自定义的服务器，比如：

* HTTP服务器
* TCP服务器
* UDP服务器
* FTP服务器
* RPC服务器

### 3.3 Netty的原理

> 基于主从Reactor多线程模型。

下图展示Netty的总体框架:

<img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143850.png" alt="netty框架" style="zoom: 80%;" />







**线程模型**

​	基于主从Reactor多线程模型，它维护两个线程池，一个是处理Accept连接，另一个是处理读写事件。

**两大线程池**

* `BossGroup`

  负责客户端Accept的连接

* `WorkerGroup`

  负责事件Event的读写

**组件：**

`Channel` : 代表一个连接或者一个请求

`ChannlePipeline` ：责任链，每个channel有自己的pipeline， 每个pipeline也包含对应的channel, 里面有各种handler，底层是一个双向链表，包含入站和出站

`ChannelHandler`  : 用于处理业务逻辑，它有很多系统默认的Handler

* `ChannelHandlerAdapter`
* `ChannelInboundHandler`
* `ChannelOutboudHandler`
* `SimpleChannelInboundHandler`
* `ChannelInitializer`

`ChannelHandlerContext` : 作为ChannelPipeline的节点，Handler不适合作为节点，需要context记录上下节点。

级联关系：



#### 3.3.1 服务端

创建一个服务端线程，当主启动类启动的时候启动该服务。

```java
@Slf4j
public class TcpServer extends Thread {
    private Integer port;
    public TcpServer(Integer port){
        this.port = port;
    }

    @Override
    public void run() {
        // 根据主机名和端口号创建ip套接字地址（ip地址+端口号）
        InetSocketAddress socketAddress = new InetSocketAddress(port);
        // 主线程组，处理Accept连接事件的线程，这里线程数设置为1即可，netty处理链接事件默认为单线程，过度设置反而浪费cpu资源
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 工作线程，处理hadnler的工作线程，其实也就是处理IO读写，线程数据默认为 CPU 核心数乘以2
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        // 创建ServerBootstrap实例
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup) //初始化ServerBootstrap的线程组
                .channel(NioServerSocketChannel.class)  // 设置将要被实例化的ServerChannel类
                .childHandler(new ServerChannelInitializer()) // 初始化ChannelPipeline责任链
                .localAddress(socketAddress)
                .option(ChannelOption.SO_BACKLOG, 1024) //设置队列大小
                .childOption(ChannelOption.SO_KEEPALIVE, true); // 是否启动心跳保活机制

        try {
            // 绑定端口，开始接收进来的连接，异步连接
            ChannelFuture channelFuture = serverBootstrap.bind(socketAddress).sync();
            log.info("TCP服务器开始监听端口：{}", socketAddress.getPort());
            if (channelFuture.isSuccess()) {
                log.info("TCP服务启动成功-------------------");
            }

            // 主线程执行到这里就 wait 子线程结束，子线程才是真正监听和接受请求的，
            // closeFuture()是开启了一个channel的监听器，负责监听channel是否关闭的状态，
            // 如果监听到channel关闭了，子线程才会释放，syncUninterruptibly()让主线程同步等待子线程结果
            channelFuture.channel().closeFuture().sync();
            log.info("TCP服务已关闭");
        } catch (InterruptedException e) {
            log.error("tcp server exception: {}", e.getMessage());
        } finally {
            // 关闭主线程组
            bossGroup.shutdownGracefully();
            // 关闭工作组
            workerGroup.shutdownGracefully();
        }
    }
}
```

自定义`Hander`，继承`ChannelInboundAdapterHandler`

```java
@Slf4j
public class TCPServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 客户端连接标识
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端已连接：{}", ctx.channel().localAddress().toString());
        // 获取当前客户端的唯一标识
        String uuid = ctx.channel().id().asLongText();
        log.info("当前连接的客户端id：{}", uuid);
        // 将其对应的标识和channel存入到map中
        CLIENT_MAP.put(uuid, ctx.channel());
    }


    /**
     * 读取客户端发送的消息
     * @param ctx
     * @param msg 客户端发送的数据
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 使用netty提供的ByteBuf生成字节Buffer，里面维护一个字节数组，注意不是JDK自带的ByteBuffer
        ByteBuf byteBuf = (ByteBuf) msg;
        // 读取byteBuf
        // 业务处理 
        // 回消息给客户端
        
    }

    /**
     * 客户端断开连接时触发
     * 当客户端主动断开服务端的链接后，这个通道就是不活跃的。也就是说客户端与服务端的关闭了通信通道并且不可以传输数据
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("断开前，CLIENT_MAP：{}", CLIENT_MAP);
        //当客户端断开连接时，清除map缓存的客户端信息
        CLIENT_MAP.clear();
        log.info(ctx.channel().localAddress().toString() + " 通道不活跃！并且关闭。");
        log.info("断开后，CLIENT_MAP：{}", CLIENT_MAP);
        // 关闭流
        ctx.close();
    }

    /**
     * 发生异常时触发
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("异常情况: {}", cause.toString());
    }
    
    
    /**
     * channelRead方法执行完成后调用，发送消息给客户端
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // writeAndFlush = write + flush：将数据写入缓存，并刷新
        // 需要对发送的数据进行编码
        ctx.writeAndFlush(Unpooled.copiedBuffer("收到消息，返回ok!"));
    }
}

```



#### 3.3.2 Netty客户端

```java
public class NettyClient {

    public void run(){
        // 一个事件循环组
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            // 客户端启动helper
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new MyClientHandler());
                        }
                    });
            System.out.println("客户端准备就绪，即将连接服务端...");
            // 连接服务端，并返回channelFuture对象，它用来进来异步通知
            // 一般在Socket编程中，等待响应结果都是同步阻塞的，而Netty则不会造成阻塞，因为ChannelFuture是采取类似观察者模式的形式进行获取结果
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1",6666).sync();
            // 对通道关闭进行监听
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭netty
            eventLoopGroup.shutdownGracefully();
        }
    }
}
```



#### 3.3.3 Netty分析

**任务队列中的Task的3种应用场景**

1. **用户自定义的普通任务**

   任务丢到`taskQueue`中

   ```java
   // 直接execute
   ctx.channel().eventLoop().execute(new Runnable() {
               @Override
               public void run() {
                   try {
                       Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                       logger.info("当前线程：" + Thread.currentThread().getName() + "执行任务");
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
               }
           });
   ```

2. **用户自定义定时任务**

   任务是调用`schedule(Runnable runnable)`方法，然后将任务丢到scheduledTaskQueue中。

   ```java
           ctx.channel().eventLoop().schedule(new Runnable() {
               @Override
               public void run() {
                   logger.info("当前线程：" + Thread.currentThread().getName() + "执行定时任务");
               }
           }, 5, TimeUnit.SECONDS);
   ```

   

3. **非当前Reactor线程调用Channel的各种方法**

   在启动类上记录`SocketChannel`标识。



----

### 3.4 Netty组件说明

#### 3.4.1 `EventLoopGroup`接口、`EventLoop`接口的关系

* 一个`EventLoopGroup`包含一个或者多个`EventLoop`; 
* 一个`EventLoop`在它的生命周期内只和一个Thread绑定;
* 所有由`EventLoop`处理的I/O事件都将在它专有的Thread上被处理;
* 一个`Channel`在它的生命周期内只注册于一个`EventLoop`;
* 一个`EventLoop`可能被分配给一个或多个`Channel`；



#### 3.4.2 通道与管道的关系

`Channel`通道与`ChannelPipeline`管道是一对一关系

每一个新建的`Channel`都会关联唯一的`ChannelPipeline`，而`ChannelPipeline`中又维护一个由`ChannelHandlerContext`组成的双向链表结构，链表的头部为`HeadContext`，尾部为`TailContext`，且每个`ChannelHandlerContext`又关联一个`ChannelHandler`，`ChannelHandlerContext`是沟通handler与ChannelPipeline的桥梁。

<img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143901.png" alt="Channel" style="zoom: 50%;" />



事件经过`Channel`流经`ChannelPipeline`会由`ChannelHandler`（入站`ChannelInboundHandler`或者出站`ChannelOutboundHandler`）处理，然后通过调用`ChannelHandlerContext`它会被转发给下一个`CahnnelHandler`。



#### 3.4.3 入站和出站

`ChannelInboundHandler`：入站

`ChannelOutboundHandler`: 出站

服务端角度来看：接收客户端数据入站，发送数据给客户端表示出站

客户端角度来看：接收服务端数据入站，发送数据给服务端表示出站

```java
                                                  I/O Request
                                             via {@link Channel} or
                                         {@link ChannelHandlerContext}
                                                       |
   +---------------------------------------------------+---------------+
   |                           ChannelPipeline         |               |
   |                                                  \|/              |
   |    +---------------------+            +-----------+----------+    |
   |    | Inbound Handler  N  |            | Outbound Handler  1  |    |
   |    +----------+----------+            +-----------+----------+    |
   |              /|\                                  |               |
   |               |                                  \|/              |
   |    +----------+----------+            +-----------+----------+    |
   |    | Inbound Handler N-1 |            | Outbound Handler  2  |    |
   |    +----------+----------+            +-----------+----------+    |
   |              /|\                                  .               |
   |               .                                   .               |
   | ChannelHandlerContext.fireIN_EVT() ChannelHandlerContext.OUT_EVT()|
   |        [ method call]                       [method call]         |
   |               .                                   .               |
   |               .                                  \|/              |
   |    +----------+----------+            +-----------+----------+    |
   |    | Inbound Handler  2  |            | Outbound Handler M-1 |    |
   |    +----------+----------+            +-----------+----------+    |
   |              /|\                                  |               |
   |               |                                  \|/              |
   |    +----------+----------+            +-----------+----------+    |
   |    | Inbound Handler  1  |            | Outbound Handler  M  |    |
   |    +----------+----------+            +-----------+----------+    |
   |              /|\                                  |               |
   +---------------+-----------------------------------+---------------+
                   |                                  \|/
   +---------------+-----------------------------------+---------------+
   |               |                                   |               |
   |       [ Socket.read() ]                    [ Socket.write() ]     |
   |                                                                   |
   |  Netty Internal I/O Threads (Transport Implementation)            |
   +-------------------------------------------------------------------+

```



#### 3.4.4 **Netty重写JDK的Future**

**异步模型**

`Future` ：表示异步的执行结果，可以通过它提供的方法来检测执行是否完成

`ChannelFuture` : 继承`Future`的接口，能够添加监听器。

```java
public interface Future<V> extends java.util.concurrent.Future<V>
```

原因：由于异步操作的结果是需要根据`future.get()`获取，而你不知道何时去调用它，并且`future.isDone()`方法当结果异常、取消或正常情况下都为true，因此Netty为了结果添加`isSuccess()`、`cause()`、监听器等方法。

`ChannelFuture`

​	Netty中的所有IO操作都是异步的。这意味着任何IO调用都将立即返回，而不能保证在调用结束时已完成请求的IO操作。相反，您将返回一个`ChannelFuture`实例，该实例为您提供有关IO操作的结果或状态的信息。

​	在正常情况下，`addListener(GenericFutureListener)`能够在完成I/O操作并执行任何后续任务时得到通知。`addListener(GenericFutureListener)`是非阻塞的。 只需将指定的`ChannelFutureListener`添加到`ChannelFuture `，并且与将来关联的I/O操作完成时，I/O线程将通知监听器。 `ChannelFutureListener`完全不会阻塞，因此可以产生最佳的性能和资源利用率。

```java
/**
*                                      +---------------------------+
*                                      | Completed successfully    |
*                                      +---------------------------+
*                                 +---->      isDone() = true      |
* +--------------------------+    |    |   isSuccess() = true      |
* |        Uncompleted       |    |    +===========================+
* +--------------------------+    |    | Completed with failure    |
* |      isDone() = false    |    |    +---------------------------+
* |   isSuccess() = false    |----+---->      isDone() = true      |
* | isCancelled() = false    |    |    |       cause() = non-null  |
* |       cause() = null     |    |    +===========================+
* +--------------------------+    |    | Completed by cancellation |
*                                 |    +---------------------------+
*                                 +---->      isDone() = true      |
*                                      | isCancelled() = true      |
*                                      +---------------------------+
*
*/
```

​	

----

### 3.5 源码分析

**Netty的组件说明**

`EventLoopGroup` [文章](https://www.jianshu.com/p/0d0eece6d467)中详细剖析过，就是一个死循环，不停地检测IO事件，处理IO事件，执行任务

`ServerBootstrap` 是服务端的一个启动辅助类，通过给他设置一系列参数来绑定端口启动服务

`group(bossGroup, workerGroup)` 我们需要两种类型的人干活，一个是老板，一个是工人，老板负责从外面接活，接到的活分配给工人干，放到这里，`bossGroup`的作用就是不断地accept到新的连接，将新的连接丢给`workerGroup`来处理

`.channel(NioServerSocketChannel.class)` 表示服务端启动的是nio相关的channel，channel在netty里面是一大核心概念，可以理解为一条channel就是一个连接或者一个服务端bind动作，后面会细说

`.handler(new SimpleServerHandler()` 表示服务器启动过程中，需要经过哪些流程，这里`SimpleServerHandler`最终的顶层接口为`ChannelHander`，是netty的一大核心概念，表示数据流经过的处理器，可以理解为流水线上的每一道关卡

`childHandler(new ChannelInitializer<SocketChannel>)...`表示一条新的连接进来之后，该怎么处理，也就是上面所说的，老板如何给工人配活

`ChannelFuture f = b.bind(8888).sync();` 这里就是真正的启动过程了，绑定8888端口，等待服务器启动完毕，才会进入下行代码

`f.channel().closeFuture().sync();` 等待服务端关闭socket

`bossGroup.shutdownGracefully(); workerGroup.shutdownGracefully();` 关闭两组死循环



**Netty**与**NIO**服务端和客户端的区别

|        | Netty                    | NIO                   |
| ------ | ------------------------ | --------------------- |
| 服务端 | `NioServerSocketChannel` | `ServerSocketChannel` |
| 客户端 | `NioSocketChannel`       | `SocketChanel`        |

----

`EventLoopGroup`接口

> 特殊的`EventExecutorGroup` ，它允许注册Channel ，该事件在事件循环期间进行处理以供以后选择。

```java
public interface EventLoopGroup extends EventExecutorGroup {
    /**
     * 返回下一个EventLoop
     */
    @Override
    EventLoop next();

    /**
     * 将channel注册到事件循环EventLoop，当注册完成之后，返回一个ChannelFuture
     */
    ChannelFuture register(Channel channel);

    /**
     * 将channel注册到EventLoop, ChannelPromise里面包含channel
     */
    ChannelFuture register(ChannelPromise promise);

    /**
     * 过期方法，推荐使用register(ChannelPromise promise);
     *
     * @deprecated Use {@link #register(ChannelPromise)} instead.
     */
    @Deprecated
    ChannelFuture register(Channel channel, ChannelPromise promise);
}
```

`EventLoop`接口

> 注册后将处理Channel所有I / O操作。 一个`EventLoop`实例通常将处理多个Channel但这可能取决于实现细节和内部。

```java
public interface EventLoopGroup extends EventExecutorGroup {
    /**
     * Return the next {@link EventLoop} to use
     */
    @Override
    EventLoop next();

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The returned {@link ChannelFuture}
     * will get notified once the registration was complete.
     */
    ChannelFuture register(Channel channel);

    /**
     * Register a {@link Channel} with this {@link EventLoop} using a {@link ChannelFuture}. The passed
     * {@link ChannelFuture} will get notified once the registration was complete and also will get returned.
     */
    ChannelFuture register(ChannelPromise promise);

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The passed {@link ChannelFuture}
     * will get notified once the registration was complete and also will get returned.
     *
     * @deprecated Use {@link #register(ChannelPromise)} instead.
     */
    @Deprecated
    ChannelFuture register(Channel channel, ChannelPromise promise);
}
```



`EventExecutorGroup`接口

> `EventExecutorGroup`负责通过其next()方法提供要使用的`EventExecutor` 。 除此之外，它还负责处理其生命周期，并允许以全局方式关闭它们。

```java
public interface EventExecutorGroup extends ScheduledExecutorService, Iterable<EventExecutor> {
    ....省略
    /**
     * 根据EventExecutorGroup返回一个EventExecutor.
     */
    EventExecutor next();
    ...省略
}

```

`EventExecutor`接口

> `EventExecutor`是一个特殊的`EventExecutorGroup` ，它带有一些方便的方法来查看Thread是否在事件循环中执行。 除此之外，它还扩展了`EventExecutorGroup`以允许使用通用方法访问方法

```java
public interface EventExecutor extends EventExecutorGroup {

    /**
     * Returns a reference to itself.
     */
    @Override
    EventExecutor next();

    /**
     * Return the {@link EventExecutorGroup} which is the parent of this {@link EventExecutor},
     */
    EventExecutorGroup parent();

    /**
     * Calls {@link #inEventLoop(Thread)} with {@link Thread#currentThread()} as argument
     */
    boolean inEventLoop();

    /**
     * Return {@code true} if the given {@link Thread} is executed in the event loop,
     * {@code false} otherwise.
     */
    boolean inEventLoop(Thread thread);

    /**
     * Return a new {@link Promise}.
     */
    <V> Promise<V> newPromise();

    /**
     * Create a new {@link ProgressivePromise}.
     */
    <V> ProgressivePromise<V> newProgressivePromise();

    /**
     * Create a new {@link Future} which is marked as succeeded already. So {@link Future#isSuccess()}
     * will return {@code true}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */
    <V> Future<V> newSucceededFuture(V result);

    /**
     * Create a new {@link Future} which is marked as failed already. So {@link Future#isSuccess()}
     * will return {@code false}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */
    <V> Future<V> newFailedFuture(Throwable cause);
}
```



**1 初始化**`ServerSocketChannel`

（1）调用`bind(int port)`方法

```java
public abstract class AbstractBootstrap{
    // 绑定端口
    private ChannelFuture doBind(final SocketAddress localAddress) {
        // 初始化ServerSocketChannel并注册
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        if (regFuture.isDone()) {
            // At this point we know that the registration was complete and successful.
            ChannelPromise promise = channel.newPromise();
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();

                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }

	final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            // 反射创建ServerSocketChannel
            channel = channelFactory.newChannel();
            // 初始化channel，获取ChannelPipeline并向其中添加ChannelHandler
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
                // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }
        
        // 获取bossGroup线程组，然后获取里面的线程(next()方法轮询)，将channel注册
        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    i.e. It's safe to attempt bind() or connect() now:
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         because register(), bind(), and connect() are all bound to the same thread.

        return regFuture;
    }
}
```

经过一系列调用，执行到注册`channel`的方法。

```java
public abstract class AbstractNioChannel extends AbstractChannel {
	@Override
    protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {
                // 将ServerSocketChannel注册到Selector上
                selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
                return;
            } catch (CancelledKeyException e) {
                if (!selected) {
                    // Force the Selector to select now as the "canceled" SelectionKey may still be
                    // cached and not removed because no Select.select(..) operation was called yet.
                    eventLoop().selectNow();
                    selected = true;
                } else {
                    // We forced a select operation on the selector before but the SelectionKey is still cached
                    // for whatever reason. JDK bug ?
                    throw e;
                }
            }
        }
    }
}
```

（2）通过反射创建`ServerSocketChannel`对象。

```java
public class ReflectiveChannelFactory<T extends Channel> implements ChannelFactory<T> {
	@Override
    public T newChannel() {
        try {
            // 这里面会调用该类的默认构造方法，下面进行第3步
            return constructor.newInstance();
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class " + constructor.getDeclaringClass(), t);
        }
    }
}
```

（3）初始化`ServerSocketChannel`过程中做了哪些事情？

* 创建`ServerSocketChannel`，调用`newSocket(DEFAULT_SELECTOR_PROVIDER)`

```java
public class NioServerSocketChannel extends AbstractNioMessageChannel
                             implements io.netty.channel.socket.ServerSocketChannel{
    /** 默认选择器 */
    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();
    
    //1. 反射创建对象
    public NioServerSocketChannel() {
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }
    
    //2. 创建一条server端的channel
    private static ServerSocketChannel newSocket(SelectorProvider provider) {
        try {
            /**
             *  Use the {@link SelectorProvider} to open {@link SocketChannel} and so remove condition in
             *  {@link SelectorProvider#provider()} which is called by each ServerSocketChannel.open() otherwise.
             *
             *  See <a href="https://github.com/netty/netty/issues/2308">#2308</a>.
             */
            return provider.openServerSocketChannel();
        } catch (IOException e) {
            throw new ChannelException(
                    "Failed to open a server socket.", e);
        }
    }
    
    //3. 调用构造方法，初始化，注册感兴趣的事件为Accept事件
    public NioServerSocketChannel(ServerSocketChannel channel) {
        // 这个super调用父类的构造方法做了：生成默认的ChannelPipleline、设置感兴趣的事件为Accept事件、设置非阻塞模式
        super(null, channel, SelectionKey.OP_ACCEPT);
        // 配置类
        config = new NioServerSocketChannelConfig(this, javaChannel().socket());
    }
}
```

为了分析`super(null, channel, SelectionKey.OP_ACCEPT);`到底做了哪些事情呢？先看一下`NioServerSocketChannel`的类图：

![NioServerSocketChannel](https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143911.png)

`AbstractNioMessageChannel`类

```java
public abstract class AbstractNioMessageChannel extends AbstractNioChannel{
    protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        // 这个父类什么事都没做，继续向上调用父类
        super(parent, ch, readInterestOp);
    }
}
```

父类`AbstractNioMessageChannel`的构造方法再调用其父类`AbstractNioChannel`的构造方法

```java
public abstract class AbstractNioChannel extends AbstractChannel{
    protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        // 1. 继续向上调用父类
        super(parent);
        this.ch = ch;
        // 2. 设置感兴趣的事件为Accept事件
        this.readInterestOp = readInterestOp;
        try {
            // 3. 设置为非阻塞模式
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                ch.close();
            } catch (IOException e2) {
                logger.warn(
                            "Failed to close a partially initialized socket.", e2);
            }

            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
    }
}
```

`AbstractChannel`

```java
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel{
    private final Channel parent;
    private final ChannelId id;
    private final Unsafe unsafe;
    private final DefaultChannelPipeline pipeline;
    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        // 生成全局唯一的channel id
        id = newId();
        // new Unsafe()类
        unsafe = newUnsafe();
        // 生成channel关联的唯一pipeline
        pipeline = newChannelPipeline();
    }
    
    // 初始化ChannelPipeline
    protected DefaultChannelPipeline newChannelPipeline() {
        // 创建默认的channelPipeline
        return new DefaultChannelPipeline(this);
    }
}
```

`DefaultChannelPipeline`

```java
public class DefaultChannelPipeline implements ChannelPipeline{
    // 我们可以看出ChannelPipeline是一个双向链表结构
    protected DefaultChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        succeededFuture = new SucceededChannelFuture(channel, null);
        voidPromise =  new VoidChannelPromise(channel, true);
        // 尾结点，它是channelHandlerContext、入站handler
        tail = new TailContext(this);
        // 头结点，它是channelHandlerContext、出站handler、入站handler
        head = new HeadContext(this);
       
        // 头结点指向尾结点
        head.next = tail;
        // 尾结点的指向头结点
        tail.prev = head;
    }
}
```

注意：当向pipeline里面添加handler时，调用`addLast()`方法时，它是从最后一个结点`Tail`的上一个节点插入。

```java
public class DefaultChannelPipeline implements ChannelPipeline{
    // 插入的时候addLast(ChannelHandler... handlers), 最终会调用到addLast0这个方法;
	private void addLast0(AbstractChannelHandlerContext newCtx) {
        // 获取尾结点tail的上一个结点
        AbstractChannelHandlerContext prev = tail.prev;
        // 新结点前指针指向tail的上一个结点
        newCtx.prev = prev;
        // 新结点后指针指向tail结点
        newCtx.next = tail;
        // prev的后指针指向新结点
        prev.next = newCtx;
        // tail的前指针指向新结点
        tail.prev = newCtx;
    }
}
```

同理，`addFirst`是插在`Head`的后面。



#### 3.5.1 初始化线程池组

流程图：

<img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143918.png" alt="NioEventLoopGroup初始化 (1)" style="zoom: 50%;" />

初始化线程池组`NioEventLoopGroup`：

```java
EventLoopGroup boss = new NioEventLoopGroup(1);
```

`NioEventLoopGroup`继承关系：

<img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143924.png" alt="NioEventLoopGroup" style="zoom: 80%;" />

`NioEventLoopGroup`类：

```java
public class NioEventLoopGroup extends MultithreadEventLoopGroup {
    // 无参构造方法
    public NioEventLoopGroup() {
        this(0);
    }
    
    // 指定线程数量的构造方法
    public NioEventLoopGroup(int nThreads) {
        this(nThreads, (Executor) null);
    }
    // 指定线程数量和执行器的构造方法
    public NioEventLoopGroup(int nThreads, Executor executor) {
        // SelectorProvider.provider()根据系统选择适配的选择器提供者
        this(nThreads, executor, SelectorProvider.provider());
    }
    
    //指定线程数量、执行器、Selector的构造方法
    public NioEventLoopGroup(
            int nThreads, Executor executor, final SelectorProvider selectorProvider) {
        this(nThreads, executor, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }
    
    //指定线程数量、执行器、Selector、SelectStrategyFactory的构造方法
    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory) {
        // 调用父类MultithreadEventLoopGroup的构造方法
        super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }
}
```

===》父类`MultithreadEventLoopGroup`的构造方法

```java
public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {
    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        // 线程数 = CPU的核数*2
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
    }
    
    protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
        // 如果初始化的时候没有赋值线程数，将会使用系统默认的线程数=CPU的核数*2
        // 然后调用父类MultithreadEventExecutorGroup的构造方法
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }
}
```

===》父类`MultithreadEventExecutorGroup`的构造方法

```java
public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {
    private final EventExecutor[] children;
    
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
    }
    
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor,
                                            EventExecutorChooserFactory chooserFactory, Object... args) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        if (executor == null) {
            // 使用默认的ThreadPerTaskExecutor线程执行，它实现JDK Executor接口，是一个管理线程池的工具类
            // 它跟Executor一样，只不过自己定义了线程工厂
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }

        // 初始化EventExecutor数组
        children = new EventExecutor[nThreads];

        for (int i = 0; i < nThreads; i ++) {
            boolean success = false;
            try {
                // 初始化EventExecutor对象，args表示多个参数
                children[i] = newChild(executor, args);
                success = true;
            } catch (Exception e) {
                // TODO: Think about if this is a good exception type
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                if (!success) {
                    // 如果初始化EventExecutor对象失败，则关闭所有已经创建的EventExecutor对象
                    for (int j = 0; j < i; j ++) {
                        children[j].shutdownGracefully();
                    }

                    for (int j = 0; j < i; j ++) {
                        EventExecutor e = children[j];
                        try {
                            while (!e.isTerminated()) {
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            // Let the caller handle the interruption.
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }
        // 选择轮询执行器，这里面有两层判断：
        //（1）如果传进来的数是2的幂次方，则new PowerOfTwoEventExecutorChooser(executors)
        // (2) 如果不是，则是new GenericEventExecutorChooser(executors)
        // 最终，归根结底，都是实现轮询线程的效果
        chooser = chooserFactory.newChooser(children);

        //监听每个EventExecutor的关闭状态，所有EventExecutor都关闭了，Group也就关闭了
        final FutureListener<Object> terminationListener = new FutureListener<Object>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                if (terminatedChildren.incrementAndGet() == children.length) {
                    terminationFuture.setSuccess(null);
                }
            }
        };

        // 给每个EventExecutor添加关闭状态监听器
        for (EventExecutor e: children) {
            e.terminationFuture().addListener(terminationListener);
        }

        Set<EventExecutor> childrenSet = new LinkedHashSet<EventExecutor>(children.length);
        Collections.addAll(childrenSet, children);
        // 创建一个只读的集合
        readonlyChildren = Collections.unmodifiableSet(childrenSet);
    }
}
```

至此，线程池组就初始化完毕。

`NioEventLoop`和`NioEventLoopGroup`的区别

1. `NioEventLoopGroup`是`NioEventLoop`的容器，提供了生成`NioEventLoop`的方法
2. `NioEventLoop`负责IO的读写之外,还兼顾处理以下两类任务：系统Task、定时任务

`NioEventLoop`UML图

<img src="https://gitee.com/zhangyijun0229/picture-for-pic-go/raw/master/img/20210406143934.png" alt="NioEventLoop" style="zoom: 80%;" />



从上面可以看出，线程池组已经`EventLoopGroup`已经初始化完毕，并且创建了指定的`EventLoop`。

---

下面开始查看它是如何绑定端口，并且注册感兴趣的事件为`Accept`。

以启动类里面的这行代码为出发点开始进入源代码分析。

```java
ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(6666)).sync();
```

`ServerBootstrap`继承自`AbstractBootstrap`，由于`ServerBootstrap`没有`bind()`方法，需要进入`AbstractBootstrap`类中查看

```java
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {
    public ChannelFuture bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }
    // 我们可以看出bind方法的返回值是ChannelFuture（extends Futrure<Void>），这是为了调用sync()方法准备的。
    public ChannelFuture bind(SocketAddress localAddress) {
        // 验证参数，主要验证：group、channelFactory、childHandler是否为null，如果为null，则抛出异常
        // childGroup如果为null，则使用parentGroup替代
        validate();
        return doBind(ObjectUtil.checkNotNull(localAddress, "localAddress"));
    }
    
    /** 私有方法 绑定端口*/
    private ChannelFuture doBind(final SocketAddress localAddress) {
        // 1. 初始化并且注册Selector的感兴趣的事件为Accept
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        if (regFuture.isDone()) {
            // At this point we know that the registration was complete and successful.
            ChannelPromise promise = channel.newPromise();
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();

                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }
    
    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            // 1.1 反射创建ServerSocketChannel对象，这里面创建何种对象有ServerBootStrap.channel(XxxxServerSocketChannel.class)指定
            channel = channelFactory.newChannel();
            // 1.9 初始化
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
                // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        // 1.10 注册channel
        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    i.e. It's safe to attempt bind() or connect() now:
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         because register(), bind(), and connect() are all bound to the same thread.

        return regFuture;
    }
}
```

紧接着上述步骤1.1，我们来看看初始化`ServerSocketChannel`通道做了哪些事情。

以`NioServerSocketChannel`为例，我们来看看初始化过程做了哪些事情，以及它的继承关系。

```java
public class NioServerSocketChannel extends AbstractNioMessageChannel
                             implements io.netty.channel.socket.ServerSocketChannel {
    // 默认选择器提供程序
    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();
    
    // 1.2 反射调用无参构造方法创建对象
    public NioServerSocketChannel() {
        // 1.3 根据默认选择器创建ServerSocketChannel，然后调用有参构造方法
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }
    
    public NioServerSocketChannel(ServerSocketChannel channel) {
        // 1.4 调用父类的构造方法
        super(null, channel, SelectionKey.OP_ACCEPT);
        config = new NioServerSocketChannelConfig(this, javaChannel().socket());
    }
}
```



下面分析这些父类初始化过程做了哪些事情。

`AbstractNioMessageChannel` ：什么都没做，抛给其父类`AbstractNioChannel`去做

```java
public abstract class AbstractNioMessageChannel extends AbstractNioChannel {
    boolean inputShutdown;

    /**
     * @see AbstractNioChannel#AbstractNioChannel(Channel, SelectableChannel, int)
     */
    protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        // 1.5 继续向上调用父类AbstractNioChannel
        super(parent, ch, readInterestOp);
    }
}
```

 `AbstractNioChannel` : 

```java
public abstract class AbstractNioChannel extends AbstractChannel {
    private final SelectableChannel ch;
    // 感兴趣的事件
    protected final int readInterestOp; 
    protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        // 1.6 AbstractChannel父类执行构造方法，主要是初始化ChannelPipeline
        super(parent);
        this.ch = ch;
        // 1.7 设置感兴趣的事件为Accept
        this.readInterestOp = readInterestOp;
        try {
            // 1.8 设置Accept事件为非阻塞模式
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                ch.close();
            } catch (IOException e2) {
                logger.warn(
                            "Failed to close a partially initialized socket.", e2);
            }

            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
    }
}
```

`AbstractChannel`

```java
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {
    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
        unsafe = newUnsafe();
         // 1.6.1 初始化默认的ChannelPipeline
        pipeline = newChannelPipeline();
    }
    protected DefaultChannelPipeline newChannelPipeline() {
        return new DefaultChannelPipeline(this);
    }
}
```

```java
public class DefaultChannelPipeline implements ChannelPipeline {
    // 头结点
    final AbstractChannelHandlerContext head;
    // 尾结点
    final AbstractChannelHandlerContext tail;
    
    // AbstractChannelHandlerContext类里定义
    volatile AbstractChannelHandlerContext next;
    volatile AbstractChannelHandlerContext prev;
    
    /** 1.6.2 初始化ChannelPipeline，双向链表结构
     *  head ---> tail
     *       <---
     */
    protected DefaultChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        succeededFuture = new SucceededChannelFuture(channel, null);
        voidPromise =  new VoidChannelPromise(channel, true);

        // 尾结点
        tail = new TailContext(this);
        // 头结点
        head = new HeadContext(this);

        // 头结点的后指针指向尾结点
        head.next = tail;
        // 尾结点的前指针指向头结点
        tail.prev = head;
    }
}
```



至此，`ServerSocketChannel`已初始化完成，下面来分析`init(channel)`方法

`ServerBootstrap`类重写了`init(Channel channel)`方法。

```java
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {
    /**
     * 1.9 初始化channel
     */
    @Override
    void init(Channel channel) {
        setChannelOptions(channel, newOptionsArray(), logger);
        setAttributes(channel, attrs0().entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY));

        ChannelPipeline p = channel.pipeline();

        final EventLoopGroup currentChildGroup = childGroup;
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(EMPTY_OPTION_ARRAY);
        }
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs = childAttrs.entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY);
        
        // 主要是给ChannelPipeline添加入站ChannelInboundHandlerAdapter
        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }
}
```



下面分析1.10--注册Channel

```java
ChannelFuture regFuture = config().group().register(channel);
```

`config().group()`返回的是`NioEventLoopGroup`对象，由于它没有重写`register(channel)`方法，需要向上调用父类的`register(channel)`方法。\

`MultithreadEventLoopGroup`

```java
public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {
    /**
     * 1.10.1 
     */
    @Override
    public ChannelFuture register(Channel channel) {
        // 1.10.1
        // next()是获取从executors数组里面获取EventExecutor对象，在本例中，其实际对象为NioEventLoop
        // NioEventLoop类从没有重写register(channel)方法，需要进入其父类SingleThreadEventLoop调用
        return next().register(channel);
    }
}
```

`SingleThreadEventLoop`

```java
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {
    @Override
    public ChannelFuture register(Channel channel) {
        // 1.10.2 调用register
        return register(new DefaultChannelPromise(channel, this));
    }
    
    @Override
    public ChannelFuture register(final ChannelPromise promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        // 1.10.3 获取AbstractUnsafe对象调用register
        promise.channel().unsafe().register(this, promise);
        return promise;
    }
}
```

`AbstractChannel`

```java
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {
    protected abstract class AbstractUnsafe implements Unsafe {
        /**
         * 
         */
        @Override
        public final void register(EventLoop eventLoop, final ChannelPromise promise) {
            ObjectUtil.checkNotNull(eventLoop, "eventLoop");
            if (isRegistered()) {
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }
            if (!isCompatible(eventLoop)) {
                promise.setFailure(
                        new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }

            AbstractChannel.this.eventLoop = eventLoop;

            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            // 1.10.4
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }
        
        private void register0(ChannelPromise promise) {
            try {
                // check if the channel is still open as it could be closed in the mean time when the register
                // call was outside of the eventLoop
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                boolean firstRegistration = neverRegistered;
                // 注册
                doRegister();
                neverRegistered = false;
                registered = true;

                // Ensure we call handlerAdded(...) before we actually notify the promise. This is needed as the
                // user may already fire events through the pipeline in the ChannelFutureListener.
                // 
                pipeline.invokeHandlerAddedIfNeeded();

                safeSetSuccess(promise);
                pipeline.fireChannelRegistered();
                // Only fire a channelActive if the channel has never been registered. This prevents firing
                // multiple channel actives if the channel is deregistered and re-registered.
                if (isActive()) {
                    if (firstRegistration) {
                        pipeline.fireChannelActive();
                    } else if (config().isAutoRead()) {
                        // This channel was registered before and autoRead() is set. This means we need to begin read
                        // again so that we process inbound data.
                        //
                        // See https://github.com/netty/netty/issues/4805
                        beginRead();
                    }
                }
            } catch (Throwable t) {
                // Close the channel directly to avoid FD leak.
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }
    }
}
```

`AbstractNioChannel`

```java
public abstract class AbstractNioChannel extends AbstractChannel {	
	@Override
    protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {
                // 将channel注册到eventLoop对应的selector上，并返回selectionKey
                selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
                return;
            } catch (CancelledKeyException e) {
                if (!selected) {
                    // Force the Selector to select now as the "canceled" SelectionKey may still be
                    // cached and not removed because no Select.select(..) operation was called yet.
                    eventLoop().selectNow();
                    selected = true;
                } else {
                    // We forced a select operation on the selector before but the SelectionKey is still cached
                    // for whatever reason. JDK bug ?
                    throw e;
                }
            }
        }
    }
}

```

