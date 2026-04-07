package org.auscope.nvcl.server.vo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import au.com.bytecode.opencsv.CSVWriter;

public class TSAScalarArrayVo {
  private static final Logger logger = LogManager.getLogger(TSAScalarArrayVo.class);
  public List<TSAScalarVo> scalarArray = new ArrayList<TSAScalarVo>();
  private Map<String, Integer> mapHeader = new HashMap();

  public TSAScalarArrayVo() {
  }

  public void add(TSAScalarVo tsaScalar) {
    String[] header = tsaScalar.getHeader();
    for (String prop : header) {
      if (mapHeader.get(prop) == null) {
        mapHeader.put(prop, 1);
      }
    }
    scalarArray.add(tsaScalar);
  }

  public int writeScalarCSV(String fileName) {
    CSVWriter writer = null;
    try {
      File file = new File(fileName);
      boolean writeHeader = !file.exists() || file.length() == 0;

      writer = new CSVWriter(new FileWriter(fileName, true));

      // Write header only if file is new or empty
      if (writeHeader) {
        Iterator<Map.Entry<String, Integer>> it = this.mapHeader.entrySet().iterator();
        String[] rowHeader = new String[this.mapHeader.size()];
        int i = 0;

        while (it.hasNext()) {
          Map.Entry<String, Integer> pair = it.next();
          rowHeader[i++] = pair.getKey().toString();
        }
        writer.writeNext(rowHeader);
      }

      // Write data rows
      String[] rowValues = new String[this.mapHeader.size()];
      for (TSAScalarVo tsaScalar : scalarArray) {
        Iterator<Map.Entry<String, Integer>> it = this.mapHeader.entrySet().iterator();
        int i = 0;

        while (it.hasNext()) {
          Map.Entry<String, Integer> pair = it.next();
          String value = tsaScalar.get(pair.getKey().toString());
          rowValues[i++] = value;
        }
        writer.writeNext(rowValues);
      }

    } catch (IOException e) {
      logger.error("failed to write CSV " + fileName + " Exception was:", e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException ignored) {
        }
      }
    }

    return scalarArray.size();
  }

}
