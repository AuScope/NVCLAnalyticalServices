package org.auscope.nvcl.server.vo;
/**
 * BoreholeResultVo
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */
public class BoreholeResultVo {
    private String id;
    private String msg;
    public BoreholeResultVo() {
    }
    public BoreholeResultVo(String id, String msg) {
        this.id = id;
        this.msg = msg;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
}
