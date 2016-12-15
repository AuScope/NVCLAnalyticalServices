package org.auscope.nvcl.server.vo;

/**
 * SpectralLogVo
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */
public class TSGScalarVo {
    private String depthS;
    private float depth;
    private boolean mask;//1 is valid, 0 is skip
    private int     color;
    private double value;
    private int count;
    public TSGScalarVo(String depthS, boolean mask, double value) {
        this.depthS = depthS;
        this.depth = Float.parseFloat(depthS);
        this.mask = mask;
        this.value = value;
        if (Double.isNaN(value)) {
            this.mask = false;
        }
        this.setCount(0);
    }
    public TSGScalarVo(String depthS, boolean mask, double value,int count) {
        this.depthS = depthS;
        this.depth = Float.parseFloat(depthS);
        this.mask = mask;
        this.value = value;
        if (Double.isNaN(value)) {
            this.mask = false;
        }
        this.setCount(count);
    }    
    public String getDepthS() {
        return depthS;
    }
    public void setDepthS(String depthS) {
        this.depthS = depthS;
    }
    public float getDepth() {
        return depth;
    }
    public void setDepth(float depth) {
        this.depth = depth;
    }
    public boolean isMask() {
        return mask;
    }
    public void setMask(boolean mask) {
        this.mask = mask;
    }
    public int getColor() {
        return color;
    }
    public void setColor(int color) {
        this.color = color;
    }
    public double getValue() {
        return value;
    }
    public void setValue(double value) {
        this.value = value;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
  
}