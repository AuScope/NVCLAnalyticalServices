package org.auscope.nvcl.server.service;


import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.auscope.nvcl.server.service.SpringFrameworkJmsSender.ReferenceHolderMessagePostProcessor;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.ConfigVo;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
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
   
        NVCLAnalyticalJobProcessorManager processorManager = new NVCLAnalyticalJobProcessorManager();
        messageVo.setJobStartTime(Utility.getCurrentTime());
        if( processorManager.processRequest(messageVo)) {            
            messageVo.setStatus("Success");
            messageVo.setMessage("Success:job finished" );
            String jobResultUrl = config.getWebappURL() + "getNVCLAnalyticalJobResult.do?jobid=" + messageVo.getJobid();
            messageVo.setJoburl(jobResultUrl);
        } else {
            messageVo.setStatus("Failed");
            messageVo.setMessage("Failed:processor.processStage4");      
            messageVo.setJoburl("Failed");
            logger.debug("Failed:processor.processStage4");     
        }
        messageVo.setJobEndTime(Utility.getCurrentTime());
		logger.debug("create reply message....");

			
		AnalyticalJobResultVo jobResultVo = processorManager.getSumJobResultVo();
		//finally, create reply message
		// create another message in the nvcl.status.queue with correlation id
		// same as the request message id
		try {			
	        ReferenceHolderMessagePostProcessor messagePostProcessor = new ReferenceHolderMessagePostProcessor();
	        int msgTTL = Integer.parseInt(NVCLAnalyticalRequestSvc.config.getMsgTimetoLiveDays());//days.
	        this.jmsTemplate.setTimeToLive(((long)msgTTL)*86400000);
	        this.jmsTemplate.setExplicitQosEnabled(true);
	        this.jmsTemplate.convertAndSend(this.status, messageVo, messagePostProcessor);
		    Message sentMessage = messagePostProcessor.getSentMessage();		    
		    logger.debug("Generated JMSMessageID" + sentMessage.getJMSMessageID());
		    logger.debug("Generated JMSCorrelationID" + sentMessage.getJMSCorrelationID());

	        this.jmsTemplate.convertAndSend(this.result, jobResultVo, messagePostProcessor);
		    
		} catch (JMSException jmse) {
			logger.error("JMSException : " + jmse);
		}
		
	  if (config.getSendEmails()==true)
	      sendResultEmail(jobResultVo,messageVo.getJoburl(),messageVo.getJobStartTime(),messageVo.getJobEndTime());
	  else 
	      logger.debug("Notification emails disabled, skipping email step.");
	}
		
	/**
	  * sends an email to the requestor's email address indicating success and providing a download link
	  * or in the case of failure describing next steps to request support.
	 * @param jobResultUrl 
	  * 
	  * @param	messaveVo	message value object 
	  */
	private void sendResultEmail(AnalyticalJobResultVo messageVo, String jobResultUrl, String jobStartTime,String jobEndTime) {
	    
	    SimpleMailMessage msg = new SimpleMailMessage();
	    //http://auscope-portal-dev.arrc.csiro.au/gmap.html?nvclanid=8f664b74ed93bd5892307a0b4fb20dee
	    String jobResultVisualUrl = config.getPortalURL() + "?nvclanid=" + messageVo.getJobid();
		String jobResultDownloadUrl = config.getWebappURL() + "downloadNVCLJobResult.do?jobid=" + messageVo.getJobid();
		String jobResultTsgDownloadUrl = config.getWebappURL() + "downloadTsgJobData.do?jobid=" + messageVo.getJobid();

	    try {
	        msg.setTo(messageVo.getEmail());
	        
	        	String msgtext;
	        	msg.setSubject("NVCL Analytical job='" + messageVo.getJobDescription() + "' result is ready");
	        	msgtext="This is an automated email from the NVCL Analytical Service.\n\nThe analytical job you requested :" 
	        	+ messageVo.getJobDescription() 
	        	+ " is ready for collect.\n  "
	        	+ "The result link is :\n"
	        	+  jobResultUrl + "\n"
	        	+ "The job started at " + jobStartTime + " and ended at " + jobEndTime + "\n"	        
				+ "The NVCL-Job download link is :\n"
	        	+  jobResultDownloadUrl + "\n"
				+ "The TSG-Job download link is :\n"
	        	+  jobResultTsgDownloadUrl + "\n"
	        	+ "The visualization result link is :\n"
	        	+ jobResultVisualUrl + "\n"
	        	+"This link will remain available for download for "
	        	+ NVCLAnalyticalRequestSvc.config.getMsgTimetoLiveDays() +" days.\n\nTo view the content of these files you will need a json reader.";
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
	//Injects dataaccess from Config.properties
	
	public static DataAccess dataAccess;
	public void setDataAccess(DataAccess dataAccess) {
			NVCLAnalyticalRequestSvc.dataAccess = dataAccess;
	}
	
	//Injects config from Config.properties
	
	public static ConfigVo config;
	public void setConfig(ConfigVo config) {
			NVCLAnalyticalRequestSvc.config = config;
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
