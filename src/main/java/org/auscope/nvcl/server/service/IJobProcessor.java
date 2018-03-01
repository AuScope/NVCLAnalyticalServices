package org.auscope.nvcl.server.service;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
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
		HttpPost method = null;
		for (String serviceUrl : this.serviceUrlsList) {
			String serviceHost = Utility.getHost(serviceUrl);
			String servicePathOfData;
			// NSW NVCL dataservices are running on the NVCLDownloadServices
			// path
			if (serviceUrls.contains("auscope.dpi.nsw.gov.au")) {
				servicePathOfData = "NVCLDownloadServices/";
			} else {
				servicePathOfData = "NVCLDataServices/";
			}
			try {
				method = (HttpPost) this.makePostMethod(serviceUrl, this.layerName, this.filter, 0, null,
						ResultType.Results, null, null);

				String responseString = this.httpServiceCaller.getMethodResponseAsString(method);

				Document responseDoc = Utility.buildDomFromString(responseString);
				checkForExceptionResponse(responseDoc);

				XPathExpression exp = Utility.compileXPathExpr(
						"/*[local-name() = 'FeatureCollection']/*[local-name() = 'featureMembers']/*[local-name() = 'BoreholeView']/*[local-name() = 'identifier']");
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

	public static final String WFS_VERSION = "1.1.0";

	/**
	 * An enumeration of the values that can be used for the 'resultType'
	 * parameter
	 *
	 */
	public enum ResultType {
		/**
		 * Requests the full set of results be returned
		 */
		Results,
		/**
		 * Requests that only the count of the results be returned
		 */
		Hits
	}

	/**
	 * Creates a PostMethod given the following parameters.
	 *
	 * @param serviceUrl
	 *            - required, exception thrown if not provided
	 * @param featureType
	 *            - required, exception thrown if not provided
	 * @param filterString
	 *            - optional - an OGC Filter String
	 * @param maxFeatures
	 *            - Set to non zero to specify a cap on the number of features
	 *            to fetch
	 * @param srsName
	 *            - Can be null or empty
	 * @param resultType
	 *            - Can be null - The type of response set you wish to request
	 *            (default is Results)
	 * @param outputFormat
	 *            - Can be null - The format you wish the response to take
	 * @param startIndex
	 *            - This is for services that supports paging.
	 * @return
	 */
	public HttpRequestBase makePostMethod(String serviceUrl, String featureType, String filterString, int maxFeatures,
			String srsName, ResultType resultType, String outputFormat, String startIndex) {

		// Make sure the required parameters are given
		if (featureType == null || featureType.equals("")) {
			throw new IllegalArgumentException("featureType parameter can not be null or empty.");
		}

		if (serviceUrl == null || serviceUrl.equals("")) {
			throw new IllegalArgumentException("serviceUrl parameter can not be null or empty.");
		}

		HttpPost httpMethod = new HttpPost(serviceUrl);

		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append(String.format("<GetFeature service=\"WFS\" version=\"%1$s\"", WFS_VERSION));

		if (maxFeatures > 0) {
			sb.append(" maxFeatures=\"" + Integer.toString(maxFeatures) + "\"");
		}

		if (startIndex != null) {
			sb.append(" startIndex=\"" + startIndex + "\"");
		}

		if (resultType != null) {
			switch (resultType) {
			case Hits:
				sb.append(" resultType=\"hits\"");
				break;
			case Results:
				sb.append(" resultType=\"results\"");
				break;
			default:
				throw new IllegalArgumentException("Unknown resultType " + resultType);
			}
		}
		if (outputFormat != null && !outputFormat.isEmpty()) {
			sb.append(" outputFormat=\"" + outputFormat + "\"");
		}

		sb.append(">\n");
		sb.append("<Query typeName=\"" + featureType + "\"");

		if (srsName != null && !srsName.isEmpty()) {
			sb.append(" srsName=\"" + srsName + "\"");
		}
		sb.append(">");
		if (filterString != null) {
			sb.append(filterString.replaceAll("<[a-z]*:", "<").replaceAll("</[a-z]*:", "</"));
		}
		sb.append("</Query>\n");
		sb.append("</GetFeature>");

		logger.debug("Service URL:\n\t" + serviceUrl);
		logger.debug("Get Feature Query:\n" + sb.toString());

		// If this does not work, try params: "text/xml; charset=ISO-8859-1"
		httpMethod.setEntity(new StringEntity(sb.toString(), "UTF-8"));

		return httpMethod;
	}

	public void run() {
		logger.info("IJobProcessor starting :");
	}

}
