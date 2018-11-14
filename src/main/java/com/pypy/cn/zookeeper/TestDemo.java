package com.pypy.cn.zookeeper;

import com.alibaba.fastjson.JSONObject;
import org.apache.zookeeper.*;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestDemo {

    private ZooKeeper zk;
    //--zk服务器的ip地址和端口号
    //--客户端链接服务端的超时时间，单位是毫秒
    //--watcher接口，监听接口
    //--zk的网络通信框架使用的是netty,netty的底层使用的是NIO，
    //因此zk的链接是非阻塞的
    @Before
    public void Connect() throws Exception {
        //通过闭锁，将一个非阻塞链接变为阻塞链接
        final CountDownLatch cdl = new CountDownLatch(1);
        zk = new ZooKeeper("192.168.80.72:2181", 3000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if(event.getState().equals(Event.KeeperState.SyncConnected)){
                    System.out.println("链接成功");
                    cdl.countDown();
                }
            }
        });
        cdl.await();
    }

    @Test
    public void createNode() throws KeeperException, InterruptedException {
        zk.create("/park01","hello".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    @Test
    public void getDate() throws KeeperException, InterruptedException {
        byte[] data = zk.getData("/park01", null, null);
        System.out.println(new String(data));
    }


    @Test
    public void setDate() throws KeeperException, InterruptedException {
        //--路径，更新数据，数据版本号
        //版本号一般写成-1，无论什么版本都会更新
        zk.setData("/park01","123".getBytes(),-1);
    }

    @Test
    public void deleteNode() throws KeeperException, InterruptedException {
        zk.delete("/park01",-1);
    }

    @Test
    public void getChildNode() throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren("/park01", null);
        System.out.println(JSONObject.toJSONString(children));
    }

    @Test
    public void watchDateChange() throws KeeperException, InterruptedException {
        for(;;){
            final CountDownLatch cdl = new CountDownLatch(1);
            zk.getData("/park01", new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    //--EventType.NodeDataChangeed:节点数据发生变化的事件
                    if(event.getType().equals(Event.EventType.NodeDataChanged)){
                        System.out.println("有数据发生变化");
                        try {
                            byte[] data = zk.getData("/park01", null, null);
                            System.out.println("变化数据："+new String(data));
                            System.out.println("改变一下");
                        } catch (KeeperException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        cdl.countDown();
                    }
                }
            },null);
            cdl.await();
        }
    }

    @Test
    public void watchDelete() throws KeeperException, InterruptedException {
        zk.exists("/park01", new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if(event.getType().equals(Event.EventType.NodeDeleted)){
                    System.out.println("节点被删除");
                }
            }
        });
        while(true);
    }

    @Test
    public void watchChildChanged() throws KeeperException, InterruptedException {
        for(;;){
            final CountDownLatch cdl = new CountDownLatch(1);
            zk.getChildren("/park01", new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if(event.getType().equals(Event.EventType.NodeChildrenChanged)){
                        System.out.println("子节点发生变化");
                        try {
                            List<String> children = zk.getChildren("/park01", null, null);
                            for (String path: children) {
                                path = "/park01/"+path;
                                zk.getData(path, new Watcher() {
                                    @Override
                                    public void process(WatchedEvent event) {
                                        if(event.getType().equals(Event.EventType.NodeDataChanged)){
                                            System.out.println("有数据发生变化");
                                        }
                                    }
                                },null);
                            }
                            System.out.println();
                        } catch (KeeperException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        cdl.countDown();
                    }
                }
            });
            cdl.await();
        }
    }
}
