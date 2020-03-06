package org.auscope.nvcl.server.vo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import au.com.bytecode.opencsv.CSVWriter;

public class TSAScalarArrayVo {
	private static final Logger logger = LogManager.getLogger(TSAScalarArrayVo.class);
    public List<TSAScalarVo> scalarArray = new ArrayList<TSAScalarVo>();
    private Map<String, Integer> mapHeader = new HashMap(); 
    public TSAScalarArrayVo() {
    }
        
    public void add(TSAScalarVo tsaScalar) {
      String[] header = tsaScalar.getHeader();
      for (String prop : header) {
        if (mapHeader.get(prop) == null) {
          mapHeader.put(prop, 1);
        } 
      }
      scalarArray.add(tsaScalar);
    }
/*
    public int writeScalarCSV2(String fileName) {
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(fileName));
            boolean bHeader = false;
            String header = "";
            for (TSAScalarVo tsaScalar : scalarArray) {
              if (!bHeader) {
                header = tsaScalar.getHeader();
                writer.writeNext(header.split("\\|"));
                bHeader = true;
              } else {
                //logger.error("Wrong tsaScalar's header: "+tsaScalar.getHeader());
                assert(header.equalsIgnoreCase(tsaScalar.getHeader()) == true);
              }
              writer.writeNext(tsaScalar.getScalar().split("\\|"));
            }
            writer.close();     
        } catch (IOException e) {
            logger.error("failed to write CSV "+fileName+"Exception was:"+e);
        }                
        return scalarArray.size();        
    }
    */
    public int writeScalarCSV(String fileName) {
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(fileName));
            Iterator it = this.mapHeader.entrySet().iterator();
            String [] rowHeader = new String[this.mapHeader.size()];
            int i = 0;
            while (it.hasNext()) {
              Map.Entry pair = (Map.Entry)it.next();
              rowHeader[i++] = pair.getKey().toString();
            }
            //System.out.println( Arrays.toString(rowHeader));
            writer.writeNext(rowHeader);
            String [] rowValues = new String[this.mapHeader.size()];
            for (TSAScalarVo tsaScalar : scalarArray) {
              it = this.mapHeader.entrySet().iterator();
              i = 0;
              while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String value = tsaScalar.get(pair.getKey().toString());
                rowValues[i++] = value;
              }            
              //System.out.println( Arrays.toString(rowValues));
              writer.writeNext(rowValues);
            }
            writer.close();     
        } catch (IOException e) {
            logger.error("failed to write CSV "+fileName+"Exception was:"+e);
        }                
        return scalarArray.size();        
    }
    
}
