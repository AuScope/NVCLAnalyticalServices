package org.auscope.nvcl.server.util;

import java.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TsgMod {
	
	private static final Logger logger = LogManager.getLogger(TsgMod.class);
	
    static {
    	// note: library name is case sensitive and should be TsgMod.dll in windows and libTsgMod.so in linux
    	System.loadLibrary("TsgMod");
        logger.info("TsgMod:load c lib");
    }

    private native int checkHandle(long h);

    private native long copyHandle(long h);

    private native int freeHandle(long h);

    private native long parseOneBatchScript(String jbtxt, float[] jwvl, int libwuflags, int hdr, int strict, int extra);

    private native long parseOneScalarMethod(String jbtxt, float[] jwvl, int libwuflags, int strict);

    private native double batchCalcOne(long h, float[] spv);

    private native double scalarCalcOne(long h, float[] spv);

    private native int scalarCalcMany(long h, double[] retv, float[] spv, int nch, int nsp);

    public boolean parseOneScalarMethod(double[] rv,String script,float[] wvl,int wavelenthscount,float[] spv ,int samplecount, float value, float pctBench) {
        int nsp = samplecount;///* 99 sample count*/ 
        int nch = wavelenthscount;//531;/*wvl count*/
        int cc = 0;
        try {
  
            if (script == null) {
                return false;
            }

            long h = parseOneBatchScript(script, wvl, 64,  0, 1, 1);
            logger.debug("TsgMod:parseOneBatchScript: " + h);
            if (h > 0){
                checkHandle(h);
                scalarCalcMany(h, rv, spv, nch, nsp);

                for (int i = 0; i < nsp; i++) {
                    if (!Double.isNaN(rv[i])) {
                        if (rv[i] > value)
                        cc++;
                    }
                }
//                if (log.isDebugEnabled()) {
//                for (int i = 0; i < 100; i=i+5)
//                    logger.debug("Calc:%d--%d:%.4f:%.4f:%.4f:%.4f:%.4f\n" , i, i+5,rv[i],rv[i+1],rv[i+2],rv[i+3],rv[i+4]);
//                }

            } else {
            	logger.error("TsgMod:Handle: is wrong ");
                return false;
            }
            freeHandle(h);

        } catch (Exception ex) {
        	logger.error("Exception:parseOneScalarMethod");
        	logger.error(ex.toString());
        }
        
        float pct =(float) cc /(float)nsp;
        return (pct>pctBench)? true:false;
    }
    
}
