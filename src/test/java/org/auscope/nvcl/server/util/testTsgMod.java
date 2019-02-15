package org.auscope.nvcl.server.util;

import org.junit.Test;

public class testTsgMod {
    @Test
    public void test(String argv[]) {
        System.out.println("testTsgMod:enter");        
        TsgMod tsgMod = new TsgMod();
        int ret = tsgMod.testTsg (2222);
        System.out.println(ret);        
        System.out.println("testTsgMod:exit");                
   }
}
