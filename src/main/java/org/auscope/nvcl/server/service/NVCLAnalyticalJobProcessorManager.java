package org.auscope.nvcl.server.service;

import java.util.ArrayList;
import java.util.List;

import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.BoreholeResultVo;


public class NVCLAnalyticalJobProcessorManager{
    private List<NVCLAnalyticalJobProcessor> processorList = new ArrayList<NVCLAnalyticalJobProcessor>();
    private AnalyticalJobResultVo sumJobResultVo = new AnalyticalJobResultVo();
    public AnalyticalJobResultVo getSumJobResultVo() {
        return this.sumJobResultVo;
    }
    public boolean processRequest(AnalyticalJobVo messageVo) {
        String serviceUrls = messageVo.getServiceUrls(); //"http://nvclwebservices.vm.csiro.au/geoserverBH/wfs";//"http://geology.data.nt.gov.au/geoserver/wfs"; //
        if (serviceUrls == null)
            return false;
        String[] serviceUrlArray = serviceUrls.split(",");
        sumJobResultVo.setEmail(messageVo.getEmail());
        sumJobResultVo.setJobDescription(messageVo.getJobDescription());
        sumJobResultVo.setJobid(messageVo.getJobid());
        
        for (String serviceUrl : serviceUrlArray) {
            AnalyticalJobVo jobVo = new AnalyticalJobVo(messageVo);
            jobVo.setServiceUrls(serviceUrl);
            NVCLAnalyticalJobProcessor processor = new NVCLAnalyticalJobProcessor();
            processor.setAnalyticalJob(jobVo);
            processorList.add(processor);
            processor.start();
        }
        try
        {
            for (NVCLAnalyticalJobProcessor processor : processorList) {
                processor.join();
                AnalyticalJobResultVo jobResultVo = processor.getJobResult();
                for ( BoreholeResultVo boreholeResultVo : jobResultVo.boreholes) {
                    sumJobResultVo.addBoreholes(boreholeResultVo);
                }
                for ( BoreholeResultVo boreholeResultVo : jobResultVo.failedBoreholes) {
                    sumJobResultVo.addFailedBoreholes(boreholeResultVo);
                }     
                for ( BoreholeResultVo boreholeResultVo : jobResultVo.errorBoreholes) {
                    sumJobResultVo.addErrorBoreholes(boreholeResultVo);
                }                 
            }
        }catch(InterruptedException e)
        {
           System.out.println("Thread interrupted.");
        }
        int boreholesSize = sumJobResultVo.boreholes.size();
        int failedBoreholesSize = sumJobResultVo.failedBoreholes.size();
        int errorBoreholesSize = sumJobResultVo.errorBoreholes.size();
        System.out.println("total result:" + boreholesSize + ":" + failedBoreholesSize + ":" + errorBoreholesSize );
        System.out.println("NVCLAnalyticalJobProcessorManager::processRequest:ended:");
        return true;
    }
}