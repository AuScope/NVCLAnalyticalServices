package org.auscope.nvcl.server.service;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import org.apache.http.client.methods.HttpPost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.http.NVCLDataServiceMethodMaker;
import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobResultVo;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.auscope.nvcl.server.vo.BoreholeVo;
import org.auscope.portal.core.server.http.HttpServiceCaller;
import org.auscope.portal.core.services.methodmakers.WFSGetFeatureMethodMaker;
import org.auscope.portal.core.services.responses.ows.OWSExceptionParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
	protected WFSGetFeatureMethodMaker wfsMethodMaker;
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
		this.wfsMethodMaker = new WFSGetFeatureMethodMaker();
		this.nvclMethodMaker = new NVCLDataServiceMethodMaker();

	}

	protected void setAlgoutidList(String algorithmOutputID) {
		if (Utility.stringIsBlankorNull(algorithmOutputID))	return;
		String[] stringArray = algorithmOutputID.split(",");
		algoutidList.clear();
		for (int i = 0; i < stringArray.length; i++) {
			algoutidList.add(Integer.parseInt(stringArray[i]));
		}
		return;
	}

	protected void setServiceUrls(String serviceUrls) {
		if (Utility.stringIsBlankorNull(serviceUrls)) return;
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

	public boolean getBoreholeList() {
		logger.debug("getting list of boreholes from service urls :" + this.serviceUrls);
		HttpPost method = null;
		for (String serviceUrl : this.serviceUrlsList) {
			String serviceHost = Utility.getHost(serviceUrl);
			String servicePathOfData;
			// NSW NVCL dataservices are running on the NVCLDownloadServices path
			if (serviceUrls.contains("auscope.dpi.nsw.gov.au")) {
				servicePathOfData = "NVCLDownloadServices/";
			} else {
				servicePathOfData = "NVCLDataServices/";
			}
			try {
				method = (HttpPost) this.wfsMethodMaker.makePostMethod(serviceUrl, this.layerName, this.filter, 0);
				String responseString = httpServiceCaller.getMethodResponseAsString(method);

		        Document responseDoc = Utility.buildDomFromString(responseString);
				OWSExceptionParser.checkForExceptionResponse(responseDoc);
				//NVCLNamespaceContext nc = new NVCLNamespaceContext();

				XPathExpression exp = Utility.compileXPathExpr("/*[local-name() = 'FeatureCollection']/*[local-name() = 'featureMembers']/*[local-name() = 'BoreholeView']/*[local-name() = 'identifier']");
				NodeList publishedDatasets = (NodeList) exp.evaluate(responseDoc, XPathConstants.NODESET);
				logger.debug(publishedDatasets.getLength() + " boreholes returned from " + serviceHost);
				for (int i = 0; i < publishedDatasets.getLength(); i++) {
					Element eleHoleUrl = (Element) publishedDatasets.item(i);
					String holeUrl = eleHoleUrl.getFirstChild().getNodeValue();
					if (holeUrl != null) {
						String[] urnBlocks = holeUrl.split("/");
						if (urnBlocks.length > 1) {
							String holeIdentifier = urnBlocks[urnBlocks.length - 1];
							boreholeList.add(new BoreholeVo(holeIdentifier, holeUrl, serviceUrl, serviceHost,
									servicePathOfData));
						}
					}

				}
			} catch (Exception ex) {
				logger.error("IJobProcessor::processStage1 for " + serviceUrl + " failed");
			} finally {
				if (method != null) {
					method.releaseConnection();
				}
			}
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
