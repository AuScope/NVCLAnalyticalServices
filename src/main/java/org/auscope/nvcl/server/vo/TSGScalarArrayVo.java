package org.auscope.nvcl.server.vo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import au.com.bytecode.opencsv.CSVWriter;

public class TSGScalarArrayVo {

	private static final Logger logger = LogManager.getLogger(TSGScalarArrayVo.class);
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
            logger.debug(depth + "    " + value + "    " + scalar.isMask());


            if (depth >= depthStart && depth < depthEnd) {
                if (!scalar.isMask()) //skip the masked one.
                    continue;            
                sumValue += value;
                count ++;
            } else if (depth >= depthEnd) {
                if (count >0 && sumValue > 0.0) {        
                    float downSampledDepth = (depthStart + depthEnd)/2;
                    downSampledScalarArray.add(new TSGScalarVo(String.valueOf(downSampledDepth), true, sumValue/count,count));
                    logger.debug("Bin:" + downSampledDepth + ":count:"  + count + ":sumValue:" + sumValue + ":avgValue:" + sumValue/count + ":count:" + count);
                }
                depthStart = depthEnd;
                depthEnd = (depthEnd + interval) > ceilingDepth ? ceilingDepth : (depthEnd + interval); //depthEnd + interval; 
                sumValue = 0.0;
                count = 0;
                
            } else {
            	logger.error("TSGScalarArrayVo:Exception: terrible array sequence!!!!");
            }
        }
        
        //the last downSampled
        
        if (count >0 && sumValue > 0.0) {
            float downSampledDepth = (depthStart + depthEnd)/2;
            downSampledScalarArray.add(new TSGScalarVo(String.valueOf(downSampledDepth), true, sumValue/count,count));
            logger.debug("Bin:" + downSampledDepth + ":count:"  + count + ":sumValue:" + sumValue + ":avgValue" + sumValue/count);      
        }
        logger.debug("TSGScalarArrayVo:downSample:totalSize=" + downSampledScalarArray.size());
        return downSampledScalarArray.size();
        
    }
    
    public int downSample() {
        // Align floorDepth to the nearest lower multiple of interval
        float alignedFloor = (float)(Math.floor(floorDepth / interval) * interval);
        float alignedCeiling = (float)(Math.ceil(ceilingDepth / interval) * interval);
        int size = (int)((alignedCeiling - alignedFloor) / interval);

        // Initialize bins
        for (int i = 0; i < size; i++) {
            float downSampledDepth = alignedFloor + i * interval;
            downSampledScalarArray.add(new TSGScalarVo(downSampledDepth,downSampledDepth+interval));
        }
        logger.debug("InitDownSampledScalarArraySize=" + downSampledScalarArray.size());

        // Accumulate values into bins
        for (TSGScalarVo scalar : scalarArray) {
            float depth = scalar.getDepth();
            double value = scalar.getValue();

            if (!scalar.isMask()) continue;

            int index = (int)((depth - alignedFloor) / interval);
            if (index < 0 || index >= size) {
                logger.error("TSGScalarArrayVo:Exception: depth out of bin range. Range is " + floorDepth + " to "
                        + ceilingDepth + " aligned to " + alignedFloor + "to" + alignedCeiling
                        + " and requested value is " + depth + " calculated index value is " + index
                        + " which is larger than maximum " + size);
                continue;
            }

            TSGScalarVo downSampledScalar = downSampledScalarArray.get(index);
            double sumValue = downSampledScalar.getValue() + value;
            downSampledScalar.setValue(sumValue);
            downSampledScalar.setCount(downSampledScalar.getCount() + 1);
        }

        // Finalize averages
        for (int i = 0; i < size; i++) {
            TSGScalarVo downSampledScalar = downSampledScalarArray.get(i);
            if (downSampledScalar.getCount() == 0) {
                downSampledScalar.setValue(Double.NaN);
            } else {
                downSampledScalar.setValue(downSampledScalar.getValue() / downSampledScalar.getCount());
            }
        }

        logger.debug("TSGScalarArrayVo:downSample:totalSize=" + downSampledScalarArray.size());
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
                // only write values if mask == true and the value is not NaN.
                // The client should normally apply this masking but portal currently doesn't
                // so its done here.
                if (mask && !Double.isNaN(value))
                {
                	String record = depth + "," + value + "," + mask ;
                	writer.writeNext(record.split(","));
                }
            }
            writer.close();     
        } catch (IOException e) {
            logger.error("failed to write CSV "+fileName+"Exception was:"+e);
        }                
        return scalarArray.size();        
    }
    public int writeDownSampledScalarCSV(String fileName) {
        if (downSampledScalarArray == null || downSampledScalarArray.isEmpty()) {
            logger.warn("No data to write to CSV: downSampledScalarArray is empty.");
            return 0;
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName))) {
            writer.writeNext(new String[]{"startdepth", "enddepth", "value", "count"});
            for (TSGScalarVo scalar : downSampledScalarArray) {
                String[] row = {String.valueOf(scalar.getDepth()),String.valueOf(scalar.getEndDepth()),String.valueOf(scalar.getValue()),String.valueOf(scalar.getCount())};
                writer.writeNext(row);
            }  
        } catch (IOException e) {
        	logger.error("failed to write CSV "+fileName+"Exception was:"+e);
        }                
        return downSampledScalarArray.size();        
    }

    public boolean query(String units, String logicalOp, double threshHold) {
        boolean isHit = false;
        // double valueSum = 0.0;
        float ratio = (float) 0.0;
        /* Now, iterate over the map's contents, sorted by key. */
        for (TSGScalarVo spectralData : downSampledScalarArray) {
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
                	logger.error("TSGScalarArrayVo:Exception: Unrecognize logicalOp:" + logicalOp);
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
                	logger.error("TSGScalarArrayVo:Exception: Unrecognize logicalOp:" + logicalOp);
                }
            } else {
            	logger.error("TSGScalarArrayVo:Exception: Unrecognize units:" + units);
            }
        }

        return isHit;
    }
    public Integer queryMaxCountSum() {
        Integer countMax = 0;
        /* Now, iterate over the map's contents, sorted by key. */
        for (TSGScalarVo spectralData : downSampledScalarArray) {
            Integer count = spectralData.getCount();
            if (count>countMax){
                countMax = count;
            }
        }
        return countMax;
    }    
}
