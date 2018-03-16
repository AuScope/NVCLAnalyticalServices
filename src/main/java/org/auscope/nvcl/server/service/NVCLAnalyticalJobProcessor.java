package org.auscope.nvcl.server.service;


import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.util.Utility;
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
 
	private static final Logger logger = LogManager.getLogger(NVCLAnalyticalJobProcessor.class);
	
	
    private boolean isdecimalTypeScalar;    
    public NVCLAnalyticalJobProcessor() {
        this.isdecimalTypeScalar = false;
   
    }

    public void run()
    {
    	logger.info("NVCLAnalyticalJobProcessor starting:" + this.serviceUrls);
        if (!getBoreholeList()) {
        	logger.error("Failed:processor.getBoreholeList");
          }
          if (!getDataCollection()) {
        	  logger.error("Failed:processor.getDatasetContents");
            }
          if (!getDownSampledData()) {
        	  logger.error("Failed:processor.getDownSampledData");            
          }
          logger.info("NVCLAnalyticalJobProcessor finished");
    }

    public boolean getDataCollection() {

        int totalLogids = 0;
        for (BoreholeVo boreholeVo : boreholeList) {
            String holeIdentifier = boreholeVo.getHoleIdentifier();
            String nvclDataServiceUrl = boreholeVo.getServiceHost() + boreholeVo.getServicePathOfData();
            try {
                
                String responseString = NVCLAnalyticalRequestSvc.dataAccess.getDatasetCollection(nvclDataServiceUrl, holeIdentifier);
                		
                Document responseDoc = Utility.buildDomFromString(responseString);
                XPathExpression expr = Utility.compileXPathExpr("DatasetCollection/Dataset/Logs/Log");
                NodeList nodeList = (NodeList) expr.evaluate(responseDoc, XPathConstants.NODESET);
                XPathExpression exprLogID = Utility.compileXPathExpr("LogID");
                XPathExpression exprLogName = Utility.compileXPathExpr("logName");
                XPathExpression exprLogType = Utility.compileXPathExpr("logType");
                XPathExpression exprAlgorithmoutID = Utility.compileXPathExpr("algorithmoutID");                
                XPathExpression exprIsPublic = Utility.compileXPathExpr("ispublic");                                
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
                            logger.debug("add LogID: " + strLogID + " from borehole: " + holeIdentifier);
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
                                logger.debug("add LogID :" + strLogID + " from borehole: " + holeIdentifier);
                            }
                        }
                    }
                    logger.debug("exprLogID: " + strLogID + " : " + strLogName + " : " + strLogType + " : " + strAlgorithmoutID);                        
                }
                if (isError) {
                    String resultMsg;
                    if (!Utility.stringIsBlankorNull(logName)) {
                        resultMsg = "log named : " + this.logName + " doesn't exist in this dataset";
                    } else {
                        resultMsg = "log produced by algorithm with id \"" + this.algorithmOutputID + "\" doesn't exist in this dataset";
                    }
                    boreholeVo.setStatus(1); //error status;
                    jobResultVo.addErrorBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                }

            }catch (Exception ex) {
                // if Exception happened, log it and let it continue for the next borehole.
                logger.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage2 for '%s' failed", nvclDataServiceUrl));
                String resultMsg = "Error:" + ex.toString();
                boreholeVo.setStatus(1); //error status;
                jobResultVo.addErrorBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
            } 
        }
        logger.info("Total Logids:" + totalLogids + ":" + this.serviceUrls);
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
                logger.debug("decimalLogType");
            }
        }
        return;
    }
    public boolean getDownSampledData() {
        String resultMsg = "InitMessage";
        int totalProcessedLogid = 0;
        for (BoreholeVo boreholeVo : boreholeList) {
            String nvclDataServiceUrl = boreholeVo.getServiceHost() + boreholeVo.getServicePathOfData();
            if (boreholeVo.getStatus()!= 0) {
            	logger.debug("skip: error borehole:" + boreholeVo.getHoleIdentifier() + ":status:" + boreholeVo.getStatus());
                continue;
            }
            String holeIdentifier = boreholeVo.getHoleIdentifier();                    boreholeVo.setStatus(1); //error status;

            boolean isHit = false;
            for(String logid : boreholeVo.logidList) {
                totalProcessedLogid++;
                logger.debug("Stage3:process:boreholeid: " + holeIdentifier + " logid: " + logid);
                try {
                	
                    String responseString = NVCLAnalyticalRequestSvc.dataAccess.getDownSampledDataMethod(nvclDataServiceUrl, logid, span, startDepth, endDepth, "csv");
                 
                    String csvLine;

                    BufferedReader csvBuffer = new BufferedReader(new StringReader(responseString)); 
                    TreeMap<String, Float> depthMap = new TreeMap<String, Float>(); //depth:countSum;
                    TreeMap<String, Float> depthClassificationMap = new TreeMap<String, Float>();  
                    csvLine = csvBuffer.readLine();//skip the header


                    while ((csvLine = csvBuffer.readLine()) != null) {

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
                        if( !depthClassificationMap.containsKey(depth)) {
                            depthClassificationMap.put(depth, (float) 0.0);
                        }
                        if (csvClassfication.equalsIgnoreCase(classification)) {
                            depthClassificationMap.put(depth, count);
                        } 
                    }

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
                            resultMsg = "Hit: "+boreholeVo.getHoleUrl()+" " + this.classification + " with value " + String.valueOf(units.equalsIgnoreCase("pct")?ratio:count) + " " + logicalOp + " threshhole " + String.valueOf(value) + " " +units;
                            boreholeVo.setStatus(2); //hit status;
                            jobResultVo.addBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                            logger.info(resultMsg);
                            break;
                        
                    }
                }catch (Exception ex) {
                    //if exception happened, let it continue for next logid
                    logger.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage3 for borehole:'%s' logid: '%s' failed", holeIdentifier,logid));
                    //return false;
                } 
            } //logid loop
            if(!isHit) {
                resultMsg = "Miss: "+boreholeVo.getHoleIdentifier()+" " + this.classification + " with value " + logicalOp + " threshhold " + String.valueOf(value)+ " " +units + " NOT found";
                boreholeVo.setStatus(3); //Failed status;
                jobResultVo.addFailedBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                logger.info(resultMsg);
            }            
        }//borehole loop  
        logger.info("Successfully retrieved data for " + totalProcessedLogid + " boreholes from " + this.serviceUrls);
        return true;
    }
    

   
}
