package org.auscope.nvcl.server.service;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.BoreholeResultVo;
import org.auscope.nvcl.server.vo.TSGJobVo;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * A service that read messages in a JMS queue filter by JMSCorrelationID (same
 * as requestor's email or jobid). Store the message detail into JMSMessageVo
 * object and return a list of JMSMessageVo.
 *
 * @author Peter Warren
 * @author Lingbo Jiang
 * @author Florence Tan
 */

public class NVCLAnalyticalQueueBrowser {

    private static final Logger logger = LogManager.getLogger(NVCLAnalyticalQueueBrowser.class);
    
    public List<AnalyticalJobVo> browseQueueSubmit(final String email, final Destination destination) {
        List<AnalyticalJobVo> msgList = (ArrayList<AnalyticalJobVo>) this.jmsTemplate.execute(new SessionCallback<List<AnalyticalJobVo>>() {

            public List<AnalyticalJobVo> doInJms(Session session) throws JMSException {
                int count = 0;
                List<AnalyticalJobVo> msgList = new ArrayList<AnalyticalJobVo>();
                logger.debug("getting messages in the " + destination +" queue by email address");
                String msgSelector = "JMSCorrelationID  = '" + email + "'";
                QueueBrowser browser = session.createBrowser((Queue) destination, msgSelector);
                Enumeration<?> messages = browser.getEnumeration();
                while (messages.hasMoreElements()) {
                    AnalyticalJobVo jmsMsgVo = new AnalyticalJobVo();
                    count++;
                    Message message = (Message) messages.nextElement();
                    logger.debug("Message " + count + " : " + message);
                    if (message instanceof MapMessage) {
                        MapMessage mapMessage = (MapMessage) message;   
                        // convert long to date
                        long timestamp = mapMessage.getJMSTimestamp();
                        Date date = new Date(timestamp);
                        DateFormat df = DateFormat.getDateTimeInstance();
                        String newtimestamp = df.format(date);
                        jmsMsgVo.setJMSTimestamp(newtimestamp);
                        jmsMsgVo.setJMSMsgID(mapMessage.getJMSMessageID());
                        jmsMsgVo.setJMSCorrelationID(mapMessage.getJMSCorrelationID());
                        
                        jmsMsgVo.setRequestType(mapMessage.getString("requestType"));      
                        jmsMsgVo.setEmail(mapMessage.getString("email"));                        
                        jmsMsgVo.setStatus(mapMessage.getString("status"));
                        jmsMsgVo.setJobid(mapMessage.getString("jobid"));
                        jmsMsgVo.setJobDescription(mapMessage.getString("jobDescription"));
                        jmsMsgVo.setServiceUrls(mapMessage.getString("serviceUrls"));
                        jmsMsgVo.setFilter(mapMessage.getString("filter"));
                        jmsMsgVo.setStartDepth(mapMessage.getInt("startDepth"));
                        jmsMsgVo.setEndDepth(mapMessage.getInt("endDepth"));                        
                        jmsMsgVo.setLogName(mapMessage.getString("logName"));                        
                        jmsMsgVo.setClassification(mapMessage.getString("classification"));       
                        jmsMsgVo.setAlgorithmOutputID(mapMessage.getString("algorithmOutputID"));       
                        jmsMsgVo.setSpan(mapMessage.getFloat("span"));                             
                        jmsMsgVo.setUnits(mapMessage.getString("units"));   
                        jmsMsgVo.setValue(mapMessage.getFloat("value"));   
                        jmsMsgVo.setLogicalOp(mapMessage.getString("logicalOp"));     
                    }
                    msgList.add(0, jmsMsgVo);
                }
                if (count > 0) {
                    return msgList;
                } else {
                    return null;
                }
            }
        }, true);

        return msgList;
    }   

    public List<AnalyticalJobVo> browseQueueStatus(final String email, final Destination destination) {

        List<AnalyticalJobVo> msgList = (ArrayList<AnalyticalJobVo>) this.jmsTemplate.execute(new SessionCallback<List<AnalyticalJobVo>>() {

            public List<AnalyticalJobVo> doInJms(Session session) throws JMSException {
                int count = 0;
                List<AnalyticalJobVo> msgList = new ArrayList<AnalyticalJobVo>();
                logger.debug("getting messages in the " + destination + " queue by email address");

                String msgSelector = "JMSCorrelationID  = '" + email + "'";
                QueueBrowser browser = session.createBrowser((Queue) destination, msgSelector);
                Enumeration<?> messages = browser.getEnumeration();
                while (messages.hasMoreElements()) {
                    AnalyticalJobVo jmsMsgVo = new AnalyticalJobVo();
                    count++;
                    Message message = (Message) messages.nextElement();
                    logger.debug("Message " + count + " : " + message);
                    if (message instanceof MapMessage) {
                        MapMessage mapMessage = (MapMessage) message;
                        // convert long to date
                        long timestamp = mapMessage.getJMSTimestamp();
                        Date date = new Date(timestamp);
                        DateFormat df = DateFormat.getDateTimeInstance();
                        String newtimestamp = df.format(date);
                        jmsMsgVo.setJMSTimestamp(newtimestamp);
                        jmsMsgVo.setJMSMsgID(mapMessage.getJMSMessageID());
                        jmsMsgVo.setJMSCorrelationID(mapMessage.getJMSCorrelationID());
                        jmsMsgVo.setStatus(mapMessage.getString("status"));
                        jmsMsgVo.setJobid(mapMessage.getString("jobid"));
                        jmsMsgVo.setJobDescription(mapMessage.getString("jobDescription"));        
                        jmsMsgVo.setEmail(mapMessage.getString("email"));         
                        jmsMsgVo.setJoburl(mapMessage.getString("joburl"));         
                        
                        jmsMsgVo.setRequestType(mapMessage.getString("requestType"));    
                        jmsMsgVo.setServiceUrls(mapMessage.getString("serviceUrls"));
                        jmsMsgVo.setFilter(mapMessage.getString("filter"));
                        jmsMsgVo.setStartDepth(mapMessage.getInt("startDepth"));
                        jmsMsgVo.setEndDepth(mapMessage.getInt("endDepth"));                        
                        jmsMsgVo.setLogName(mapMessage.getString("logName"));                        
                        jmsMsgVo.setClassification(mapMessage.getString("classification"));       
                        jmsMsgVo.setAlgorithmOutputID(mapMessage.getString("algorithmOutputID"));     
                        jmsMsgVo.setSpan(mapMessage.getFloat("span"));                             
                        jmsMsgVo.setUnits(mapMessage.getString("units"));   
                        jmsMsgVo.setValue(mapMessage.getFloat("value"));   
                        jmsMsgVo.setLogicalOp(mapMessage.getString("logicalOp"));    
                        
                        jmsMsgVo.setJobStartTime(mapMessage.getString("jobStartTime"));
                        jmsMsgVo.setJobEndTime(mapMessage.getString("jobEndTime"));
                    }
                    msgList.add(0, jmsMsgVo);
                }
                if (count > 0) {
                    return msgList;
                } else {
                    return null;
                }
            }
        }, true);

        return msgList;
    }
    public List<AnalyticalJobResultVo> browseQueueResult(final String jobid, final Destination destination) {

        List<AnalyticalJobResultVo> msgList = (ArrayList<AnalyticalJobResultVo>) this.jmsTemplate.execute(new SessionCallback<List<AnalyticalJobResultVo>>() {

            public List<AnalyticalJobResultVo> doInJms(Session session) throws JMSException {
                int count = 0;
                List<AnalyticalJobResultVo> msgList = new ArrayList<AnalyticalJobResultVo>();
                logger.debug("getting messages in the " + destination + " queue by email address");
                String msgSelector = "JMSCorrelationID  = '" + jobid + "'";
                QueueBrowser browser = session.createBrowser((Queue) destination, msgSelector);
                Enumeration<?> messages = browser.getEnumeration();
                while (messages.hasMoreElements()) {
                    //AnalyticalJobResultVo jmsMsgVo = new AnalyticalJobResultVo();
                    count++;
                    Message message = (Message) messages.nextElement();
                    logger.debug("Message " + count + " : " + message);
                    if (message instanceof MapMessage) {
                        MapMessage mapMessage = (MapMessage) message;
                        String jobResult = mapMessage.getString("jobResult");
                        AnalyticalJobResultVo jmsMsgVo = new Gson().fromJson(jobResult, new TypeToken<AnalyticalJobResultVo>() {}.getType());         
                        msgList.add(0, jmsMsgVo);
                    }

                }
                if (count > 0) {
                    return msgList;
                } else {
                    return null;
                }
            }
        }, true);

        return msgList;
    }    

    // Injects JmsTemplate
    private JmsTemplate jmsTemplate;

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }
    public List<TSGJobVo> browseTsgJob(String boreholeid, String email, Destination destination) {
        List<TSGJobVo> msgList = (ArrayList<TSGJobVo>) this.jmsTemplate.execute(new SessionCallback<List<TSGJobVo>>() {

            public List<TSGJobVo> doInJms(Session session) throws JMSException {
                int count = 0;
                List<TSGJobVo> msgList = new ArrayList<TSGJobVo>();
                logger.debug("getting messages in the " + destination + " queue by email address");

                QueueBrowser browser = session.createBrowser((Queue) destination);
                Enumeration<?> messages = browser.getEnumeration();
                while (messages.hasMoreElements()) {
                    //AnalyticalJobResultVo jmsMsgVo = new AnalyticalJobResultVo();
                    count++;
                    Message message = (Message) messages.nextElement();
                    logger.debug("Message " + count + " : " + message);
                    if (message instanceof MapMessage) {
                        MapMessage mapMessage = (MapMessage) message;
                        String jobResult = mapMessage.getString("jobResult");
                        AnalyticalJobResultVo jmsMsgVo = new Gson().fromJson(jobResult, new TypeToken<AnalyticalJobResultVo>() {}.getType());
                        if (!Utility.stringIsBlankorNull(email) && !email.equals(jmsMsgVo.getEmail())) continue;
                        String jobid = jmsMsgVo.getJobid();
                        String jobname = jmsMsgVo.getJobDescription();
                    	String dataPath = NVCLAnalyticalRequestSvc.config.getDataPath();
                        for (BoreholeResultVo boreholeResultVo : jmsMsgVo.boreholes) {
                            String id = boreholeResultVo.getId();
                            if (id.toLowerCase().contains(boreholeid.toLowerCase()))
                            {
                                String csvFile = dataPath + jobid + "/" + boreholeid + "-scalar.csv";
                                if (new File(csvFile).exists()){
									TSGJobVo jobVo = new TSGJobVo(boreholeid, jobid, jobname);
									String bPublished = Boolean.toString(true);
									try {
										bPublished = SparkeyServiceSingleton.getInstance().get(jobid);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									jobVo.setPublished(bPublished);
									msgList.add(0, jobVo);
	                                logger.debug("boreholeid:" + boreholeid + ",jobid:" + jobid + ",jobname:" + jobname);
                                }
                            }
                        }
                        for (BoreholeResultVo boreholeResultVo : jmsMsgVo.failedBoreholes) {
                            String id = boreholeResultVo.getId();
                            if (id.toLowerCase().contains(boreholeid.toLowerCase()))
                            {
                                String csvFile = dataPath + jobid + "/" + boreholeid + "-scalar.csv";
                                if (new File(csvFile).exists()){
	                            	msgList.add(0, new TSGJobVo(boreholeid,jobid,jobname));
	                            	logger.debug("boreholeid:" + boreholeid + ",jobid:" + jobid + ",jobname:" + jobname);
                                }
                            }
                        }
                    }

                }
                if (count > 0) {
                    return msgList;
                } else {
                    return null;
                }
            }
        }, true);

        return msgList;
    }


}
