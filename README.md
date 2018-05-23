## zkclient 项目
### 项目介绍：
zkclient 是对zookeeper java客户端进行的封装，主要实现了连接、断线重连，watch事件改为listen监听事件,分布式锁等

**注意：** 使用时需要自行编译安装到maven或打成jar使用

### 使用方式：
```java
	<dependency>
		<artifactId>zk-client</artifactId>
    	<groupId>com.danyy.zk</groupId>
    	<version>1.0.0-SNAPSHOT</version>
	</dependency>
```

```
后期完善
```


##### zookeeper分布式锁实现思路（即SimpleLock思路）

```
1.  首先指定一个作为锁的znode，通常用它来描述被锁定的实体，称为/leader；
2.  然后希望获得锁的客户端创建一些短暂顺序znode，作为锁znode的子节点。
3.  在任何时间点，顺序号最小的客户端将持有锁。 

例如，有两个客户端差不多同时创建znode，分别为/leader/lock0000000001和/leader/lock0000000002，
那么创建/leader/lock-1的客户端将会持有锁，因为它的znode顺序号最小。ZooKeeper服务是顺序的仲裁者，因为它负责分配顺序号。

4.  通过删除znode /leader/lock0000000001和即可简单地将锁释放；
5.  另外，如果客户端进程死亡，对应的短暂znode也会被删除。
6.  接下来，创建/leader/lock0000000002的客户端将持有锁，因为它顺序号紧跟前一个。
7.  通过创建一个关于znode删除的观察，可以使客户端在获得锁时得到通知
```