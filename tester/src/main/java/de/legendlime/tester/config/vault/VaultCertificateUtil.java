package de.legendlime.tester.config.vault;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.CertificateBundle;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateResponse;
import org.springframework.vault.support.VaultHealth;
import org.springframework.vault.support.VaultResponseSupport;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Helper functions for certificate handling with vault. Based on the original
 * code from Mark Paluch from the Vault developer team
 * 
 * @author  Thomas Kautenburger
 * @version 1.0
 *
 */

public final class VaultCertificateUtil {
	
	private final static Logger logger = LoggerFactory.getLogger(VaultCertificateUtil.class);
	
	// Refresh period in seconds before certificate expires.
	private final static long REFRESH_PERIOD_BEFORE_EXPIRY = 60;
	
	// time stamp with the current certificate's expiration time
	private static long expires = 0L;
	
	/**
	 * Request SSL Certificate from Vault or retrieve cached certificate.
	 * <p>
	 * If {@link VaultPkiProperties#isReuseValidCertificate()} is enabled this method
	 * attempts to read a cached Certificate from Vault at {@code secret/$
	 * spring.application.name}/cert/${spring.cloud.vault.pki.commonName}}. Valid
	 * certificates will be reused until they expire. A new certificate is requested and
	 * cached if no valid certificate is found.
	 *
	 * @param vaultProperties
	 * @param vaultOperations
	 * @param pkiProperties
	 * @return the {@link CertificateBundle}.
	 */
	public static CertificateBundle getOrRequestCertificate(
			VaultProperties vaultProperties, VaultOperations vaultOperations,
			VaultPkiProperties pkiProperties) {

		CertificateBundle validCertificate = findValidCertificate(vaultProperties,
				vaultOperations, pkiProperties);

		if (!pkiProperties.isReuseValidCertificate()) {
			return validCertificate;
		}

		String cacheKey = createCacheKey(vaultProperties, pkiProperties);
		vaultOperations.delete(cacheKey);

		VaultCertificateResponse certificateResponse = requestCertificate(
				vaultOperations, pkiProperties);

		VaultHealth health = vaultOperations.opsForSys().health();
		storeCertificate(cacheKey, vaultOperations, health, certificateResponse);

		return certificateResponse.getData();
	}
	
	/**
	 * Find a valid, possibly cached, {@link CertificateBundle}.
	 *
	 * @param vaultProperties
	 * @param vaultOperations
	 * @param pkiProperties
	 * @return the {@link CertificateBundle} or {@literal null}.
	 */
	public static CertificateBundle findValidCertificate(VaultProperties vaultProperties,
			VaultOperations vaultOperations, VaultPkiProperties pkiProperties) {

		if (!pkiProperties.isReuseValidCertificate()) {
			return requestCertificate(vaultOperations, pkiProperties).getData();
		}

		String cacheKey = createCacheKey(vaultProperties, pkiProperties);

		VaultResponseSupport<CachedCertificateBundle> readResponse = vaultOperations
				.read(cacheKey, CachedCertificateBundle.class);

		VaultHealth health = vaultOperations.opsForSys().health();
		if (isValid(health, readResponse)) {

			logger.info("Found valid SSL certificate in Vault for: {}",
					pkiProperties.getCommonName());
			expires = readResponse.getData().expires * 1000;
			return getCertificateBundle(readResponse);
		}

		return null;
	}

	private static void storeCertificate(String cacheKey,
			VaultOperations vaultOperations, VaultHealth health,
			VaultCertificateResponse certificateResponse) {

		CertificateBundle certificateBundle = certificateResponse.getData();
		long secondExpires = (certificateBundle.getX509Certificate().getNotAfter().getTime() / 1000) - REFRESH_PERIOD_BEFORE_EXPIRY;
		
		// the the expiration time stamp
		expires = secondExpires * 1000;
		
		CachedCertificateBundle cachedCertificateBundle = new CachedCertificateBundle();

		cachedCertificateBundle.setExpires(secondExpires);
		cachedCertificateBundle.setTimeRequested(health.getServerTimeUtc());
		cachedCertificateBundle.setPrivateKey(certificateBundle.getPrivateKey());
		cachedCertificateBundle.setCertificate(certificateBundle.getCertificate());
		cachedCertificateBundle.setIssuingCaCertificate(certificateBundle
				.getIssuingCaCertificate());
		cachedCertificateBundle.setSerialNumber(certificateBundle.getSerialNumber());

		vaultOperations.write(cacheKey, cachedCertificateBundle);
	}
	
	/**
	 * Gets the current certificate's expiration time stamp
	 * @return expiration time stamp
	 */
	public static long getExpires() {
		return expires;
	}

