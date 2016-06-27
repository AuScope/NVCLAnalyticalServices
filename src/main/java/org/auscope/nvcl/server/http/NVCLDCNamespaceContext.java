package org.auscope.nvcl.server.http;

import org.auscope.portal.core.services.namespaces.IterableNamespace;

/**
 * Namespace context for NVCL DataCollection types
 * 
 * @author Lingbo Jiang
 *
 */
public class NVCLDCNamespaceContext extends IterableNamespace {

    public static final String PUBLISHED_DATASETS_TYPENAME = "nvcl:ScannedBoreholeCollection";

    public NVCLDCNamespaceContext() {
        map.put("ns2", "http://www.w3.org/1999/xlink");
        map.put("ns3", "http://www.auscope.org/nvcl");
        map.put("ns4", "http://www.opengis.net/gml");
        map.put("ns5", "http://www.opengis.net/wfs");        
    }
}
