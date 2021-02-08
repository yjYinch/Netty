# Netty

**Netty是什么？**

* `Netty`是一个**异步的**、**基于事件驱动**的网络应用框架，用以快速开发高性能、高可可靠性的网络`IO`程序
* `Netty`主要针对在`TCP`协议下，面向Clients端的高并发应用，或者Peer-to-Peer场景下的大量数据持续传输的应用
* `Netty`本质是一个`NIO`框架，适用于服务器通讯相关的多种应用场景

**Netty的结构**

![netty组成结构](E:\study\pictures\Netty组成结构.PNG)

**Netty的应用场景**

* 分布式服务的远程服务调用`RPC`框架

* `Netty`作为高性能的基础通信组件，提供了`TCP/UDP`和`HTTP`协议栈，方便定制和开发私有协议栈

---



## 一、IO模型

### 1.1 前言

​		学习`Netty`之前，需要先理解`IO`模型是什么？以及在`Java`中支持的`IO`模式。

**什么是I/O模型？**

> 简单理解就是用什么样的通道进行数据的发送和接受，并且很大程序上决定了程序通信的性能。

**`Java`中支持的3中网络编程模型/IO模式**

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

**工作原理图**

![image-20210206223302882](E:\study\pictures\BIO模型.PNG)

**BIO编程流程**

1. 服务端启动一个`ServerSocket`
2. 客户端启动`Socket`对服务器进行通信，默认情况下服务器端需要对每个客户建立一个线程与之通讯
3. 客户端发送请求后，先咨询服务器是否有线程响应，如果没有则会等待，或者被拒绝
4. 如果有响应，客户端线程会等待请求结束后，再继续执行



服务端：

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



### 1.3 NIO模型

#### 1.3.1 **NIO工作原理**

![image-20210206232947444](E:\study\pictures\NIO模型.PNG)

**上图流程说明：**

```markdown
# 1. 当有客户端连接时，会通过服务端的ServerSocketChannel得到SocketChannel

# 2. selector开始监听...

# 2. 将SocketChannel注册到Channel (一个Selector可以注册多个SocketChannel)，注册方法是register

# 3. 注册后返回一个SelectionKey，会和该Selector关联（集合）

# 4. Selector进行监听select方法，返回有事件发生的通道的个数total

# 5. 当total > 0（表示有事件发生） 时，获取各个SelectionKey

# 6. 根据SelectionKey反向获取注册的SocketChannel

# 7.根据得到的SocketChannel完成读写任务
```

**`NIO`三大核心部分**：

* **Channel通道**
* **Buffer缓冲区**
* **Selector选择器**

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

   ![buffer初始化](E:\study\pictures\buffer初始化.JPG)

2. 写入数据后

   ![buffer写入数据后](E:\study\pictures\buffer写入数据后.JPG)

3. flip切换后

   ![buffer flip切换后](E:\study\pictures\buffer flip切换后.JPG)

4. 读完数据后

   ![buffer读完数据后](E:\study\pictures\buffer读完数据后.JPG)

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

![channel](E:\study\pictures\channel.JPG)

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

![Selector](E:\study\pictures\Selector.JPG)

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



#### 1.3.5 总结

* 每个Channel对应一个Buffer
* selector --> thread --> 多个 channel
* buffer是双向的，既可以读也可以写，但是需要flip切换，而BIO的读写只能是单向的，要么是input要么是output
* channel是双向的



## 二、NIO

服务端：

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

