package de.legendlime.tester.config.client;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import de.legendlime.tester.config.cert.KeyStoreBean;
import de.legendlime.tester.config.cert.TrustStoreBean;

/**
 * Bean containing the HTTP request factory for REST clients with the
 * current valid certificate information for the service
 * 
 * @author  Thomas Kautenburger
 * @version 1.0
 *
 */

@Component
@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
public class ClientHttpRequestFactoryBean {
	
	private final static Logger logger = LoggerFactory.getLogger(ClientHttpRequestFactoryBean.class);
	
	private KeyStore keyStore, trustStore;
	private HttpComponentsClientHttpRequestFactory factory;
	
	public ClientHttpRequestFactoryBean(KeyStoreBean keyStoreBean, TrustStoreBean trustStoreBean) 
			throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
		renew(keyStoreBean, trustStoreBean);
	}

	public HttpComponentsClientHttpRequestFactory getFactory() {
		return factory;
	}

	public void setFactory(HttpComponentsClientHttpRequestFactory factory) {
		this.factory = factory;
	}
	
	public void renew(KeyStoreBean keyStoreBean, TrustStoreBean trustStoreBean) 
			throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
		this.keyStore = keyStoreBean.getKeyStore();
		this.trustStore = trustStoreBean.getTrustStore();
		
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(new SSLContextBuilder()
				.loadTrustMaterial(this.trustStore, null)
				.loadKeyMaterial(this.keyStore, "".toCharArray())
				.build());

		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		HttpClient httpClient = clientBuilder.setSSLSocketFactory(socketFactory).build();
		
		this.factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		logger.debug("Renewed HTTP request factory with certificate: {}", this.keyStore.getCertificate("vault") );
	}
}
