package de.legendlime.tester.config.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.Tracer;

@Configuration
public class FilterConfig {
	
	@Autowired
	Tracer tracer;

	@Autowired
	private ApplicationContext applicationContext;
	
	@Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new RequestLoggingFilter(tracer, applicationContext));
        registrationBean.addUrlPatterns("/v1/*");

        return registrationBean;
    }
	
	@Bean
    public FilterRegistrationBean<ResponseLoggingFilter> responseLoggingFilter() {
        FilterRegistrationBean<ResponseLoggingFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new ResponseLoggingFilter(tracer, applicationContext));
        registrationBean.addUrlPatterns("/v1/*");

        return registrationBean;
    }
}
