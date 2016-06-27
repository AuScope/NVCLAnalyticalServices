package org.auscope.nvcl.server.service;


import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.auscope.portal.core.server.http.HttpServiceCaller;
import org.auscope.portal.core.services.methodmakers.WFSGetFeatureMethodMaker;
import org.auscope.portal.core.services.responses.ows.OWSExceptionParser;
import org.auscope.portal.core.util.DOMUtil;
import org.auscope.nvcl.server.http.NVCLDataServiceMethodMaker;
import org.auscope.nvcl.server.http.NVCLNamespaceContext;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.BoreholeResultVo;
import org.auscope.nvcl.server.vo.BoreholeVo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

/*
 * NVCLAnalyticalJobProcessor will process a analytical job from AnalyticalJobVo.
 *  
 * @author Linbo Jiang
 * @author Peter Warren
 */

public class NVCLAnalyticalJobProcessor  extends Thread{
    private final Log log = LogFactory.getLog(getClass());  
    
    protected HttpServiceCaller httpServiceCaller;
    private NVCLDataServiceMethodMaker nvclMethodMaker;    
    protected WFSGetFeatureMethodMaker wfsMethodMaker;    
    private AnalyticalJobResultVo jobResultVo;

    private String serviceUrls;
    private List<String> serviceUrlsList = new ArrayList<String>();
    private String filter;
    private int startDepth;
    private int endDepth;    
    private String logName;
    private String classification;
    private String algorithmOutputID;//"128,12,34" string of integer array
    private List<Integer> algoutidList = new ArrayList<Integer>();
    private float span;
    private String units;
    private float value;
    private String logicalOp;
    private String layerName;    
    private String analyticalServiceUrl; 
    private List<BoreholeVo> boreholeList = new ArrayList<BoreholeVo>(); 
//    private String serviceHost;//"http://nvclwebservices.vm.csiro.au/";//"http://geology.data.nt.gov.au/";//
//    private String servicePathOfData;

    private void setAlgoutidList (String algorithmOutputID) {
        if (algorithmOutputID == null)
            return;
        String[] stringArray = algorithmOutputID.split(",");
        algoutidList.clear();
        for (int i = 0; i < stringArray.length; i++) {
           String numberAsString = stringArray[i];
           algoutidList.add(Integer.parseInt(numberAsString));
        }
    }
    private void setServiceUrls (String serviceUrls) {
        if (serviceUrls == null)
            return;
        String[] serviceUrlArray = serviceUrls.split(",");
        serviceUrlsList.clear();
        for (int i = 0; i < serviceUrlArray.length; i++) {
           serviceUrlsList.add(serviceUrlArray[i]);
        }
        return;
    }
    public void setAnalyticalJob(AnalyticalJobVo messageVo) {
        this.jobResultVo.setJobid(messageVo.getJobid());
        this.jobResultVo.setJobDescription(messageVo.getJobDescription());        
        this.jobResultVo.setEmail(messageVo.getEmail());
        
        this.serviceUrls = messageVo.getServiceUrls(); //"http://nvclwebservices.vm.csiro.au/geoserverBH/wfs";//"http://geology.data.nt.gov.au/geoserver/wfs"; //
        setServiceUrls(serviceUrls);
        this.algorithmOutputID = messageVo.getAlgorithmOutputID();
        setAlgoutidList(algorithmOutputID);
        this.classification = messageVo.getClassification();
        this.logName = messageVo.getLogName();
        this.startDepth = messageVo.getStartDepth();
        this.endDepth = messageVo.getEndDepth();
        this.logicalOp = messageVo.getLogicalOp();
        this.value = messageVo.getValue();
        this.units = messageVo.getUnits();
        this.span = messageVo.getSpan();
        this.filter = messageVo.getFilter();        
        //this.servicePathOfData = "NVCLDataServices/";
        this.layerName = "gsmlp:BoreholeView";        
    }     
        
