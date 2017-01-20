package org.auscope.nvcl.server.service;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConversionException;
import org.auscope.nvcl.server.service.SpringFrameworkJmsSender.ReferenceHolderMessagePostProcessor;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/*
 * NVCLAnalyticalGateway will create a JMS queue in the specified destination in 
 * ActiveMQ using JmsTemplate.
 *  
 * @author Peter Warren
 * @author Linbo Jiang
 * @author Florence Tan
 */

public class NVCLAnalyticalGateway {
	
	private static final Logger logger = LogManager.getLogger(NVCLAnalyticalGateway.class);
   
	
	/** 
	 * The createNVCLAnalyticalReqMsg method create new message in the specified destination by 
	 * call up JmsTemplate's convertAndSend() method which will automatically calls 
	 * the NVCLAnalyticalMessageConverter.toMessage() method that convert the java object 
	 * into message before sending the message to the destination.
	 * 
	 * Note : NVCLAnalyticalMessageConverter has been auto wired into JmsTemplate in 
	 *        applicationContext.xml
	 * 
	 * @param AnalyticalJobVo	AnalyticalJobVo object that consists of the configuration information
	 * 					needed for trigger the AnalyticalJob
	 */ 
	public String createNVCLAnalyticalReqMsg(final AnalyticalJobVo jobVo)   {
		
		
		try {
	        ReferenceHolderMessagePostProcessor messagePostProcessor = new ReferenceHolderMessagePostProcessor();
	        this.jmsTemplate.convertAndSend(this.destination, jobVo, messagePostProcessor);        	        
	        Message sentMessage = messagePostProcessor.getSentMessage();
	        logger.debug("Generated JMSMessageID : " + sentMessage.getJMSMessageID());
	        return sentMessage.getJMSMessageID();
		} catch (JMSException jmse) {
			logger.error("JMSException : " + jmse);
		} catch (MessageConversionException  mce) {
			logger.error("MessageConversionException : " + mce);
		}
		return null;		
	}
	

    
	//Injects JmsTemplate
	private JmsTemplate jmsTemplate;
	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}
	
	//Injects Destination
	private Destination destination;
	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	
}
