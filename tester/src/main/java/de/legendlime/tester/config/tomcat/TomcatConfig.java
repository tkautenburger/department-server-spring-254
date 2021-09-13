package de.legendlime.tester.config.tomcat;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * Add additional insecure (HTTP) connector to tomcat servlet engine. This shall be
 * used for actuator health probes only which must be configured in WebSecurity configuration
 */

@Configuration
public class TomcatConfig {

	@Value("${server.http.port}")
	private int httpPort;

	@Bean
	public ConfigurableServletWebServerFactory webServerFactory() {

		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.addAdditionalTomcatConnectors(insecureConnector());
		return factory;
	}

	private Connector insecureConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setScheme("http");
		connector.setPort(httpPort);
		connector.setSecure(false);
		return connector;
	}

}
