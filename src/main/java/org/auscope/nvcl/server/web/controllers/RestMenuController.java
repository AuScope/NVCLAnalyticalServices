package org.auscope.nvcl.server.web.controllers;

import java.awt.Menu;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipOutputStream;

import javax.jms.Destination;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.service.NVCLAnalyticalGateway;
import org.auscope.nvcl.server.service.NVCLAnalyticalQueueBrowser;
import org.auscope.nvcl.server.service.NVCLAnalyticalRequestSvc;
import org.auscope.nvcl.server.service.SparkeyServiceSingleton;
import org.auscope.nvcl.server.service.TSGScriptCache;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.util.ZipUtil;
import org.auscope.nvcl.server.vo.AnalyticalJobResponse;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.TSGJobVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * restful Controller that handles all {@link Menu}-related requests
 *
 * @author Lingbo Jiang
 * *
 */

@RestController
public class RestMenuController {
    private static final Logger logger = LogManager.getLogger(RestMenuController.class);    

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper();
    
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

    @Autowired
    @Qualifier(value = "tsgscripts")
    private TSGScriptCache tsgscripts;
    
    
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
        
        String jobid = Utility.getMD5HashValue();
        if (jobname.startsWith("test-"))
            jobid = jobname;
        if (Utility.stringIsBlankorNull(serviceUrls)) {
            String errMsg = "serviceUrls are not valid.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);
        }
        if (!Utility.ValidateEmail(email)) {
            String errMsg = "email is not valid.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);
        } else {
            email = email.toLowerCase();
        }
        
        if ((Utility.stringIsBlankorNull(algorithmOutputID) && Utility.stringIsBlankorNull(logName)) || 
                (!Utility.stringIsBlankorNull(algorithmOutputID) && !Utility.stringIsBlankorNull(logName))) {
            String errMsg = "you must to provide either a logName or an algorithmOutputID.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);
        }
        
        if (!Utility.stringIsBlankorNull(algorithmOutputID) && !Utility.checkAlgoutiIDs(algorithmOutputID)) {
            String errMsg = "invalid algorithmOutputID : "+algorithmOutputID;
            return  new AnalyticalJobResponse("ERROR" , errMsg);            
        }
        
        if (filter == null || filter.isEmpty()) {
            filter ="<ogc:Filter><PropertyIsEqualTo> <PropertyName>gsmlp:nvclCollection</PropertyName> <Literal>true</Literal> </PropertyIsEqualTo></ogc:Filter>";
        }

        if (Utility.stringIsBlankorNull(units)) { 
            String errMsg = "jobid="+jobid +": units is not valid.";
            return  new AnalyticalJobResponse("ERROR" , errMsg);
        }


        AnalyticalJobVo jobVo = new AnalyticalJobVo();

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
        jobVo.setStatus("Processing");
        logger.debug("New ANALYTICAL job message created : " + jobVo.printVo());

        logger.debug("Adding ANALYTICAL job message to the queue.");
        nvclAnalyticalGateway.setDestination(nvclSubmitDestination);
        String messageID = nvclAnalyticalGateway.createNVCLAnalyticalReqMsg(jobVo);

        if (messageID == null) {
            logger.error("Failed to create ANALYTICAL job message");
            return  new AnalyticalJobResponse("ERROR" ,"Failed to create ANALYTICAL job message");
        }

