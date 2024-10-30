package org.auscope.nvcl.server.service;

import java.io.File;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.http.HttpServiceCaller;
import org.auscope.nvcl.server.http.NVCLDataServiceMethodMaker;
import org.auscope.nvcl.server.http.NVCLDataServiceMethodMaker.ResultType;
import org.auscope.nvcl.server.util.Utility;

public class DataAccess {

	private static final Logger logger = LogManager.getLogger(DataAccess.class);
	private static final String getDatasetCollectionPath = "getDSCol";
	private static final String getSpectralData = "getSpecData";
	private static final String getScalarData = "getScalarData";
	private static final String getDownSampScalarData = "getDownSampScalarData";
	private String cachePath;
	private String bhInfoUrl;
	protected NVCLDataServiceMethodMaker nvclMethodMaker;
	protected HttpServiceCaller httpServiceCaller;

	public String getCachePath() {
		return cachePath;
	}

	public void setCachePath(String cachePath) {
		this.cachePath = cachePath;
	}
	public String getbhInfoUrl() {
		return bhInfoUrl;
	}

	public void setbhInfoUrl(String bhInfoUrl) {
		this.bhInfoUrl = bhInfoUrl;
	}
	public DataAccess() {
		super();
		this.nvclMethodMaker = new NVCLDataServiceMethodMaker();
		this.httpServiceCaller = new HttpServiceCaller(90000);
	}
	
	private static String makeHostnameFileSystemSafe(String hostname){
		String result = hostname.replace("https://", "").replace("http://", "").replaceAll("\\W+", "");
		return result;
	}

	private String makeHoleIdentifierFileSystemSafe(String inputName) {
		return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
	}

	public String getDatasetCollection(String nvclDataServiceUrl, String holeIdentifier)
			throws ConnectException, ConnectTimeoutException, UnknownHostException, Exception {
		String host = makeHostnameFileSystemSafe(Utility.getHost(nvclDataServiceUrl));
		File f = new File(cachePath + "//" + getDatasetCollectionPath + "//" + host + "//" + makeHoleIdentifierFileSystemSafe(holeIdentifier));
		if (f.exists() && !f.isDirectory()) {
			logger.debug("Read dataset info from cache for hole " + host + ":" + holeIdentifier);
			return FileUtils.readFileToString(f,Charset.defaultCharset());
		} else {
			HttpRequestBase method = nvclMethodMaker.getDatasetCollectionMethod(nvclDataServiceUrl, holeIdentifier);

			String result = httpServiceCaller.getMethodResponseAsString(method);
			if(!Utility.stringIsBlankorNull(result)) FileUtils.writeStringToFile(f, result,Charset.defaultCharset());
			logger.debug("fetched and saved to cache dataset info for hole " + host + ":" + holeIdentifier);
			method.releaseConnection();

			return result;
		}
	}

	public byte[] getSpectralDataMethod(String nvclDataServiceUrl, String logid, int start, int end) throws Exception {
		String host = makeHostnameFileSystemSafe(Utility.getHost(nvclDataServiceUrl));
		File f = new File(cachePath + "//" + getSpectralData + "//" + host + "//" + logid + "//" + start + "_" + end);
		if (f.exists() && !f.isDirectory()) {
			logger.debug("Read spectral data from cache for logid " + host + ":" + logid + " start:" + start + " end:"
					+ end);
			return FileUtils.readFileToByteArray(f);
		} else {
			HttpRequestBase methodSpectralData = nvclMethodMaker.getSpectralDataMethod(nvclDataServiceUrl, logid, start,
					end);
			logger.debug("starting request");
			byte[] result = httpServiceCaller.getMethodResponseAsBytes(methodSpectralData);
			logger.debug("going to sleep");
			Thread.sleep(2000);
			logger.debug("waking up!");
			if(result.length>0) FileUtils.writeByteArrayToFile(f, result);
			logger.debug("fetched and saved to cache spectral data for logid " + host + ":" + logid + " start:" + start
					+ " end:" + end);
			methodSpectralData.releaseConnection();

			return result;
		}
	}

