package org.auscope.nvcl.server.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.BoreholeResultVo;


public class NVCLAnalyticalJobProcessorManager{
	
    private static final Logger logger = LogManager.getLogger(NVCLAnalyticalMessageConverter.class);
	
    private List<IJobProcessor> processorList = new ArrayList<IJobProcessor>();
    private AnalyticalJobResultVo sumJobResultVo = new AnalyticalJobResultVo();
    
    public AnalyticalJobResultVo getSumJobResultVo() {
        return this.sumJobResultVo;
    }
    
    public boolean processRequest(AnalyticalJobVo messageVo) {
        String serviceUrls = messageVo.getServiceUrls();
        String requestType = messageVo.getRequestType();
        if (Utility.stringIsBlankorNull(serviceUrls))
            return false;
        String[] serviceUrlArray = serviceUrls.split(",");
        sumJobResultVo.setEmail(messageVo.getEmail());
        sumJobResultVo.setJobDescription(messageVo.getJobDescription());
        sumJobResultVo.setJobid(messageVo.getJobid());
        
        for (String serviceUrl : serviceUrlArray) {
            AnalyticalJobVo jobVo = new AnalyticalJobVo(messageVo);
            IJobProcessor processor = null;
            jobVo.setServiceUrls(serviceUrl);
            switch (requestType) {
            case "ANALYTICAL":
                processor = new NVCLAnalyticalJobProcessor();
                break;
            case "TSGMOD":
                processor = new TSGModJobProcessor();                
                break;
            default:
                break;
            }
            processor.setAnalyticalJob(jobVo);
            processorList.add(processor);
            processor.start();
        }
        try
        {
            for (IJobProcessor processor : processorList) {
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
           logger.error("Job processor thread interrupted.");
        }
        int boreholesSize = sumJobResultVo.boreholes.size();
        int failedBoreholesSize = sumJobResultVo.failedBoreholes.size();
        int errorBoreholesSize = sumJobResultVo.errorBoreholes.size();
        logger.info("NVCLAnalyticalJobProcessorManager commplete. Boreholes that exceeded threshold: " + boreholesSize + ". Boreholes that fell below threshold : " + failedBoreholesSize + ". Boreholes that caused errors : " + errorBoreholesSize );
        return true;
    }
}