    /**
     * Constructor Construct all the member variables.
     * 
     */    
    public NVCLAnalyticalJobProcessor() {
        this.jobResultVo = new AnalyticalJobResultVo();        
        this.httpServiceCaller = new HttpServiceCaller(90000);
        this.wfsMethodMaker = new WFSGetFeatureMethodMaker();
        this.nvclMethodMaker = new NVCLDataServiceMethodMaker();

    }
    public void run()
    {
        System.out.println("Thread:start:" + this.serviceUrls);
        if (! getBoreholeList()) {
            System.out.println("Failed:processor.processStage1");
          }
          if (!getDataCollection()) {
  
              System.out.println("Failed:processor.processStage2");
            }
          if (!getDownSampledData()) {
              System.out.println("Failed:processor.processStage3");            
          }
          if (!processStage4()) {          
              System.out.println("Failed:processor.processStage4");
           }     
          System.out.println("Thread:end:" + this.serviceUrls);
    }
    public boolean getBoreholeList() {
        //if (true) return true;
        System.out.println("Thread:getBoreholeList:in:" + this.serviceUrls);
        HttpPost method = null;
        for (String serviceUrl : this.serviceUrlsList) {
        String serviceHost = Utility.getHost(serviceUrl);
        String servicePathOfData;
        if (serviceUrls.contains("auscope.dpi.nsw.gov.au")) {
            servicePathOfData = "NVCLDownloadServices/";
            //nsw has no nvclCollection ready, so use brokenhill bbox as an example.
            //this.filter = "<ogc:filter><ogc:BBOX><ogc:PropertyName>gsmlp:shape</ogc:PropertyName><gml:Envelope srsName=\"EPSG:4326\"><gml:lowerCorner>141.00 -32.1</gml:lowerCorner><gml:upperCorner>141.2 -32.0</gml:upperCorner></gml:Envelope></ogc:BBOX></ogc:filter>";
        }
        else {
            servicePathOfData = "NVCLDataServices/";
        }        
        try {
            method = (HttpPost) this.wfsMethodMaker.makePostMethod(serviceUrl, this.layerName, this.filter, 0);
            String responseString = httpServiceCaller.getMethodResponseAsString(method);
            //System.out.println("response=" + responseString);
            Document responseDoc = DOMUtil.buildDomFromString(responseString);
            OWSExceptionParser.checkForExceptionResponse(responseDoc);
            NVCLNamespaceContext nc = new NVCLNamespaceContext();
            XPathExpression exp = DOMUtil.compileXPathExpr("/wfs:FeatureCollection/gml:featureMembers/gsmlp:BoreholeView/gsmlp:identifier",nc);///
            NodeList publishedDatasets = (NodeList) exp.evaluate(responseDoc, XPathConstants.NODESET);
            for (int i = 0; i < publishedDatasets.getLength(); i++) {
                Element eleHoleUrl = (Element) publishedDatasets.item(i);
                String holeUrl = eleHoleUrl.getFirstChild().getNodeValue();
                if (holeUrl != null) {
                    String[] urnBlocks = holeUrl.split("/");
                    if (urnBlocks.length > 1) {
                        String holeIdentifier = urnBlocks[urnBlocks.length-1];
                        boreholeList.add(new BoreholeVo(holeIdentifier,holeUrl,serviceUrl,serviceHost,servicePathOfData));
                    }
                }

            }
        }catch (Exception ex) {
            log.warn(String.format("NVCLAnalyticalJobProcessor::processStage1 for '%s' failed", serviceUrl));         
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        } 
        }
        System.out.println("Thread:getBoreholeList:out:" + boreholeList.size() + ":" +  this.serviceUrls);
        return true;
    }
    public boolean getDataCollection() {

        int totalLogids = 0;
        for (BoreholeVo boreholeVo : boreholeList) {
            String holeIdentifier = boreholeVo.getHoleIdentifier();
            String nvclDataServiceUrl = boreholeVo.getServiceHost() + boreholeVo.getServicePathOfData();
            try {
                HttpRequestBase method = nvclMethodMaker.getDatasetCollectionMethod(nvclDataServiceUrl, holeIdentifier);
                String responseString = httpServiceCaller.getMethodResponseAsString(method);
                Document responseDoc = DOMUtil.buildDomFromString(responseString);
                XPathExpression expr = DOMUtil.compileXPathExpr("DatasetCollection/Dataset/Logs/Log");
                NodeList nodeList = (NodeList) expr.evaluate(responseDoc, XPathConstants.NODESET);
                XPathExpression exprLogID = DOMUtil.compileXPathExpr("LogID");
                XPathExpression exprLogName = DOMUtil.compileXPathExpr("logName");
                XPathExpression exprLogType = DOMUtil.compileXPathExpr("logType");
                XPathExpression exprAlgorithmoutID = DOMUtil.compileXPathExpr("algorithmoutID");                
                XPathExpression exprIsPublic = DOMUtil.compileXPathExpr("ispublic");                                
                boolean isError = true;
                for (int i = 0; i < nodeList.getLength(); i++) {  
                    Element eleLogID = (Element) exprLogID.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strLogID = eleLogID.getFirstChild().getNodeValue(); 
                    Element eleLogName = (Element) exprLogName.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strLogName = eleLogName.getFirstChild().getNodeValue(); 
                    Element eleLogType = (Element) exprLogType.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strLogType = eleLogType.getFirstChild().getNodeValue(); 
                    Element eleAlgorithmoutID = (Element) exprAlgorithmoutID.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strAlgorithmoutID = eleAlgorithmoutID.getFirstChild().getNodeValue();
                    int     intAlgorithmoutID = Integer.parseInt(strAlgorithmoutID);                    
                    Element eleIsPublic = (Element) exprIsPublic.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strIsPublic = eleIsPublic.getFirstChild().getNodeValue();                     
                    if (logName != null && logName.length() > 0) {
                        if (strLogName.equalsIgnoreCase(logName)) {
                            boreholeVo.logidList.add(strLogID);
                            //System.out.println("add LogID:" + strLogID + "from borehole:" + holeIdentifier);
                            isError = false;
                            totalLogids++;
                            break;
                        }
                    } else {
                        if (strIsPublic.equalsIgnoreCase("true")) {
                            if (algoutidList.contains(intAlgorithmoutID)) {
                                boreholeVo.logidList.add(strLogID);
                                isError = false;
                                totalLogids++;
                                //System.out.println("add LogID:" + strLogID + "from borehole:" + holeIdentifier);
                            }
                        }
                    }
                    
                    
                   // System.out.println("exprLogID:" + strLogID + ":" + strLogName + ":" + strLogType + ":" + strAlgorithmoutID);                        
                }
                if (isError) {
                    String resultMsg;
                    if (logName != null && logName.length() > 0) {
                        resultMsg = "Error by with no logName of " + this.logName + " exist";
                    } else {
                        resultMsg = "Error by with no algorithmoutid of \"" + this.algorithmOutputID + "\" exist";
                    }
                    boreholeVo.setStatus(1); //error status;
                    jobResultVo.addErrorBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                }
                method.releaseConnection();
            }catch (Exception ex) {
                log.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage2 for '%s' failed", nvclDataServiceUrl));
                return false;
            } 
        }
        System.out.println("Total Logids:" + totalLogids + ":" + this.serviceUrls);
        return true;
    }
    

