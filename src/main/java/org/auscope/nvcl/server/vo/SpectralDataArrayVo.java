package org.auscope.nvcl.server.vo;

import java.util.ArrayList;
import java.util.List;

public class SpectralDataArrayVo {
    private float interval = (float) 1.0;
    private float floorDepth = 99999;
    private float ceilingDepth = -99999;
    public List<SpectralDataVo> spectralDataArray = new ArrayList<SpectralDataVo>();
    public List<SpectralDataVo> binnedArray = new ArrayList<SpectralDataVo>();    
    public SpectralDataArrayVo(float interval) {
        this.interval = interval;
    }
        
    public void add(SpectralDataVo spectralData) {
        float depthValue = spectralData.getDepth();
        if (depthValue < floorDepth) 
            floorDepth = depthValue;
        
        if (depthValue > ceilingDepth) 
            ceilingDepth = depthValue;
        
        spectralDataArray.add(spectralData);
    }

    public float getInterval() {
        return interval;
    }

    public void setInterval(float interval) {
        this.interval = interval;
    }
    
    public int bin() {
        int f = (int)(floorDepth/interval);
        float depthStart =  f*interval;
        float depthEnd = depthStart + interval;
        float depth;
        int count = 0;
        double sumValue = 0.0;
        double value = 0.0;        
        for (SpectralDataVo spectralData : spectralDataArray) {
            depth = spectralData.getDepth();
            if (depth >= depthStart && depth < depthEnd) {
                if (!spectralData.isMask()) {
                    value = spectralData.getValue();
                    sumValue += value;
                    count ++;
                }
            } else if (depth >= depthEnd) {
                binnedArray.add(new SpectralDataVo(String.valueOf(depthStart), false, sumValue/count));
                System.out.println("Bin:" + depthStart + ":count:"  + count + ":sumValue:" + sumValue + ":avgValue" + sumValue/count);
                depthStart = depthEnd;
                depthEnd = depthEnd + interval;
                sumValue = 0.0;
                count = 0;
                
            } else {
                System.out.println("Exception: terrible array sequence!!!!");
            }
        }
        return binnedArray.size();
        
    }
    public boolean query(String units,String logicalOp,double threshHold) {
        boolean isHit = false;
        String depthS;
        double value = 0.0;
        double valueSum = 0.0;
        float ratio = (float) 0.0;
        /* Now, iterate over the map's contents, sorted by key. */
        for (SpectralDataVo spectralData : binnedArray) {
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
