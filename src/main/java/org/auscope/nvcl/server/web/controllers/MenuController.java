package org.auscope.nvcl.server.web.controllers;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.service.NVCLAnalyticalRequestSvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller that handles all {@link Menu}-related requests
 *
 * @author Florence Tan
 * @author Peter Warren (CSIRO Earth Science and Resource Engineering)
 */

@Controller
public class MenuController {

    private static final Logger logger = LogManager.getLogger(MenuController.class);


    @RequestMapping("/")
    public String index(HttpServletRequest request, HttpServletResponse response) {

        return "index";
    }


    /**
     * Handling error code 403 and error code 500
     *
     * @return
     */
    @RequestMapping("/error_page.html")
    public ModelAndView access_error() {
        String errMsg = "Please contact the system administrator.";
        return new ModelAndView("error_page", "errmsg", errMsg);
    }

    @RequestMapping("/testlinks.html")
    public String testlinks(HttpServletRequest request, HttpServletResponse response) {

        return "testlinks";
    }

    @RequestMapping("/tsgdemo.html")
    public String tsgdemo(HttpServletRequest request, HttpServletResponse response) {

        return "tsgdemo";
    }

    /**
     * Handling request when doDownloadscalar.do is called. Will make request
     * to NVCLAnalytical to retrieve the scalars values, set to csv file and 
     * response back as a csv file
     *
     * @param request 
     * @param response
     * @param jobid  as string 
     * @param boreholeid as string
     *
     * @return csv file containing scalar result
     *
     * @throws ServletException
     * @throws IOException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @RequestMapping("/doDownloadscalar.do")
    public ModelAndView doDownloadscalar(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = true, value = "jobid") String jobid,
            @RequestParam(required = true, value = "boreholeid") String boreholeid)
            throws ServletException, IOException, Exception {

        // mandatory field : logid
        // ensure logid is not null, empty or missing
        if (jobid == null || jobid.length() == 0) {
            String errMsg = "A valid jobid must be provided for this service to function.";
            return new ModelAndView("error_page", "errmsg", errMsg);
        }
        if (boreholeid == null || boreholeid.length() == 0) {
            String errMsg = "A valid boreholeid must be provided for this service to function.";
            return new ModelAndView("error_page", "errmsg", errMsg);
        }
        /**
         * Data verification : a) check that the log id(s) can be found in LOGS
         * table b) check that the log id(s) all having the same DOMAINLOG_ID c)
         * check that the log id(s) will have only log type 1 or 2
         */
        try {
            // set response as csv attachement
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\""+ boreholeid + ".csv\"");

            // writing scalar details as csv file using opencsv lib
            // BufferedWriter out = new BufferedWriter(new
            // FileWriter("scalars.csv"));
            BufferedWriter out = new BufferedWriter(response.getWriter());
            //CSVWriter writer = new CSVWriter(out, ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, "\n");
            
            String dataPath = NVCLAnalyticalRequestSvc.config.getDataPath();
            String csvFile = dataPath + jobid + "/" + boreholeid + "-scalar.csv";
            String line = "";
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                while ((line = br.readLine()) != null) {
                    out.write(line);
                    out.newLine();
                }
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        } catch (Exception e) {
            logger.error("Exception occurred in DownloadScalarHandler : " + e);
            String errMsg = "Exception occurred : " + e;
            return new ModelAndView("error_page", "errmsg", errMsg);
        }
    }    
}
