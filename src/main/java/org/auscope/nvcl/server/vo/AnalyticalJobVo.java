package org.auscope.nvcl.server.vo;

/**
 * NVCLAnalyticalVo
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */

//algorithmoutputid = 108
//classification="Muscovite"
//startdpeth=0
//enddepth=999999
//logicalop="gt"
//value=5
//units="pct"
//span=1
//serviceurls="http://nvclwebservices.vm.csiro.au/geoserverBH/wfs"
//filter="<?xml version="1.0" encoding="UTF-8"?>
//<wfs:GetFeature service="WFS" version="1.1.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" xmlns:er="urn:cgi:xmlns:GGIC:EarthResource:1.1" xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML:2.0" >
//<wfs:Query typeName="gsmlp:BoreholeView" srsName="EPSG:4326">
//<ogc:Filter>
//<ogc:And>
//<ogc:BBOX>
//<gml:Envelope srsName="EPSG:4326">
//<gml:lowerCorner>78.75 -45.644768 </gml:lowerCorner>
//<gml:upperCorner>352.08984 -2.4601812 </gml:upperCorner>
//</gml:Envelope>
//</ogc:BBOX>
//<PropertyIsEqualTo>
//<PropertyName>nvclCollection</PropertyName>
//<Literal>true</Literal>
//</PropertyIsEqualTo>
//</ogc:And>
//</ogc:Filter>
//</wfs:Query>
//</wfs:GetFeature>"

public class AnalyticalJobVo {
        private String jmstimestamp;
        private String jmsmsgID;
        private String jmscorrelationID;
    
        //Spring will populate these fields through Dependency Injection.
        private String jobid;
        private String jobDescription;
        private String email;        
        private String serviceUrls;
        private String filter;
        private int startDepth;
        private int endDepth;       
        private String classification;
        private String logName;
        private String algorithmOutputID;
        private float span;
        private String units;
        private float value;
        private String logicalOp;
        private String status;        
        private String requestType;      
        private String joburl;        
        private String message;    
        private String tsgScript; //For TsgModJob only
        private String jobStartTime;
        private String jobEndTime;

        public AnalyticalJobVo(AnalyticalJobVo messageVo) {
            this.jobid = messageVo.getJobid();
            this.jobDescription = messageVo.getJobDescription();
            this.email = messageVo.getEmail();
            this.serviceUrls = messageVo.getServiceUrls();
            this.filter = messageVo.getFilter();
            this.startDepth = messageVo.getStartDepth();
            this.endDepth = messageVo.getEndDepth();
            this.classification = messageVo.getClassification();
            this.logName = messageVo.getLogName();
            this.algorithmOutputID = messageVo.getAlgorithmOutputID();
            this.span = messageVo.getSpan();
            this.units = messageVo.getUnits();
            this.value = messageVo.getValue();
            this.logicalOp = messageVo.getLogicalOp();
            this.status = messageVo.getStatus();
            this.requestType = messageVo.getRequestType();       
            this.tsgScript = messageVo.getTsgScript();       
            this.joburl = messageVo.getJoburl();
            this.message = messageVo.getMessage();                   
        }

        public AnalyticalJobVo() {
        }

        public String printVo() {
            String toString = "AnalyticalJobVo:";
            toString += "jobid=" + jobid;
            toString += "jobDescription=" + jobDescription;
            toString += "email="+ email;            
            toString += "serviceUrls=" + serviceUrls;
            toString += "filter=" + filter;
            toString += "startDepth="+ startDepth;
            toString += "endDepth=" + endDepth;
            toString += "classification=" + classification;
            toString += "logName=" + logName;            
            toString += "algorithmOutputID="+ algorithmOutputID;
            toString += "span=" + span;
            toString += "units=" + units;
            toString += "value=" + value;
            toString += "logicalOp=" + logicalOp;
            toString += "joburl=" + joburl;
            toString += "message=" + message;
            toString += "tsgScript=" + tsgScript;     
            return toString;
        }

        public String getServiceUrls() {
            return serviceUrls;
        }

        public void setServiceUrls(String serviceurls) {
            this.serviceUrls = serviceurls;
        }



        public String getFilter() {
            return filter;
        }



        public void setFilter(String filter) {
            this.filter = filter;
        }



        public int getStartDepth() {
            return startDepth;
        }



        public void setStartDepth(int startDepth) {
            this.startDepth = startDepth;
        }



        public int getEndDepth() {
            return endDepth;
        }



        public void setEndDepth(int endDepth) {
            this.endDepth = endDepth;
        }



        public String getClassification() {
            return classification;
        }



        public void setClassification(String classification) {
            this.classification = classification;
        }



        public String getAlgorithmOutputID() {
            return algorithmOutputID;
        }



        public void setAlgorithmOutputID(String algorithmOutputID2) {
            this.algorithmOutputID = algorithmOutputID2;
        }



        public float getSpan() {
            return span;
        }



        public void setSpan(float span) {
            this.span = span;
        }



        public String getUnits() {
            return units;
        }



        public void setUnits(String units) {
            this.units = units;
        }



        public float getValue() {
            return value;
        }



        public void setValue(float value) {
            this.value = value;
        }



        public String getLogicalOp() {
            return logicalOp;
        }



        public void setLogicalOp(String logicalOp) {
            this.logicalOp = logicalOp;
        }

        public String getJobid() {
            return jobid;
        }

        public void setJobid(String jobid) {
            this.jobid = jobid;
        }

        public String getJobDescription() {
            return jobDescription;
        }

        public void setJobDescription(String jobDescription) {
            this.jobDescription = jobDescription;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }

        public String getRequestType() {
            return requestType;
        }

        public void setRequestType(String requestType) {
            this.requestType = requestType;
        }

        public String getLogName() {
            return logName;
        }

        public void setLogName(String logName) {
            this.logName = logName;
        }

        public String getJoburl() {
            return joburl;
        }

        public void setJoburl(String joburl) {
            this.joburl = joburl;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getJMSTimestamp() {
            return jmstimestamp;
        }

        public void setJMSTimestamp(String jMSTimestamp) {
            jmstimestamp = jMSTimestamp;
        }

        public String getJMSMsgID() {
            return jmsmsgID;
        }

        public void setJMSMsgID(String jMSMsgID) {
            jmsmsgID = jMSMsgID;
        }

        public String getJMSCorrelationID() {
            return jmscorrelationID;
        }

        public void setJMSCorrelationID(String jMSCorrelationID) {
            jmscorrelationID = jMSCorrelationID;
        }

        public String getTsgScript() {
            return tsgScript;
        }

        public void setTsgScript(String tsgScript) {
            this.tsgScript = tsgScript;
        }

        public String getJobStartTime() {
            return jobStartTime;
        }

        public void setJobStartTime(String jobStartTime) {
            this.jobStartTime = jobStartTime;
        }

        public String getJobEndTime() {
            return jobEndTime;
        }

        public void setJobEndTime(String jobEndTime) {
            this.jobEndTime = jobEndTime;
        }

}
