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

package EngineAPI;

import java.net.URI;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import Shared.Interfaces.AuthCertificate;
import Shared.Interfaces.ChannelListener;
import java.util.ArrayList;
import java.util.List;

/**
 * QlikWebSocketClient.java (UTF-8)
 * An example of a class that acts as a WebSocket client to communicate with the Qlik Engine, 
 * which uses JSON-RPC.
 * 
 * @version 1.0
 * @author Steven Jenkins De Haro
 */
public class QlikWebSocketClient extends WebSocketClient {
    
    private final List<ChannelListener> _listeners = new ArrayList<ChannelListener>();
    
    public QlikWebSocketClient(String wssServerUrl, AuthCertificate qlikCert) throws Exception {
        super(new URI(wssServerUrl), 
                Map.of("X-Qlik-User", "UserDirectory=internal; UserId=sa_engine"));
        
        /*
         * When target hostname is not listed in server's certificate SAN field,
         * use this as a whitelist for exceptions to continue. For example,
         * hostname.equals("xx.xx.xx.xx" or "localhost") ? true : false
         * See https://support.qlik.com/articles/000078616 for more info.
         */
        HttpsURLConnection.setDefaultHostnameVerifier((String hostname, SSLSession session) -> true);
        
        this.setSocketFactory(qlikCert.getSSLContext().getSocketFactory());
    }
    
    public void addListener(ChannelListener toAdd) {
        _listeners.add(toAdd);
    }
    
    public void removeListener(ChannelListener toRemove) {
	_listeners.remove(toRemove);
    }
    
    // Our event to notify everybody interested.
    private void onResponseReceived(String message) {
        _listeners.forEach(listener -> {
            listener.responseReceived(message);
        });
    }

    @Override
    public void onOpen(ServerHandshake sh) {
        onResponseReceived("Connected");
    }

    @Override
    public void onMessage(String message) {
        onResponseReceived(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The codes are documented in class org.java_websocket.framing.CloseFrame
        onResponseReceived("Connection closed by " + (remote ? "remote peer." : "us.") 
                + "\nCode: " + code + (reason.isBlank() ? "" : "\nReason: " + reason));
    }

    @Override
    public void onError(Exception ex) {
        onResponseReceived("Error: " + ex.getMessage());
        ex.printStackTrace(System.out);
    }
}