        logger.debug("Successfully Added TSGMOD job message to the queue.");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            
        }        

        return  new AnalyticalJobResponse("SUCCESS" ,"jobid="+jobid +": Your job has been successfully submitted. Please check your jobs status later");
    } 
    

    /**
     * Browse queue message(s) from both submit and status queue and merge them
     * to a AnalyticalJobVo List.
     * 
     * @param  email   Requestor's email, use as key for retrieving
     *                 queue message(s)  
     * @return List     Returning a list that consists of two lists : 
     *                 a) a list of submit message(s)
     *                 b) a list of status message(s)            
     */
    @RequestMapping("/checkNVCLAnalyticalJobStatus.do")    
    public String  checkNVCLJobStatus(HttpServletRequest request, HttpServletResponse response,@RequestParam(value="email") String email ) 
            throws ServletException,IOException {     

 
        if (!Utility.ValidateEmail(email)) {
        	logger.debug("email is not valid.");
            return null;
        } else {
            email = email.toLowerCase();
        }
        
        logger.debug("getting messages from the submit and status queues for email : " + email);
        
        NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
        nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate);
        List<AnalyticalJobVo> jobSubmitList = (ArrayList<AnalyticalJobVo>) nvclAnalyticalQueueBrowser.browseQueueSubmit(email, nvclSubmitDestination);
        List<AnalyticalJobVo> jobStatusList = (ArrayList<AnalyticalJobVo>) nvclAnalyticalQueueBrowser.browseQueueStatus(email, nvclStatusDestination);
        if (jobStatusList == null) 
            jobStatusList = new ArrayList<AnalyticalJobVo>();
        if (jobSubmitList != null) { //Merge the submit and status queue together. 
            for (AnalyticalJobVo jobVo : jobSubmitList) {
                jobStatusList.add(0,jobVo);
            }
        }
        response.setContentType("application/json");
        return jsonObjectMapper.writeValueAsString(jobStatusList);

    }    

    @RequestMapping("/getNVCLAnalyticalJobResult.do")
    public String getNVCLAnalyticalJobResult( HttpServletRequest request, HttpServletResponse response,
    		@RequestParam(required = false,value="jobid") String jobID ) throws ServletException,
            IOException {

        if (Utility.stringIsBlankorNull(jobID)) {
            return "jobID is not valid.";
        }
    	
        NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
        nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate);
        List<AnalyticalJobResultVo> jobResultList = (ArrayList<AnalyticalJobResultVo>) nvclAnalyticalQueueBrowser.browseQueueResult(jobID, nvclResultDestination);
        AnalyticalJobResultVo jobResultVo = null;
        if (jobResultList == null) {
            logger.info("result queue is null");
        } else {
            for (Iterator<?> it1 = jobResultList.iterator(); it1.hasNext();) {
                jobResultVo = (AnalyticalJobResultVo) it1.next();
            }
        }      
        response.setContentType("application/json");
        return jsonObjectMapper.writeValueAsString(jobResultVo);
    }
    @RequestMapping("/getTsgAlgorithms.do")
    public String getTsgAlgorithms( HttpServletRequest request, HttpServletResponse response,
    		@RequestParam(required = false, value="tsgAlgName") String tsgAlgName,
    		@RequestParam(required = false, value="outputFormat") String outputFormat) throws ServletException,
            IOException {

        if (tsgAlgName == null) {
    		if (outputFormat!=null && outputFormat.equals("json"))
    		{
	    		response.setContentType("application/json");
	    		return jsonObjectMapper.writeValueAsString(tsgscripts.getScripts());
    		}
    		else return tsgscripts.getScripts().toString();
    	}
    	else if (tsgscripts.getScripts().containsKey(tsgAlgName)) 
    	{
    		if (outputFormat!=null && outputFormat.equals("json"))
    		{
	    		response.setContentType("application/json");
	    		return jsonObjectMapper.writeValueAsString(tsgscripts.getScripts().get(tsgAlgName));
    		}
    		else return tsgscripts.getScripts().get(tsgAlgName);
    	}
        else 
        {
            if (outputFormat!=null && outputFormat.equals("json"))	{
                return  jsonObjectMapper.writeValueAsString("Not defined");
            }
            else return "Not defined";
        }
    }    
    
    @RequestMapping("/listTsgAlgorithms.do")
    public String listTsgAlgorithms( HttpServletRequest request, HttpServletResponse response,
    		@RequestParam(required = false, value="outputFormat") String outputFormat) throws ServletException,
        IOException {

    	List<String> algnames = new ArrayList<String>();
    	tsgscripts.getScripts().forEach((k,v) -> algnames.add(k));
    	if (outputFormat!=null && outputFormat.equals("json"))
    	{
	    	response.setContentType("application/json");
	    	return jsonObjectMapper.writeValueAsString(algnames);
    	}
        else return algnames.toString();
    }     
        
    @RequestMapping("/submitNVCLTSGModJob.do")
    public AnalyticalJobResponse submitNVCLTSGModJob(
            @RequestParam(required = true, value = "serviceurls") String serviceUrls,
            @RequestParam(required = true, value = "email") String email, 
            @RequestParam(required = true, value = "jobname") String jobname,
            @RequestParam(required = false, value = "tsgAlgName") String tsgAlgName,
            @RequestParam(required = false, value = "tsgScript") String tsgScript,            
            @RequestParam(required = false, value = "filter") String filter, 
            @RequestParam(required = true, value = "startdepth") int startDepth,
            @RequestParam(required = true, value = "enddepth") int endDepth, 
            @RequestParam(required = true, value = "span") float span, 
            @RequestParam(required = true, value = "units") String units,
            @RequestParam(required = true, value = "value") float value, 
            @RequestParam(required = true, value = "logicalop") String logicalOp) throws ServletException,
            IOException, SQLException, ParserConfigurationException, TransformerException {

        String jobid = Utility.getMD5HashValue();
        
        if (jobname.startsWith("test-"))
            jobid = jobname;
        
        if (Utility.stringIsBlankorNull(serviceUrls)) {
            String errMsg = "serviceUrls are not valid.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        }
        
        if (!Utility.ValidateEmail(email)) {
            String errMsg ="email is not valid.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        } else {
            email = email.toLowerCase();
        }

        if (Utility.stringIsBlankorNull(filter)) {
            filter = "<ogc:Filter><PropertyIsEqualTo> <PropertyName>gsmlp:nvclCollection</PropertyName> <Literal>true</Literal> </PropertyIsEqualTo></ogc:Filter>";
        }
        
        float minDownSampleInterval = NVCLAnalyticalRequestSvc.config.getMinDownSampleInterval();     
        span = Math.max(span, minDownSampleInterval);

        if (Utility.stringIsBlankorNull(tsgScript)) {
            String errMsg = "script is not valid.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        }

        if (Utility.stringIsBlankorNull(units)) {
            String errMsg = "units is not valid.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        }
        // Encode using basic encoder
        String base64TsgScript = Base64.getEncoder().encodeToString(tsgScript.getBytes("utf-8"));
        AnalyticalJobVo jobVo = new AnalyticalJobVo();

        jobVo.setRequestType("TSGMOD");
        jobVo.setJobid(jobid);
        jobVo.setTsgScript(base64TsgScript);
        jobVo.setJobDescription(jobname);
        jobVo.setServiceUrls(serviceUrls);
        jobVo.setEmail(email);
        jobVo.setFilter(filter);
        jobVo.setStartDepth(startDepth);
        jobVo.setEndDepth(endDepth);
        jobVo.setSpan(span);
        jobVo.setUnits(units);
        jobVo.setValue(value);
        jobVo.setLogicalOp(logicalOp);
        jobVo.setStatus("Processing");
        logger.debug("New TSGMOD job message created : " + jobVo.printVo());

        logger.debug("Adding TSGMOD job message to the queue.");
        nvclAnalyticalGateway.setDestination(nvclSubmitDestination);
        String messageID = nvclAnalyticalGateway.createNVCLAnalyticalReqMsg(jobVo);

        if (messageID == null) {
            logger.error("Failed to create TSGMOD job message");
            return new AnalyticalJobResponse("ERROR", "jobid=" + jobid + ": Failed to create TSGMOD job message");
        }

        logger.debug("Successfully Added TSGMOD job message to the queue.");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {

        }

        return new AnalyticalJobResponse("SUCCESS", "jobid=" + jobid + ": Your job has been successfully submitted. Please check your jobs status later");
    }
    @RequestMapping("/getTsgJobsByBoreholeid.do")
    public String getTsgJobsByBoreholeid( HttpServletRequest request, HttpServletResponse response,
    		@RequestParam(required = true, value = "boreholeid") String boreholeid,
    		@RequestParam(required = false, value = "email") String email ) throws ServletException, IOException {
    	
    	
    	if (Utility.stringIsBlankorNull(boreholeid)) return "boreholeid is not valid.";
    	
        if (!Utility.stringIsBlankorNull(email))
        {
        	if(!Utility.ValidateEmail(email)) return "email is not valid.";
        	else email = email.toLowerCase();
        }
        
        NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
        nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate);
        
        List<TSGJobVo> tsgJobVoList = (ArrayList<TSGJobVo>) nvclAnalyticalQueueBrowser.browseTsgJob(boreholeid, email, nvclResultDestination);
   
        response.setContentType("application/json");
        return jsonObjectMapper.writeValueAsString(tsgJobVoList);
    }      
    @RequestMapping("/publishNvclJob.do")
    public String publishNvclJob( HttpServletRequest request, HttpServletResponse response,
    		@RequestParam(required = true, value = "jobid", defaultValue = "jobid") String jobid,
    		@RequestParam(required = true, value = "publish") Boolean bPublish) throws ServletException, IOException {
    	
    	
    	if (Utility.stringIsBlankorNull(jobid)) return "jobid is not valid.";
    	
    	SparkeyServiceSingleton.getInstance().put(jobid,Boolean.toString(bPublish));

        response.setContentType("application/json");
        return jsonObjectMapper.writeValueAsString(bPublish);
    }      
    @RequestMapping("/getNvclJobPublishStatus.do")
    public String publishNvclJob( HttpServletRequest request, HttpServletResponse response,
    		@RequestParam(required = true, value = "jobid", defaultValue = "jobid") String jobid) throws ServletException, IOException {
    	
    	
    	if (Utility.stringIsBlankorNull(jobid)) return "jobid is not valid.";
    	
    	String publishStatus = SparkeyServiceSingleton.getInstance().get(jobid);

        response.setContentType("application/json");
        return jsonObjectMapper.writeValueAsString(publishStatus);
    }
    //Download TsgModJob's scalar csv.
    @RequestMapping("/downloadTsgJobData.do")
    public void downloadTsgJobData(@RequestParam("jobid") String jobId, HttpServletResponse response) throws Exception {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition","inline; filename=nvclanalytics-" + jobId + ".zip;");
        ZipOutputStream zout = new ZipOutputStream(response.getOutputStream());
        try{
            String jobFolderPath = NVCLAnalyticalRequestSvc.config.getDataPath() + jobId;
            logger.info("jobFolderPath=" + jobFolderPath);
            File jobFolder = new File (jobFolderPath);
            if (jobFolder.isDirectory()) {
                ZipUtil.addFolderToZip(jobFolder, jobFolder.getName(), zout);
            }
            zout.finish();
            zout.flush();
        } finally {
            zout.close();
        }
    }    
}
