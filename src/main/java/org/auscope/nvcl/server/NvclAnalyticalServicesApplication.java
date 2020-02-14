package org.auscope.nvcl.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.jms.ConnectionFactory;

import org.apache.activemq.command.ActiveMQQueue;
import org.auscope.nvcl.server.service.DataAccess;
import org.auscope.nvcl.server.service.NVCLAnalyticalGateway;
import org.auscope.nvcl.server.service.NVCLAnalyticalMessageConverter;
import org.auscope.nvcl.server.service.NVCLAnalyticalQueueBrowser;
import org.auscope.nvcl.server.service.NVCLAnalyticalRequestSvc;
import org.auscope.nvcl.server.service.TSGScriptCache;
import org.auscope.nvcl.server.vo.ConfigVo;

@SpringBootApplication
@Configuration
public class NvclAnalyticalServicesApplication {

    private JavaMailSenderImpl mailSender = null;
    private JmsTemplate jmsTemplate = null;
    private NVCLAnalyticalMessageConverter nvclAnalyticalMessageConverter = null;
    private ConfigVo config = null;

    private ActiveMQQueue nvclSubmitDestination = null;
    private ActiveMQQueue nvclStatusDestination = null;
    private ActiveMQQueue nvclResultDestination = null;
    private NVCLAnalyticalRequestSvc nvclAnalyticalRequestSvc;
    private DataAccess dataAccess = null;
    private NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser = null;
    private MessageListenerAdapter nvclAnalyticalRequestListener = null;
    private SimpleMessageListenerContainer nvclAnalyticalRequestContainer = null;
    private NVCLAnalyticalGateway nvclAnalyticalGateway =null;
    private TSGScriptCache tsgscripts = null;
    
