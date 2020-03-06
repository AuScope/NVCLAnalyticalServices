package org.auscope.nvcl.server.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.vo.TSAScalarArrayVo;
import org.auscope.nvcl.server.vo.TSAScalarVo;

public class TsgMod {

    private static final Logger logger = LogManager.getLogger(TsgMod.class);

    static {
        // note: library name is case sensitive and should be TsgMod.dll in windows and
        // libTsgMod.so in linux
        System.loadLibrary("TsgMod");
        logger.info("TsgMod:load c lib from " + System.getProperty("java.library.path"));

    }

    private native int testTsg(long h);

    private native long testParseOneBatchScript(String jbtxt, float[] jwvl, int libwuflags, float[] jwvl2, int libwuflags2, int hdr, int strict, int extra);

    private native int checkHandle(long h);

    private native long copyHandle(long h);

    private native int freeHandle(long h);

    private native long parseOneScalarMethod(String jbtxt, float[] jwvl, int libwuflags, float[] jwvl2, int libwuflags2, int strict);

    private native double batchCalcOne(long h, float[] spv);

    private native double scalarCalcOne(long h, float[] spv, float[] spv2);

    private native int scalarCalcMany(long h, double[] retv, float[] spv, float[] spv2, byte[] mask, int nch, int nsp);

    private native int tsaJsonQuery(long h, byte[] jbuf, int q, int nmix);

    public Boolean parseTSAScript(String tsaFilePath, String script, float[] wvl, int wavelenthscount, float[][] spv,
            int specCount) {
        if (script == null) {
            return false;
        }
        /// TSA only
        TSAScalarArrayVo tsaScalarArray = new TSAScalarArrayVo();
        try {

            long h = testParseOneBatchScript(script, wvl, 64, null, 0, 0, 1, 1);
            logger.debug("TsgMod:parseOneBatchScript: " + h);
            if (script.indexOf("outputFormat = Complex") >= 0) {
                // TSA script
                int queryno = 21, nmix = 3;
                // Extract nmix from script if there is any.
                Pattern pattern = Pattern.compile("nmix ?= ?([0-9]*),");
                Matcher matcher = pattern.matcher(script);
                if (matcher.find())
                {
                    nmix = Integer.parseInt(matcher.group(1));
                    logger.debug("TsgMod:parse nmix = " + nmix);
                }                
                byte[] qtx = new byte[4096];
                for (int i = 0; i < specCount; i++) {
                    double ret = scalarCalcOne(h,spv[i], null);
                    // System.out.println("TSA:tsmScalarCalcOne: return =" + ret);
                    
                    int tsaRet = tsaJsonQuery(h, qtx, queryno, nmix);
                    String text = new String(qtx, "UTF-8");
                    // System.out.println("TSA:tsaJsonQuery: return =" + tsaRet + "qtx=" + text);
                    tsaScalarArray.add(new TSAScalarVo(text, String.valueOf(i)));
                }
                qtx = null;
                tsaScalarArray.writeScalarCSV(tsaFilePath);
                tsaScalarArray = null;
                return true;
                /* queryno = 11
                 * qtx={ "tsgresult": "SWIR TSA", "level": "Mineral", "status": "Ok", "nmix": 0,
                 * "srss": 156.114932, "index": [15, 5], "prop": [0.553905, 0.446095] }
                 */
            }
            freeHandle(h);

        } catch (Exception ex) {
            logger.error("Exception:parseTSAScript");
            logger.error(ex.toString());
        }
        return false;

    }

    public boolean parseTSGScript(double[] rv, String script, float[] wvl, int wavelenthscount, float[] spv,
            int samplecount, float value, float pctBench) {
        int nsp = samplecount;/// * 99 sample count*/
        int nch = wavelenthscount;// 531;/*wvl count*/
        int cc = 0;
        try {

            if (script == null) {
                return false;
            }
            // checkHandle(5555);
            long h = testParseOneBatchScript(script, wvl, 64, null, 0, 0, 1, 1);
            // long h = parseScript(script, wvl, 64, null, 0, 0, 1, 1);
            logger.debug("TsgMod:parseOneBatchScript: " + h);
            if (h > 0) {
                // checkHandle(h);
                int ret = scalarCalcMany(h, rv, spv, null, null, nch, nsp);
                System.out.println("scalarCalcMany:" + ret);
                for (int i = 0; i < nsp; i++) {
                    if (!Double.isNaN(rv[i])) {
                        if (rv[i] > value)
                            cc++;
                    }
                }

            } else {
                logger.error("TsgMod:Handle: is wrong ");
                return false;
            }

            freeHandle(h);

        } catch (Exception ex) {
            logger.error("Exception:parseOneScalarMethod");
            logger.error(ex.toString());
        }

        float pct = (float) cc / (float) nsp;
        return (pct > pctBench) ? true : false;
    }

}
