package org.auscope.nvcl.server.service;

import static org.junit.Assert.*;

import org.auscope.nvcl.server.util.Utility;
import org.auscope.nvcl.server.vo.AnalyticalJobVo;
import org.junit.Test;
/*
 * Junit test for NVCLAnalyticalJobProcessor.java
 * This test will test the Job process in NVCLAnalyticalJobProcessor.java, it will simulate a AnalyticalJobVo and run the
 * Process method to carry out an actual analytical query job, save the result into AnalyticalJobResultVo.
 * Author : Lingbo Jiang
 */
public class NVCLAnalyticalJobProcessorTest {
    
    /*
     * test process AnalyticalJob request Search Dickite with logs with the name
     * "Min1 sTSAS" dickite greater than 5 sample count within 99999 metre span.
     */
    @Test
    public void testDickiteJob() throws Exception {

        AnalyticalJobVo jobVo = new AnalyticalJobVo();
        jobVo.setRequestType("ANALYTICAL");
        jobVo.setJobid("test-" + Utility.getHashValue());
        jobVo.setJobDescription("logs with the name \"Min1 sTSAS\" dickite greater than 5 sample count within 99999 metre span.");
        jobVo.setServiceUrls("http://auscope.dpi.nsw.gov.au/geoserver/wfs");
        jobVo.setEmail("lingbo.jiang@csiro.au");
        jobVo.setLogName("Min1 sTSAS");
        // Filter was setted to broken hill area.
        jobVo.setFilter("<ogc:filter><ogc:BBOX><ogc:PropertyName>gsmlp:shape</ogc:PropertyName><gml:Envelope srsName=\"EPSG:4326\"><gml:lowerCorner>141.00 -32.1</gml:lowerCorner><gml:upperCorner>141.2 -32.0</gml:upperCorner></gml:Envelope></ogc:BBOX></ogc:filter>");
        jobVo.setStartDepth(0);
        jobVo.setEndDepth(99999);
        jobVo.setClassification("Dickite");
        jobVo.setSpan((float) 99999);
        jobVo.setUnits("count");
        jobVo.setValue(5);
        jobVo.setLogicalOp("gt");

        NVCLAnalyticalJobProcessor processor = new NVCLAnalyticalJobProcessor();
        processor.setAnalyticalJob(jobVo);
        if (!processor.getBoreholeList()) {
            return;
        }
        if (!processor.getDataCollection()) {
            return;
        }
        if (!processor.getDownSampledData()) {
            return;
        }
        System.out.println("\nStage Finished:OK");
    }
    /*
     * test process AnalyticalJob request
     * Search Garnet with all Thermal TSA group (regardless of version) Garnet over 2% of entire dataset
     */
    @Test
    public void testGarnetJob() throws Exception {

        AnalyticalJobVo jobVo = new AnalyticalJobVo();
        jobVo.setRequestType("ANALYTICAL");
        jobVo.setJobid("test-" + Utility.getHashValue());
        jobVo.setJobDescription("ALL Thermal TSA group (regardless of version) Garnet over 2% of entire dataset");
        jobVo.setServiceUrls("http://geology.data.nt.gov.au/geoserver/wfs");
        jobVo.setEmail("lingbo.jiang@csiro.au");
        jobVo.setAlgorithmOutputID("57,63,69,103");
        jobVo.setFilter("<ogc:Filter><PropertyIsEqualTo><PropertyName>gsmlp:nvclCollection</PropertyName><Literal>true</Literal></PropertyIsEqualTo></ogc:Filter>");
        jobVo.setStartDepth(0);
        jobVo.setEndDepth(99999);
        jobVo.setClassification("GARNET");
        jobVo.setSpan((float) 1.0);
        jobVo.setUnits("pct");
        jobVo.setValue(2);
        jobVo.setLogicalOp("gt");

        NVCLAnalyticalJobProcessor processor = new NVCLAnalyticalJobProcessor();
        processor.setAnalyticalJob(jobVo);
        if (!processor.getBoreholeList()) {
            return;
        }
        if (!processor.getDataCollection()) {
            return;
        }
        if (!processor.getDownSampledData()) {
            return;
        }
        System.out.println("\nStage Finished:OK");
    }

    /*
     * test process AnalyticalJob request
     * Search Garnet with all Thermal TSA group (regardless of version) Garnet over 2% of entire dataset
     */
    @Test
    public void testMultiThreadJob() throws Exception {

        AnalyticalJobVo jobVo = new AnalyticalJobVo();
        jobVo.setRequestType("ANALYTICAL");
        jobVo.setJobid("test-" + Utility.getHashValue());
        jobVo.setJobDescription("Muscovite great than 20 pecent");
        //https://sarigdata.pir.sa.gov.au/nvcl/geoserver/wfs,http://geossdi.dmp.wa.gov.au:80/services/wfs,
        jobVo.setServiceUrls("http://geology.data.vic.gov.au/nvcl/wfs,http://geology.data.nt.gov.au:80/geoserver/wfs,http://geology.information.qld.gov.au/geoserver/wfs,http://www.mrt.tas.gov.au:80/web-services/wfs,http://auscope.dpi.nsw.gov.au:80/geoserver/wfs");
        jobVo.setEmail("lingbo.jiang@csiro.au");
        jobVo.setLogName("Min1 uTSAS");
        jobVo.setFilter("<ogc:Filter><PropertyIsEqualTo><PropertyName>gsmlp:nvclCollection</PropertyName><Literal>true</Literal></PropertyIsEqualTo></ogc:Filter>");
        jobVo.setStartDepth(0);
        jobVo.setEndDepth(99999);
        jobVo.setClassification("Muscovite");
        jobVo.setSpan((float) 1.0);
        jobVo.setUnits("pct");
        jobVo.setValue(20);
        jobVo.setLogicalOp("gt");

        NVCLAnalyticalJobProcessorManager processorManager = new NVCLAnalyticalJobProcessorManager();
        if( !processorManager.processRequest(jobVo)) {
            System.out.println("Error:processorManager.processRequest");
            return;       
        }        
        System.out.println("OK:processorManager.processRequest");
    }
    
    @Test
    public void testCheckAlgoutiIDs()
    {
        assertFalse("This will be False.", Utility.checkAlgoutiIDs(""));
        assertFalse("This will be False.", Utility.checkAlgoutiIDs(","));
        assertFalse("This will be False.", Utility.checkAlgoutiIDs("a,123,b"));
        assertFalse("This will be False.", Utility.checkAlgoutiIDs(",123"));
        assertTrue("This will be true.", Utility.checkAlgoutiIDs("123,"));        
        assertTrue("This will be true.", Utility.checkAlgoutiIDs("123"));
        assertTrue("This will be true.", Utility.checkAlgoutiIDs("1,2,3"));
    }
    
}
