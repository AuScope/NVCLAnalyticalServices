package org.auscope.nvcl.server.service;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.auscope.nvcl.server.vo.ConfigVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;

@Component
class SendGridMailService {

    SendGrid sendGrid;
	private static final Logger logger = LogManager.getLogger(NVCLAnalyticalRequestSvc.class);

    public SendGridMailService(SendGrid sendGrid) {
        this.sendGrid = sendGrid;
    }

	@Autowired
	private ConfigVo configVo;

    void sendMail(String recipient,String subject, String message) {
        Email from = new Email(configVo.getSysAdminEmail());
        Email to = new Email(recipient);
        Content content = new Content("text/plain", message);
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            this.sendGrid.api(request);

        } catch (IOException ex) {
            logger.debug(
					"Send Email failed. Service not configured correctly or the email server is down." + ex.toString());
        }
    }
}