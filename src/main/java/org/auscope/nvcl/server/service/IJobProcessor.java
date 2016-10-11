package org.auscope.nvcl.server.service;

import org.auscope.nvcl.server.vo.AnalyticalJobVo;

public class IJobProcessor extends Thread {
    public void run()
    {
        System.out.println("Thread:start:");
        System.out.println("Thread:end:");
    }
    public void setAnalyticalJob(AnalyticalJobVo messageVo) {}    
}
