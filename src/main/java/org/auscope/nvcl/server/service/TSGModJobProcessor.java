package org.auscope.nvcl.server.service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.TreeMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

/*
 * TSGModJobProcessor will process a TSGMod Analytical job(based on the live calculated scalar) from AnalyticalJobVo.
 *  
 * @author Linbo Jiang
 * @author Peter Warren
 */

public class TSGModJobProcessor  extends IJobProcessor{
	private static final Logger logger = LogManager.getLogger(TSGModJobProcessor.class);
    //private final Log log = LogFactory.getLog(getClass());      
    private TsgMod tsgMod = new TsgMod(); 
    private String tsgScript;
    private String dataPath;
    private String wvLogname = "Reflectance";
    private Integer countSumMax = 0;
    
	    
    /**
     * Constructor Construct all the member variables.
     * 
     */    
    public TSGModJobProcessor() {
        dataPath = NVCLAnalyticalRequestSvc.config.getDataPath();

    }
    public void run()
    {
    	logger.info("TSGModJobProcessor starting:" + this.serviceUrls);
        if (! getBoreholeList()) {
        	logger.error("Failed:processor.getBoreholeList");
          }
          if (!getDataCollection()) {
        	  logger.error("Failed:processor.getDataCollection");
            }
          if (!getSpectralData()) {
        	  logger.error("Failed:processor.getDownSampledData");            
          }
   
          logger.info("TSGModJobProcessor finished");
    }
    public void setAnalyticalJob(AnalyticalJobVo messageVo) {
        byte[] byteTsgScript = Base64.getDecoder().decode(messageVo.getTsgScript());
        this.tsgScript = new String(byteTsgScript);
        if (this.tsgScript.contains("Spectype = TIR")) {            
            this.wvLogname = "Base Refl";
        } else {
            this.wvLogname = "Reflectance";
        }
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
        	

            XPath xPath = XPathFactory.newInstance().newXPath();
        	
            XPathExpression exprMask = xPath.compile("DatasetCollection/Dataset/Logs/Log"); //"DatasetCollection/Dataset/SpectralLogs/SpectralLog");//
            NodeList nodeListMask = (NodeList) exprMask.evaluate(doc, XPathConstants.NODESET);
            XPathExpression exprLogIDMask = Utility.compileXPathExpr("LogID");
            XPathExpression exprLogNameMask = Utility.compileXPathExpr("logName");
            for (int j = 0; j < nodeListMask.getLength(); j++) {
                Element eleLogIDMask = (Element) exprLogIDMask.evaluate(nodeListMask.item(j), XPathConstants.NODE);
                String logid = eleLogIDMask.getFirstChild().getNodeValue(); 
                Element eleLogNameMask = (Element) exprLogNameMask.evaluate(nodeListMask.item(j), XPathConstants.NODE);
                String strLogNameMask = eleLogNameMask.getFirstChild().getNodeValue();     
                if (strLogNameMask.replaceAll(" ","").replaceAll("_","").compareToIgnoreCase("FinalMask")==0) {
                    strLogIDMask = logid;
                    logger.debug("final mask scalar found with id "+ strLogIDMask);
                    break;
                }
            }
        } catch ( Exception e) {
        	logger.warn("Couldn't get final mask log id.");
        }
        return strLogIDMask; 
    }
    
    /**
     * Get the base domain's logid from Document.
     * Only required if the final mask is missing
     *  @param doc    XML Document
     * return finalMaskLogid String 
     */    
    private String getBaseDomainLogid(Document doc) 
    {
        String strLogIDMask = null;
        try {
            XPathExpression exprMask;
            exprMask = Utility.compileXPathExpr("DatasetCollection/Dataset/Logs/Log"); //"DatasetCollection/Dataset/SpectralLogs/SpectralLog");//
            NodeList nodeListMask = (NodeList) exprMask.evaluate(doc, XPathConstants.NODESET);
            XPathExpression exprLogIDMask = Utility.compileXPathExpr("LogID");
            XPathExpression exprLogNameMask = Utility.compileXPathExpr("logName");
            for (int j = 0; j < nodeListMask.getLength(); j++) {
                Element eleLogIDMask = (Element) exprLogIDMask.evaluate(nodeListMask.item(j), XPathConstants.NODE);
                String logid = eleLogIDMask.getFirstChild().getNodeValue(); 
                Element eleLogNameMask = (Element) exprLogNameMask.evaluate(nodeListMask.item(j), XPathConstants.NODE);
                String strLogNameMask = eleLogNameMask.getFirstChild().getNodeValue();     
                if (strLogNameMask.toLowerCase().contains("mask") ) {//equalsIgnoreCase("Domain")) {
                    strLogIDMask = logid;
                    logger.debug("Domain scalar found with id "+ strLogIDMask);
                    break;
                }
            }
        } catch ( Exception e) {
        	logger.warn("Couldn't get Domain log id.");
        }
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
            //test for andulsite only
            logger.debug(holeIdentifier);
            // if (!holeIdentifier.contains("WTB5")) // 44653"))
            //   continue;
            // ///////////////////////
            try {
                String responseString = NVCLAnalyticalRequestSvc.dataAccess.getDatasetCollection(nvclDataServiceUrl, holeIdentifier);
                Document responseDoc = Utility.buildDomFromString(responseString);
                XPathExpression expr = Utility.compileXPathExpr("DatasetCollection/Dataset/SpectralLogs/SpectralLog");//DatasetCollection/Dataset/Logs/Log");//
                NodeList nodeList = (NodeList) expr.evaluate(responseDoc, XPathConstants.NODESET);
                if ( nodeList.getLength() == 0) {
                    logger.debug("getDataCollection: No dataset for serviceUrl:" + nvclDataServiceUrl + " :boreholeid:" + holeIdentifier);                                            
                    continue;
                }
                XPathExpression exprLogID = Utility.compileXPathExpr("logID");
                XPathExpression exprLogName = Utility.compileXPathExpr("logName");
                XPathExpression exprSampleCount = Utility.compileXPathExpr("sampleCount");
                XPathExpression exprWavelengths = Utility.compileXPathExpr("wavelengths");
                boolean isError = true;

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
                    
                    if (intSampleCount > 0 && strLogName.equalsIgnoreCase(this.wvLogname)) {
                        boreholeVo.spectralLogList.add(new SpectralLogVo(strLogID,strSampleCount,strWavelengths,this.wvLogname));
                        isError = false;
                        totalLogids++;                        
                        logger.debug("getDataCollection:" + this.wvLogname + ":boreholeid:" + holeIdentifier + ":LogID:" + strLogID + ":" + strLogName + ":" + strSampleCount );
                        
                        
                        //get final_mask logid
                        String finalMaskLogid = getFinalMaskLogid(responseDoc);
                        boreholeVo.setFinalMaskLogid(finalMaskLogid);                       

                        if (Utility.stringIsBlankorNull(finalMaskLogid))
                        {
                        	String domainLogid =getBaseDomainLogid(responseDoc);
                        	boreholeVo.setDomainLogid(domainLogid);
                        }
                        
                        break;
                        
                    }                         
                }
                if (isError) {
                    String resultMsg = formatMessage(0);
                    boreholeVo.setStatus(1); //error status;
                    jobResultVo.addErrorBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
                }

            }catch (Exception ex) {
                // if Exception happened, log it and let it continue for the next borehole.
                logger.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage2 for '%s' failed", nvclDataServiceUrl));
                String resultMsg = formatMessage(3) + ex.toString();
                boreholeVo.setStatus(1); //error status;
                jobResultVo.addErrorBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
            } 
        }
        logger.debug("getDataCollection:Total spectalLogs:" + totalLogids + ":" + this.serviceUrls);
        return true;
    }
    /**
     * getSpectralData
     * It download the SpetralData based on logid. then it called the TSGMod to do the caculation. 
     * It download the finalMask and apply the finalMask on result. 
     * It saved the result into jobResultVo(hit or missed)
     * @return true for successfully processing the information
     */     
    public boolean getSpectralData() {
        String resultMsg = "InitMessage";
        int totalProcessedLogid = 0;

        ////////
        logger.debug( "getSpectralData:" + this.serviceUrls);
        for (BoreholeVo boreholeVo : boreholeList) {
            String nvclDataServiceUrl = boreholeVo.getServiceHost() + boreholeVo.getServicePathOfData();
            if (boreholeVo.getStatus()!= 0) {
                logger.debug("skip: error borehole:" + boreholeVo.getHoleIdentifier() + ":status:" + boreholeVo.getStatus());
                continue;
            }
            String holeIdentifier = boreholeVo.getHoleIdentifier();
            String finalMaskLogid = boreholeVo.getFinalMaskLogid();
            String domainlogid = boreholeVo.getDomainLogid();
            boreholeVo.setStatus(1); //error status;

            boolean isHit = false;
            //assume only one spectra per borehole at the moment
            assert(boreholeVo.spectralLogList.size() == 1);
            for(SpectralLogVo spectralLog : boreholeVo.spectralLogList) {

                totalProcessedLogid++;
                String logid =  spectralLog.getLogID();
                int sampleCount = spectralLog.getSampleCount();
                float[] wvl = spectralLog.getWvl();
                int waveLengthCount = wvl.length;
                byte[] spectralData = new byte[sampleCount*waveLengthCount*4];
                double[] tsgRV = new double[sampleCount];
                ByteBuffer target = ByteBuffer.wrap(spectralData);     

                logger.debug("getSpectralData:start:BoreholeId:" + boreholeVo.getHoleIdentifier() + ":logid:" + logid);

                try {
                    int step = 4000;
                    logger.debug("getSpectralData:Downlaod Spectrum:" );
                    for (int i = 0; i < sampleCount; i = i + step) {
                        // LJ
                        // sample:http://geossdi.dmp.wa.gov.au/NVCLDataServices/getspectraldata.html?speclogid=baddb3ed-0872-460e-bacb-9da380fd1de
                        int start = i;
                        int end = (i + step > sampleCount) ? sampleCount - 1 : i + step - 1;
                        target.put(NVCLAnalyticalRequestSvc.dataAccess.getSpectralDataMethod(nvclDataServiceUrl, logid, start, end));

                    }
                    if (this.tsgScript.indexOf("outputFormat = Complex") < 0) { //TSG
                        // tsgMod.parseTSGScript(tsgRV, this.tsgScript, wvl, waveLengthCount, Utility.getFloatSpectralData2D(spectralData, waveLengthCount), sampleCount, value, (float) 0.2);
                        tsgMod.parseTSGScript(tsgRV, this.tsgScript, wvl, waveLengthCount, Utility.getFloatSpectralData(spectralData), sampleCount, value, (float) 0.2);

                        ////////
                        logger.debug("getSpectralData:Call TsgMod:TSG");

                        isHit = getDownSampledData (tsgRV,sampleCount,nvclDataServiceUrl,holeIdentifier,finalMaskLogid,domainlogid );                        
                    } else { //TSA
                        String filePath = dataPath + jobid;
                        Utility.createDirectorys(filePath);
                        String host = Utility.getHostOnly(nvclDataServiceUrl);
                        String fileFullPath = filePath + "/" + holeIdentifier + "-TsaScalar-" + host +".csv";
                        isHit = tsgMod.parseTSAScript(fileFullPath,tsgScript, wvl, waveLengthCount, Utility.getFloatSpectralData2D(spectralData, waveLengthCount),sampleCount); //sampleCount);
                        logger.debug("getSpectralData:Call TsgMod:TSA");
                    }

                    ////////
                    if (isHit) {
                        resultMsg = formatMessage(1);
                        boreholeVo.setStatus(2); // hit status;
                        jobResultVo.addBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(), resultMsg));
                        logger.info("hit: " + boreholeVo.getHoleUrl());
                        break;

                    }
                    
                } catch (Exception ex) {
                    // if exception happened, let it continue for next logid
                    logger.warn(String.format("Exception:NVCLAnalyticalJobProcessor::processStage3 for borehole:'%s' logid: '%s' failed", holeIdentifier, logid));
                }
                spectralData = null;
                tsgRV = null;
                
            } //logid loop
            if(!isHit) {
                resultMsg = formatMessage(2);
                boreholeVo.setStatus(3); //Failed status;
                jobResultVo.addFailedBoreholes(new BoreholeResultVo(boreholeVo.getHoleUrl(),resultMsg ));
             //   logger.info("Miss: " +boreholeVo.getHoleIdentifier());
            }         
        }//borehole loop  
        logger.debug( "getSpectralData:total Processed Logid:" + totalProcessedLogid );   

        return true;
    }
    
    public boolean getDownSampledData(double[] tsgRV, int sampleCount, String nvclDataServiceUrl, String holeIdentifier,String finalMaskLogid, String domainlogid) {
        boolean isHit = false;
        logger.debug("getDownSampledData:getFinalMask with id : "+finalMaskLogid);
        try {
        	TreeMap<String, Boolean> depthMaskMap = new TreeMap<String, Boolean>();
            TSGScalarArrayVo scalarArray = new TSGScalarArrayVo(this.span);
        	if (!Utility.stringIsBlankorNull(finalMaskLogid))
        	{

	            String strMask = NVCLAnalyticalRequestSvc.dataAccess.getScalarData(nvclDataServiceUrl, finalMaskLogid);

	            String csvLine;
	    
	            BufferedReader csvBuffer = new BufferedReader(new StringReader(strMask));
	            // startDepth, endDepth, final_mask(could be null)

	            csvLine = csvBuffer.readLine();// skip the header
        	
	            int index = 0;

	            try {	
	                while ((csvLine = csvBuffer.readLine()) != null) {

	                    List<String> cells = Arrays.asList(csvLine.split("\\s*,\\s*"));
	                    String depth = cells.get(0);
	                    boolean mask = false;
	                  //Lingbo some of mask could be "null" as well beside "0" or "1" 
	                    if (!cells.get(2).equalsIgnoreCase("0")){
	                        mask = true;
	                    }
	                    scalarArray.add(new TSGScalarVo(depth,mask,tsgRV[index]));
	                    depthMaskMap.put(depth, mask);
	                    index++;
	                }
				}					
				catch (Exception e) {
	                logger.error("Exception: on getDownSampledData.parseCSV service url = "+nvclDataServiceUrl+" masklogid=" + finalMaskLogid);
	            }
	            csvBuffer = null;
	            logger.debug(index + " lines of mask values read ");  
        	}
        	else if (!Utility.stringIsBlankorNull(domainlogid))
        	{

	            String strDomain = NVCLAnalyticalRequestSvc.dataAccess.getScalarData(nvclDataServiceUrl, domainlogid);
	            	    
	            String csvLine;
	    
	            BufferedReader csvBuffer = new BufferedReader(new StringReader(strDomain));
	            // startDepth, endDepth, domain

	            csvLine = csvBuffer.readLine();// skip the header
        	
	            int index = 0;

	            try {	
					while ((csvLine = csvBuffer.readLine()) != null) {

	                    List<String> cells = Arrays.asList(csvLine.split("\\s*,\\s*"));
	                    String depth = cells.get(0);
	                    
	                    scalarArray.add(new TSGScalarVo(depth,true,tsgRV[index]));
	                    depthMaskMap.put(depth, true);
	                    index++;
	                } 
	            }
				catch (Exception e) {
	                logger.error("Exception: on getDownSampledData.parseCSV service url = "+nvclDataServiceUrl+" domlogid=" + finalMaskLogid);
	            }  
	            csvBuffer = null;
	            logger.debug(index + " lines of domain values read ");  
        	}
        	else throw new Exception("no Final Mask or Domain scalar was available to provide the required depth values.");
            logger.debug( "getDownSampledData:downSample:");
            
			try {
	            scalarArray.downSample();
			}
			catch (Exception e) {
				logger.error("Exception: on scalarArray.downSample()"+e);
			}
			try {
				isHit = scalarArray.query(this.units, this.logicalOp,this.value);
			}
			catch (Exception e) {
				logger.error("Exception: on evaluating comparison result"+e);
			}
            logger.debug( Utility.getCurrentTime() + "getDownSampledData:writeCSV:");
            String host = Utility.getHostOnly(nvclDataServiceUrl);
            String filePath = dataPath + this.jobid;
            Utility.createDirectorys(filePath);
            String fileFullPath = filePath + "/" + holeIdentifier;
            scalarArray.writeScalarCSV(fileFullPath + "-tsgScalar.csv");
            scalarArray.writeDownSampledScalarCSV(fileFullPath + "-tsgScalarDownSampled-" + host + ".csv");
            this.countSumMax = scalarArray.queryMaxCountSum();
            scalarArray = null;
        } catch (Exception e) {
            logger.error("Exception: on getDownSampledData general failure "+e);
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
            resultMsg = "Hit with value " +  this.logicalOp + " " + String.valueOf(this.value) + " " +  this.units;
            break;
        case 2: //Fail message
            resultMsg = "Failed with no value "  + this.logicalOp + " " + String.valueOf(this.value) + " " + this.units;
            break;
        case 3:
            resultMsg = "Error:unknow exception:";
            break;
        default:
            resultMsg = "InitMessage:";
            break;            
        }
        return  String.valueOf(this.countSumMax) + ","+ resultMsg;

    }
}
