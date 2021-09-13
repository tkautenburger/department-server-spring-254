package de.legendlime.tester.config.cert;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.CertificateBundle;

import de.legendlime.tester.config.vault.VaultCertificateUtil;
import de.legendlime.tester.config.vault.VaultPkiProperties;

/**
 * Spring bean containing the certificateBundle acquired from the Vault CA 
 * 
 * @author  Thomas Kautenburger
 * @version 1.0
 *
 */

@Component
@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
public class CertificateBundleBean {

	private final static Logger logger = LoggerFactory.getLogger(CertificateBundleBean.class);
	
	// Unique name of the lock for the group of services to be synchronized
	private final static String LOCK_NAME = "example-lock";

	private CertificateBundle certificateBundle;
	private VaultProperties vaultProperties;
	private VaultOperations vaultOperations;
	private VaultPkiProperties pkiProperties;
	private RedissonClient redissonClient;

	public CertificateBundleBean(VaultProperties vaultProperties, VaultOperations vaultOperations,
			VaultPkiProperties pkiProperties, RedissonClient redissonClient) throws InterruptedException {

		this.vaultProperties = vaultProperties;
		this.vaultOperations = vaultOperations;
		this.pkiProperties = pkiProperties;
		this.redissonClient = redissonClient;
		
		renew();
	}

	public CertificateBundle getCertificateBundle() {
		return certificateBundle;
	}

	public void renew() throws InterruptedException {
		
		// Redis lock to synchronize multiple services racing for certificate requests
		RLock lock = redissonClient.getLock(LOCK_NAME);

		certificateBundle = VaultCertificateUtil.findValidCertificate(vaultProperties, vaultOperations, pkiProperties);
		if (certificateBundle == null) {
			boolean locked = lock.tryLock(pkiProperties.getStartupLockTimeout(), TimeUnit.MILLISECONDS);
			if (!locked) {
				// timeout is up and still no lock, synchronizations with other services failed
				logger.warn("Failed to aquire Redis lock");
				throw new IllegalStateException(String.format("Could not obtain SSL synchronization lock within %d %s",
						pkiProperties.getStartupLockTimeout(), TimeUnit.MILLISECONDS));
			}
			try {
				// Got the lock, get the current certificate or request a new one, if the current is expired
				logger.info("Acquired Redis lock, get certificate from Vault");
				certificateBundle = VaultCertificateUtil.getOrRequestCertificate(vaultProperties, vaultOperations,
						pkiProperties);
			} finally {
				lock.unlock();
			}
		}
	}

}