	public String getScalarData(String nvclDataServiceUrl, String logid)
			throws ConnectException, ConnectTimeoutException, UnknownHostException, Exception {
		String host = makeHostnameFileSystemSafe(Utility.getHost(nvclDataServiceUrl));
		File f = new File(cachePath + "//" + getScalarData + "//" + host + "//" + logid);
		if (f.exists() && !f.isDirectory()) {
			logger.debug("Read scalar data from cache for logid " + host + ":" + logid);
			return FileUtils.readFileToString(f,Charset.defaultCharset());
		} else {
			HttpRequestBase methodMask = nvclMethodMaker.getDownloadScalarsMethod(nvclDataServiceUrl, logid);

			String result = httpServiceCaller.getMethodResponseAsString(methodMask);
			if(!Utility.stringIsBlankorNull(result)){
				FileUtils.writeStringToFile(f, result,Charset.defaultCharset());
				logger.debug("fetched and saved to cache scalar data for logid " + host + ":" + logid);
			}
			else {
				logger.debug("failed to get mask data with id "+logid+" from host"+host);
			}
			methodMask.releaseConnection();
			return result;
		}
	}

	public String getDownSampledDataMethod(String nvclDataServiceUrl, String logid, float span, int startDepth,
			int endDepth, String outputFormat) throws ConnectException, ConnectTimeoutException, UnknownHostException, Exception {
		String host = makeHostnameFileSystemSafe(Utility.getHost(nvclDataServiceUrl));
		File f = new File(cachePath + "//" + getDownSampScalarData + "//" + host + "//" + logid+"//"+span+"//"+startDepth+"_"+endDepth+"."+outputFormat);
		if (f.exists() && !f.isDirectory()) {
			logger.debug("Read down sampled scalar data from cache for logid " + host + ":" + logid + " start:" + startDepth + " end:"+endDepth +" outputformat:"+outputFormat);
			return FileUtils.readFileToString(f,Charset.defaultCharset());
		} else {

			HttpRequestBase method = nvclMethodMaker.getDownSampledDataMethod(nvclDataServiceUrl, logid, span, startDepth,
					endDepth, outputFormat);
			String result = httpServiceCaller.getMethodResponseAsString(method);
			if(!Utility.stringIsBlankorNull(result)) FileUtils.writeStringToFile(f, result,Charset.defaultCharset());
			logger.debug("fetched and saved to cache down sampled scalar data for logid " + host + ":" + logid+ " start:" + startDepth + " end:"+endDepth +" outputformat:"+outputFormat);
			method.releaseConnection();
			return result;
		}
	}

	public String makeWFSGetFeaturePostMethod(String serviceUrl, String wfsVersion, String outputFormat, String layerName, String filter, int maxFeatures, int startIndex) throws ConnectException, ConnectTimeoutException, UnknownHostException, Exception {
		HttpPost method = (HttpPost) nvclMethodMaker.makePostMethod(serviceUrl,  wfsVersion, layerName, filter, maxFeatures, null,ResultType.Results, outputFormat, startIndex);
		System.out.println("debug:");
		System.out.println(method.toString());

		// this result will not be cached as it will change regularly and is only performed once per serviceUrl anyway
		int retryCount = 0;
		boolean isSuccess = false;
		String result = "";
		while( retryCount < 5 && isSuccess == false) {
			try{
				retryCount += 1;
				result = this.httpServiceCaller.getMethodResponseAsString(method);
				isSuccess = true;
			} catch (Exception e) {
				logger.debug("RETRY: makeWFSGetFeaturePostMethod: "+retryCount);
				Thread.sleep(30 * 1000);
			}
		}
		if (isSuccess == false) {
			throw new Exception("Giveup try after 5 times");
		}
		method.releaseConnection();
		return result;
	}

    public String getNVCLBHInfoCSV() {
		HttpGet method;
		String result = null;
		try {
			method = (HttpGet) nvclMethodMaker.getMethod(this.bhInfoUrl);
			result = this.httpServiceCaller.getMethodResponseAsString(method);
			method.releaseConnection();		
		} catch (Exception e) {
			logger.debug("ERROR: getNVCLBHInfoCSV:");
		}
		return result;
    }

}
