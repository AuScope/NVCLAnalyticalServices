package org.auscope.nvcl.server.util;

public class ThreadDemo  extends Thread {

    public void run() {
    
    Thread t = Thread.currentThread();
    String tName = t.getName();
    int count = 10;
    for (int i =0 ;i< count; i++) {
        System.out.println(tName +", status = " + t.isAlive() + "count:" + i);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    }

    public static void main(String args[]) throws Exception {
    
    Thread t1 = new ThreadDemo();
    Thread t2 = new ThreadDemo();
    Thread t3 = new ThreadDemo();    
    t1.start();
    t2.start();    
    t3.start();
    
    t1.join();
    t2.join();
    t3.join();    
    }
 } 