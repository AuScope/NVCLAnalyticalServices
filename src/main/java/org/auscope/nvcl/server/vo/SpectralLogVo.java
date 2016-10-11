package org.auscope.nvcl.server.vo;

/**
 * SpectralLogVo
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */
public class SpectralLogVo {
    private String logID;
    private int sampleCount;
    private String wavelengths;
    private float[] wvl;
    public SpectralLogVo(String logID, String sampleCount, String wavelengths) {
        this.setLogID(logID);
        int iSampleCount = Integer.parseInt(sampleCount);
        this.setSampleCount(iSampleCount);
        this.setWavelengths(wavelengths);
        String[] strWvl = wavelengths.split(",");
        int length = strWvl.length;
        wvl = new float[length];
        for (int i=0;i< length; i++) {
            wvl[i] = Float.parseFloat(strWvl[i]);
        }        
    }
    public String getLogID() {
        return logID;
    }
    public void setLogID(String logID) {
        this.logID = logID;
    }
    public int getSampleCount() {
        return sampleCount;
    }
    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }
    public float[] getWvl() {
        return wvl;
    }
    public String getWavelengths() {
        return wavelengths;
    }
    public void setWavelengths(String wavelengths) {
        this.wavelengths = wavelengths;
    }    
}