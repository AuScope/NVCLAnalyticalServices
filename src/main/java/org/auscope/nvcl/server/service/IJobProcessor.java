package org.auscope.nvcl.server.service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.http.HttpServiceCaller;
import org.auscope.nvcl.server.http.NVCLDataServiceMethodMaker;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.BoreholeVo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVParser;
/*
 * IJobProcessor the base class for JobProcessor
 * it is extends from Thread class. 
 * it process an analyticalJob against one serviceUrl only.  
 * it need setAnalyticalJob first before running the job.
 * the jobResult is saved in jobResultVo.
 *  
 * @author Peter Warren
 * @author Linbo Jiang
 */
public class IJobProcessor extends Thread {

	private static final Logger logger = LogManager.getLogger(IJobProcessor.class);

	protected HttpServiceCaller httpServiceCaller;
	protected NVCLDataServiceMethodMaker nvclMethodMaker;
	protected AnalyticalJobResultVo jobResultVo;

	protected String serviceUrls;
	protected String jobid;
	protected List<String> serviceUrlsList = new ArrayList<String>();
	protected String filter;
	protected int startDepth;
	protected int endDepth;
	protected String logName;
	protected String classification;
	protected String algorithmOutputID;// "128,12,34" string of integer array
	protected List<Integer> algoutidList = new ArrayList<Integer>();
	protected float span;
	protected String units;
	protected float value;
	protected String logicalOp;
	protected String layerName;
	protected String analyticalServiceUrl;
	protected List<BoreholeVo> boreholeList = new ArrayList<BoreholeVo>();

	/**
	 * Constructor Construct all the member variables.
	 * 
	 */
	public IJobProcessor() {
		this.jobResultVo = new AnalyticalJobResultVo();
		this.httpServiceCaller = new HttpServiceCaller(90000);
		this.nvclMethodMaker = new NVCLDataServiceMethodMaker();

	}

	protected void setAlgoutidList(String algorithmOutputID) {
		if (Utility.stringIsBlankorNull(algorithmOutputID))
			return;
		String[] stringArray = algorithmOutputID.split(",");
		algoutidList.clear();
		for (int i = 0; i < stringArray.length; i++) {
			algoutidList.add(Integer.parseInt(stringArray[i]));
		}
		return;
	}

	protected void setServiceUrls(String serviceUrls) {
		if (Utility.stringIsBlankorNull(serviceUrls))
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
		this.jobid = messageVo.getJobid();
		this.serviceUrls = messageVo.getServiceUrls();
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
		this.layerName = "gsmlp:BoreholeView";
	}

	/**
	 * Will attempt to parse an <ows:Exception> element where ows will be
	 * http://www.opengis.net/ows.
	 *
	 * Will throw an OWSException if document does contain an
	 * <ows:ExceptionReport>, otherwise it will do nothing
	 *
	 * @param doc
	 *            the doc
	 * @throws OWSException
	 *             the oWS exception
	 */
	public static void checkForExceptionResponse(Document doc) throws Exception {

		try {
			// Check for an exception response
			NodeList exceptionNodes = (NodeList) Utility
					.compileXPathExpr("/*[local-name() = 'ExceptionReport']/*[local-name() = 'Exception']")
					.evaluate(doc, XPathConstants.NODESET);
			if (exceptionNodes.getLength() > 0) {
				Node exceptionNode = exceptionNodes.item(0);

				Node exceptionTextNode = (Node) Utility.compileXPathExpr("*[local-name() = 'ExceptionText']")
						.evaluate(exceptionNode, XPathConstants.NODE);
				String exceptionText = (exceptionTextNode == null) ? "[Cannot extract error message]"
						: exceptionTextNode.getTextContent();
				String exceptionCode = (String) Utility.compileXPathExpr("@exceptionCode").evaluate(exceptionNode,
						XPathConstants.STRING);

				throw new Exception(String.format("Code='%1$s' Message='%2$s'", exceptionCode, exceptionText));
			}
		} catch (XPathExpressionException ex) {
			// This should *hopefully* never occur
			logger.error("Error whilst attempting to check for errors", ex);
		}
	}

	public boolean getBoreholeList() {
		logger.debug("getting list of boreholes from service urls :" + this.serviceUrls);

		for (String serviceUrl : this.serviceUrlsList) {
			String serviceHost = Utility.getHost(serviceUrl);
			String servicePathOfData = "NVCLDataServices/";
			int count = 0;
			String wfsVersion = "1.0.0";
			String outputFormat = "csv";
			String responseString;
			int startIndex = 0;
			int maxFeatures = 10000;
			int pageCount = maxFeatures;
			int totalCount = 0;
			int pages = 0 ;			
			if (this.filter.indexOf("ogc:Intersects") < 0) {
				//normal filter use 1.1.0
				startIndex = -1;
				maxFeatures = -1;
				wfsVersion = "1.1.0";
				pageCount = maxFeatures;
			} else {
				//polygon filter use 1.0.0
				startIndex = 0;
				maxFeatures = 10000;
				wfsVersion = "1.0.0";	
				pageCount = maxFeatures;
			}

			try {
				while (pageCount == maxFeatures){
					logger.info("getBoreholeList:page:"  + pages + ":pageCount:" + pageCount);
					pages++;
					pageCount = 0;
					responseString = NVCLAnalyticalRequestSvc.dataAccess.makeWFSGetFeaturePostMethod(serviceUrl, wfsVersion, outputFormat, this.layerName, this.filter, maxFeatures, startIndex);		
					startIndex += maxFeatures ;
					String csvLine[];
					CSVReader reader = new CSVReader(new StringReader(responseString), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.DEFAULT_ESCAPE_CHARACTER);
					csvLine = reader.readNext();//skip the header
					while ((csvLine = reader.readNext()) != null) {
						pageCount++;
						totalCount++;
						if (csvLine.length < 34) {
							logger.error("getBoreholeList:wrong csv:"  + serviceUrl + ":" + Arrays.toString(csvLine));
							continue;
						}
						String nvclCollection = csvLine[28];
						String holeUrl = csvLine[6];
						if (holeUrl != null && "true".equalsIgnoreCase(nvclCollection)) {
							String[] urnBlocks = holeUrl.split("/");
							if (urnBlocks.length > 1) {
								String holeIdentifier = urnBlocks[urnBlocks.length - 1];
								boreholeList.add(new BoreholeVo(holeIdentifier, holeUrl, serviceUrl, serviceHost, servicePathOfData));
								count++;
							}
						}					
					}
					reader.close();
					reader = null;
				}
			} catch (Exception ex) {
				logger.error("IJobProcessor::processStage1 for " + serviceUrl + " failed");
			} 
			logger.info("IJobProcessor getBoreholeList total:" + serviceUrl + ":count:" + count + ":totalCount:" + totalCount);

		}
		
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


	public void run() {
		logger.info("IJobProcessor starting :");
	}

}
