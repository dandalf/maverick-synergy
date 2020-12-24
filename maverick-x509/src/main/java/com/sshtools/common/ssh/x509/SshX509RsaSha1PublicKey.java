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
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshX509PublicKey;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Basic implementation of X509 certificate support.
 * 
 * @author not attributable
 */
public class SshX509RsaSha1PublicKey extends Ssh2RsaPublicKey implements SshX509PublicKey {

	public static final String X509V3_SIGN_RSA_SHA1 = "x509v3-sign-rsa-sha1";
	Certificate cert;

	public SshX509RsaSha1PublicKey() {
	}

	public SshX509RsaSha1PublicKey(Certificate cert) {
		super((RSAPublicKey) cert.getPublicKey());
		this.cert = cert;
	}

	/**
	 * Get the algorithm name for the public key.
	 * 
	 * @return the algorithm name, for example "ssh-dss"
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public String getAlgorithm() {
		return X509V3_SIGN_RSA_SHA1;
	}

	/**
	 * Encode the public key into a blob of binary data, the encoded result will
	 * be passed into init to recreate the key.
	 * 
	 * @return an encoded byte array
	 * @throws SshException
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public byte[] getEncoded() throws SshException {

		ByteArrayWriter baw = new ByteArrayWriter();

		try {

			baw.writeString(getAlgorithm());
			baw.writeBinaryString(cert.getEncoded());
			return baw.toByteArray();
		} catch (Throwable ex) {
			throw new SshException("Failed to encoded key data",
					SshException.INTERNAL_ERROR, ex);
		} finally {
			try {
				baw.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Initialize the public key from a blob of binary data.
	 * 
	 * @param blob
	 *            byte[]
	 * @param start
	 *            int
	 * @param len
	 *            int
	 * @throws SshException
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public void init(byte[] blob, int start, int len) throws SshException {

		ByteArrayReader bar = new ByteArrayReader(blob, start, len);
		try {

			String header = bar.readString();

			if (!header.equals(X509V3_SIGN_RSA_SHA1)) {
				throw new SshException("The encoded key is not X509 RSA",
						SshException.INTERNAL_ERROR);
			}

			byte[] encoded = bar.readBinaryString();

			ByteArrayInputStream is = new ByteArrayInputStream(encoded);

			CertificateFactory cf = JCEProvider
					.getProviderForAlgorithm(JCEAlgorithms.JCE_X509) == null ? CertificateFactory
					.getInstance(JCEAlgorithms.JCE_X509) : CertificateFactory
					.getInstance(JCEAlgorithms.JCE_X509, JCEProvider
							.getProviderForAlgorithm(JCEAlgorithms.JCE_X509));

			this.cert = cf.generateCertificate(is);

			if (!(cert.getPublicKey() instanceof RSAPublicKey))
				throw new SshException(
						"Certificate public key is not an RSA public key!",
						SshException.BAD_API_USAGE);

			this.pubKey = (RSAPublicKey) cert.getPublicKey();

		} catch (Throwable ex) {
			throw new SshException(ex.getMessage(), SshException.JCE_ERROR, ex);
		} finally {
			bar.close();
		}
	}

    public Certificate getCertificate() {
        return cert;
    }
    
    public Certificate[] getCertificateChain() {
    	return new Certificate[] { cert};
    }

}
