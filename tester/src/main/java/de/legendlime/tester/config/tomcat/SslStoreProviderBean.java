package de.legendlime.tester.config.tomcat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.stereotype.Component;

import de.legendlime.tester.config.cert.KeyStoreBean;
import de.legendlime.tester.config.cert.TrustStoreBean;
import de.legendlime.tester.config.vault.VaultPkiProperties;


/**
 * Spring bean containing SSL configuration with key store and trust store This
 * bean will be loaded into the containers SSL configuration each time a
 * certificate must be renewed
 * 
 * @author Thomas Kautenburger
 * @version 1.0
 *
 */

@Component
@ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
public class SslStoreProviderBean implements SslStoreProvider {

	private KeyStore keyStore, trustStore;
	private VaultPkiProperties pkiProperties;

	public SslStoreProviderBean(KeyStoreBean keyStoreBean, TrustStoreBean trustStoreBean,
			VaultPkiProperties pkiProperties) throws FileNotFoundException, IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException {
		this.keyStore = keyStoreBean.getKeyStore();
		this.trustStore = trustStoreBean.getTrustStore();
		this.pkiProperties = pkiProperties;
		if (pkiProperties.isPersistEnabled()) {
			// store keystore in file system, private key entry must be password protected
			// for Kafka to work with the keystore
			keyStorePersister(this.keyStore);
		}
	}

	@Override
	public KeyStore getKeyStore() throws Exception {
		return keyStore;
	}

	@Override
	public KeyStore getTrustStore() throws Exception {
		return trustStore;
	}

	public void renew(KeyStoreBean keyStoreBean, TrustStoreBean trustStoreBean)
			throws FileNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, UnrecoverableEntryException {
		this.keyStore = keyStoreBean.getKeyStore();
		this.trustStore = trustStoreBean.getTrustStore();
		if (pkiProperties.isPersistEnabled()) {
			// store keystore in file system, private key entry must be password protected
			// for Kafka to work with the keystore
			keyStorePersister(this.keyStore);
		}
	}

	private void keyStorePersister(KeyStore keystore) throws KeyStoreException, FileNotFoundException, IOException,
			NoSuchAlgorithmException, UnrecoverableEntryException, CertificateException {
		char[] pwdArray = System.getenv("KEY_STORE_PASSWORD").toCharArray();
		KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection("".toCharArray());
		KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("vault", protParam);
		PrivateKey privateKey = pkEntry.getPrivateKey();
		keyStore.setKeyEntry("vault", privateKey, pwdArray, pkEntry.getCertificateChain());
		try (FileOutputStream fos = new FileOutputStream(System.getenv("KEY_STORE_PATH"))) {
			keyStore.store(fos, pwdArray);
		}
		keyStore.setKeyEntry("vault", privateKey, "".toCharArray(), pkEntry.getCertificateChain());

	}
}
