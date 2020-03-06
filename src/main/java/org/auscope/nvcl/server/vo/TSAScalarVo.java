package org.auscope.nvcl.server.vo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import org.json.JSONObject;

/**
 * TSAScalarVo and allow getting the values thru getter and setter method.
 * 
 * qtx={ "tsgresult": "SWIR TSA", "level": "Mineral", "status": "Ok", "nmix": 0,
 * "srss": 156.114932, "index": [15, 5], "prop": [0.553905, 0.446095] }
 * 
 * @author Lingbo Jiang
 */
public class TSAScalarVo {
    //private String scalar;
    //private String header;
    private Map<String, String> hashMap = new HashMap();

    public TSAScalarVo(String scalar, String sampleNumber) {
        try {
            //this.scalar = sampleNumber + '|';
            //this.header = "sampleNumber|";
            this.hashMap.put("sampleNumber", sampleNumber);

            JSONObject jsonObject = new JSONObject(scalar);

            Iterator<String> keys = jsonObject.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                String valueS = value.toString();
                hashMap.put(key, valueS);
                //this.scalar += (valueS + '|') ;
                //this.header += (key + '|');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }        
    }

    // public String getScalar() {
    //     return this.scalar;
    // } 
    public String[] getHeader() {
        Iterator it = this.hashMap.entrySet().iterator();
        String [] rowHeader = new String[this.hashMap.size()];
        int i = 0;
        while (it.hasNext()) {
          Map.Entry pair = (Map.Entry)it.next();
          rowHeader[i++] = pair.getKey().toString();
        }        
        return rowHeader;
    } 
    public String get(String key) {
        return this.hashMap.get(key);
    }   
}