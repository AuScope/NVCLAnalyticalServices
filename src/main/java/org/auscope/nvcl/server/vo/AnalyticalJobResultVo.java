package org.auscope.nvcl.server.vo;

import java.util.ArrayList;
import java.util.List;


/**
 * AnalyticalJobResultVo
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */

public class AnalyticalJobResultVo {
    private String jobid;
    private String jobDescription;
    private String email;
    public List<BoreholeResultVo> boreholes = new ArrayList<BoreholeResultVo>();
    public List<BoreholeResultVo> failedBoreholes = new ArrayList<BoreholeResultVo>();
    public List<BoreholeResultVo> errorBoreholes = new ArrayList<BoreholeResultVo>();
    
    public AnalyticalJobResultVo() {
    }
    
    public String getJobid() {
        return jobid;
    }

    public void setJobid(String jobid) {
        this.jobid = jobid;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public void addBoreholes(BoreholeResultVo boreholeVo) {
        boreholes.add(boreholeVo);
    }
    
    public void addFailedBoreholes(BoreholeResultVo boreholeVo) {
        failedBoreholes.add(boreholeVo);
    }
    public void addErrorBoreholes(BoreholeResultVo boreholeVo) {
        errorBoreholes.add(boreholeVo);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
