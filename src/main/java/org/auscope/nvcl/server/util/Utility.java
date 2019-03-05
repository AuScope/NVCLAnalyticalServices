package org.auscope.nvcl.server.util;

import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.http.client.HttpClient;


/**
 * This utility program provides useful method for validating the URL parameters
 *
 * @author Lingbo Jiang
 * @author Florence Tan
 * @author Peter Warren (CSIRO Earth Science and Resource Engineering)
 *
 */

public class Utility {
    private static HttpClient httpClient = null;    
	
	public static int pallet[] = {255,767,1279,1791,2303,3071,3583,4095,4863,5375,5887,6655,7167,7935,8447,9215,9727,10495,11007,11775,12543,13055,13823,14591,15103,15871,16639,17407,18175,18943,19711,20223,20991,21759,22527,23295,24319,25087,25855,26623,27391,28159,29183,29951,30719,31743,32511,33279,34303,35071,36095,37119,37887,38911,39679,40703,41727,42751,43519,44543,45567,46591,47615,48639,49663,50687,51711,52735,53759,54783,56063,57087,58111,59391,60415,61439,62719,63743,65023,65532,65527,65523,65518,65513,65509,65504,65499,65494,65489,65485,65480,65475,65470,65465,65460,65455,65450,65445,65439,65434,65429,65424,65419,65413,65408,65403,65398,65392,65387,65381,65376,65371,65365,65360,65354,65349,65343,65338,65332,65327,65321,65316,65310,65305,65299,65293,65288,65282,196352,589568,917248,1310464,1703680,2031360,2424576,2752256,3145472,3473152,3866368,4194048,4587264,4914944,5308160,5635840,6029056,6356736,6684416,7077632,7405312,7798528,8126208,8453888,8781568,9174784,9502464,9830144,10157824,10485504,10878720,11206400,11534080,11861760,12189440,12517120,12844800,13172480,13500160,13762304,14089984,14417664,14745344,15073024,15335168,15662848,15990528,16252672,16580352,16776448,16775168,16774144,16772864,16771840,16770816,16769536,16768512,16767488,16766208,16765184,16764160,16763136,16762112,16761088,16760064,16759040,16758016,16756992,16755968,16754944,16754176,16753152,16752128,16751104,16750336,16749312,16748544,16747520,16746496,16745728,16744704,16743936,16743168,16742144,16741376,16740608,16739584,16738816,16738048,16737280,16736512,16735744,16734720,16733952,16733184,16732416,16731648,16731136,16730368,16729600,16728832,16728064,16727296,16726528,16726016,16725248,16724480,16723968,16723200,16722432,16721920,16721152,16720640,16719872,16719360,16718592,16718080,16717312,16716800,16716288,16715520,16715008,16714496,16713728,16713216,16712704,16712192,16711680};
    /**
     * Convert BGR Colour to Java Colour
     *
     * @param BGRColorNumber
     *            colour code in BGR format
     * @return Color colour code in Java Colour format
     */
    public static Color BGRColorToJavaColor(int BGRColorNumber) {
        // colour codes as a in form "BGR"
        return new Color((BGRColorNumber & 255), (BGRColorNumber & 65280) >> 8,
                (BGRColorNumber >> 16));
    }