	public static void main(String[] args) {
		SpringApplication.run(NvclAnalyticalServicesApplication.class, args);
	}
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*");
            }
        };
    }
    
    @Bean("createConfig")
    @ConfigurationProperties
	public ConfigVo createConfig() {
        if (this.config==null) {
            this.config = new ConfigVo();
            return this.config;
        }
        else return this.config;
    }

    @Bean
    @DependsOn({"createConfig"})
	public TSGScriptCache tsgscripts() {
        if (this.tsgscripts==null) {
            this.tsgscripts = new TSGScriptCache();
            return this.tsgscripts;
        }
        else return this.tsgscripts;
    }


    @Bean
    @DependsOn({"createConfig"})
    public MessageConverter nvclAnalyticalMessageConverter() {
        if (this.nvclAnalyticalMessageConverter==null) {
            this.nvclAnalyticalMessageConverter = new NVCLAnalyticalMessageConverter();
            return this.nvclAnalyticalMessageConverter;
        }
        else
            return this.nvclAnalyticalMessageConverter;

    }
    
    @Bean
    @DependsOn({"createConfig"})
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory){
        if (this.jmsTemplate==null) {
            this.jmsTemplate = new JmsTemplate();
            this.jmsTemplate.setConnectionFactory(connectionFactory);
            this.jmsTemplate.setMessageConverter(nvclAnalyticalMessageConverter());
            return this.jmsTemplate;
        }
        else
            return this.jmsTemplate;
    }

    @Bean
    @DependsOn({"createConfig"})
    @ConfigurationProperties(prefix = "smtp")
    public JavaMailSenderImpl mailSender(){
        if (this.mailSender==null) {
            this.mailSender = new JavaMailSenderImpl();
            return this.mailSender;
        }
        else
            return this.mailSender;
    }

    @Bean
    @DependsOn({"createConfig"})
    public ActiveMQQueue nvclSubmitDestination(){
        if (this.nvclSubmitDestination==null) {
            this.nvclSubmitDestination = new ActiveMQQueue("nvcl.submit.queue");
            return this.nvclSubmitDestination;
        }
        else
            return this.nvclSubmitDestination;
    }
    
    @Bean
    @DependsOn({"createConfig"})
    public ActiveMQQueue nvclStatusDestination(){
        if (this.nvclStatusDestination==null) {
            this.nvclStatusDestination = new ActiveMQQueue("nvcl.status.queue");
            return this.nvclStatusDestination;
        }
        else
            return this.nvclStatusDestination;
    }
    
    @Bean
    @DependsOn({"createConfig"})
    public ActiveMQQueue nvclResultDestination(){
        if (this.nvclResultDestination==null) {
            this.nvclResultDestination = new ActiveMQQueue("nvcl.result.queue");
            return this.nvclResultDestination;
        }
        else
            return this.nvclResultDestination;
    }

    @Bean
    @DependsOn({"createConfig"})
    public DataAccess dataAccess(){
        if (this.dataAccess==null) {
            this.dataAccess = new DataAccess();
            this.dataAccess.setCachePath(this.config.getDataCachePath());
            return this.dataAccess;
        }
        else
            return this.dataAccess;
    }
    @Bean
    @DependsOn({"createConfig"})
    public NVCLAnalyticalRequestSvc nvclAnalyticalRequestSvc(ConnectionFactory connectionFactory){
        if (this.nvclAnalyticalRequestSvc==null) {
            this.nvclAnalyticalRequestSvc = new NVCLAnalyticalRequestSvc();
            this.nvclAnalyticalRequestSvc.setJmsTemplate(jmsTemplate(connectionFactory));
            this.nvclAnalyticalRequestSvc.setStatus(nvclStatusDestination());
            this.nvclAnalyticalRequestSvc.setResult(nvclResultDestination());
            this.nvclAnalyticalRequestSvc.setMailSender(mailSender());
            this.nvclAnalyticalRequestSvc.setConfig(createConfig());
            this.nvclAnalyticalRequestSvc.setDataAccess(dataAccess());
            return this.nvclAnalyticalRequestSvc;
        }
        else
            return this.nvclAnalyticalRequestSvc;
    }

    @Bean
    @DependsOn({"createConfig"})
    public NVCLAnalyticalQueueBrowser nvclAnalyticalQueueBrowser(ConnectionFactory connectionFactory){
        if (this.nvclAnalyticalQueueBrowser==null) {
            this.nvclAnalyticalQueueBrowser = new NVCLAnalyticalQueueBrowser();
            this.nvclAnalyticalQueueBrowser.setJmsTemplate(jmsTemplate(connectionFactory));
            return this.nvclAnalyticalQueueBrowser;
        }
        else
            return this.nvclAnalyticalQueueBrowser;
    }

    @Bean
    @DependsOn({"createConfig"})
    public MessageListenerAdapter nvclAnalyticalRequestListener(ConnectionFactory connectionFactory){
        if (this.nvclAnalyticalRequestListener==null) {
            this.nvclAnalyticalRequestListener = new MessageListenerAdapter();
            this.nvclAnalyticalRequestListener.setDelegate(nvclAnalyticalRequestSvc(connectionFactory));
            this.nvclAnalyticalRequestListener.setDefaultListenerMethod("processRequest");
            this.nvclAnalyticalRequestListener.setMessageConverter(nvclAnalyticalMessageConverter());
            this.nvclAnalyticalRequestListener.setDefaultResponseDestination(nvclStatusDestination());
            return this.nvclAnalyticalRequestListener;
        }
        else
            return this.nvclAnalyticalRequestListener;
    }

    @Bean
    @DependsOn({"createConfig"})
    public SimpleMessageListenerContainer nvclAnalyticalRequestContainer(ConnectionFactory connectionFactory){
        if (this.nvclAnalyticalRequestContainer==null) {
            this.nvclAnalyticalRequestContainer = new SimpleMessageListenerContainer();
            this.nvclAnalyticalRequestContainer.setConnectionFactory(connectionFactory);
            this.nvclAnalyticalRequestContainer.setDestination(nvclSubmitDestination());
            this.nvclAnalyticalRequestContainer.setMessageListener(nvclAnalyticalRequestListener(connectionFactory));
            return this.nvclAnalyticalRequestContainer;
        }
        else
            return this.nvclAnalyticalRequestContainer;
    }
    @Bean
    @DependsOn({"createConfig"})
    public NVCLAnalyticalGateway nvclAnalyticalGateway(ConnectionFactory connectionFactory){
        if (this.nvclAnalyticalGateway==null) {
            this.nvclAnalyticalGateway = new NVCLAnalyticalGateway();
            this.nvclAnalyticalGateway.setJmsTemplate(jmsTemplate(connectionFactory));
            return this.nvclAnalyticalGateway;
        }
        else
            return this.nvclAnalyticalGateway;
    }
    
}
