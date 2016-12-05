package org.auscope.nvcl.server.vo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

public class TSGScalarArrayVo {
    private float interval = (float) 1.0;
    private float floorDepth = 99999;
    private float ceilingDepth = -99999;
    public List<TSGScalarVo> scalarArray = new ArrayList<TSGScalarVo>();
    public List<TSGScalarVo> downSampledScalarArray = new ArrayList<TSGScalarVo>();    
    public TSGScalarArrayVo(float interval) {
        this.interval = interval;
    }
        
    public void add(TSGScalarVo spectralData) {
        float depthValue = spectralData.getDepth();
        if (depthValue < floorDepth) 
            floorDepth = depthValue;
        
        if (depthValue > ceilingDepth) 
            ceilingDepth = depthValue;
        
        scalarArray.add(spectralData);
    }

    public float getInterval() {
        return interval;
    }

    public void setInterval(float interval) {
        this.interval = interval;
    }
    
    public int downSample() {
        int f = (int)(floorDepth/interval);
        float depthStart =  f*interval;
        float depthEnd = depthStart + interval;
        float depth;
        int count = 0;
        double sumValue = 0.0;
        double value = 0.0;        
        for (TSGScalarVo scalar : scalarArray) {
            depth = scalar.getDepth();
            value = scalar.getValue();
            System.out.println(depth + "    " + value + "    " + scalar.isMask());
            if (depth >= depthStart && depth < depthEnd) {
                if (scalar.isMask()) {
                    sumValue += value;
                    count ++;
                }
            } else if (depth >= depthEnd) {
                downSampledScalarArray.add(new TSGScalarVo(String.valueOf(depthStart), true, sumValue/count));
                System.out.println("Bin:" + depthStart + ":count:"  + count + ":sumValue:" + sumValue + ":avgValue" + sumValue/count);
                depthStart = depthEnd;
                depthEnd = depthEnd + interval;
                sumValue = 0.0;
                count = 0;
                
            } else {
                System.out.println("Exception: terrible array sequence!!!!");
            }
        }
        return downSampledScalarArray.size();
        
    }
    public int writeScalarCSV(String fileName) {
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(fileName));
            writer.writeNext("depth,value,mask".split(","));
            float depth;
            double value = 0.0;        
            boolean mask;
            for (TSGScalarVo spectralData : scalarArray) {
                depth = spectralData.getDepth();
                value = spectralData.getValue();
                mask = spectralData.isMask();
                String record = depth + "," + value + "," + mask ;
                writer.writeNext(record.split(","));
            }
            writer.close();     
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }                
        return scalarArray.size();        
    }
    public int writeDownSampledScalarCSV(String fileName) {
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(fileName));
            writer.writeNext("depth,value".split(","));
            float depth;
            double value = 0.0;    
            for (TSGScalarVo scalar : downSampledScalarArray) {
                depth = scalar.getDepth();
                value = scalar.getValue();
                String record = depth + "," + value ;
                writer.writeNext(record.split(","));
            }
            writer.close();     
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }                
        return downSampledScalarArray.size();        
    }
    public boolean query(String units,String logicalOp,double threshHold) {
        boolean isHit = false;
        String depthS;
        double value = 0.0;
        double valueSum = 0.0;
        float ratio = (float) 0.0;
        /* Now, iterate over the map's contents, sorted by key. */
        for (TSGScalarVo spectralData : downSampledScalarArray) {
            depthS = spectralData.getDepthS();
            value = spectralData.getValue();
          if (units.equalsIgnoreCase("pct")) {
              if (logicalOp.equalsIgnoreCase("gt")) {
                  if (ratio > value) {
                      isHit = true;
                      break;
                  }
              } else if (logicalOp.equalsIgnoreCase("lt")){
                  if (ratio < value) {
                      isHit = true;
                      break;
                  }
              } else if (logicalOp.equalsIgnoreCase("eq")){
                  if (Math.abs(ratio - value) < 1.0) { //float equal when abs < 1%
                      isHit = true;
                      break;
                  }
              }
              
          } else {
              if (logicalOp.equalsIgnoreCase("gt")) {                      
                  if (value > threshHold) {
                      isHit = true;
                      break;                          
                  }
              } else if (logicalOp.equalsIgnoreCase("lt")) {
                  if (value < threshHold) {
                      isHit = true;
                      break;
                  }
              } else if (logicalOp.equalsIgnoreCase("eq")){
                  if (Math.abs(value - threshHold) < 1.0) { //float equal when abs < 1
                      isHit = true;
                      break;
                  }
              }
          }
        }
        
        return isHit;      
    }
}
