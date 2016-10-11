package org.auscope.nvcl.server.service;


import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
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

/*
 * TSGModJobProcessor will process a analytical job by using tsgMod.
 *  
 * @author Linbo Jiang
 * @author Peter Warren
 */

public class TSGModJobProcessor  extends IJobProcessor{
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
    private boolean isdecimalTypeScalar;
    private List<BoreholeVo> boreholeList = new ArrayList<BoreholeVo>(); 
    private TsgMod tsgMod = new TsgMod();
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
    public TSGModJobProcessor() {
        this.jobResultVo = new AnalyticalJobResultVo();        
        this.httpServiceCaller = new HttpServiceCaller(90000);
        this.wfsMethodMaker = new WFSGetFeatureMethodMaker();
        this.nvclMethodMaker = new NVCLDataServiceMethodMaker();
        
        
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
          if (!getSpectralData()) {
              System.out.println("Failed:processor.getDownSampledData");            
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
                System.out.println(responseString);
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
                        System.out.println("Reflectance:LogID:" + strLogID + ":" + strLogName + ":" + strSampleCount + ":" + strWavelengths); 
                    } else {
                        System.out.println("LogID:" + strLogID + ":" + strLogName + ":" + strSampleCount + ":" + strWavelengths); 
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
            //For debug purpose only
//            if (totalLogids > 0)
//                break;
        }
        System.out.println("Total spectalLogs:" + totalLogids + ":" + this.serviceUrls);
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
                System.out.println("getSpectralData:BoreholeId:" + boreholeVo.getHoleIdentifier() + ":logid:" + logid);
                //System.out.println("Stage3:process:borehole:" + holeIdentifier + "  logid:" + logid);
                try {
                    int step = 4000;
                    for (int i=0;i<sampleCount;i=i+step) {
                    //LJ sample:http://geossdi.dmp.wa.gov.au/NVCLDataServices/getspectraldata.html?speclogid=baddb3ed-0872-460e-bacb-9da380fd1de
                        int start = i;
                        int end = (i+step > sampleCount)? sampleCount-1:i+step-1;
                        int count = end - start +1;
                        HttpRequestBase methodSpectralData =nvclMethodMaker.getSpectralDataMethod(nvclDataServiceUrl,logid,start, end);
                        target.put(httpServiceCaller.getMethodResponseAsBytes(methodSpectralData,Utility.getProxyHttpClient("130.116.24.73",3128)));
                        methodSpectralData.releaseConnection();  
                        System.out.println("tsgProcessed:start:" + start + ":end:" + end + ":count:" + count);
                    //tsgMod.parseOneScalarMethod(null, wvl, waveLengthCount , Utility.getFloatspectraldata(spectralData),sampleCount);                    
                    }
                    tsgMod.parseOneScalarMethod(null, wvl, waveLengthCount , Utility.getFloatSpectralData(spectralData),sampleCount);   
                    
                    
                            //getspectraldata (logid) -> binary stream of numberofwvls*samplecount*4 bytes   ->java float array
                            //nvclMethodMaker.getDownSampledDataMethod(nvclDataServiceUrl, logid, span, startDepth, endDepth, "csv");

                    HttpRequestBase methodMask =nvclMethodMaker.getDownloadScalarsMethod(nvclDataServiceUrl, logid);
                    String strMask = "";//httpServiceCaller.getMethodResponseAsString(methodMask);
                    methodMask.releaseConnection();                    
                    //String spectralData = httpServiceCaller.getMethodResponseAsString(methodSpectralData);

                    
                    //String strFloat[] = spectralData.split("(?<=\\G....)");
                    
                    
//                    float:-1.174494E29
//                    float:-0.09371766
//                    float:-1.174494E29
//                    float:-1.174494E29
//                    float:-0.09371778
//                    float:7.1685425E24
//                    float:-1.174494E29
//                    float:0.18270138
//                    float:0.18979286
//                    float:7.5167616E30
//                    float:0.20169362
//                    float:1.5374046E-25
//                    float:6.5197514E12
//                    float:1.04316023E14
//                    float:4.17264092E14
//                    float:1.66905637E15
//                    float:-5.891011E28
//                    float:1.4664491E-31
//                    float:1.06819608E17
//                    float:1.06819608E17
//                    float:2.2914148E-33
//                    float:6.8364549E18
//                    float:-5.8917293E28
//                    float:2.734582E19
//                    float:-5.8919446E28
                    
                    
//                    for (String s:strFloat){
//                        byte[] bytes = s.getBytes("UTF-8");
//                        Float f = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                        System.out.println("float:" + f);                        
//                    }
                    
                    
                    
                    String csvLine;

                    BufferedReader csvBuffer = new BufferedReader(new StringReader(strMask)); 
                    //startDepth, endDepth, final_mask(could be null)
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
                        //count = Float.parseFloat(cells.get(1));
                         //   csvClassfication="averageValue";

                     }
                    System.out.println("lines read " +linesread);
                    if (isHit) {
                            resultMsg = "Hitted by " + this.classification + " with value " ;
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
