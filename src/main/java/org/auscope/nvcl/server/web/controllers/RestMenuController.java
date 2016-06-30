package org.auscope.nvcl.server.web.controllers;

import java.awt.Menu;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jms.Destination;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.service.NVCLAnalyticalGateway;
import org.auscope.nvcl.server.service.NVCLAnalyticalQueueBrowser;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobStatusVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.ConfigVo;
import org.auscope.nvcl.server.vo.AnalyticalJobResponse;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
/**
 * restful Controller that handles all {@link Menu}-related requests
 *
 * @author Lingbo Jiang
 * *
 */

@RestController
public class RestMenuController {
    private static final Logger logger = LogManager.getLogger(RestMenuController.class);    
    @Autowired
    @Qualifier(value = "createConfig")
    private ConfigVo configVo;

    @Autowired
    @Qualifier(value = "nvclAnalyticalGateway")
    private NVCLAnalyticalGateway nvclAnalyticalGateway;

    @Autowired
    @Qualifier(value = "jmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    @Qualifier(value = "nvclSubmitDestination")
    private Destination nvclSubmitDestination;

    @Autowired
    @Qualifier(value = "nvclStatusDestination")
    private Destination nvclStatusDestination;    
    
    @Autowired
    @Qualifier(value = "nvclResultDestination")
    private Destination nvclResultDestination;    
//    @RequestMapping("/")
//    public String index(HttpServletRequest request, HttpServletResponse response) {
//
//        return "index";
//    }    
    @RequestMapping("/submitNVCLAnalyticalJob.do")
    public AnalyticalJobResponse submitNVCLAnalyticalJob(
            @RequestParam(required = true, value = "serviceurls") String serviceUrls ,
            @RequestParam(required = true, value = "email") String email ,          
            @RequestParam(required = true, value = "jobname") String jobname ,                  
            @RequestParam(required = false, value = "filter") String filter ,
            @RequestParam(required = true, value = "startdepth") int startDepth ,
            @RequestParam(required = true, value = "enddepth") int endDepth ,
            @RequestParam(required = false, value = "logname") String logName ,            
            @RequestParam(required = true, value = "classification") String classification ,
            @RequestParam(required = false, value = "algorithmoutputid") String algorithmOutputID ,
            @RequestParam(required = true, value = "span") int span ,
            @RequestParam(required = true, value = "units") String units ,
            @RequestParam(required = true, value = "value") float value ,
            @RequestParam(required = true, value = "logicalop") String logicalOp
            ) throws ServletException,
            IOException, SQLException, ParserConfigurationException, TransformerException {
        // mandatory field : holeIdentifier - validate if holeIdentifier is null
        
        String jobid = Utility.getMD5HashValue();
        if (jobname.startsWith("test-"))
            jobid = jobname;
        if (Utility.stringIsBlankorNull(serviceUrls)) {
            String errMsg = "jobid="+jobid +": holeidentifier is not valid.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);
        }
        if (Utility.ValidateEmail(email)) {
            String errMsg = "jobid="+jobid +": email is not valid.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);
        } else {
            email = email.toLowerCase();
        }
        
        if ((algorithmOutputID == null && logName == null) || 
                (algorithmOutputID !=null && logName != null)) {
            String errMsg = "jobid="+jobid +": you has to provide either logName or algorithmOutputID.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);
        }
        
        if ( algorithmOutputID !=null && !Utility.checkAlgoutiIDs(algorithmOutputID)) {
            String errMsg = "your algorithmOutputID="+algorithmOutputID +" is in wrong format.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);            
        }
        
        if (filter == null || filter.isEmpty()) {
            filter ="<ogc:Filter><PropertyIsEqualTo> <PropertyName>gsmlp:nvclCollection</PropertyName> <Literal>true</Literal> </PropertyIsEqualTo></ogc:Filter>";
        }

        AnalyticalJobVo jobVo = new AnalyticalJobVo();
        // set TSG as requestType to ConfigVo
        jobVo.setRequestType("ANALYTICAL");
        jobVo.setJobid(jobid);
        jobVo.setJobDescription(jobname);
        jobVo.setServiceUrls(serviceUrls);
        jobVo.setEmail(email);
        jobVo.setFilter(filter);
        jobVo.setStartDepth(startDepth);
        jobVo.setEndDepth(endDepth);
        jobVo.setLogName(logName);
        jobVo.setClassification(classification);
        jobVo.setAlgorithmOutputID(algorithmOutputID);
        jobVo.setSpan(span);
        jobVo.setUnits(units);
        jobVo.setValue(value);
        jobVo.setLogicalOp(logicalOp);
        logger.debug("Get a job request ..." + jobVo.printVo());
        System.out.println(jobVo.printVo());

        // url parameters validation start

        // mandatory field : validate if email null or empty or missing

        if (Utility.stringIsBlankorNull(units)) { 
            String errMsg = "jobid="+jobid +": units is not valid.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);
        }

        logger.debug("Start create JMS message ...");
        nvclAnalyticalGateway.setDestination(nvclSubmitDestination);
        String messageID = nvclAnalyticalGateway.createNVCLAnalyticalReqMsg(jobVo);

        if (messageID == null) {
            logger.error("Failed creating JMS message in queue ...");
            return  new AnalyticalJobResponse("ERROR" ,"jobid="+jobid +": Failed creating JMS message in queue ...");
        }

        logger.debug("JMS message created successfully ... ");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            
        }        

        return  new AnalyticalJobResponse("SUCCESS" ,"jobid="+jobid +": Your job has been successfully submitted. Please check your jobs status later");
    } 
    
//    @RequestMapping("/checkNVCLAnalyticalJob.do")
//    public List < AnalyticalJobVo > checkNVCLAnalyticalJob(@RequestParam(value="email") String email ) 
//            throws ServletException,IOException {         
//        NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
//        nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate);
//        List<AnalyticalJobVo> jobList = (ArrayList<AnalyticalJobVo>) nvclAnalyticalQueueBrowser.browseQueueSubmit(email, nvclSubmitDestination);
//        return jobList;
//    }
    
