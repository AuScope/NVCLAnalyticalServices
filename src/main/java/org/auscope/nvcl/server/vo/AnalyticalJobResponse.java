package org.auscope.nvcl.server.vo;
/**
 * AnalyticalJobResponse
 * and allow getting the values thru getter and setter method.
 * 
 * @author Lingbo Jiang
 */
public class AnalyticalJobResponse {
        private String response;
        private String message;
        public AnalyticalJobResponse(String response, String message) { 
           this.setResponse(response);
           this.setMessage(message);
        }
        public String getResponse() {
            return response;
        }
        public void setResponse(String response) {
            this.response = response;
        }
        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
}
