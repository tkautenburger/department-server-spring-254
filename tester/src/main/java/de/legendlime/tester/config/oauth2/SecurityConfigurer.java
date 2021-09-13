package de.legendlime.tester.config.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.legendlime.tester.config.opa.OPAVoter;
import de.legendlime.tester.config.client.RestTemplateBean;
import de.legendlime.tester.config.opa.OPAProperties;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * SecurityConfigurer is to configure ResourceServer and HTTP Security.
 * <p>
 *   Please make sure you check HTTP Security configuration and change is as per your needs.
 * </p>
 *
 * Note: Use {@link SecurityProperties} to configure required CORs configuration and enable or disable security of application.
 */
@Configuration
@EnableWebSecurity
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(prefix = "rest.security", value = "enabled", havingValue = "true")
@Import({SecurityProperties.class})
public class SecurityConfigurer extends ResourceServerConfigurerAdapter {

	 @Autowired
	  RestTemplateBean restTemplateBean;
	  
	  private ResourceServerProperties resourceServerProperties;
	  private SecurityProperties securityProperties;
	  private OPAProperties opaProperties;

	  /* Using spring constructor injection, @Autowired is implicit */
	  public SecurityConfigurer(ResourceServerProperties resourceServerProperties, 
			  SecurityProperties securityProperties, OPAProperties opaProperties) {
	    this.resourceServerProperties = resourceServerProperties;
	    this.securityProperties = securityProperties;
	    this.opaProperties = opaProperties;
	  }

  @Override
  public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
    resources.resourceId(resourceServerProperties.getResourceId());
  }


  /**
   * Set CORS and CSRF settings and provide Spring Boot security configurations. 
   * All requests to URLs from the application properties antMatcher pattern must be authenticated, all others not.
   * Actuator calls for readiness and liveness probes do not need to be TLS-secured, any others must.
   * RBAC is provided in the controller mappings.
   */
  @Override
  public void configure(final HttpSecurity http) throws Exception {
	  
	if (opaProperties.isEnabled()) {
	  // OPA enabled, use OPA access decision manager for policy based authorization
	  http.cors()
	      .configurationSource(corsConfigurationSource())
        .and()
          .headers()
          .frameOptions().disable()
	    .and()
	      // .csrf().disable() CWE-352
        .authorizeRequests()
          .antMatchers(securityProperties.getApiMatcher()).authenticated()
            // next line is for OPA policy based security
            .accessDecisionManager(accessDecisionManager())
        .and()
          .requiresChannel()
            .antMatchers("/actuator/**").requiresInsecure()
          .anyRequest().requiresSecure();
	} else {
	  // OPA disabled, use standard Spring Boot RBAC authorization
	  http.cors()
	      .configurationSource(corsConfigurationSource())
        .and()
          .headers()
          .frameOptions().disable()
	    .and()
	      // .csrf().disable() CWE-352
        .authorizeRequests()
          .antMatchers(securityProperties.getApiMatcher()).authenticated()
        .and()
          .requiresChannel()
            .antMatchers("/actuator/**").requiresInsecure()
          .anyRequest().requiresSecure();
	}
	
  }
  
  @Bean
  @ConditionalOnProperty(prefix = "legendlime.opa", name = "enabled", havingValue = "true")
  public AccessDecisionManager accessDecisionManager() {
      List<AccessDecisionVoter<? extends Object>> decisionVoters = Arrays
              .asList(new OPAVoter(opaProperties.getOpaUrl(), restTemplateBean, opaProperties));
      return new UnanimousBased(decisionVoters);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    if (null != securityProperties.getCorsConfiguration()) {
      source.registerCorsConfiguration("/**", securityProperties.getCorsConfiguration());
    }
    return source;
  }

  @Bean
  public JwtAccessTokenCustomizer jwtAccessTokenCustomizer(ObjectMapper mapper) {
    return new JwtAccessTokenCustomizer(mapper);
  }
}
