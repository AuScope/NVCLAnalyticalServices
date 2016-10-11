package org.auscope.nvcl.server.util;

import java.io.*;
import java.util.Arrays;

public class TsgMod {
    private static String libPath = "/home/jia020/work/ws/tsgmod/";
    static {
         System.loadLibrary("TsgMod");
        // System.load("/usr/lib/x86_64-linux-gnu/libm.so");
        System.out.println("load c lib");
        //System.load(libPath + "libTsgMod.so"); // c:\\cprojects\\0tsg\\debug\\tsgmod.dll");
    }

    private native int checkHandle(long h);

    private native long copyHandle(long h);

    private native int freeHandle(long h);

    private native long parseOneBatchScript(String jbtxt, float[] jwvl, int libwuflags, int hdr, int strict, int extra);

    private native long parseOneScalarMethod(String jbtxt, float[] jwvl, int libwuflags, int strict);

    private native double batchCalcOne(long h, float[] spv);

    private native double scalarCalcOne(long h, float[] spv);

    private native int scalarCalcMany(long h, double[] retv, float[] spv, int nch, int nsp);

    public boolean parseOneScalarMethod(String script,float[] wvl,int wavelenthscount,float[] spv ,int samplecount) {
        try {
            //int handle = checkHandle(0);    
            int nsp = samplecount;///* 99 sample count*/ 
            int nch = wavelenthscount;//531;/*wvl count*/
            double[] rv = new double[nsp];

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
            System.out.println("parseOneBatchScript: " + h);
            if (h > 0){
                checkHandle(h);
                scalarCalcMany(h, rv, spv, nch, nsp);
                int cc = 0;
                for (int i = 0; i < nsp; i++) {
                    if (!Double.isNaN(rv[i])) {
                       // System.out.println("Calc1 " + i + " = " + rv[i]);
                        cc++;
                    }
                }
                System.out.println("tsgProcessed2:" + rv.length +":TotalValidValue:" + cc);

            } else {
                System.out.println("!!!Handle: is wrong ");
                return false;
            }
            freeHandle(h);

        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
        return true;
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
            FileInputStream fis = new FileInputStream(libPath + "trox.sli");
            DataInputStream eph = new DataInputStream(fis);
            for (i = 0; i < nch * nsp; i++)
                spv[i] = eph.readFloat();
            fis.close();
            eph.close();

            //
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
            //
            long h2 = parseOneBatchScript(scrtx, wvl, 64, 0, 1, 1);
            System.out.println("ParseOneBatchScript: " + h2);
            System.out.println("ScalarCalcMany: " + scalarCalcMany(h2, rv, spv, nch, nsp));
            for (i = 0; i < nsp; i++)
                System.out.println("Calc1 " + i + " = " + rv[i]);
            
//            for (i = 0; i < nsp; i++) {
//                float[] sp = Arrays.copyOfRange(spv, i * nch, (i + 1) * nch);
//                System.out.println("Calc2 " + i + " = " + batchCalcOne(h2, sp));
//            }
            // double * data = env->GetDoubleArrayElements(*arr, NULL);
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
