package org.auscope.nvcl.server.vo;

import java.util.Hashtable;

/**
 * AnalyticalJobStatusVo
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */

public class AnalyticalJobStatusVo {
    private String JMSTimestamp;
    private String JMSMsgID;
    private String JMSCorrelationID;
    
    private String jobid;
    private String jobDescription;
    private String email;
    private String status;
    private String joburl;
    private String message;
    
     
    public String getJMSTimestamp() {
        return JMSTimestamp;
    }
 
    public void setJMSTimestamp(String JMSTimestamp) {
        this.JMSTimestamp = JMSTimestamp;
    }
    
    public String getJMSMsgID() {
        return JMSMsgID;
    }
 
    public void setJMSMsgID(String JMSMsgID) {
        this.JMSMsgID = JMSMsgID;
    }
    
    public String getJMSCorrelationID() {
        return JMSCorrelationID;
    }
 
    public void setJMSCorrelationID(String JMSCorrelationID) {
        this.JMSCorrelationID = JMSCorrelationID;
    }
    
    public AnalyticalJobStatusVo()
    {
    
    }
    
    public AnalyticalJobStatusVo(String jobid,String status,String joburl,String jobDescription,String message) {
        setJobid(jobid);
        setStatus(status);
        setMessage(message);
        setJobDescription(jobDescription);
        setJoburl(joburl);
    }
    public String getJobid() {
        return jobid;
    }
    public void setJobid(String jobid) {
        this.jobid = jobid;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getJobDescription() {
        return jobDescription;
    }
    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }
    public String getJoburl() {
        return joburl;
    }
    public void setJoburl(String joburl) {
        this.joburl = joburl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
}