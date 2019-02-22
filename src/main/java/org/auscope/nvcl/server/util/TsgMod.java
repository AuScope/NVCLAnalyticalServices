package org.auscope.nvcl.server.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TsgMod {
	
	private static final Logger logger = LogManager.getLogger(TsgMod.class);
	
    static {
    	// note: library name is case sensitive and should be TsgMod.dll in windows and libTsgMod.so in linux
    	System.loadLibrary("TsgMod");
        logger.info("TsgMod:load c lib from "+System.getProperty("java.library.path"));
        
    }
    private native int testTsg(long h);    
    
    private native long testParseOneBatchScript(String jbtxt, float[] jwvl, int libwuflags, float[] jwvl2, int libwuflags2,int hdr, int strict, int extra);
  
    private native int checkHandle(long h);

    private native long copyHandle(long h);

    private native int freeHandle(long h);

    private native long parseOneScalarMethod(String jbtxt, float[] jwvl, int libwuflags,  float[] jwvl2, int libwuflags2, int strict);

    private native double batchCalcOne(long h, float[] spv);

    private native double scalarCalcOne(long h, float[] spv, float[] spv2);
    
    private native int scalarCalcMany(long h, double[] retv, float[] spv,  float[] spv2,byte[] mask, int nch, int nsp);    
   
    public boolean parseOneScalarMethod(double[] rv,String script,float[] wvl,int wavelenthscount,float[] spv ,int samplecount, float value, float pctBench) {
        int nsp = samplecount;///* 99 sample count*/ 
        int nch = wavelenthscount;//531;/*wvl count*/
        int cc = 0;
        try {
  
            if (script == null) {
                return false;
            }
 //           checkHandle(5555);
            long h = testParseOneBatchScript(script, wvl, 64, null, 0, 0, 1, 1);
          //  long h = parseScript(script, wvl, 64, null, 0, 0, 1, 1);
            logger.debug("TsgMod:parseOneBatchScript: " + h);
            if (h > 0){
                //checkHandle(h);
                int ret = scalarCalcMany(h, rv, spv, null,null, nch, nsp);
                System.out.println("scalarCalcMany:" + ret);
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
