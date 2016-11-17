package org.auscope.nvcl.server.service;


import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
//import org.auscope.portal.core.server.http.HttpServiceCaller;
import org.auscope.portal.core.services.methodmakers.WFSGetFeatureMethodMaker;
import org.auscope.nvcl.server.http.HttpServiceCaller;
import org.auscope.portal.core.services.responses.ows.OWSExceptionParser;
import org.auscope.portal.core.util.DOMUtil;
import org.auscope.nvcl.server.http.NVCLDataServiceMethodMaker;
import org.auscope.nvcl.server.http.NVCLNamespaceContext;
import org.auscope.nvcl.server.util.TsgMod;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.BoreholeResultVo;
import org.auscope.nvcl.server.vo.BoreholeVo;
import org.auscope.nvcl.server.vo.SpectralLogVo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/*
 * TSGModJobProcessor will process a TSGMod Analytical job(based on the live calculated scalar) from AnalyticalJobVo.
 *  
 * @author Linbo Jiang
 * @author Peter Warren
 */

public class TSGModJobProcessor  extends IJobProcessor{
    private final Log log = LogFactory.getLog(getClass());      
    private TsgMod tsgMod = new TsgMod(); 
    private String tsgScript;
    /**
     * Constructor Construct all the member variables.
     * 
     */    
    public TSGModJobProcessor() {
        

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
          if (!getSpectralData()) {
              System.out.println("Failed:processor.getDownSampledData");            
          }
   
          System.out.println("Thread:end:" + this.serviceUrls);
    }
    public void setAnalyticalJob(AnalyticalJobVo messageVo) {
        byte[] byteTsgScript = Base64.getDecoder().decode(messageVo.getTsgScript());
        this.tsgScript = new String(byteTsgScript);
//              System.out.println("Base64 decoded String (Basic) :" + decodedTsgScript);messageVo.getTsgScript();
        super.setAnalyticalJob(messageVo);
    }

