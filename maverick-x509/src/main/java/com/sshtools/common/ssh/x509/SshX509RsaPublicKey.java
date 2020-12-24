/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh.x509;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshX509PublicKey;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKey;

/**
 * Basic implementation of X509 certificate support.
 *
 * @author not attributable
 */
public class SshX509RsaPublicKey extends Ssh2RsaPublicKey implements SshX509PublicKey {

    public static final String X509V3_SIGN_RSA = "x509v3-sign-rsa";
    Certificate cert;

    public SshX509RsaPublicKey() {
    }

    public SshX509RsaPublicKey(Certificate cert) {
            super((RSAPublicKey)cert.getPublicKey());
            this.cert = cert;
    }

    /**
     * Get the algorithm name for the public key.
     *
     * @return the algorithm name, for example "ssh-dss"
     * @todo Implement this com.maverick.ssh.SshPublicKey method
     */
    public String getAlgorithm() {
        return X509V3_SIGN_RSA;
    }
    
    public String getSigningAlgorithm() {
    		return getAlgorithm();
    }
	
    /**
     * Encode the public key into a blob of binary data, the encoded result
     * will be passed into init to recreate the key.
     *
     * @return an encoded byte array
     * @throws SshException
     * @todo Implement this com.maverick.ssh.SshPublicKey method
     */
    public byte[] getEncoded() throws SshException {
        
    	try {
			return cert.getEncoded();
		} catch (Throwable ex) {
			throw new SshException("Failed to encoded key data",
					SshException.INTERNAL_ERROR, ex);
		}
    }

    /**
     * Initialize the public key from a blob of binary data.
     *
     * @param blob byte[]
     * @param start int
     * @param len int
     * @throws SshException
     * @todo Implement this com.maverick.ssh.SshPublicKey method
     */
    public void init(byte[] blob, int start, int len) throws SshException {

        try {
            
			
        	ByteArrayInputStream is = new ByteArrayInputStream(blob, start, len);

             CertificateFactory cf = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_X509)==null ? 
            		 CertificateFactory.getInstance(JCEAlgorithms.JCE_X509) : 
            		 CertificateFactory.getInstance(JCEAlgorithms.JCE_X509, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_X509));
            		 
             this.cert = cf.generateCertificate(is);
             if (!(cert.getPublicKey() instanceof RSAPublicKey ) )
                throw new SshException("Certificate public key is not an RSA public key!", SshException.BAD_API_USAGE);

             this.pubKey = (RSAPublicKey)cert.getPublicKey();

         } catch (Throwable ex) {
             throw new SshException(ex.getMessage(), SshException.JCE_ERROR, ex);
         }
    }

    public Certificate getCertificate() {
        return cert;
    }
    
    public Certificate[] getCertificateChain() {
    	return new Certificate[] { cert};
    }

}