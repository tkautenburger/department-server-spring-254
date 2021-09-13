package de.legendlime.tester.config.client;

import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
public class RestTemplateBean {
	
	private RestTemplate restTemplate;
	
	class SecureRequestFactorySupplier implements Supplier<ClientHttpRequestFactory> {

		private ClientHttpRequestFactory factory;
		
		public SecureRequestFactorySupplier(ClientHttpRequestFactoryBean factoryBean) {
			this.factory = factoryBean.getFactory();
		}

		@Override
		public ClientHttpRequestFactory get() {
			return this.factory;
		}
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	public RestTemplateBean(RestTemplateBuilder restTemplateBuilder, ClientHttpRequestFactoryBean factory) {
		this.restTemplate = restTemplateBuilder.requestFactory(new SecureRequestFactorySupplier(factory)).build();
	}
	
	public void renew(RestTemplateBuilder restTemplateBuilder, ClientHttpRequestFactoryBean factory) {
		this.restTemplate = restTemplateBuilder.requestFactory(new SecureRequestFactorySupplier(factory)).build();
	}

}
