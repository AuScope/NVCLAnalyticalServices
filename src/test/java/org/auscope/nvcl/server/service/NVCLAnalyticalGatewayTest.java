package org.auscope.nvcl.server.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobStatusVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.ConfigVo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;

/*
 * Junit test for NVCLAnalyticalGateway.java
 * 
 * This test will create three new messages, one in nvcl.submit.queue , 
 * one in nvcl.status.queue and last in nvcl.result.queue
 * 
 * Author : Lingbo Jiang
 */

public class NVCLAnalyticalGatewayTest {

    private static final Logger logger = LogManager.getLogger(NVCLAnalyticalGatewayTest.class);
    private static ApplicationContext ctx;

    @BeforeClass
    public static void setup() throws Exception {
        /**
         * load applicationContext.xml
         */
        ctx = new ClassPathXmlApplicationContext("file:src/main/webapp/WEB-INF/applicationContext.xml");
    }

    @AfterClass
    public static void tearDown() {
        // do nothing
    }

    /*
     * test sending message to Message Container (tsgdownload.request.queue)
     */
    @Test
    public void testSendNVCLAnalyticalJob() throws Exception {
        //http://localhost:8080/NVCLAnalyticalServices/submitNVCLAnalyticalJob.do?serviceurls=http://nvclwebservices.vm.csiro.au/geoserverBH/wfs&email=lingbo.jiang@csiro.au&jobname=test001&algorithmoutputid=108&logname=Min1%20uTSAS&classification=Muscovite&startdepth=0&enddepth=999999&logicalop=gt&value=50&units=pct&span=1&filter=%3Cogc%3AFilter%3E%3CPropertyIsEqualTo%3E%3CPropertyName%3Egsmlp%3AnvclCollection%3C%2FPropertyName%3E%3CLiteral%3Etrue%3C%2FLiteral%3E%3C%2FPropertyIsEqualTo%3E%3C%2Fogc%3AFilter%3E
        logger.info("send message to nvcl.request.queue testing start....");
        // create new message
        ConfigVo configVo = (ConfigVo) ctx.getBean("createConfig");
        AnalyticalJobVo jobVo = new AnalyticalJobVo();
        jobVo.setRequestType("ANALYTICAL");
        jobVo.setJobid("test-" + Utility.getHashValue());
        jobVo.setJobDescription("Analytical test job 002");
        jobVo.setServiceUrls("http://nvclwebservices.vm.csiro.au/geoserverBH/wfs");
        jobVo.setEmail("lingbo.jiang@csiro.au");
        jobVo.setFilter("<ogc:Filter><PropertyIsEqualTo><PropertyName>gsmlp:nvclCollection</PropertyName><Literal>true</Literal></PropertyIsEqualTo></ogc:Filter>");
        jobVo.setStartDepth(0);
        jobVo.setEndDepth(9999);
        jobVo.setLogName("Min1 uTSAS");
        jobVo.setClassification("Muscovite");
        jobVo.setSpan((float) 1.0);
        jobVo.setUnits("pct");
        jobVo.setValue(50);
        jobVo.setLogicalOp("gt");

        JmsTemplate jmsTemplate = (JmsTemplate) ctx.getBean("jmsTemplate");
        Destination nvclSubmitDestination = (Destination) ctx.getBean("nvclSubmitDestination");
        NVCLAnalyticalGateway nvclAnalyticalGateway = (NVCLAnalyticalGateway) ctx.getBean("nvclAnalyticalGateway");
        nvclAnalyticalGateway.setJmsTemplate(jmsTemplate);
        nvclAnalyticalGateway.setDestination(nvclSubmitDestination);
        nvclAnalyticalGateway.createNVCLAnalyticalReqMsg(jobVo);
        // browse message(s) in the queue
        NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
        nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate);
        //List<AnalyticalJobVo> reqMsgList = (ArrayList<AnalyticalJobVo>) nvclAnalyticalQueueBrowser.browseQueueMessages(email, reqDestination);
        Destination nvclStatusDestination = (Destination) ctx.getBean("nvclStatusDestination");
        List<AnalyticalJobVo> jobStatusList = (ArrayList<AnalyticalJobVo>) nvclAnalyticalQueueBrowser.browseQueueStatus("lingbo.jiang@csiro.au", nvclStatusDestination);        

        if (jobStatusList == null) {
            logger.info("status queue is null");
        } else {
            for (AnalyticalJobVo jobStatusVo : jobStatusList) {
                logger.info("-------------------------------------------------");
                logger.info("timestamp : " + jobStatusVo.getJMSTimestamp());
                logger.info("job id : " + jobStatusVo.getJMSMsgID());
                logger.info("job status : " + jobStatusVo.getStatus());
                logger.info("job description : " + jobStatusVo.getJobDescription());
                logger.info("job url : " + jobStatusVo.getJoburl());
                logger.info("-------------------------------------------------");
            }
        }
    }
}
