package org.auscope.nvcl.server.service;

import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import java.io.StringReader;

public class NVCLBHInfoCache {
	private static final Logger logger = LogManager.getLogger(NVCLBHInfoCache.class);
    public HashMap<String, String> bhInfoMap = new HashMap<String, String>();
	public NVCLBHInfoCache() {
		super();
		load();
	}
    public void load() {
        this.bhInfoMap.clear();
        try {
            String responseString = NVCLAnalyticalRequestSvc.dataAccess.getNVCLBHInfoCSV();
            String csvLine[];
            String bhInfo, bhURI;
            CSVReader reader = new CSVReader(new StringReader(responseString), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.DEFAULT_ESCAPE_CHARACTER);
            csvLine = reader.readNext();//read the header
            //find Indentifier index
            int iIndentifier = 0;
            for (int i = 0; i< csvLine.length; i++) {
                if (csvLine[i].equalsIgnoreCase("BoreholeURI"))
                    iIndentifier= i;							
            }
            if (iIndentifier > 0) {
                while ((csvLine = reader.readNext()) != null) {
                    bhInfo = String.join(",", csvLine);
                    if (csvLine.length < iIndentifier) {
                        logger.error("NVCLBHInfoCache:wrong csv:"  + bhInfo);
                        continue;
                    }
                    bhURI = csvLine[iIndentifier];
                    if (bhURI != null) {
                        this.bhInfoMap.put(bhURI,bhInfo);
                    }					
                }
                logger.info("NVCLBHInfoCache:Successed on loading NVCLBHInfoCache, and size is :" + this.bhInfoMap.size());
            }
            reader.close();
            reader = null;
        }catch (Exception ex) {
            logger.error("NVCLBHInfoCache:Failed to load NVCLBHInfoCache");
        }
    }
}
