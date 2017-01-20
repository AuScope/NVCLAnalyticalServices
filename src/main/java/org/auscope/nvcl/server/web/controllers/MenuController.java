package org.auscope.nvcl.server.web.controllers;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
