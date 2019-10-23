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
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

/**
 * TicketRequest.java (UTF-8)
 * An example of a class that can request a Ticket from the Qlik Sense Proxy Service using
 * standard certificates exported from Qlik Sense without needing to convert them to
 * Java KeyStore (*.jks) certificates.
 * 
 * @version 1.1
 * @author Steven Jenkins De Haro
 */
public class TicketRequest {
    
    private static final String XRFKEY = "1234567890123456"; // Xrfkey to prevent CSRF attacks.
    private static final String PROTOCOL = "TLS";
    private final String _apiUrl;
    private final String _clientCertPath; // Client certificate with private key. 
    private final char[] _clientCertPassword;
    private final String _rootCertPath; // Required in this example because Qlik Sense certs are used. 
    
    /**
     * Constructions a new {@see TicketRequest} instance to make Ticket requests.
     * @param hostname Hostname of the Qlik Sense server used for requests.
     * @param virtualProxyPrefix Optional prefix of virtual proxy if one is used.
     * @param clientCertPath Path to a PKCS#12 client certificate.
     * @param clientCertPassword Password for the PKCS#12 certificate.
     * @param rootCertPath Path to the X.509 root certificate of the client certificate.
     */
    public  TicketRequest(String hostname, Optional<String> virtualProxyPrefix, 
                String clientCertPath, char[] clientCertPassword, 
                String rootCertPath) {
        
        _apiUrl = String.format("https://%1$s:4243/qps%2$s/ticket?xrfkey=%3$s", 
                hostname, virtualProxyPrefix.isPresent() ? "/" + virtualProxyPrefix.get() : "", XRFKEY);
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
    private SSLContext getSSLContext() 
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
    
    /**
     * Requests a ticket from the Qlik Sense Proxy Service that is valid for one minute.
     * @param userDirectory Directory associated with user.
     * @param userId Login name of user.
     * @return Ticket to claim within one minute.
     * @throws MalformedURLException
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException 
     */
    public String getTicket(String userDirectory, String userId) 
            throws MalformedURLException, IOException, KeyStoreException, 
                CertificateException, NoSuchAlgorithmException, 
                UnrecoverableKeyException, KeyManagementException {
        
        var jsonRequestBody = String.format("{ 'UserId':'%1$s','UserDirectory':'%2$s','Attributes': [] }",
                userId, userDirectory);
        var url = new URL(_apiUrl);
        var connection = (HttpsURLConnection) url.openConnection();

        /*
           * When target hostname is not listed in server's certificate SAN field,
           * use this as a whitelist for exceptions to continue. For example,
           * hostname.equals("xx.xx.xx.xx" or "localhost") ? true : false
           * See https://support.qlik.com/articles/000078616 for more info.
         */
        HttpsURLConnection.setDefaultHostnameVerifier((String hostname, SSLSession session) -> true);

        connection.setSSLSocketFactory(getSSLContext().getSocketFactory());
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
    
    /**
     * Requests a ticket asynchronously from the Qlik Sense Proxy Service that is valid for one minute.Note: This function uses a more modern API than the {@link #getTicket(String, String) getTicket} function.
     * @param userDirectory Directory associated with user.
     * @param userId Login name of user.
     * @return CompletableFuture with Ticket to claim within one minute.
     * @throws MalformedURLException
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException 
     */
    public CompletableFuture<String> getTicketAsync(String userDirectory, String userId) 
            throws MalformedURLException, IOException, KeyStoreException, 
                CertificateException, NoSuchAlgorithmException, 
                UnrecoverableKeyException, KeyManagementException {
        
        var jsonRequestBody = String.format("{ 'UserId':'%1$s','UserDirectory':'%2$s','Attributes': [] }",
                userId, userDirectory);
        final Properties props = System.getProperties();
        
        /*
         * Disables hostname validation when hostname is not listed in server's  
         * certificate SAN field.
         */    
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        var client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(Redirect.NORMAL)
            .sslContext(getSSLContext())
            .build();
        
        var request = HttpRequest.newBuilder()
            .uri(URI.create(_apiUrl))
            .timeout(Duration.ofSeconds(30))
            .header("X-Qlik-xrfkey", XRFKEY)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
            .build();
        
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.body());
    }
}
