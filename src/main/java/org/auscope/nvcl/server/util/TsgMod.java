package org.auscope.nvcl.server.util;

import java.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TsgMod {
    static {
         System.loadLibrary("TsgMod");
        System.out.println("TsgMod:load c lib");
    }
    //private final Log log = LogFactory.getLog(getClass());
    
	private static final Logger logger = LogManager.getLogger(TsgMod.class);
	
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
            //int handle = checkHandle(0);    
            if (script == null) {
                script = "name = Hematite-goethite_distr, 9\n" +
                            "p1 = profile, layer=ref, stat=depth, bkrem=div, fit=3, wcentre=913, wradius=137\n" +
                            "p2= profile, layer=ref, stat=mean, wcentre=1650, wradius=0\n"+
                            "p3= profile, layer=ref, stat=mean, wcentre=450, wradius=0\n"+
                            "p4= expr, param1=p3, param2=p2, arithop=div\n"+
                            "p5 = expr, param1=p4, const2=1, arithop=lle, nullhandling=out\n"+
                            "p6= expr, param1=p5, param2=p1, arithop=mult\n"+
                            "p7= expr, param1=p6, const2=0.025, arithop=lgt, nullhandling=out\n"+
                            "p8= pfit, layer=ref, wunits=nm, wmin=776, wmax=1050, bktype=hull, bksub=div, order=4, product=0, bktype=hull, bksub=div\n"+
                            "return=expr, param1=p8, param2=p7, arithop=mult ";
                //String txt = "pfit, layer=Reflectance, wunits=nm, wmin=2257.0, wmax=2277.0, fwmin=2257.0, fwmax=2277.0, bktype=hull, bksub=div, order=4, root=auto, product=depth, inflex=no, peaks=no";
            }

            long h = parseOneBatchScript(script, wvl, 64,  0, 1, 1);
            System.out.println("TsgMod:parseOneBatchScript: " + h);
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
//                    System.out.printf("Calc:%d--%d:%.4f:%.4f:%.4f:%.4f:%.4f\n" , i, i+5,rv[i],rv[i+1],rv[i+2],rv[i+3],rv[i+4]);
//                }

            } else {
                System.out.println("TsgMod:Handle: is wrong ");
                return false;
            }
            freeHandle(h);

        } catch (Exception ex) {
            System.out.println("Exception:parseOneScalarMethod");
            System.out.println(ex.toString());
        }
        
        float pct =(float) cc /(float)nsp;
        return (pct>pctBench)? true:false;
    }
    
    public boolean test() {
        try {
            String txt = "pfit, layer=Reflectance, wunits=nm, wmin=2257.0, wmax=2277.0, fwmin=2257.0, fwmax=2277.0, bktype=hull, bksub=div, order=4, root=auto, product=depth, inflex=no, peaks=no";
            int i, nsp = 99, nch = 531;
            float[] wvl = new float[nch];
            // scrtx = new String(Files.readAllBytes(Paths.get(libPath +
            // "0tstscript.txt")), "ASCII");
            String scrtx = "name = Hematite-goethite_distr, 9\n" + "p1 = profile, layer=ref, stat=depth, bkrem=div, fit=3, wcentre=913, wradius=137\n"
                    + "p2= profile, layer=ref, stat=mean, wcentre=1650, wradius=0\n" + "p3= profile, layer=ref, stat=mean, wcentre=450, wradius=0\n"
                    + "p4= expr, param1=p3, param2=p2, arithop=div\n" + "p5 = expr, param1=p4, const2=1, arithop=lle, nullhandling=out\n"
                    + "p6= expr, param1=p5, param2=p1, arithop=mult\n" + "p7= expr, param1=p6, const2=0.025, arithop=lgt, nullhandling=out\n"
                    + "p8= pfit, layer=ref, wunits=nm, wmin=776, wmax=1050, bktype=hull, bksub=div, order=4, product=0, bktype=hull, bksub=div\n"
                    + "return=expr, param1=p8, param2=p7, arithop=mult ";

            float[] spv = new float[nch * nsp];
            for (i = 0; i < nch; i++)
                wvl[i] = (float) (380.0 + i * 4.0);
            String libPath = "/home/jia020/work/ws/tsgmod/";
            FileInputStream fis = new FileInputStream(libPath + "trox.sli");
            DataInputStream eph = new DataInputStream(fis);
            for (i = 0; i < nch * nsp; i++)
                spv[i] = eph.readFloat();
            fis.close();
            eph.close();

            double[] rv = new double[nsp];
            long h = parseOneScalarMethod(txt, wvl, 64, 1);
            System.out.println("ParseOneScalarMethod: " + h);
            System.out.println("CheckHandle: " + checkHandle(h));
            for (i = 0; i < nsp; i++)
                System.out.println("pre-Calc1 " + i + " = " + rv[i]);
            System.out.println("ScalarCalcMany: " + scalarCalcMany(h, rv, spv, nch, nsp));
            for (i = 0; i < nsp; i++)
                System.out.println("Calc1 " + i + " = " + rv[i]);
            System.out.println("FreeHandle: " + freeHandle(h));

            long h2 = parseOneBatchScript(scrtx, wvl, 64, 0, 1, 1);
            System.out.println("ParseOneBatchScript: " + h2);
            System.out.println("ScalarCalcMany: " + scalarCalcMany(h2, rv, spv, nch, nsp));
            for (i = 0; i < nsp; i++)
                System.out.println("Calc1 " + i + " = " + rv[i]);

            System.out.println("FreeHandle: " + freeHandle(h2));
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("start");
        TsgMod tsgMod = new TsgMod();
        System.out.println("1");
        tsgMod.test();
        System.out.println("end");
        return;
    }
}