    /**
     * Get the finalMask's logid from Document.
     *  @param doc    XML Document
     * return finalMaskLogid String 
     */    
    private String getFinalMaskLogid(Document doc) 
    {
        String strLogIDMask = null;
        try {
            XPathExpression exprMask;
            exprMask = DOMUtil.compileXPathExpr("DatasetCollection/Dataset/Logs/Log"); //"DatasetCollection/Dataset/SpectralLogs/SpectralLog");//
            NodeList nodeListMask = (NodeList) exprMask.evaluate(doc, XPathConstants.NODESET);
            XPathExpression exprLogIDMask = DOMUtil.compileXPathExpr("LogID");
            XPathExpression exprLogNameMask = DOMUtil.compileXPathExpr("logName");
            for (int j = 0; j < nodeListMask.getLength(); j++) {
                Element eleLogIDMask = (Element) exprLogIDMask.evaluate(nodeListMask.item(j), XPathConstants.NODE);
                String logid = eleLogIDMask.getFirstChild().getNodeValue(); 
                Element eleLogNameMask = (Element) exprLogNameMask.evaluate(nodeListMask.item(j), XPathConstants.NODE);
                String strLogNameMask = eleLogNameMask.getFirstChild().getNodeValue();     
                //System.out.println(strLogNameMask);
                if (strLogNameMask.equalsIgnoreCase("Final Mask")) {
                    strLogIDMask = logid;
                    System.out.println("maskLogid:" + strLogIDMask );
                    break;
                    //return strLogIDMask;
                }
            }
        } catch ( Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }//DatasetCollection/Dataset/Logs/Log");//
        
        return strLogIDMask;
        
    }
    /**
     * getDataCollection
     * It download the datasetCollection ,then it extract the logid, samplecount and wavelength. 
     * It saved the extracted information into BoreholeVo 
     * 
     * @return true for successfully extracting the information
     */      
    public boolean getDataCollection() {

        int totalLogids = 0;
        for (BoreholeVo boreholeVo : boreholeList) {
            String holeIdentifier = boreholeVo.getHoleIdentifier();
            String nvclDataServiceUrl = boreholeVo.getServiceHost() + boreholeVo.getServicePathOfData();
            try {
                HttpRequestBase method = nvclMethodMaker.getDatasetCollectionMethod(nvclDataServiceUrl, holeIdentifier);
                String responseString = httpServiceCaller.getMethodResponseAsString(method);
                Document responseDoc = DOMUtil.buildDomFromString(responseString);
                //System.out.println(responseString);
                XPathExpression expr = DOMUtil.compileXPathExpr("DatasetCollection/Dataset/SpectralLogs/SpectralLog");//DatasetCollection/Dataset/Logs/Log");//
                NodeList nodeList = (NodeList) expr.evaluate(responseDoc, XPathConstants.NODESET);
                XPathExpression exprLogID = DOMUtil.compileXPathExpr("logID");
                XPathExpression exprLogName = DOMUtil.compileXPathExpr("logName");
                XPathExpression exprSampleCount = DOMUtil.compileXPathExpr("sampleCount");
                XPathExpression exprWavelengths = DOMUtil.compileXPathExpr("wavelengths");
                boolean isError = true;
                //logName=Reflectance  logID (sampleCount>0) wavelengths
                // assume "Reflectance" pass it to the process in the swir/vnir parameter
                for (int i = 0; i < nodeList.getLength(); i++) {  
                    Element eleLogID = (Element) exprLogID.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strLogID = eleLogID.getFirstChild().getNodeValue(); 
                    Element eleLogName = (Element) exprLogName.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strLogName = eleLogName.getFirstChild().getNodeValue(); 
                    Element eleSampleCount = (Element) exprSampleCount.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strSampleCount = eleSampleCount.getFirstChild().getNodeValue(); 
                    int intSampleCount = Integer.parseInt(strSampleCount);
                    Element eleWavelengths = (Element) exprWavelengths.evaluate(nodeList.item(i), XPathConstants.NODE);
                    String strWavelengths = eleWavelengths.getFirstChild().getNodeValue();
                    
                    if (intSampleCount > 0 && strLogName.equalsIgnoreCase("Reflectance")) {                        
                        boreholeVo.spectralLogList.add(new SpectralLogVo(strLogID,strSampleCount,strWavelengths));
                        isError = false;
                        totalLogids++;                        
                        System.out.println("Reflectance:" + holeIdentifier + ":LogID:" + strLogID + ":" + strLogName + ":" + strSampleCount + ":" + strWavelengths);
                        
                        
                        //get final_mask logid
                        String finalMaskLogid = getFinalMaskLogid(responseDoc);
                        boreholeVo.setFinalMaskLogid(finalMaskLogid);                       

//                        <Log>
//                        <LogID>dd5c574e-7028-4077-a70e-f5d3430677d</LogID>
//                        <logName>Final Mask</logName>
//                        <ispublic>false</ispublic>
//                        <logType>6</logType>
//                        <algorithmoutID>0</algorithmoutID>
//                        </Log>
                        break;
                        
                    } else {
                        //System.out.println("LogID:" + strLogID + ":" + strLogName + ":" + strSampleCount + ":" + strWavelengths); 
                    }
                   // System.out.println("exprLogID:" + strLogID + ":" + strLogName + ":" + strLogType + ":" + strAlgorithmoutID);                        
                }
                if (isError) {
                    String resultMsg;
                    resultMsg = "Error by with SampleCountZero";
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
        System.out.println("Total spectalLogs:" + totalLogids + ":" + this.serviceUrls);
        return true;
    }
    /**
     * getSpectralData
     * It download the SpetralData based on logid. then it called the TSGMod to do the caculation. 
     * It download the finalMask and apply the finalMask on result. 
     * It saved the result into jobResultVo(hitted or failed)
     * @return true for successfully processing the information
     */     
    public boolean getSpectralData() {
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
            String holeIdentifier = boreholeVo.getHoleIdentifier();
            String finalMaskLogid = boreholeVo.getFinalMaskLogid();
            boreholeVo.setStatus(1); //error status;

            boolean isHit = false;

            
            for(SpectralLogVo spectralLog : boreholeVo.spectralLogList) {

                totalProcessedLogid++;
                String logid =  spectralLog.getLogID();
                int sampleCount = spectralLog.getSampleCount();
                float[] wvl = spectralLog.getWvl();
                int waveLengthCount = wvl.length;
                byte[] spectralData = new byte[sampleCount*waveLengthCount*4];
                ByteBuffer target = ByteBuffer.wrap(spectralData);            
                System.out.println("getSpectralData:start:BoreholeId:" + boreholeVo.getHoleIdentifier() + ":logid:" + logid);

                // System.out.println("Stage3:process:borehole:" +
                // holeIdentifier + "  logid:" + logid);
                try {
                    int step = 4000;
                    System.out.println("getSpectralData:download:step:" + step);
                    for (int i = 0; i < sampleCount; i = i + step) {
                        // LJ
                        // sample:http://geossdi.dmp.wa.gov.au/NVCLDataServices/getspectraldata.html?speclogid=baddb3ed-0872-460e-bacb-9da380fd1de
                        int start = i;
                        int end = (i + step > sampleCount) ? sampleCount - 1 : i + step - 1;
                        int count = end - start + 1;
                        HttpRequestBase methodSpectralData = nvclMethodMaker.getSpectralDataMethod(nvclDataServiceUrl, logid, start, end);
                        target.put(httpServiceCaller.getMethodResponseAsBytes(methodSpectralData));
                        methodSpectralData.releaseConnection();
                        
                        System.out.println("start:" + start + ":end:" + end + ":count:" + count);
                        // tsgMod.parseOneScalarMethod(null, wvl,
                        // waveLengthCount ,
                        // Utility.getFloatspectraldata(spectralData),sampleCount);
                    }
                    System.out.println("getSpectralData:Call TsgMod");
                    isHit = tsgMod.parseOneScalarMethod(this.tsgScript, wvl, waveLengthCount, Utility.getFloatSpectralData(spectralData), sampleCount, value, (float) 0.2);

                    System.out.println("getSpectralData:getFinalMask");
                    HttpRequestBase methodMask = nvclMethodMaker.getDownloadScalarsMethod(nvclDataServiceUrl, finalMaskLogid);
                    String strMask = httpServiceCaller.getMethodResponseAsString(methodMask);
                    methodMask.releaseConnection();

                    String csvLine;

                    BufferedReader csvBuffer = new BufferedReader(new StringReader(strMask));
                    // startDepth, endDepth, final_mask(could be null)
                    TreeMap<String, Byte> depthMaskMap = new TreeMap<String, Byte>(); // depth:Mask;
                    csvLine = csvBuffer.readLine();// skip the header
                    System.out.println("csv:" + csvLine);
                    int linesread = 0;
                    while ((csvLine = csvBuffer.readLine()) != null) {
                        linesread++;
                        List<String> cells = Arrays.asList(csvLine.split("\\s*,\\s*"));
                        String depth = cells.get(0);
                        Byte mask = 0;
                        mask = Byte.parseByte(cells.get(2));
                        depthMaskMap.put(depth, mask);
                        //System.out.println("csv:" + csvLine);
                        // csvClassfication="averageValue";

                    }
                    System.out.println("lines read " + linesread);
                    if (isHit) {
                        resultMsg = "Hitted by " + this.classification + " with value " + String.valueOf(value) + " " + units;
                        boreholeVo.setStatus(2); // hitted status;
                        jobResultVo.addBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(), resultMsg));
                        System.out.println("*****************hitted:" + boreholeVo.getHoleIdentifier());
                        break;

                    }
                } catch (Exception ex) {
                    // if exception happened, let it continue for next logid
                    System.out.println("*****************Exception: at 394****************");
                    log.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage3 for borehole:'%s' logid: '%s' failed", holeIdentifier, logid));
                    // return false;
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
