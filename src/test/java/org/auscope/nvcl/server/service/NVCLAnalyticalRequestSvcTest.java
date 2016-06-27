package org.auscope.nvcl.server.service;

import javax.jms.Destination;

import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.ConfigVo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mail.MailSender;


/*
 * Junit test for NVCLAnalyticalRequestSvc.java
 * This test will test the ProcessRequest() method in NVCLAnalyticalRequestSvc.java, it will simulate a request message and run the
 * ProcessRequest() method to carry out actual analytical query job, save the result into activeMQ, send the result to user by email if sendemails is true.
 * Author : Lingbo Jiang
 */

public class NVCLAnalyticalRequestSvcTest {

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
    	//do nothing
    }

	/*
	 * test process AnalyticalJob request
	 *
	 */
	@Test
	public void testProcessRequest() throws Exception {

    	System.out.println("start testing ProcessRequest() method....");
		JmsTemplate jmsTemplate = (JmsTemplate) ctx.getBean("jmsTemplate");
		Destination status = (Destination) ctx.getBean("nvclStatusDestination");
        Destination result = (Destination) ctx.getBean("nvclResultDestination");		
		ConfigVo configVo = (ConfigVo) ctx.getBean("createConfig");

		AnalyticalJobVo jobVo = new AnalyticalJobVo();
        jobVo.setRequestType("ANALYTICAL");        
        jobVo.setJobid("test-" + Utility.getHashValue());
        jobVo.setJobDescription("Analytical test job 003");  
        jobVo.setServiceUrls("http://nvclwebservices.vm.csiro.au/geoserverBH/wfs");
        jobVo.setEmail("lingbo.jiang@csiro.au");
        jobVo.setFilter("<ogc:Filter><PropertyIsEqualTo><PropertyName>gsmlp:nvclCollection</PropertyName><Literal>true</Literal></PropertyIsEqualTo></ogc:Filter>");
        jobVo.setStartDepth(0);        
        jobVo.setEndDepth(9999);             
        jobVo.setLogName("Min1 uTSAS");
        jobVo.setClassification("Muscovite");
        jobVo.setSpan((float) 5.0);
        jobVo.setUnits("pct");
        jobVo.setValue(5);
        jobVo.setLogicalOp("gt");
		NVCLAnalyticalRequestSvc nvclAnalyticalRequestSvc = new NVCLAnalyticalRequestSvc();

		nvclAnalyticalRequestSvc.setJmsTemplate(jmsTemplate);
		nvclAnalyticalRequestSvc.setStatus(status);
        nvclAnalyticalRequestSvc.setResult(result);		
		nvclAnalyticalRequestSvc.setMailSender((MailSender)ctx.getBean("mailSender"));
		nvclAnalyticalRequestSvc.setConfig(configVo);
		nvclAnalyticalRequestSvc.processRequest(jobVo);

		System.out.println("message created... testing end ....");
	}
}