    public static int getMaxValue(int[] numbers) {
        int maxValue = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] > maxValue) {
                maxValue = numbers[i];
            }
        }
        return maxValue;
    }

    public static int getMinValue(int[] numbers) {
        int minValue = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] < minValue) {
                minValue = numbers[i];
            }
        }
        return minValue;
    }

    public static boolean stringIsBlankorNull(String str) {
    	int strLen;
    	if (str == null || (strLen = str.length()) == 0) {
    		return true;
    	}
    	for (int i = 0; i < strLen; i++) {
    		if ((Character.isWhitespace(str.charAt(i)) == false)) {
    			return false;
    		}
    	}
    	return true;
    }


    public static boolean ValidateEmail(String email) {
    	boolean validEmail = false;
    	
    	if(Utility.stringIsBlankorNull(email)) return false;
    	
    	Pattern pattern = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*((\\.[A-Za-z]{2,}){1}$)");
    	Matcher matcher = pattern.matcher(email);

	    if (matcher.matches()) {
	    	validEmail = true;
	    }else {
	    	validEmail = false;;
	    }
	    
	    return validEmail;
    	
    }
    
    /**
     * Basic validation used for GUIDs
     * @param	str		input String
     * @return	boolean	return true if the input String is not null or empty and contains only letters, numbers and hyphens
     */
    public static boolean isAlphanumericOrHyphen(String str){

    	if(Utility.stringIsBlankorNull(str)) return false;

    	Pattern pattern = Pattern.compile("^[0-9a-zA-Z-]+$");
    	Matcher matcher = pattern.matcher(str);
    	if (!matcher.matches()) return false;

    	return true;

    }
    /**
     * Basic validation used for algorithmOutputIDs
     * @param   algorithmOutputIDs    input String "701,702,703"
     * @return  boolean return true if the input String is not null or empty and contains only numbers.
     */    
    public static boolean checkAlgoutiIDs (String algorithmOutputIDs) {
        if(Utility.stringIsBlankorNull(algorithmOutputIDs)) return false;
        String[] stringArray = algorithmOutputIDs.split(",");
        if (stringArray.length < 1) return false;
        Pattern pattern = Pattern.compile("^[0-9]+$");
        for (int i = 0; i < stringArray.length; i++) {
           String numberAsString = stringArray[i];
           Matcher matcher = pattern.matcher(numberAsString);
           if (!matcher.matches()) return false;           
        }
        return true;
    }
    
    public static String floatArrayToString(float[] fltarray)
    {
		if(fltarray==null || fltarray.length<=0) return null;
    	StringBuffer str = new StringBuffer();
		for(int i=0;i<fltarray.length;i++)
		{
			str.append(String.format("%f", fltarray[i]));
			str.append(',');
		}
		str.delete(str.length()-1, str.length());
		return str.toString();
    }
    public static String getMD5HashValue() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss.SSS");
        String strDate = dateFormat.format(date);
        return DigestUtils.md5Hex(strDate);
    }
    public static String getHashValue() {
        Date date = new Date();
        return Integer.toHexString(date.hashCode());
    }  
    /**
     * Will take a url such as http://www.stackoverflow.com/abc/efg.html and return http://www.stackoverflow.com/
     * 
     * @param url
     * @return
     */    
    public static String getHost(String url) {
        if (url == null || url.length() == 0)
            return "";
    //    int doubleslash = url.indexOf("//");
    //    if (doubleslash == -1)
    //        doubleslash = 0;
     //   else
     //       doubleslash += 2;

    //    int end = url.indexOf('/', doubleslash);
     //   end = end >= 0 ? end : url.length();

      //  int port = url.indexOf(':', doubleslash);
     //   end = (port > 0 && port < end) ? port : end;

        String host = url.substring(0, url.lastIndexOf('/',url.lastIndexOf('/')-1)+1);
        return host;

    }
    /**
     * Will try parse a string to integer/
     * 
     * @param string of integer
     * @return true for integer false for wrong number format.
     */     
    public static boolean tryParseInt(String value) {  
        try {  
            Integer.parseInt(value);  
            return true;  
         } catch (NumberFormatException e) {  
            return false;  
         }  
   } 
    /**
     * Cast byte array to float array /
     * 
     * @param array of byte
     * @return array of float
     */     
    public static float[] getFloatSpectralData(byte[] spectraldata) {
        float[] specfloats = new float[spectraldata.length/4];
        for (int i=0;i<spectraldata.length;i+=4){
            ByteBuffer buffer = ByteBuffer.wrap(spectraldata,i,4).order(ByteOrder.LITTLE_ENDIAN);
            specfloats[i/4]= buffer.getFloat();
        }
        return specfloats;
    }
    /**
     * Get HttpClient with Proxy settings /
     * 
     * @param ProxyIP
     * @param ProxyPort
     * @return HttpClient with Proxy settings.
     */   
    /*  Proxy replaced by manual caching of web service results
    public static HttpClient getProxyHttpClient(String proxyIP,int proxyPort) {
        if (httpClient == null) {
            int connectionTimeOut = 90000;
            RequestConfig requestConfig = RequestConfig.custom().
                    setConnectTimeout(connectionTimeOut)
                    .setSocketTimeout(connectionTimeOut)
                    .build();
    
            HttpClientConnectionManager man = new PoolingHttpClientConnectionManager();
    
            httpClient = HttpClientBuilder.create()
                    .useSystemProperties()
                    .setConnectionManager(man)
                    .setDefaultRequestConfig(requestConfig)
                    .setProxy(new HttpHost(proxyIP,proxyPort))
                    .build();       
        }
        return httpClient;
    }*/
    public static String getCurrentTime() {
        String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH.mm.ss").format(new java.util.Date());
        return timeStamp;
    }
    
    public static boolean createDirectorys(String directoryPath) {
            Path path = Paths.get(directoryPath);
            //if directory exists?
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    //fail to create directory
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
    }

    /**
     * Utility for accessing a consistent DocumentBuilderFactory (irregardless of what is on the classpath)
     *
     * @return
     */
    private static DocumentBuilderFactory getDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(
                "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl", null);
        return factory;
}
    
    /**
     * Compiles the specified XPath (as a string) into an XPathExpression.
     *
     * @param xPathStr
     *            A string representing a valid XPath expression
     * @param nc
     *            The namespace that the xPathStr is referencing
     * @return
     * @throws XPathExpressionException
     */
    public static XPathExpression compileXPathExpr(String xPathStr, NamespaceContext nc)
            throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(nc);
        return xPath.compile(xPathStr);
    }

    /**
     * Compiles the specified XPath (as a string) into an XPathExpression.
     *
     * @param xPathStr
     *            A string representing a valid XPath expression
     * @return
     * @throws XPathExpressionException
     */
    public static XPathExpression compileXPathExpr(String xPathStr) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        return xPath.compile(xPathStr);
    }
    
    /**
     * Given a String containing XML, parse it and return a DOM object representation (that is namespace aware).
     *
     * @param xmlString
     *            A string containing valid XML
     * @return
     */
    public static Document buildDomFromString(String xmlString) throws ParserConfigurationException, IOException,
            SAXException {
        return buildDomFromString(xmlString, true);
    }

    /**
     * Given a String containing XML, parse it and return a DOM object representation
     *
     * @param xmlString
     *            A string containing valid XML
     * @param isNamespaceAware
     *            Will this DOM document take into account namespaces?
     * @return
     */
    public static Document buildDomFromString(String xmlString, boolean isNamespaceAware)
            throws ParserConfigurationException, IOException, SAXException {
        //build the XML dom
        DocumentBuilderFactory factory = getDocumentBuilderFactory();
        factory.setNamespaceAware(isNamespaceAware); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlString.toString()));
        Document doc = builder.parse(inputSource);
        return doc;
}
}
