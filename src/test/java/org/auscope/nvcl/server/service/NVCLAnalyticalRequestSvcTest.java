package org.auscope.nvcl.server.service;

import java.util.Base64;

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
        String tsgScript = "name = Hematite-goethite_distr, 9\n" +
                    "p1 = profile, layer=ref, stat=depth, bkrem=div, fit=3, wcentre=913, wradius=137\n" +
                    "p2= profile, layer=ref, stat=mean, wcentre=1650, wradius=0\n"+
                    "p3= profile, layer=ref, stat=mean, wcentre=450, wradius=0\n"+
                    "p4= expr, param1=p3, param2=p2, arithop=div\n"+
                    "p5 = expr, param1=p4, const2=1, arithop=lle, nullhandling=out\n"+
                    "p6= expr, param1=p5, param2=p1, arithop=mult\n"+
                    "p7= expr, param1=p6, const2=0.025, arithop=lgt, nullhandling=out\n"+
                    "p8= pfit, layer=ref, wunits=nm, wmin=776, wmax=1050, bktype=hull, bksub=div, order=4, product=0, bktype=hull, bksub=div\n"+
                    "return=expr, param1=p8, param2=p7, arithop=mult ";
        // Encode using basic encoder
        String base64TsgScript = Base64.getEncoder().encodeToString(tsgScript.getBytes("utf-8"));
        
		AnalyticalJobVo jobVo = new AnalyticalJobVo();
        jobVo.setRequestType("ANALYTICAL");        
        jobVo.setTsgScript(base64TsgScript);
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
