/**
 * This file is part of Qlik Sense Java Examples <https://github.com/StevenJDH/Qlik-Sense-Java-Examples>.
 * Copyright (C) 2020 Steven Jenkins De Haro.
 *
 * Qlik Sense Java Examples is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Qlik Sense Java Examples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Qlik Sense Java Examples.  If not, see <http://www.gnu.org/licenses/>.
 */

package Shared;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import Shared.Interfaces.AuthCertificate;

/**
 * QlikAuthCertificate.java (UTF-8)
 * A class that makes use of standard certificates exported from Qlik Sense without needing
 * to convert them to Java KeyStore (*.jks) certificates.
 * 
 * @version 1.0
 * @author Steven Jenkins De Haro
 */
public class QlikAuthCertificate implements AuthCertificate {
    
    private final String _clientCertPath; // Client certificate with private key. 
    private final char[] _clientCertPassword;
    private final String _rootCertPath; // Required in this example because Qlik Sense certs are used. 

    /**
     * Constructions a new {@see QlikAuthCertificate} instance for certificate authentication.
     * @param clientCertPath Path to a PKCS#12 client certificate.
     * @param clientCertPassword Password for the PKCS#12 certificate.
     * @param rootCertPath Path to the X.509 root certificate of the client certificate.
     */
    public QlikAuthCertificate(String clientCertPath, char[] clientCertPassword, 
            String rootCertPath) {
        
        _clientCertPath = clientCertPath;
        _clientCertPassword = clientCertPassword;
        _rootCertPath = rootCertPath;
    }
    
    /**
     * Configures the needed certificates to validate the identity of the HTTPS 
     * server against a list of trusted certificates and to authenticate to the 
     * HTTPS server using a private key. 
     * @return An initialized secure socket context for TLS/SSL connections.
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException 
     */
    @Override
    public SSLContext getSSLContext() 
            throws KeyStoreException, IOException, CertificateException, 
                NoSuchAlgorithmException, UnrecoverableKeyException, 
                KeyManagementException {
        
        var kmf = KeyManagerFactory.getInstance("SunX509");
        var tmf = TrustManagerFactory.getInstance("SunX509");
        var keyStore = getKeyStore(_clientCertPath, _clientCertPassword, false);
        var trustStore = getKeyStore(_rootCertPath, null, true); 
        var context = SSLContext.getInstance(PROTOCOL);
        
        kmf.init(keyStore, _clientCertPassword);
        tmf.init(trustStore);
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        return context;
    }
    
    /**
     * Gets a new instance of a {@see KeyStore} in PKCS#12 Format configured with 
     * standard certificates that are loaded from a file.
     * @param certPath Path to a PKCS#12 certificate or to a X.509 public key only certificate.
     * @param certPassword Password for the PKCS#12 certificate.
     * @param isClientCheck Set true if KeyStore is used for client check, and false if not.
     * @return A new KeyStore instance configured with standard certificates.
     * @throws KeyStoreException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException 
     */
    private KeyStore getKeyStore(String certPath, char[] certPassword, boolean isClientCheck) 
            throws KeyStoreException, FileNotFoundException, IOException, 
                NoSuchAlgorithmException, CertificateException {  
        
        var ks = KeyStore.getInstance("PKCS12");
        
        try (var inputStream = new FileInputStream(certPath)) {
            if (true == isClientCheck) {
                var certificateFactoryX509 = CertificateFactory.getInstance("X.509");
                var caCertificate = (X509Certificate) certificateFactoryX509.generateCertificate(inputStream);
                ks.load(null, null);
                ks.setCertificateEntry("ca-certificate", caCertificate);
            } else {
                ks.load(inputStream, certPassword);
            }
        }
        
        return ks;
    }
}
