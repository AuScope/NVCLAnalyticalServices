package org.auscope.nvcl.server.vo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ConfigVo Manage the Config file and allow getting the values thru getter and
 * setter method.
 * 
 * @author Lingbo Jiang
 */

@Component
public class ConfigVo {
    //Spring will populate these fields from config.properties through Dependency Injection.
    @Value("${smtp.enabled}")
    private boolean sendEmails;

    @Value("${msgTimetoLiveDays}")
    private String msgTimetoLiveDays;
    
    @Value("${sysadmin.email}")	
    private String sysAdminEmail;

    @Value("${dataCachePath}")
    private String dataPath;

    @Value("${webapp.url}")
    private String webappURL;
    
    @Value("${portal.url}")
    private String portalURL;
    
    @Value("${tsg.downsample.minInterval}")
    private float minDownSampleInterval;
    
    @Value("${sparkey.dataPath}")
    private String sparkeyDataPath;
    
    public boolean getSendEmails() {
        return sendEmails;
    }
    public void setSendEmails(boolean sendEmails) {
        this.sendEmails = sendEmails;
    }
    public String getMsgTimetoLiveDays() {
        return msgTimetoLiveDays;
    }
    public void setMsgTimetoLiveDays(String msgTimetoLiveDays) {
        this.msgTimetoLiveDays = msgTimetoLiveDays;
    }
    public String getSysAdminEmail() {
        return sysAdminEmail;
    }
    public void setSysAdminEmail(String sysAdminEmail) {
        this.sysAdminEmail = sysAdminEmail;
    }
    public String getWebappURL() {
        return webappURL;
    }
    public void setWebappURL(String webappURL) {
        this.webappURL = webappURL;
    }
    public String getPortalURL() {        
        return portalURL;
    }
    public void setPortalURL(String portalURL) {
        this.portalURL = portalURL;
    }
    public float getMinDownSampleInterval() {
        return minDownSampleInterval;
    }
    public void setMinDownSampleInterval(float minDownSampleInterval) {
        this.minDownSampleInterval = minDownSampleInterval;
    }
    public String getDataPath() {
        return dataPath;
    }
    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }
	public String getSparkeyDataPath() {
		return sparkeyDataPath;
	}
	public void setSparkeyDataPath(String sparkeyDataPath) {
		this.sparkeyDataPath = sparkeyDataPath;
	}
}
