/**
 * This file is part of Qlik Sense Java Examples <https://github.com/StevenJDH/Qlik-Sense-Java-Examples>.
 * Copyright (C) 2019 Steven Jenkins De Haro.
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

package TicketAPI;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * TicketRequest.java (UTF-8)
 * An example of a class that can request a Ticket from the Qlik Sense Proxy Service using
 * standard certificates exported from Qlik Sense without needing to convert them to
 * Java KeyStore (*.jks) certificates.
 * 
 * @version 1.0
 * @author Steven Jenkins De Haro
 */
public class TicketRequest {
    private static final String XRFKEY = "1234567890123456"; // Xrfkey to prevent cross-site issues.
    private static final String PROTOCOL = "TLS";
    private final String _host;
    private final String _vproxy;
    private final String _clientCertPath; // Client certificate with private key. 
    private final char[] _clientCertPassword;
    private final String _rootCertPath; // Required in this example because Qlik Sense certs are used. 
    
    public  TicketRequest(String host, Optional<String> virtualProxyPrefix, 
                String clientCertPath, char[] clientCertPassword, 
                String rootCertPath) {
        
        _host = host;
        _vproxy = virtualProxyPrefix.isPresent() ? "/" + virtualProxyPrefix.get() : "";
        _clientCertPath = clientCertPath;
        _clientCertPassword = clientCertPassword;
        _rootCertPath = rootCertPath;
    }
    
    /**
     * Configures the needed certificates to validate the identity of the HTTPS 
     * server against a list of trusted certificates and to authenticate to the 
     * HTTPS server using a private key. 
     * @return A layered socket factory for TLS/SSL connections.
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException 
     */
    private SSLSocketFactory getSSLSocketFactory() 
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

        return context.getSocketFactory();
    }
    
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
    
    public String getTicket(String userDirectory, String userId) 
            throws MalformedURLException, IOException, KeyStoreException, 
                CertificateException, NoSuchAlgorithmException, 
                UnrecoverableKeyException, KeyManagementException {
        
        var apiUrl = String.format("https://%1$s:4243/qps%2$s/ticket?xrfkey=%3$s", _host, _vproxy, XRFKEY);
        var jsonRequestBody = String.format("{ 'UserId':'%1$s','UserDirectory':'%2$s','Attributes': [] }",
                userId, userDirectory);
        var url = new URL(apiUrl);
        var connection = (HttpsURLConnection) url.openConnection();
        
        // When target hostname is not listed in server's certificate SAN field,
        // use this as a whitelist for exceptions to continue. For example,
        // hostname.equals("xx.xx.xx.xx" or "localhost") ? true : false
        // See https://support.qlik.com/articles/000078616 for more info.
        HttpsURLConnection.setDefaultHostnameVerifier((String hostname, SSLSession session) -> true);
        
        connection.setSSLSocketFactory(getSSLSocketFactory());
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-Qlik-xrfkey", XRFKEY);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        try (OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream())) {
            wr.write(jsonRequestBody);
        }

        var sb = new StringBuilder();
        
        // Gets the response from the QPS BufferedReader.
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {        
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
        }
        
        return sb.toString();
    } 
}
