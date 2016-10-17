package org.auscope.nvcl.server.service;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;

import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobStatusVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;

/*
 * This class perform message conversion through Spring's MessageConverter interface.
 * The MessageConverter interface has only two methods that must be implemented:
 * <ol>
 * <li>toMessage() : for sending message, converts an object to a Message</li>
 * <li>fromMessage() : for receiving message, converts an incoming Message into an Object</li>
 * </ol>
 * This class has been wired into JmsTemplate's messageConverter property through 
 * applicationContext.xml 
 * 
 * @author Peter Warren
 * @author Lingbo Jiang
 * @author Florence Tan
 */

public class NVCLAnalyticalMessageConverter implements MessageConverter {

	private static final Logger logger = LogManager.getLogger(NVCLAnalyticalMessageConverter.class);

	public NVCLAnalyticalMessageConverter() {}

	
	/*
	 * Convert Message to Object.   
	 * In this application - will be triggered before a new message in nvcl.request.queue
	 * being processed by the NVCLAnalyticalRequestSvc.processRequest(MessageVo messageVo) method.
	 * It will read the message from the message body and store them into messageVo object,
	 * pass it to processRequest(MessageVo messageVo) for processing.
	 */
	public Object fromMessage(Message message) throws JMSException,
			MessageConversionException {
		
		logger.debug("converting message (" + message.getJMSMessageID() + ") to object... !");
		logger.debug("JMSCorrelationID : " + message.getJMSCorrelationID());
		
		if (!(message instanceof MapMessage)) {
			throw new MessageConversionException("Message isn't a MapMessage");
		}
		MapMessage mapMessage = (MapMessage) message;
		AnalyticalJobVo messageVo = new AnalyticalJobVo();
		if ( mapMessage.getString("requestType").equals("ANALYTICAL") ||
		     mapMessage.getString("requestType").equals("TSGMOD")  ) {
		    messageVo.setRequestType(mapMessage.getString("requestType"));
            messageVo.setTsgScript(mapMessage.getString("tsgScript"));		    
            messageVo.setJobid(mapMessage.getString("jobid"));
            messageVo.setJobDescription(mapMessage.getString("jobDescription"));
            messageVo.setEmail(mapMessage.getString("email"));
            messageVo.setServiceUrls(mapMessage.getString("serviceUrls"));
            messageVo.setFilter(mapMessage.getString("filter"));
            messageVo.setStartDepth(mapMessage.getInt("startDepth"));
            messageVo.setEndDepth(mapMessage.getInt("endDepth"));
            messageVo.setLogName(mapMessage.getString("logName"));            
            messageVo.setClassification(mapMessage.getString("classification"));
            messageVo.setAlgorithmOutputID(mapMessage.getString("algorithmOutputID"));
            messageVo.setSpan(mapMessage.getFloat("span"));
            messageVo.setUnits(mapMessage.getString("units"));
            messageVo.setValue(mapMessage.getFloat("value"));
            messageVo.setLogicalOp(mapMessage.getString("logicalOp"));
            messageVo.setStatus("PROCESSING");   
		}
		return messageVo;
	}

	/*
	 * Converts Object (ConfigVo or MessageVo) to Message
	 * In this application, it will be triggered before creating new message in the
	 * specified request queue (convert AnalyticalJobVo object to message) and
	 * specified reply queue (convert AnalyticalJobStatusVo to message) and 
	 * specified result queue (convert AnalyticalJobResultVo object to message). 
	 */
	public Message toMessage(Object object, Session session)
			throws JMSException, MessageConversionException {
		
		logger.debug("Converting object (" + object + ") to message ... !");
		
		if (!(object instanceof AnalyticalJobVo) && 
		    !(object instanceof AnalyticalJobStatusVo) &&
		    !(object instanceof AnalyticalJobResultVo)) {
			throw new MessageConversionException("Object is neither a AnalyticalJobVo or AnalyticalJobStatusVo");
		}
		
		
		if (object instanceof AnalyticalJobVo) {
			//creating job message
		  //  AnalyticalJobVo configVo = (AnalyticalJobVo) object;
			MapMessage message = session.createMapMessage();
			
            AnalyticalJobVo messageVo = (AnalyticalJobVo) object;
            message.setString("requestType", messageVo.getRequestType());
            message.setJMSCorrelationID(messageVo.getEmail());      
            message.setString("email",messageVo.getEmail());
            message.setString("status", messageVo.getStatus());
            message.setString("jobDescription", messageVo.getJobDescription());
            message.setString("jobid", messageVo.getJobid());
            message.setString("serviceUrls", messageVo.getServiceUrls());
            message.setString("filter", messageVo.getFilter());
            message.setInt("startDepth",  messageVo.getStartDepth());
            message.setInt("endDepth",  messageVo.getEndDepth());
            message.setString("logName", messageVo.getLogName());            
            message.setString("classification", messageVo.getClassification());
            message.setString("algorithmOutputID", messageVo.getAlgorithmOutputID());            
            message.setFloat("span", messageVo.getSpan());
            message.setString("units", messageVo.getUnits());
            message.setFloat("value",  messageVo.getValue());
            message.setString("logicalOp",  messageVo.getLogicalOp());          
            message.setString("joburl", messageVo.getJoburl());
            message.setString("message", messageVo.getMessage());       
            message.setString("tsgScript", messageVo.getTsgScript());              
            return message;
		} else if (object instanceof AnalyticalJobStatusVo){
			//creating status message
		    AnalyticalJobStatusVo messageVo = (AnalyticalJobStatusVo) object;
			MapMessage message = session.createMapMessage();	
            message.setJMSCorrelationID(messageVo.getEmail());		    
			message.setString("jobid",messageVo.getJobid());
			message.setString("jobDescription", messageVo.getJobDescription());
            message.setString("email", messageVo.getEmail());			
			message.setString("status", messageVo.getStatus());
			message.setString("joburl", messageVo.getJoburl());
			message.setString("message", messageVo.getMessage());
			return message;
		} else if (object instanceof AnalyticalJobResultVo){
            //creating result message
		    AnalyticalJobResultVo messageVo = (AnalyticalJobResultVo) object;
            MapMessage message = session.createMapMessage();    
            message.setJMSCorrelationID(messageVo.getJobid());          
            message.setString("jobResult",new Gson().toJson(messageVo));
            return message;
        }
		return null;
	}
	
}
