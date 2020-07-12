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

package Shared.Interfaces;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;

/**
 * AuthCertificate.java (UTF-8)
 * Defines the contract for certification authentication to build against an interface.
 * 
 * @version 1.0
 * @author Steven Jenkins De Haro
 */
public interface AuthCertificate {
    
    static final String PROTOCOL = "TLS";
    
    public SSLContext getSSLContext() throws KeyStoreException, IOException, CertificateException, 
                NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException;
}
