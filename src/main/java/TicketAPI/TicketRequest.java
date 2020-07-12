/**
 * This file is part of Qlik Sense Java Examples <https://github.com/StevenJDH/Qlik-Sense-Java-Examples>.
 * Copyright (C) 2019-2020 Steven Jenkins De Haro.
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

import Shared.Interfaces.AuthCertificate;
import java.io.BufferedReader;
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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * TicketRequest.java (UTF-8)
 * An example of a class that can request a Ticket from the Qlik Sense Proxy Service using
 * standard certificates exported from Qlik Sense without needing to convert them to
 * Java KeyStore (*.jks) certificates.
 * 
 * @version 1.2
 * @author Steven Jenkins De Haro
 */
public class TicketRequest {
    
    private static final String XRFKEY = "1234567890123456"; // Xrfkey to prevent CSRF attacks.
    private final String _apiUrl;
    private final AuthCertificate _qlikCert;
    
    /**
     * Constructions a new {@see TicketRequest} instance to make Ticket requests.
     * @param hostname Hostname of the Qlik Sense server used for requests.
     * @param virtualProxyPrefix Optional prefix of virtual proxy if one is used.
     * @param qlikCert Qlik certificate used for authentication.
     */
    public  TicketRequest(String hostname, Optional<String> virtualProxyPrefix, 
                AuthCertificate qlikCert) {
        
        _apiUrl = String.format("https://%1$s:4243/qps%2$s/ticket?xrfkey=%3$s", 
                hostname, virtualProxyPrefix.isPresent() ? "/" + virtualProxyPrefix.get() : "", XRFKEY);
        _qlikCert = qlikCert;
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

        connection.setSSLSocketFactory(_qlikCert.getSSLContext().getSocketFactory());
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
     * Requests a ticket asynchronously from the Qlik Sense Proxy Service that 
     * is valid for one minute. Note: This function uses a more modern API than 
     * the {@link #getTicket(String, String) getTicket} function.
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
            .sslContext(_qlikCert.getSSLContext())
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
