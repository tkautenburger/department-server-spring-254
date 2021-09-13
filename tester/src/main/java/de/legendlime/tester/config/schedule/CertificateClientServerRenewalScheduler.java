package de.legendlime.tester.config.schedule;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;

import de.legendlime.tester.config.cert.CertificateBundleBean;
import de.legendlime.tester.config.cert.KeyStoreBean;
import de.legendlime.tester.config.cert.TrustStoreBean;
import de.legendlime.tester.config.client.ClientHttpRequestFactoryBean;
import de.legendlime.tester.config.client.RestTemplateBean;
import de.legendlime.tester.config.tomcat.SslStoreProviderBean;
import de.legendlime.tester.config.vault.VaultCertificateUtil;

/**
 * Spring Bean scheduling the next certificate renewal based on the current 
 * certificate's expiration date.
 * 
 * @author  Thomas Kautenburger
 * @version 1.0
 *
 */

@Component
@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
public class CertificateClientServerRenewalScheduler {

	private final static Logger logger = LoggerFactory.getLogger(CertificateClientServerRenewalScheduler.class);

	// filter pattern to find tomcat connector threads
	private static final String JMX_THREAD_POOL_NAME = "*:type=ThreadPool,name=*";
	
	// JMX command to reload the SSL host configurations
	private static final String JMX_OPERATION_RELOAD_SSL_HOST_CONFIGS_NAME = "reloadSslHostConfigs"; 
	

	CertificateBundleBean certificateBundleBean;
	KeyStoreBean keyStoreBean;
	TrustStoreBean trustStoreBean;
	SslStoreProviderBean sslStoreProviderBean;
	ClientHttpRequestFactoryBean clientHttpRequestFactoryBean;

	ConfigurableServletWebServerFactory container;
	
	RestTemplateBean restTemplateBean = null;
	RestTemplateBuilder restTemplateBuilder;
	
	private TaskScheduler scheduler;
	
	public CertificateClientServerRenewalScheduler(CertificateBundleBean certificateBundleBean, 
			KeyStoreBean keyStoreBean, TrustStoreBean trustStoreBean, 
			SslStoreProviderBean sslStoreProviderBean, 
			ClientHttpRequestFactoryBean clientHttpRequestFactoryBean,
			RestTemplateBean restTemplateBean, RestTemplateBuilder restTemplateBuilder, 
			ConfigurableServletWebServerFactory container) {
		
		this.container = container;
		
		this.certificateBundleBean = certificateBundleBean;
		this.keyStoreBean = keyStoreBean;
		this.trustStoreBean = trustStoreBean;
		this.sslStoreProviderBean = sslStoreProviderBean;
		this.clientHttpRequestFactoryBean = clientHttpRequestFactoryBean;
		
		if (restTemplateBean != null) {
			this.restTemplateBean = restTemplateBean;
			this.restTemplateBuilder = restTemplateBuilder;
		}
		
		if (VaultCertificateUtil.getExpires() > 0L) {
			logger.info("Scheduled next certificate renewal date: {}", new Date(VaultCertificateUtil.getExpires()));
			executeCertificateRenewalTask(VaultCertificateUtil.getExpires());
		}
		
	}
	
	/**
	 *  Renew the beans for the certificate bundle, key store, trust store and the 
	 *  server's SSL store and schedule the next certificate renewal
	 */
	
	private void renewSslConfiguration() {
		try {
			certificateBundleBean.renew();
			keyStoreBean.renew(certificateBundleBean);
			trustStoreBean.renew(certificateBundleBean);
			clientHttpRequestFactoryBean.renew(keyStoreBean, trustStoreBean);
			
			if (restTemplateBean != null) {
				restTemplateBean.renew(restTemplateBuilder, clientHttpRequestFactoryBean);
				logger.info("Renewed SSL context for RestTemplate");
			}
			if (VaultCertificateUtil.getExpires() > 0L) {
				logger.info("Scheduled next certificate renewal date: {}", new Date(VaultCertificateUtil.getExpires()));
				executeCertificateRenewalTask(VaultCertificateUtil.getExpires());
			}
			
			sslStoreProviderBean.renew(keyStoreBean, trustStoreBean);
			try {
				logger.debug("Updated SSL certificate: {}", sslStoreProviderBean.getKeyStore().getCertificate("vault").toString());
			} catch (Exception e) {
				logger.error("Exception: ", e);
			}

		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
				| InterruptedException | KeyManagementException | UnrecoverableEntryException e) {
			logger.error("Renewing SSL store beans", e);
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Runnable job that renews the SSL configuration, sets the SSL store for the 
	 * tomcat container and reloads / restarts all Connectors that have an 
	 * SSL configuration
	 */

	Runnable renewCerts = new Runnable() {
		@Override
		public void run() {
			renewSslConfiguration();
			container.setSslStoreProvider(sslStoreProviderBean);
			reloadSSLConfigsOnConnectors();
		}
	};

	@Async
	public void executeCertificateRenewalTask(long time) {
		ScheduledExecutorService localExecutor = Executors.newSingleThreadScheduledExecutor();
		scheduler = new ConcurrentTaskScheduler(localExecutor);
		scheduler.schedule(renewCerts, new Date(time));
	}

	/*
	 * Helper functions to reload the SSL configuration and restart tomcat
	 * SSL configuration via JMX
	 */
	
	private void reloadSSLConfigsOnConnectors() {
	    try {
	        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
	        ObjectName objectName = new ObjectName(JMX_THREAD_POOL_NAME);
	        Set<ObjectInstance> allTP = server.queryMBeans(objectName, null);
	        logger.info("MBeans found: {}", allTP.size());
	        allTP.forEach(tp -> reloadSSLConfigOnThreadPoolJMX(server, tp));
	    } catch (Exception e) {
	        logger.error("", e);
	    }
	}

	private void reloadSSLConfigOnThreadPoolJMX(MBeanServer server, ObjectInstance tp) {
	    try {
	        logger.info("Invoking operation SSL reload on {}", tp.getObjectName());
	        server.invoke(tp.getObjectName(), JMX_OPERATION_RELOAD_SSL_HOST_CONFIGS_NAME, new Object[]{}, new String[]{});
	        logger.trace("Successfully invoked");
	    } catch (Exception e) {
	        logger.error("Invoking SSL reload", e);
	    }
	}
}
