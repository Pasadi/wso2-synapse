/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.transport.http.conn;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
import org.apache.synapse.transport.certificatevalidation.CertificateVerificationException;
import org.apache.synapse.transport.certificatevalidation.RevocationVerificationManager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ServerSSLSetupHandler implements SSLSetupHandler {
    
    private final SSLClientAuth clientAuth;
    /** Enabled SSL handshake protocols (e.g. SSLv3, TLSv1) */
    private final String[] httpsProtocols;
    private RevocationVerificationManager verificationManager;

    public ServerSSLSetupHandler(final SSLClientAuth clientAuth, final String[] httpsProtocols,
                                 final RevocationVerificationManager verificationManager) {
        this.clientAuth = clientAuth;
        this.httpsProtocols = httpsProtocols;
        this.verificationManager = verificationManager;
    }

    public void initalize(
        final SSLEngine sslengine) throws SSLException {
        if (clientAuth != null) {
            switch (clientAuth) {
            case OPTIONAL:
                sslengine.setWantClientAuth(true);
                break;
            case REQUIRED:
                sslengine.setNeedClientAuth(true);
            }
        }
        // set handshake protocols if they are specified in transport
        // configuration.
        if (httpsProtocols != null) {
            sslengine.setEnabledProtocols(httpsProtocols);
        }

    }

    public void verify(
            final IOSession iosession,
            final SSLSession sslsession) throws SSLException {
        SocketAddress remoteAddress = iosession.getRemoteAddress();
        String address;
        if (remoteAddress instanceof InetSocketAddress) {
            address = ((InetSocketAddress) remoteAddress).getHostName();
        } else {
            address = remoteAddress.toString();
        }

        if (verificationManager != null) {
            try {
                verificationManager.verifyRevocationStatus(sslsession.getPeerCertificateChain());
            } catch (CertificateVerificationException e) {
                throw new SSLException("Certificate Chain Validation failed for host : " + address, e);
            }
        }

    }

}