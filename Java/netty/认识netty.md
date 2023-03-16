netty
--
>Netty 是一个利用 Java 的高级网络的能力，隐藏其背后的复杂性而提供一个易于使用的 API 的客户端/服务器框架。
Netty 是一个广泛使用的 Java 网络编程框架（Netty 在 2011 年获得了Duke's Choice Award，见https://www.java.net/dukeschoice/2011 它活跃和成长于用户社区，像大型公司 Facebook 和 Instagram 以及流行 开源项目如 Infinispan, HornetQ, Vert.x, Apache Cassandra 和 Elasticsearch 等，都利用其强大的对于网络抽象的核心代码
### io模型
```
# IO 模型简单的理解，就是用什么样的通道进行数据的发送和接收，很大程度上决定了程序通信的性能。

# Java 共支持 3 种网络编程模型: BIO、NIO、AIO

# Java BIO: <同步并阻塞>
	# 服务器实现模式为一个连接一个线程，即客户端有连接请求时服务器端就需要启动一个线程进行处理，如果这个连接不做任何事，就会造成不必要的线程开销。
	
# Java NIO: <同步非阻塞>
	# 服务器实现模式为一个线程处理多个请求连接，即客户端发送的连接请求都会注册到多路复用器上，多路复用器轮训到连接有 I/O 请求就进行处理
	
# Java AIO: <异步非阻塞>
	# AIO 引入了异步通道的概念，采用了 Proactor 模式，简化了程序编写，有效的请求才启动线程。
	# 特点是先由操作系统完成后才通知服务端程序启动线程去处理，一般适用于连接数较多且连续时间较长的应用
```
###BIO、NIO、AIO 适用场景分析
```
# BIO 方式适用于连接数目较小且固定的架构，这种方式对服务器资源要求比较高，并发局限于应用中，JDK1.4 以前的唯一选择，但程序简单易理解。

# NIO 方式适用于连接数目多且连接比较短<轻操作> 的架构，比如聊天服务器，弹幕系统，服务器间通讯等，变成比较复杂，JDK1.4 开始支持。

# AIO 方式适用于连接数目多且连接比较长<重操作> 的架构，比如相册服务器，充分调用 OS 参与并发操作，编程比较复杂，JDK1.7 开始支持。
```
netty的构成
--
### Channel
Channel 是 NIO 基本的结构。它代表了一个用于连接到实体如硬件设备、文件、网络套接字或程序组件,能够执行一个或多个不同的 I/O 操作（例如读或写）的开放连接。 现在，把 Channel 想象成一个可以“打开”或“关闭”，“连接”或“断开”和作为传入和传出数据的运输工具
### Callback(回调)
callback (回调)是一个简单的方法,提供给另一种方法作为引用,这样后者就可以在某个合适的时间调用前者。这种技术被广泛使用在各种编程的情况下,最常见的方法之一通知给其他人操作已完成。
Netty 内部使用回调处理事件时。一旦这样的回调被触发，事件可以由接口 ChannelHandler 的实现来处理。如下面的代码，一旦一个新的连接建立了,调用 channelActive(),并将打印一条消息。
Listing 1.2 ChannelHandler triggered by a callback
```java
public class ConnectHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx){
        //1
        System.out.println("Client " + ctx.channel().remoteAddress() + " connected");
    }
}
```
1.当建立一个新的连接时调用 ChannelActive()
### Future
Future 提供了另外一种通知应用操作已经完成的方式。这个对象作为一个异步操作结果的占位符,它将在将来的某个时候完成并提供结果。
JDK 附带接口 java.util.concurrent.Future ,但所提供的实现只允许您手动检查操作是否完成或阻塞了。这是很麻烦的，所以 Netty 提供自己了的实现,ChannelFuture,用于在执行异步操作时使用。

ChannelFuture 提供多个附件方法来允许一个或者多个 ChannelFutureListener 实例。这个回调方法 operationComplete() 会在操作完成时调用。事件监听者能够确认这个操作是否成功或者是错误。如果是后者,我们可以检索到产生的 Throwable。简而言之, ChannelFutureListener 提供的通知机制不需要手动检查操作是否完成的。

每个 Netty 的 outbound I/O 操作都会返回一个 ChannelFuture;这样就不会阻塞。这就是 Netty 所谓的“自底向上的异步和事件驱动”。
下面例子简单的演示了作为 I/O 操作的一部分 ChannelFuture 的返回。当调用 connect() 将会直接是非阻塞的，并且调用在背后完成。由于线程是非阻塞的，所以无需等待操作完成，而可以去干其他事，因此这令资源利用更高效。
Listing 1.3 Callback in action
```
Channel channel = ...;
//不会阻塞
ChannelFuture future = channel.connect(new InetSocketAddress("192.168.0.1", 25));
```