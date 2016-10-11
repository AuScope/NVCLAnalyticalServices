package org.auscope.nvcl.server.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * BoreholeVo
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */
public class BoreholeVo {
    private String serviceUrl;
    private String serviceHost;
    private String servicePathOfData;
    private String holeIdentifier;
    private String holeUrl;
    private int status = 0; //0-wait4process,1-error,2-hit,3-fail
    public List<String> logidList = new ArrayList<String> ();
    public List<SpectralLogVo> spectralLogList = new ArrayList<SpectralLogVo>();
  
    public BoreholeVo(String holeIdentifier, String holeUrl, String serviceUrl, String serviceHost, String servicePathOfData) {
        this.holeIdentifier = holeIdentifier;
        this.holeUrl = holeUrl;        
        this.status = 0;
        this.serviceUrl = serviceUrl;
        this.serviceHost = serviceHost;
        this.servicePathOfData = servicePathOfData;
    }
    public String getHoleIdentifier() {
        return holeIdentifier;
    }
    public void setHoleIdentifier(String holeIdentifier) {
        this.holeIdentifier = holeIdentifier;
    }
    public String getHoleUrl() {
        return holeUrl;
    }
    public void setHoleUrl(String holeUrl) {
        this.holeUrl = holeUrl;
    }
    public void addLogid(String logid) {
        logidList.add(logid);
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public String getServiceUrl() {
        return serviceUrl;
    }
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
    public String getServiceHost() {
        return serviceHost;
    }
    public void setServiceHost(String serviceHost) {
        this.serviceHost = serviceHost;
    }
    public String getServicePathOfData() {
        return servicePathOfData;
    }
    public void setServicePathOfData(String servicePathOfData) {
        this.servicePathOfData = servicePathOfData;
    }
}