	private static String createCacheKey(VaultProperties vaultProperties,
			VaultPkiProperties pkiProperties) {

		return String.format("secret/%s/cert/%s", vaultProperties.getApplicationName(),
				pkiProperties.getCommonName());
	}

	private static CertificateBundle getCertificateBundle(
			VaultResponseSupport<CachedCertificateBundle> readResponse) {

		CachedCertificateBundle cachedCertificateBundle = readResponse.getData();

		return CertificateBundle.of(cachedCertificateBundle.getSerialNumber(),
				cachedCertificateBundle.getCertificate(),
				cachedCertificateBundle.getIssuingCaCertificate(),
				cachedCertificateBundle.getPrivateKey());
	}

	private static boolean isValid(VaultHealth health,
			VaultResponseSupport<CachedCertificateBundle> readResponse) {

		if (readResponse != null) {

			CachedCertificateBundle cachedCertificateBundle = readResponse.getData();
			if (health.getServerTimeUtc() < cachedCertificateBundle.getExpires()) {
				return true;
			}
		}
		// logger.info("Certificate not valid, expiration time less than current time");
		return false;
	}

	public static VaultCertificateResponse requestCertificate(
			VaultOperations vaultOperations, VaultPkiProperties pkiProperties) {

		logger.info("Requesting SSL certificate from Vault for: {}",
				pkiProperties.getCommonName());

		VaultCertificateRequest certificateRequest = VaultCertificateRequest
				.builder()
				.commonName(pkiProperties.getCommonName())
				.altNames(
						pkiProperties.getAltNames() != null ? pkiProperties.getAltNames()
								: Collections.<String> emptyList()).build();

		VaultCertificateResponse certificateResponse = vaultOperations.opsForPki(
				pkiProperties.getBackend()).issueCertificate(pkiProperties.getRole(),
				certificateRequest);

		return certificateResponse;
	}

	static class CachedCertificateBundle {

		private String certificate;

		@JsonProperty("serial_number")
		private String serialNumber;

		@JsonProperty("issuing_ca")
		private String issuingCaCertificate;

		@JsonProperty("private_key")
		private String privateKey;

		@JsonProperty("time_requested")
		private long timeRequested;

		@JsonProperty("expires")
		private long expires;

		public CachedCertificateBundle() {
			super();
		}

		public String getCertificate() {
			return certificate;
		}

		public void setCertificate(String certificate) {
			this.certificate = certificate;
		}

		public String getSerialNumber() {
			return serialNumber;
		}

		public void setSerialNumber(String serialNumber) {
			this.serialNumber = serialNumber;
		}

		public String getIssuingCaCertificate() {
			return issuingCaCertificate;
		}

		public void setIssuingCaCertificate(String issuingCaCertificate) {
			this.issuingCaCertificate = issuingCaCertificate;
		}

		public String getPrivateKey() {
			return privateKey;
		}

		public void setPrivateKey(String privateKey) {
			this.privateKey = privateKey;
		}

		public long getTimeRequested() {
			return timeRequested;
		}

		public void setTimeRequested(long timeRequested) {
			this.timeRequested = timeRequested;
		}

		public long getExpires() {
			return expires;
		}

		public void setExpires(long expires) {
			this.expires = expires;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((certificate == null) ? 0 : certificate.hashCode());
			result = prime * result + (int) (expires ^ (expires >>> 32));
			result = prime * result + ((issuingCaCertificate == null) ? 0 : issuingCaCertificate.hashCode());
			result = prime * result + ((privateKey == null) ? 0 : privateKey.hashCode());
			result = prime * result + ((serialNumber == null) ? 0 : serialNumber.hashCode());
			result = prime * result + (int) (timeRequested ^ (timeRequested >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CachedCertificateBundle other = (CachedCertificateBundle) obj;
			if (certificate == null) {
				if (other.certificate != null)
					return false;
			} else if (!certificate.equals(other.certificate))
				return false;
			if (expires != other.expires)
				return false;
			if (issuingCaCertificate == null) {
				if (other.issuingCaCertificate != null)
					return false;
			} else if (!issuingCaCertificate.equals(other.issuingCaCertificate))
				return false;
			if (privateKey == null) {
				if (other.privateKey != null)
					return false;
			} else if (!privateKey.equals(other.privateKey))
				return false;
			if (serialNumber == null) {
				if (other.serialNumber != null)
					return false;
			} else if (!serialNumber.equals(other.serialNumber))
				return false;
			if (timeRequested != other.timeRequested)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "CachedCertificateBundle [certificate=" + certificate + ", serialNumber=" + serialNumber
					+ ", issuingCaCertificate=" + issuingCaCertificate + ", privateKey=" + privateKey
					+ ", timeRequested=" + timeRequested + ", expires=" + expires + "]";
		}
	}
}
