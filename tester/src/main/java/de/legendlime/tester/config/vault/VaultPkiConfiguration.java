package de.legendlime.tester.config.vault;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.legendlime.tester.config.tomcat.SslStoreProviderBean;


/**
 * Customization of the tomcat servlet container SSL configuration. Configuration
 * changes require the reload of the SSL configuration via JMX management bean. This
 * code is based on the example code from Mark Paluch
 * 
 * @author  Thomas Kautenburger
 * @version 1.0
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VaultPkiProperties.class)
public class VaultPkiConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
	public static SslCertificateEmbeddedServletContainerCustomizer sslCertificateRequestingPostProcessor(
			ServerProperties serverProperties, SslStoreProviderBean sslStoreProviderBean) throws Exception {
		
		return createCustomizer(serverProperties, sslStoreProviderBean);
	}


	private static SslCertificateEmbeddedServletContainerCustomizer createCustomizer(
			ServerProperties serverProperties, SslStoreProviderBean sslStoreProviderBean) {

		Ssl ssl = serverProperties.getSsl();
		
		if (ssl != null) {
			ssl.setKeyAlias("vault");
			ssl.setKeyPassword("");
			ssl.setKeyStorePassword("");
		}
		return new SslCertificateEmbeddedServletContainerCustomizer(sslStoreProviderBean);
	}

	
	private static class SslCertificateEmbeddedServletContainerCustomizer
			implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

		private final SslStoreProvider sslStoreProvider;

		SslCertificateEmbeddedServletContainerCustomizer(SslStoreProviderBean sslStoreProviderBean) {
			this.sslStoreProvider = sslStoreProviderBean;
		}

		@Override
		public void customize(ConfigurableServletWebServerFactory container) {
			container.setSslStoreProvider(sslStoreProvider);
		}
	}
}
