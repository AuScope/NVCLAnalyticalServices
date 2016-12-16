package org.auscope.nvcl.server.web.controllers;

import java.awt.Menu;

import org.auscope.nvcl.server.util.TsgMod;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import javax.jms.Destination;
import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.service.NVCLAnalyticalGateway;
import org.auscope.nvcl.server.service.NVCLAnalyticalQueueBrowser;
import org.auscope.nvcl.server.service.NVCLAnalyticalRequestSvc;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
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
        if (!Utility.ValidateEmail(email)) {
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
    public List < AnalyticalJobVo >  checkNVCLJobStatus(@RequestParam(value="email") String email ) 
            throws ServletException,IOException {     
        logger.debug("in browseMessage...");
        logger.debug("email : " + email);
        logger.debug("jmsTemplate" + jmsTemplate);
        logger.debug("nvclSubmitDestination : " + nvclSubmitDestination);
        logger.debug("nvclStatusDestination : " + nvclStatusDestination);     
    
        email = email.toLowerCase();
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
        return jobStatusList;
    }    

    @RequestMapping("/getNVCLAnalyticalJobResult.do")
    public String getNVCLAnalyticalJobResult( @RequestParam(value="jobid", defaultValue="028c68636c05586c2985476dd7d7b069") String jobID ) throws ServletException,
            IOException {

        NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
        nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate);
        List<AnalyticalJobResultVo> jobResultList = (ArrayList<AnalyticalJobResultVo>) nvclAnalyticalQueueBrowser.browseQueueResult(jobID, nvclResultDestination);
        AnalyticalJobResultVo jobResultVo = null;
        if (jobResultList == null) {
            logger.info("result queue is null");
        } else {
            System.out.println("result queue is ****************");
            for (Iterator<?> it1 = jobResultList.iterator(); it1.hasNext();) {
                jobResultVo = (AnalyticalJobResultVo) it1.next();
            }
        }      
        Gson gson = new Gson();
        return gson.toJson(jobResultVo);
    }

    @RequestMapping("/submitNVCLTSGModJob.do")
    public AnalyticalJobResponse submitNVCLTSGModJob(
            @RequestParam(required = true, value = "serviceurls") String serviceUrls,
            @RequestParam(required = true, value = "email") String email, 
            @RequestParam(required = true, value = "jobname") String jobname,
            @RequestParam(required = false, value = "tsgscript") String tsgScript,            
            @RequestParam(required = false, value = "filter") String filter, 
            @RequestParam(required = true, value = "startdepth") int startDepth,
            @RequestParam(required = true, value = "enddepth") int endDepth, 
            @RequestParam(required = false, value = "logname") String logName,
            @RequestParam(required = true, value = "classification") String classification, 
            @RequestParam(required = false, value = "algorithmoutputid") String algorithmOutputID,
            @RequestParam(required = true, value = "span") float span, 
            @RequestParam(required = true, value = "units") String units,
            @RequestParam(required = true, value = "value") float value, 
            @RequestParam(required = true, value = "logicalop") String logicalOp) throws ServletException,
            IOException, SQLException, ParserConfigurationException, TransformerException {
        // mandatory field : holeIdentifier - validate if holeIdentifier is null

        String jobid = Utility.getMD5HashValue();
        if (jobname.startsWith("test-"))
            jobid = jobname;
        if (Utility.stringIsBlankorNull(serviceUrls)) {
            String errMsg = "jobid=" + jobid + ": holeidentifier is not valid.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        }
        if (!Utility.ValidateEmail(email)) {
            String errMsg = "jobid=" + jobid + ": email is not valid.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        } else {
            email = email.toLowerCase();
        }

        if ((algorithmOutputID == null && logName == null) || (algorithmOutputID != null && logName != null)) {
            String errMsg = "jobid=" + jobid + ": you has to provide either logName or algorithmOutputID.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        }

        if (algorithmOutputID != null && !Utility.checkAlgoutiIDs(algorithmOutputID)) {
            String errMsg = "your algorithmOutputID=" + algorithmOutputID + " is in wrong format.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        }

        if (filter == null || filter.isEmpty()) {
            filter = "<ogc:Filter><PropertyIsEqualTo> <PropertyName>gsmlp:nvclCollection</PropertyName> <Literal>true</Literal> </PropertyIsEqualTo></ogc:Filter>";
        }
        
        float minDownSampleInterval = NVCLAnalyticalRequestSvc.config.getMinDownSampleInterval();
        if (span < minDownSampleInterval ) {
            span = minDownSampleInterval;
        }
        
        
        if (tsgScript == null || tsgScript.isEmpty()) {
           
/*            tsgScript = "name = Hematite-goethite_distr, 9\n" +
                    "p1 = profile, layer=ref, stat=depth, bkrem=div, fit=3, wcentre=913, wradius=137\n" +
                    "p2= profile, layer=ref, stat=mean, wcentre=1650, wradius=0\n"+
                    "p3= profile, layer=ref, stat=mean, wcentre=450, wradius=0\n"+
                    "p4= expr, param1=p3, param2=p2, arithop=div\n"+
                    "p5 = expr, param1=p4, const2=1, arithop=lle, nullhandling=out\n"+
                    "p6= expr, param1=p5, param2=p1, arithop=mult\n"+
                    "p7= expr, param1=p6, const2=0.025, arithop=lgt, nullhandling=out\n"+
                    "p8= pfit, layer=ref, wunits=nm, wmin=776, wmax=1050, bktype=hull, bksub=div, order=4, product=0, bktype=hull, bksub=div\n"+
                    "return=expr, param1=p8, param2=p7, arithop=mult ";*/
            
            tsgScript = "name = Kaolinite Crystallinity,8\n" +
"description = Based on Pontual, Merry & Gamson, (1997), \"Regolith Logging\" in G-MEX Vol. 8, page 8-29, by Ausspec International Pty Ltd.   A combination of the 2180nm and 2160nm kaolinite slope indices that correlates with kaolinite crystallinity.  Index increases in v\n" +
"P1 = profile, stat=MEAN, wcentre=2184.00, wradius=1.00, layer=HQUOT, smooth=NONE, fit=NONE, bkrem=NONE\n" +
"P2 = profile, stat=MEAN, wcentre=2190.00, wradius=1.00, layer=HQUOT, smooth=NONE, fit=NONE, bkrem=NONE\n" +
"P3 = expr, param1=P1, param2=P2, arithop=DIV, mod1=PLAIN, mod2=PLAIN, mainmod=PLAIN, nullhandling=NONE\n" +
"P4 = profile, stat=MEAN, wcentre=2160.00, wradius=1.00, layer=HQUOT, smooth=NONE, fit=NONE, bkrem=NONE\n" +
"P5 = profile, stat=MEAN, wcentre=2177.00, wradius=1.00, layer=HQUOT, smooth=NONE, fit=NONE, bkrem=NONE\n" +
"P6 = expr, param1=P4, param2=P5, arithop=DIV, mod1=PLAIN, mod2=PLAIN, mainmod=PLAIN, nullhandling=NONE\n" +
"P7 = expr, param1=P6, param2=P3, arithop=SUB, mod1=PLAIN, mod2=PLAIN, mainmod=PLAIN, nullhandling=NONE\n" +
"return = expr, param1=P3, param2=P7, arithop=SUB, mod1=PLAIN, mod2=PLAIN, mainmod=PLAIN, nullhandling=NONE";

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
        jobVo.setLogName(logName);
        jobVo.setClassification(classification);
        jobVo.setAlgorithmOutputID(algorithmOutputID);
        jobVo.setSpan(span);
        jobVo.setUnits(units);
        jobVo.setValue(value);
        jobVo.setLogicalOp(logicalOp);
        jobVo.setStatus("Processing");
        logger.debug("Get a job request ..." + jobVo.printVo());
        System.out.println(jobVo.printVo());

        // url parameters validation start

        // mandatory field : validate if email null or empty or missing

        if (Utility.stringIsBlankorNull(units)) {
            String errMsg = "jobid=" + jobid + ": units is not valid.";
            return new AnalyticalJobResponse("ERROR", errMsg);
        }

        logger.debug("Start create JMS message ...");
        nvclAnalyticalGateway.setDestination(nvclSubmitDestination);
        String messageID = nvclAnalyticalGateway.createNVCLAnalyticalReqMsg(jobVo);

        if (messageID == null) {
            logger.error("Failed creating JMS message in queue ...");
            return new AnalyticalJobResponse("ERROR", "jobid=" + jobid + ": Failed creating JMS message in queue ...");
        }

        logger.debug("JMS message created successfully ... ");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {

        }

        return new AnalyticalJobResponse("SUCCESS", "jobid=" + jobid + ": Your job has been successfully submitted. Please check your jobs status later");
    }

    @RequestMapping("/doTSGMod.do")
    public String doTSGMod(@RequestParam(value = "jobid", defaultValue = "028c68636c05586c2985476dd7d7b069") String jobID) throws ServletException, IOException {

        System.out.println("start");
        TsgMod tsgMod = new TsgMod();
        System.out.println("1");
        // tsgMod.parseOneScalarMethod();
        System.out.println("end");
        Gson gson = new Gson();
        return gson.toJson(tsgMod);
    }  
}
