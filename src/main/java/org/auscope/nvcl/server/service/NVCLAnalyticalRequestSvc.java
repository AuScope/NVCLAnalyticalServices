package org.auscope.nvcl.server.service;


import java.util.ArrayList;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.auscope.nvcl.server.service.SpringFrameworkJmsSender.ReferenceHolderMessagePostProcessor;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobStatusVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.ConfigVo;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.MimeMessageHelper;
/**
 * NVCLAnalyticalRequestSvc trigger actual services that perform the AnalyticalJobProcessor.
 * At the end, it will create a new message in the JMS status and result queue with status 
 * and result information  in the message body.
 * 
 *  
 * @author Peter Warren
 * @author Lingbo Jiang
 * @author Florence Tan
 *
 */
public class NVCLAnalyticalRequestSvc {
	
	private static final Logger logger = LogManager.getLogger(NVCLAnalyticalRequestSvc.class);


	public void processRequest(AnalyticalJobVo messageVo) {
		logger.debug("in NVCLAnalyticalRequestSvc.processRequest...");
		String jobid = messageVo.getJobid();
		String jobDescription = messageVo.getJobDescription();
		AnalyticalJobStatusVo jobStatusVo = new AnalyticalJobStatusVo();
		jobStatusVo.setEmail(messageVo.getEmail());
		jobStatusVo.setJMSCorrelationID(messageVo.getEmail());
		jobStatusVo.setJobDescription(jobDescription);
		jobStatusVo.setJobid(jobid);
		
	    
//	    
//        NVCLAnalyticalJobProcessor processor = new NVCLAnalyticalJobProcessor();
//        processor.setAnalyticalJob(messageVo);
//        if (!processor.getBoreholeList()) {
//          jobStatusVo.setStatus("Failed");
//          jobStatusVo.setMessage("Failed:processor.processStage1");            
//          logger.debug("Failed:processor.processStage1");
//        }
//        if (!processor.getDataCollection()) {
//            jobStatusVo.setStatus("Failed");
//            jobStatusVo.setMessage("Failed:processor.processStage2");             
//            logger.debug("Failed:processor.processStage2");
//          }
//        if (!processor.getDownSampledData()) {
//            logger.debug("Failed:processor.processStage3");
//            jobStatusVo.setStatus("Failed");
//            jobStatusVo.setMessage("Failed:processor.processStage3");            
//        }
//        if (!processor.processStage4()) {
//            jobStatusVo.setStatus("Failed");
//            jobStatusVo.setMessage("Failed:processor.processStage4");            
//            logger.debug("Failed:processor.processStage4");
//          }     
        NVCLAnalyticalJobProcessorManager processorManager = new NVCLAnalyticalJobProcessorManager();
        if( processorManager.processRequest(messageVo)) {            
            jobStatusVo.setStatus("Success");
            jobStatusVo.setMessage("Success:job finished" );
            String jobResultUrl = config.getWebappURL() + "getNVCLAnalyticalJobResult.do?jobid=" + jobStatusVo.getJobid();
            jobStatusVo.setJoburl(jobResultUrl);
        } else {
            jobStatusVo.setStatus("Failed");
            jobStatusVo.setMessage("Failed:processor.processStage4");            
            logger.debug("Failed:processor.processStage4");     
        }

		logger.debug("create reply message....");

			
		AnalyticalJobResultVo jobResultVo = processorManager.getSumJobResultVo();
		//finally, create reply message
		// create another message in the nvcl.reply.queue with correlation id
		// same as the request message id
		try {			
	        ReferenceHolderMessagePostProcessor messagePostProcessor = new ReferenceHolderMessagePostProcessor();
	        int msgTTL = Integer.parseInt(this.config.getMsgTimetoLiveDays());//days.
	        this.jmsTemplate.setTimeToLive(((long)msgTTL)*86400000);
	        this.jmsTemplate.setExplicitQosEnabled(true);
	        this.jmsTemplate.convertAndSend(this.status, jobStatusVo, messagePostProcessor);
		    Message sentMessage = messagePostProcessor.getSentMessage();		    
		    logger.debug("Generated JMSMessageID" + sentMessage.getJMSMessageID());
		    logger.debug("Generated JMSCorrelationID" + sentMessage.getJMSCorrelationID());

	        this.jmsTemplate.convertAndSend(this.result, jobResultVo, messagePostProcessor);
		    
		} catch (JMSException jmse) {
			logger.error("JMSException : " + jmse);
		}
		
	  if (config.getSendEmails()==true)
	      sendResultEmail(jobResultVo,jobStatusVo.getJoburl());
	  else 
	      logger.debug("Notification emails disabled, skipping email step.");
	}
	
    private void sendResultRichEmail(AnalyticalJobResultVo messageVo, String jobResultUrl) {
        


//        MimeMessage mimeMessage = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "utf-8");
//        String htmlMsg = "<h3>Hello World!</h3>";
//        mimeMessage.setContent(htmlMsg, "text/html");
//        helper.setTo("someone@abc.com");
//        helper.setSubject("This is the test message for testing gmail smtp server using spring mail");
//        helper.setFrom("abc@gmail.com");
//        mailSender.send(mimeMessage);        

    }	
	/**
	  * sends an email to the requestor's email address indicating success and providing a download link
	  * or in the case of failure describing next steps to request support.
	 * @param jobResultUrl 
	  * 
	  * @param	messaveVo	message value object 
	  */
	private void sendResultEmail(AnalyticalJobResultVo messageVo, String jobResultUrl) {
	    
	    SimpleMailMessage msg = new SimpleMailMessage();
	    try {
	        msg.setTo(messageVo.getEmail());
	        
	        	String msgtext;
	        	msg.setSubject("NVCL Analytical job='" + messageVo.getJobDescription() + "' result is ready");
	        	msgtext="This is an automated email from the NVCL Analytical Service.\n\nThe analytical job you requested :" 
	        	+ messageVo.getJobDescription() 
	        	+ " is ready for collect.\n  "
	        	+ "The result link is :\n"
	        	+  jobResultUrl
	        	+"\nThis link will remain available for download for "
	        	+ this.config.getMsgTimetoLiveDays() +" days.\n\nTo view the content of these files you will need a json reader.";
	        	msgtext+="\n\n If you have any comments, suggestions or issues with the result please reply to this email.";
	        	msg.setText(msgtext);

	        logger.debug("Sending result email");
			
			msg.setFrom(config.getSysAdminEmail());
			
	        mailSender.send(msg);
	    }
	    catch(MailException ex)
	    {
	    	logger.debug("Send Email failed. Service not configured correctly or the email server is down");
	    }
	}

    private MailSender mailSender;
	public void setMailSender(MailSender mailSender) {
		this.mailSender = mailSender;
	}
	
	private ConfigVo config;
	public void setConfig(ConfigVo config) {
			this.config = config;
	}
	
	//Injects JmsTemplate
	private JmsTemplate jmsTemplate;
	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}
	
	//Injects Destination
	private Destination status;
	public void setStatus(Destination status) {
		this.status = status;
	}
    //Injects Result Destination
    private Destination result;
    public void setResult(Destination result) {
        this.result = result;
    }
}
