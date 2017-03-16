package org.auscope.nvcl.server.service;


import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpRequestBase;
import org.auscope.portal.core.util.DOMUtil;
import org.auscope.nvcl.server.util.TsgMod;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.BoreholeResultVo;
import org.auscope.nvcl.server.vo.BoreholeVo;
import org.auscope.nvcl.server.vo.SpectralLogVo;
import org.auscope.nvcl.server.vo.TSGScalarArrayVo;
import org.auscope.nvcl.server.vo.TSGScalarVo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

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
    private boolean bProxy;
    private String proxyHost;
    private int    proxyPort;
    private String dataPath;
    /**
     * Constructor Construct all the member variables.
     * 
     */    
    public TSGModJobProcessor() {
        dataPath = NVCLAnalyticalRequestSvc.config.getDataPath();
        bProxy = NVCLAnalyticalRequestSvc.config.isUseProxy();
        if (bProxy) {
            proxyHost = NVCLAnalyticalRequestSvc.config.getProxyHost();
            proxyPort = NVCLAnalyticalRequestSvc.config.getProxyPort();
        }

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
            System.out.println("getFinalMaskLogid:exception");
            e.printStackTrace();
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
                String responseString = httpServiceCaller.getMethodResponseAsString(method,Utility.getProxyHttpClient(this.proxyHost, this.proxyPort));
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
                    
                    if (intSampleCount > 0 && strLogName.equalsIgnoreCase("Reflectance")) {//9873848a-4dd5-46ba-b76d-5ea0f4c622d
                        boreholeVo.spectralLogList.add(new SpectralLogVo(strLogID,strSampleCount,strWavelengths));
                        isError = false;
                        totalLogids++;                        
                        System.out.println("getDataCollection:Reflectance:" + holeIdentifier + ":LogID:" + strLogID + ":" + strLogName + ":" + strSampleCount + ":" + strWavelengths);
                        
                        
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
                    String resultMsg = formatMessage(0);
                    boreholeVo.setStatus(1); //error status;
                    jobResultVo.addErrorBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                }
                method.releaseConnection();
                method = null;

            }catch (Exception ex) {
                // if Exception happened, log it and let it continue for the next borehole.
                log.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage2 for '%s' failed", nvclDataServiceUrl));
                String resultMsg = formatMessage(3) + ex.toString();
                boreholeVo.setStatus(1); //error status;
                jobResultVo.addErrorBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
            } 
        }
        System.out.println("getDataCollection:Total spectalLogs:" + totalLogids + ":" + this.serviceUrls);
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
        String resultMsg = "InitMessage";
        int totalProcessedLogid = 0;
        System.out.println(Utility.getCurrentTime() + "getSpectralData:" + this.serviceUrls);
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
                double[] tsgRV = new double[sampleCount];
                ByteBuffer target = ByteBuffer.wrap(spectralData);     
                System.out.println("=========================");
                System.out.println(Utility.getCurrentTime() + "getSpectralData:start:BoreholeId:" + boreholeVo.getHoleIdentifier() + ":logid:" + logid);

                // System.out.println("Stage3:process:borehole:" +
                // holeIdentifier + "  logid:" + logid);
                try {
                    int step = 4000;
                    System.out.println(Utility.getCurrentTime() + "getSpectralData:Downlaod Spectrum:" );
                    for (int i = 0; i < sampleCount; i = i + step) {
                        // LJ
                        // sample:http://geossdi.dmp.wa.gov.au/NVCLDataServices/getspectraldata.html?speclogid=baddb3ed-0872-460e-bacb-9da380fd1de
                        int start = i;
                        int end = (i + step > sampleCount) ? sampleCount - 1 : i + step - 1;
                        int count = end - start + 1;
                        HttpRequestBase methodSpectralData = nvclMethodMaker.getSpectralDataMethod(nvclDataServiceUrl, logid, start, end);
                        if (this.bProxy) {
                        target.put(httpServiceCaller.getMethodResponseAsBytes(methodSpectralData,Utility.getProxyHttpClient(this.proxyHost, this.proxyPort)));
                        } else {
                            target.put(httpServiceCaller.getMethodResponseAsBytes(methodSpectralData));
                        }
                        methodSpectralData.releaseConnection();
                        methodSpectralData = null;
                    }
                    System.out.println(Utility.getCurrentTime() + "getSpectralData:Call TsgMod");
                    tsgMod.parseOneScalarMethod(tsgRV, this.tsgScript, wvl, waveLengthCount, Utility.getFloatSpectralData(spectralData), sampleCount, value, (float) 0.2);
                    
                    ////////
                    System.out.println(Utility.getCurrentTime() + "getSpectralData:getDownSampledData:");
                    isHit = getDownSampledData (tsgRV,sampleCount,nvclDataServiceUrl,holeIdentifier,finalMaskLogid );
                    ////////
                    if (isHit) {
                        resultMsg = formatMessage(1);
                        boreholeVo.setStatus(2); // hitted status;
                        jobResultVo.addBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(), resultMsg));
                        System.out.println("***************getSpectralData:hitted:" + boreholeVo.getHoleIdentifier());
                        break;

                    }
                    
                } catch (Exception ex) {
                    // if exception happened, let it continue for next logid
                    System.out.println("*****************Exception: at 394****************");
                    log.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage3 for borehole:'%s' logid: '%s' failed", holeIdentifier, logid));
                    // return false;
                }
                spectralData = null;
                tsgRV = null;
                
            } //logid loop
            if(!isHit) {
                resultMsg = formatMessage(2);
                boreholeVo.setStatus(3); //Failed status;
                jobResultVo.addFailedBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                System.out.println("*****************failed:" +boreholeVo.getHoleIdentifier());
            }         
            System.out.println("=========================");
        }//borehole loop  
        System.out.println(Utility.getCurrentTime() + "getSpectralData:total Processed Logid:" + totalProcessedLogid );   

        return true;
    }
    
    public boolean getDownSampledData(double[] tsgRV, int sampleCount, String nvclDataServiceUrl, String holeIdentifier,String finalMaskLogid) {
        boolean isHit = false;
        System.out.println(Utility.getCurrentTime() + "getDownSampledData:getFinalMask");
        HttpRequestBase methodMask;
        try {
            methodMask = nvclMethodMaker.getDownloadScalarsMethod(nvclDataServiceUrl, finalMaskLogid);
            System.out.println(methodMask.getURI());
            String strMask;
            strMask = httpServiceCaller.getMethodResponseAsString(methodMask);
            methodMask.releaseConnection();
            methodMask = null;
    
            String csvLine;
    
            BufferedReader csvBuffer = new BufferedReader(new StringReader(strMask));
            // startDepth, endDepth, final_mask(could be null)
            TreeMap<String, Byte> depthMaskMap = new TreeMap<String, Byte>(); // depth:Mask;
            csvLine = csvBuffer.readLine();// skip the header
            //System.out.println("csv:" + csvLine);
            int index = 0;

            TSGScalarArrayVo scalarArray = new TSGScalarArrayVo(this.span);
            
            while ((csvLine = csvBuffer.readLine()) != null) {

                List<String> cells = Arrays.asList(csvLine.split("\\s*,\\s*"));
                String depth = cells.get(0);
                Byte mask = 0;
                mask = Byte.parseByte(cells.get(2));
                scalarArray.add(new TSGScalarVo(depth,mask!=0,tsgRV[index]));
                depthMaskMap.put(depth, mask);
                index++;
                //System.out.println("csv:" + csvLine);
                // csvClassfication="averageValue";    
            }
            csvBuffer = null;
            System.out.println("lines read " + index);    
            System.out.println(Utility.getCurrentTime() + "getDownSampledData:downSample:");
            int sizeOfBin = scalarArray.downSample();
            isHit = scalarArray.query(this.units, this.logicalOp,this.value);
            if (true) { //log.isDebugEnabled()) {
                System.out.println(Utility.getCurrentTime() + "getDownSampledData:writeCSV:");
                String filePath = dataPath + this.jobid;
                Utility.createDirectorys(filePath);
                String fileFullPath = filePath + "/" + holeIdentifier;
                scalarArray.writeScalarCSV(fileFullPath + "-scalar.csv");
                scalarArray.writeDownSampledScalarCSV(fileFullPath + "-scalarDownSampled.csv");
            }
            scalarArray = null;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("Exception: on getDownSampledData");
            e.printStackTrace();
        }
        return isHit;
    }   
    private String formatMessage(int type) {
        String resultMsg;
        switch (type) { 
        case 0: //Error message
            resultMsg = "Error by with SampleCountZero";
            break;
        case 1: //Hit message
            resultMsg = "Hitted by " +  " has value " +  this.logicalOp + " " + String.valueOf(this.value) + " " +  this.units;
            break;
        case 2: //Fail message
            resultMsg = "Failed by " + " has no value "  + this.logicalOp + " " + String.valueOf(this.value) + " " + this.units;
            break;
        case 3:
            resultMsg = "Error:unknow exception:";
            break;
        default:
            resultMsg = "InitMessage:";
            break;            
        }
        return resultMsg;

    }
}
