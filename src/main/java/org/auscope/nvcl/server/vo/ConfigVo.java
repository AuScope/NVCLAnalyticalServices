package org.auscope.nvcl.server.vo;

/**
 * ConfigVo
 * Manage the Config file and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */


public class ConfigVo {
    //Spring will populate these fields from config.properties through Dependency Injection.
    private boolean sendEmails;
    private String msgTimetoLiveDays;
    private String sysAdminEmail;
    private String downloadURL;
    private String webappURL;
    private String portalURL;
    private float minDownSampleInterval;
    
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
    public String getDownloadURL() {
        return downloadURL;
    }
    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
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
}
