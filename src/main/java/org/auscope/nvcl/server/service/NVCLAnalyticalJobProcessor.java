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
 * NVCLAnalyticalJobProcessor will process a nvclAnalytical job(based on traditional scalar) from AnalyticalJobVo.
 *  
 * @author Linbo Jiang
 * @author Peter Warren
 */

public class NVCLAnalyticalJobProcessor  extends IJobProcessor{
    private final Log log = LogFactory.getLog(getClass()); 
    private boolean isdecimalTypeScalar;    
    public NVCLAnalyticalJobProcessor() {
        this.isdecimalTypeScalar = false;
   
    }

    public void run()
    {
        System.out.println("Thread:start:" + this.serviceUrls);
        if (! getBoreholeList()) {
            System.out.println("Failed:processor.getBoreholeList");
          }
          if (!getDataCollection()) {
  
              System.out.println("Failed:processor.getDataCollection");
            }
          if (!getDownSampledData()) {
              System.out.println("Failed:processor.getDownSampledData");            
          }
   
          System.out.println("Thread:end:" + this.serviceUrls);
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
                            checkDecimalTypeScalar(strLogType);
                            isError = false;
                            totalLogids++;
                            break;
                        }
                    } else {
                        if (strIsPublic.equalsIgnoreCase("true")) {
                            if (algoutidList.contains(intAlgorithmoutID)) {
                                boreholeVo.logidList.add(strLogID);
                                checkDecimalTypeScalar(strLogType);
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
                // if Exception happened, log it and let it continue for the next borehole.
                log.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage2 for '%s' failed", nvclDataServiceUrl));
                String resultMsg = "Error:unknow exception:" + ex.toString();
                boreholeVo.setStatus(1); //error status;
                jobResultVo.addErrorBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
            } 
        }
        System.out.println("Total Logids:" + totalLogids + ":" + this.serviceUrls);
        return true;
    }
    
    public void checkDecimalTypeScalar(String strLogType) {
        if (Utility.tryParseInt(strLogType)) {  
            // We now know that it's safe to parse
            int intLogType = Integer.parseInt(strLogType); 
            if (intLogType == 2)
            {
                this.classification = "averageValue";
                this.units = "count";
                this.isdecimalTypeScalar = true;
                System.out.println("!!!!!!!-decimalLogType:");
            }
            //System.out.println("!!!!!!!-intLogType:" + intLogType);
        }
        return;
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
                    TreeMap<String, Float> depthMap = new TreeMap<String, Float>(); //depth:countSum;
                    TreeMap<String, Float> depthClassificationMap = new TreeMap<String, Float>();  
                    csvLine = csvBuffer.readLine();//skip the header
                    //System.out.println("csv:" + csvLine);
                    int linesread=0;
                    while ((csvLine = csvBuffer.readLine()) != null) {
                        linesread++;
                        List<String> cells = Arrays.asList(csvLine.split("\\s*,\\s*"));   
                        String depth = cells.get(0);
                        Float count =  0.0f;
                        String csvClassfication;
                        if (this.isdecimalTypeScalar == false) {
                            count = Float.parseFloat(cells.get(3));
                            csvClassfication = cells.get(1);
                        } else {
                            count = Float.parseFloat(cells.get(1));
                            csvClassfication="averageValue";
                            //get the float;
                        }
                        Float countSum = 0.0f ;
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
                    //System.out.println("lines read " +linesread);
                    String depthKey;
                    Float count = 0.0f;
                    Float countSum;
                    float ratio = (float) 0.0;
                    /* Now, iterate over the map's contents, sorted by key. */
                    for (Entry<String, Float> entry : depthClassificationMap.entrySet()) {
                        depthKey = entry.getKey();
                        count = entry.getValue();
                        countSum = depthMap.get(depthKey);
                        if (countSum!=0) ratio = (float) count*100/countSum;
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
                            System.out.println("*****************hitted:" +boreholeVo.getHoleIdentifier());
                            break;
                        
                    }
                }catch (Exception ex) {
                    //if exception happened, let it continue for next logid
                    System.out.println("*****************Exception: at 394****************");
                    log.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage3 for borehole:'%s' logid: '%s' failed", holeIdentifier,logid));
                    //return false;
                } 
            } //logid loop
            if(!isHit) {
                resultMsg = "Failed by " + this.classification + " with no value " + logicalOp + " than threshhold " + String.valueOf(value)+ " " +units;
                boreholeVo.setStatus(3); //Failed status;
                jobResultVo.addFailedBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                System.out.println("*****************failed:" +boreholeVo.getHoleIdentifier());
            }            
        }//borehole loop  
        System.out.println("total Processed Logid:" + totalProcessedLogid);   
        System.out.println("Stage 3:OK:" + this.serviceUrls);
        return true;
    }
    

   
}
