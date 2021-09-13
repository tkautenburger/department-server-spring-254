package de.legendlime.tester.config.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingFilterConfig {
 
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter
          = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(2000);
        filter.setIncludeHeaders(true);
        filter.setBeforeMessagePrefix("BEFORE REQUEST PROCESSING : ");
        filter.setAfterMessagePrefix("AFTER REQUEST PROCESSING : ");
        return filter;
    }
}