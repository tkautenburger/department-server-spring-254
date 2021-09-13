package de.legendlime.tester.config.vault;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Vault PKI properties class. Extended with RedisURL for the distributed lock mechanism
 * This code is based on the example code from Mark Paluch
 * 
 * @author  Thomas Kautenburger
 * @version 1.0
 *
 */
@Component
@Configuration
@Primary
@ConfigurationProperties("pki")
@Validated
public class VaultPkiProperties {
	
	// Use @Primary annotation, otherwise it conflicts with a properties from the Vault library

	/**
	 * Enable pki backend usage.
	 */
	private boolean enabled = true;

	/**
	 * Enable file system storage for keystore and truststore.
	 */
	private boolean persistEnabled = true;

	/**
	 * Role name for credentials.
	 */
	private String role;

	/**
	 * pki backend path.
	 */
	private String backend = "pki";

	/**
	 * The CN of the certificate. Should match the host name.
	 */
	private String commonName;

	/**
	 * Alternate CN names for additional host names.
	 */
	private List<String> altNames;

	/**
	 * Prevent certificate re-creation by storing the Valid certificate inside Vault.
	 */
	private boolean reuseValidCertificate = true;

	/**
	 * Startup/Locking timeout. Used to synchronize startup and to prevent multiple SSL
	 * certificate requests.
	 */
	private int startupLockTimeout = 10000;
	
	/**
	 * URL of the redis server that provides the distributed lock
	 */
	private String redisUrl;

	public VaultPkiProperties() {
		super();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getBackend() {
		return backend;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	public String getCommonName() {
		return commonName;
	}

	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}

	public List<String> getAltNames() {
		return altNames;
	}

	public void setAltNames(List<String> altNames) {
		this.altNames = altNames;
	}

	public boolean isReuseValidCertificate() {
		return reuseValidCertificate;
	}

	public void setReuseValidCertificate(boolean reuseValidCertificate) {
		this.reuseValidCertificate = reuseValidCertificate;
	}

	public int getStartupLockTimeout() {
		return startupLockTimeout;
	}

	public void setStartupLockTimeout(int startupLockTimeout) {
		this.startupLockTimeout = startupLockTimeout;
	}

	public String getRedisUrl() {
		return redisUrl;
	}

	public void setRedisUrl(String redisUrl) {
		this.redisUrl = redisUrl;
	}

	public boolean isPersistEnabled() {
		return persistEnabled;
	}

	public void setPersistEnabled(boolean persistEnabled) {
		this.persistEnabled = persistEnabled;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((altNames == null) ? 0 : altNames.hashCode());
		result = prime * result + ((backend == null) ? 0 : backend.hashCode());
		result = prime * result + ((commonName == null) ? 0 : commonName.hashCode());
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((redisUrl == null) ? 0 : redisUrl.hashCode());
		result = prime * result + (reuseValidCertificate ? 1231 : 1237);
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + startupLockTimeout;
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
		VaultPkiProperties other = (VaultPkiProperties) obj;
		if (altNames == null) {
			if (other.altNames != null)
				return false;
		} else if (!altNames.equals(other.altNames))
			return false;
		if (backend == null) {
			if (other.backend != null)
				return false;
		} else if (!backend.equals(other.backend))
			return false;
		if (commonName == null) {
			if (other.commonName != null)
				return false;
		} else if (!commonName.equals(other.commonName))
			return false;
		if (enabled != other.enabled)
			return false;
		if (redisUrl == null) {
			if (other.redisUrl != null)
				return false;
		} else if (!redisUrl.equals(other.redisUrl))
			return false;
		if (reuseValidCertificate != other.reuseValidCertificate)
			return false;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		if (startupLockTimeout != other.startupLockTimeout)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "VaultPkiProperties [enabled=" + enabled + ", persistEnabled=" + persistEnabled + ", role=" + role
				+ ", backend=" + backend + ", commonName=" + commonName + ", altNames=" + altNames
				+ ", reuseValidCertificate=" + reuseValidCertificate + ", startupLockTimeout=" + startupLockTimeout
				+ ", redisUrl=" + redisUrl + "]";
	}	
}