    public boolean getDownSampledData() {
        //A sample for getDownSampledData request:
        //http://nvclwebservices.vm.csiro.au/NVCLDataServices/getDownsampledData.html?logid=14b146e6-bcdf-43e1-ae53-c007b6f28d3&interval=1.0&startdepth=0&enddepth=99999&outputformat=csv
        //http://nvclwebservices.vm.csiro.au/NVCLDataServices/
        String resultMsg = "InitMessage";
        int totalProcessedLogid = 0;
        for (BoreholeVo boreholeVo : boreholeList) {
            String nvclDataServiceUrl = boreholeVo.getServiceHost() + boreholeVo.getServicePathOfData();
            if (boreholeVo.getStatus()!= 0) {
                //System.out.println("skip: error borehole:" + boreholeVo.getHoleIdentifier() + ":status:" + boreholeVo.getStatus());
                continue;
            }
            String holeIdentifier = boreholeVo.getHoleIdentifier();                    boreholeVo.setStatus(1); //error status;

            boolean isHit = false;
            for(String logid : boreholeVo.logidList) {
                totalProcessedLogid++;
                //System.out.println("Stage3:process:borehole:" + holeIdentifier + "  logid:" + logid);
                try {
                    HttpRequestBase method = nvclMethodMaker.getDownSampledDataMethod(nvclDataServiceUrl, logid, span, startDepth, endDepth, "csv");
                    String responseString = httpServiceCaller.getMethodResponseAsString(method);
                    method.releaseConnection();                    
                    String csvLine;
                    BufferedReader csvBuffer = new BufferedReader(new StringReader(responseString)); 
                    TreeMap<String, Integer> depthMap = new TreeMap<String, Integer>(); //depth:countSum;
                    TreeMap<String, Integer> depthClassificationMap = new TreeMap<String, Integer>();  
                    
                    csvLine = csvBuffer.readLine();//skip the header
                    while ((csvLine = csvBuffer.readLine()) != null) {
                        List<String> cells = Arrays.asList(csvLine.split("\\s*,\\s*"));   
                        String depth = cells.get(0);
                        Integer count = Integer.parseInt(cells.get(3));
                        String csvClassfication = cells.get(1);
                        Integer countSum = 0 ;
                        if (depthMap.get(depth) != null) {
                            countSum= depthMap.get(depth) + count;
                        } else {
                            countSum = 0 + count;
                        }
                        depthMap.put(depth, countSum);
                        if (csvClassfication.equalsIgnoreCase(classification)) {
                            depthClassificationMap.put(depth, count);
                        }
                    }
                    String depthKey;
                    Integer count = 0;
                    Integer countSum;
                    float ratio = (float) 0.0;
                    /* Now, iterate over the map's contents, sorted by key. */
                    for (Entry<String, Integer> entry : depthClassificationMap.entrySet()) {
                        depthKey = entry.getKey();
                        count = entry.getValue();
                        countSum = depthMap.get(depthKey);
                        ratio = (float) count*100/countSum;
                        //System.out.println("count:"+count+":countSum"+countSum+":ratio:"+ratio);
                      if (units.equalsIgnoreCase("pct")) {
                          if (logicalOp.equalsIgnoreCase("gt")) {
                              if (ratio > value) {
                                  isHit = true;
                                  break;
                              }
                          } else if (logicalOp.equalsIgnoreCase("lt")){
                              if (ratio < value) {
                                  isHit = true;
                                  break;
                              }
                          } else if (logicalOp.equalsIgnoreCase("eq")){
                              if (Math.abs(ratio - value) < 1.0) { //float equal when abs < 1%
                                  isHit = true;
                                  break;
                              }
                          }
                          
                      } else {
                          if (logicalOp.equalsIgnoreCase("gt")) {                      
                              if (count > value) {
                                  isHit = true;
                                  break;                          
                              }
                          } else if (logicalOp.equalsIgnoreCase("lt")) {
                              if (count < value) {
                                  isHit = true;
                                  break;
                              }
                          } else if (logicalOp.equalsIgnoreCase("eq")){
                              if (Math.abs(count - value) < 1.0) { //float equal when abs < 1
                                  isHit = true;
                                  break;
                              }
                          }
                      }
                    }
                    if (isHit) {
                            resultMsg = "Hitted by " + this.classification + " with value " + String.valueOf(units.equalsIgnoreCase("pct")?ratio:count) + " " + logicalOp + " than threshhole " + String.valueOf(value) + " " +units;
                            boreholeVo.setStatus(2); //hitted status;
                            jobResultVo.addBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                            System.out.println("*****************hitted****************");
                            break;
                        
                    }
                }catch (Exception ex) {
                    System.out.println("*****************error:exception at 380****************");
                    log.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage3 for borehole:'%s' logid: '%s' failed", holeIdentifier,logid));
                    return false;
                } 
            } //logid loop
            if(!isHit) {
                resultMsg = "Failed by " + this.classification + " with no value " + logicalOp + " than threshhold " + String.valueOf(value)+ " " +units;
                boreholeVo.setStatus(3); //Failed status;
                jobResultVo.addFailedBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                System.out.println("*****************failed****************");
            }            
        }//borehole loop  
        System.out.println("total Processed Logid:" + totalProcessedLogid);   
        System.out.println("Stage 3:OK:" + this.serviceUrls);
        return true;
    }
    public boolean processStage4() {
        // TODO Auto-generated method stub
        return true;
    }
    
    public AnalyticalJobResultVo getJobResult() {
        return jobResultVo;
    }

    public String getAnalyticalServiceUrl() {
        return analyticalServiceUrl;
    }
    public void setAnalyticalServiceUrl(String analyticalServiceUrl) {
        this.analyticalServiceUrl = analyticalServiceUrl;
    }
   
}
