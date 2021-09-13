package de.legendlime.tester.config.cert;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.vault.support.CertificateBundle;

/**
 * Spring bean containing the trust store certificates from Vault CA 
 * trust store is generated with data from certificate bundle acquired from Vault
 * 
 * @author  Thomas Kautenburger
 * @version 1.0
 *
 */

@Component
@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
public class TrustStoreBean {

	private KeyStore trustStore;
	private CertificateBundle certBundle;

	public TrustStoreBean(CertificateBundleBean myCertificateBundle) 
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		certBundle = myCertificateBundle.getCertificateBundle();
		renew(myCertificateBundle);
	}

	public KeyStore getTrustStore() {
		return trustStore;
	}

	public void renew(CertificateBundleBean myCertificateBundle) 
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

		trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);
		certBundle = myCertificateBundle.getCertificateBundle();
		trustStore.setCertificateEntry("ca", certBundle.getX509IssuerCertificate());
		trustStore.setCertificateEntry("cert", certBundle.getX509Certificate());
	}
	
}