    @RequestMapping("/checkNVCLAnalyticalJobStatus.do")
    public List < AnalyticalJobStatusVo > checkNVCLJobStatus(@RequestParam(value="email") String email ) 
            throws ServletException,IOException {     
        // Retrieve messages from request and reply JMS queue
//        List<AnalyticalJobStatusVo> jobStatusList = new ArrayList<AnalyticalJobStatusVo> ();
//        NVCLAnalyticalSvc nvclAnalyticalSvc = new NVCLAnalyticalSvc();
//        Map<String, Object> msgMap = nvclAnalyticalSvc.browseStatus(email,jmsTemplate,nvclStatusDestination);
//        ArrayList<?> statusMsgList = (ArrayList<?>) msgMap.get("status");
//
//        if (statusMsgList == null) {
//            logger.info("status queue is null");
//        } else {
//            System.out.println("status queue is ****************");
//            for (Iterator<?> it1 = statusMsgList.iterator(); it1.hasNext();) {
//                AnalyticalJobStatusVo jmsMsgVo = (AnalyticalJobStatusVo) it1.next();
//                jobStatusList.add(jmsMsgVo);
//                System.out.println("status : " + jmsMsgVo.getStatus() + " jobid: " + jmsMsgVo.getJobid() + " jobdescription:" + jmsMsgVo.getJobDescription() + " joburl:" + jmsMsgVo.getJoburl());
//            }
//        }
        email = email.toLowerCase();
        NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
        nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate);
        //List<AnalyticalJobVo> reqMsgList = (ArrayList<AnalyticalJobVo>) nvclAnalyticalQueueBrowser.browseQueueMessages(email, reqDestination);
        List<AnalyticalJobStatusVo> jobStatusList = (ArrayList<AnalyticalJobStatusVo>) nvclAnalyticalQueueBrowser.browseQueueStatus(email, nvclStatusDestination);        
        return jobStatusList;
    }
    @RequestMapping("/getNVCLAnalyticalJobResult.do")
    public String getNVCLAnalyticalJobResult( @RequestParam(value="jobid", defaultValue="028c68636c05586c2985476dd7d7b069") String jobID ) throws ServletException,
            IOException {
//        NVCLAnalyticalSvc nvclAnalyticalSvc = new NVCLAnalyticalSvc();
//        Map<String, Object> msgMap = nvclAnalyticalSvc.browseResult(jobID,jmsTemplate,nvclResultDestination);
//        AnalyticalJobResultVo jmsMsgVo = null;
//        ArrayList<?> resultMsgList = (ArrayList<?>) msgMap.get("result");
//        if (resultMsgList == null) {
//            logger.info("result queue is null");
//        } else {
//            System.out.println("result queue is ****************");
//            for (Iterator<?> it1 = resultMsgList.iterator(); it1.hasNext();) {
//                jmsMsgVo = (AnalyticalJobResultVo) it1.next();
// //             System.out.println("jobid: " + jmsMsgVo.getJobid() + "jobdescription: " + jmsMsgVo.getJobDescription() + "boreholes : " + jmsMsgVo.getBoreholes() + "failedBoreholes:" + jmsMsgVo.getFailedBoreholes() + "errorBoreholes:" + jmsMsgVo.getErrorBoreholes());
//            }
//        }

        NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
        nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate);
        List<AnalyticalJobResultVo> resultMsgList = (ArrayList<AnalyticalJobResultVo>) nvclAnalyticalQueueBrowser.browseQueueResult(jobID, nvclResultDestination);
        AnalyticalJobResultVo jmsMsgVo = null;
        if (resultMsgList == null) {
            logger.info("result queue is null");
        } else {
            System.out.println("result queue is ****************");
            for (Iterator<?> it1 = resultMsgList.iterator(); it1.hasNext();) {
                jmsMsgVo = (AnalyticalJobResultVo) it1.next();
            }
        }      
        Gson gson = new Gson();
        return gson.toJson(jmsMsgVo);
    }
    
}
