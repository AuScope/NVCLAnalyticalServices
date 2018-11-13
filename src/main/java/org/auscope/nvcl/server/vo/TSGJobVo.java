package org.auscope.nvcl.server.vo;

/**
 * TSGJobVo supply query result from getTsgJobByBoreholeid.do.
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */
public class TSGJobVo {
    private String boreholeid;
    private String jobid;
    private String jobName;
    private String tsgScript;
    private String published = Boolean.toString(true);
    public TSGJobVo(String boreholeid,String jobid,String jobName) {
        this.setBoreholeid(boreholeid);
        this.setJobid(jobid);
        this.setJobName(jobName);
    }

    public String getBoreholeid() {
        return boreholeid;
    }

    public void setBoreholeid(String boreholeid) {
        this.boreholeid = boreholeid;
    }

    public String getJobid() {
        return jobid;
    }

    public void setJobid(String jobid) {
        this.jobid = jobid;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getTsgScript() {
        return tsgScript;
    }

    public void setTsgScript(String tsgScript) {
        this.tsgScript = tsgScript;
    }

	public String getPublished() {
		return published;
	}

	public void setPublished(String published) {
		this.published = published;
	}
  
}