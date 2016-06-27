package org.auscope.nvcl.server.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HttpCaller {

        private final String USER_AGENT = "Mozilla/5.0";

        public static void main(String[] args) throws Exception {

            HttpCaller http = new HttpCaller();
           // System.out.println("Testing 1 - Send Http GET request");
            //http.sendGet();            
            System.out.println("\nTesting 2 - Send Http POST request");
            String serviceUrl = "http://nvclwebservices.vm.csiro.au/geoserverBH/ows?service=WFS";
//            String requestBody = "&version=1.1.0&request=GetFeature&typeName=gsmlp:BoreholeView&maxFeatures=50";
            String requestBody = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:er=\"urn:cgi:xmlns:GGIC:EarthResource:1.1\" xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML:2.0\" > " +
"<wfs:Query typeName=\"gsmlp:BoreholeView\" srsName=\"EPSG:4326\">"+
"<ogc:Filter><PropertyIsEqualTo> <PropertyName>nvclCollection</PropertyName><Literal>true</Literal></PropertyIsEqualTo></ogc:Filter></wfs:Query></wfs:GetFeature>";
            http.sendPost(serviceUrl,requestBody);
        }

        // HTTP GET request
        private void sendGet() throws Exception {

            String url = "http://www.google.com/search?q=mkyong";
            
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());

        }    
    // HTTP POST request
    private void sendPost(String serviceUrl, String requestBody) throws Exception {
        try {
            String url = serviceUrl;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type",
                    "application/xml;charset=utf-8");
            String urlParameters = requestBody;//"<Request xmlns=\"abc\"><ID>1</ID><Password></Password></Request>";
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            String responseStatus = con.getResponseMessage();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println("urlParameters:" + urlParameters);            
            System.out.println("response:" + response.toString());            

        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
//        URL obj = new URL(serviceUrl);
//        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//
//        // add reuqest header
//        con.setRequestMethod("POST");
//        con.setRequestProperty("Accept", "application/xml");
//        con.setRequestProperty("Content-Type", "application/xml; charset=\"utf-8\"");                 
//        con.setRequestProperty("Content-Length", "" + Integer.toString(requestBody.getBytes().length));
//        con.setRequestProperty("Content-Language", "en-US");  
//        con.setUseCaches (false);
//        // Send post request
//        con.setDoOutput(true);
//        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
//        wr.writeBytes(requestBody);
//        wr.flush();
//        wr.close();
//
//        int responseCode = con.getResponseCode();
//        System.out.println("\nSending 'POST' request to URL : " + serviceUrl);
//        System.out.println("Post parameters : " + requestBody);
//        System.out.println("Response Code : " + responseCode);
//
//        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//        String inputLine;
//        StringBuffer response = new StringBuffer();
//
//        while ((inputLine = in.readLine()) != null) {
//            response.append(inputLine);
//        }
//        in.close();
//
//        // print result
//        System.out.println(response.toString());

    }
}