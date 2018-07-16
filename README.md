# mini-redis
使用Netty实现Simple Redis服务器客户端，以及一个基于JDK NIO的版本
## 功能
1. 编码解码支持的Redis数据结构如下：
* 数组
* 单行字符串
* 定长字符串
2. 支持Redis字符串缓存（基于ConcurrentHashMap）
3. 服务端使用非阻塞I/O实现
4. 客户端使用非阻塞I/O实现
5. Netty实现版本
6. Java NIO实现版本

## 演示

开启我们服务器

![开启我们服务器](https://upload-images.jianshu.io/upload_images/12813982-c6c80e47ed2aef6d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

使用Redis客户端连接服务器

![使用Redis客户端连接服务器](https://upload-images.jianshu.io/upload_images/12813982-e797761ecd545075.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

使用我们实现的客户端连接服务器

![使用我们实现的客户端连接服务器](https://upload-images.jianshu.io/upload_images/12813982-46b194a1868b5b58.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


## 博文与参考
基于Java NIO的实现，我将思路总结在[这里](https://www.jianshu.com/p/40973c2a14dc)。

基于netty的实现，Redis协议编解码部分基于[这篇博文](https://mp.weixin.qq.com/s/sOFIHWgS88b5aFRQdp-2dA)，并进行了一些修改补充；

Netty服务器客户端的部分参考了同一个作者的[简单RPC框架](https://github.com/pyloque/rpckids)。
