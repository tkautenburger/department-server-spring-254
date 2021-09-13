package de.legendlime.tester.config.cert;

import java.security.KeyStore;
import java.security.KeyStoreException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.vault.support.CertificateBundle;

/**
 * Spring bean containing the keystore certificate and private key from Vault CA 
 * keystore is generated with data from certificate bundle acquired from Vault
 * 
 * @author  Thomas Kautenburger
 * @version 1.0
 *
 */

@Component
@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
public class KeyStoreBean {
	
	private KeyStore keyStore;
	private CertificateBundle certBundle;
	
	public KeyStoreBean(CertificateBundleBean myCertificateBundle) throws KeyStoreException {
		this.certBundle = myCertificateBundle.getCertificateBundle();
		renew(myCertificateBundle);
	}

	public KeyStore getKeyStore() {
		return keyStore;
	}

	public void renew(CertificateBundleBean myCertificateBundle) throws KeyStoreException {
		certBundle = myCertificateBundle.getCertificateBundle();
		keyStore = certBundle.createKeyStore("vault");
	}

}
