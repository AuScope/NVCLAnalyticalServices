package org.auscope.nvcl.server.vo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import au.com.bytecode.opencsv.CSVWriter;

public class TSGScalarArrayVo {
    private final Log log = LogFactory.getLog(getClass());
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
    
    public int downSample2() {
        int f = (int)(floorDepth/interval);
        float depthStart = f > 0 ? f*interval : floorDepth;
        float depthEnd = (depthStart + interval) > ceilingDepth ? ceilingDepth : (depthStart+interval);
        float depth;
        int count = 0;
        double sumValue = 0.0;
        double value = 0.0;        
        for (TSGScalarVo scalar : scalarArray) {
            depth = scalar.getDepth();
            value = scalar.getValue();
            if (false) {//log.isDebugEnabled()) {
                System.out.println(depth + "    " + value + "    " + scalar.isMask());
            }

            if (depth >= depthStart && depth < depthEnd) {
                if (!scalar.isMask()) //skip the masked one.
                    continue;            
                sumValue += value;
                count ++;
            } else if (depth >= depthEnd) {
                if (count >0 && sumValue > 0.0) {        
                    float downSampledDepth = (depthStart + depthEnd)/2;
                    downSampledScalarArray.add(new TSGScalarVo(String.valueOf(downSampledDepth), true, sumValue/count,count));
                    if (false) 
                        System.out.println("Bin:" + downSampledDepth + ":count:"  + count + ":sumValue:" + sumValue + ":avgValue:" + sumValue/count + ":count:" + count);
                }
                depthStart = depthEnd;
                depthEnd = (depthEnd + interval) > ceilingDepth ? ceilingDepth : (depthEnd + interval); //depthEnd + interval; 
                sumValue = 0.0;
                count = 0;
                
            } else {
                System.out.println("TSGScalarArrayVo:Exception: terrible array sequence!!!!");
            }
        }
        
        //the last downSampled
        
        if (count >0 && sumValue > 0.0) {
            float downSampledDepth = (depthStart + depthEnd)/2;
            downSampledScalarArray.add(new TSGScalarVo(String.valueOf(downSampledDepth), true, sumValue/count,count));
            if (false) {//log.isDebugEnabled()
                System.out.println("Bin:" + downSampledDepth + ":count:"  + count + ":sumValue:" + sumValue + ":avgValue" + sumValue/count);
            }            
        }
        System.out.println("TSGScalarArrayVo:downSample:totalSize=" + downSampledScalarArray.size());
        return downSampledScalarArray.size();
        
    }
    
    public int downSample() {
        int size = (int) ((ceilingDepth - floorDepth)/ interval) +1;
        for (int i=0;i<size;i++) {
            float downSampledDepth = floorDepth + i*interval;
            downSampledScalarArray.add(new TSGScalarVo(String.valueOf(downSampledDepth), true, 0,0));
        }
        System.out.println("InitDownSampledScalarArraySize=" + downSampledScalarArray.size());
        
        float depth;
        double value;
        for (TSGScalarVo scalar : scalarArray) {
            depth = scalar.getDepth();
            value = scalar.getValue();
            if (false) {//log.isDebugEnabled()) {
                System.out.println(depth + "    " + value + "    " + scalar.isMask());
            }
            if (!scalar.isMask()) //skip the masked one.
                continue;            
            int index = (int)((depth-floorDepth)/interval);
            if (index >= size)
                System.out.println("TSGScalarArrayVo:Exception: terrible array sequence!!!!");
                
            TSGScalarVo downSampledScalar = downSampledScalarArray.get(index);
            double sumValue = downSampledScalar.getValue() + value;
            downSampledScalar.setValue(sumValue);
            downSampledScalar.setCount(downSampledScalar.getCount() +1);
        }
        
        for (int i=0;i<size;i++) {
            TSGScalarVo downSampledScalar = downSampledScalarArray.get(i);
            downSampledScalar.setValue(downSampledScalar.getValue() / downSampledScalar.getCount());
            if (false) {//log.isDebugEnabled()
                System.out.println("Bin:" + downSampledScalar.getDepthS() + ":value:" + downSampledScalar.getValue()  + ":count:"  + downSampledScalar.getCount() );
            }                        
        }
        System.out.println("TSGScalarArrayVo:downSample:totalSize=" + downSampledScalarArray.size());
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
            writer.writeNext("depth,value,count".split(","));
            float depth;
            double value = 0.0;    
            int count = 0;
            for (TSGScalarVo scalar : downSampledScalarArray) {
                depth = scalar.getDepth();
                value = scalar.getValue();
                count = scalar.getCount();
                String record = depth + "," + value + "," + count ;
                writer.writeNext(record.split(","));
            }
            writer.close();     
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }                
        return downSampledScalarArray.size();        
    }

    public boolean query(String units, String logicalOp, double threshHold) {
        boolean isHit = false;
        // double valueSum = 0.0;
        float ratio = (float) 0.0;
        /* Now, iterate over the map's contents, sorted by key. */
        for (TSGScalarVo spectralData : downSampledScalarArray) {
            String depthS = spectralData.getDepthS();
            double value = spectralData.getValue();
            if (units.equalsIgnoreCase("pct")) {
                if (logicalOp.equalsIgnoreCase("gt")) {
                    if (ratio > value) {
                        isHit = true;
                        break;
                    }
                } else if (logicalOp.equalsIgnoreCase("lt")) {
                    if (ratio < value) {
                        isHit = true;
                        break;
                    }
                } else if (logicalOp.equalsIgnoreCase("eq")) {
                    if (Math.abs(ratio - value) < 1.0) { // float equal when abs
                                                         // < 1%
                        isHit = true;
                        break;
                    }
                }else {
                    System.out.println("TSGScalarArrayVo:Exception: Unrecognize logicalOp:" + logicalOp);
                }

            } else if (units.equalsIgnoreCase("count")){
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
                } else if (logicalOp.equalsIgnoreCase("eq")) {
                    if (Math.abs(value - threshHold) < 1.0) { // float equal
                                                              // when abs < 1
                        isHit = true;
                        break;
                    }
                } else {
                    System.out.println("TSGScalarArrayVo:Exception: Unrecognize logicalOp:" + logicalOp);
                }
            } else {
                System.out.println("TSGScalarArrayVo:Exception: Unrecognize units:" + units);
            }
        }

        return isHit;
    }
}
