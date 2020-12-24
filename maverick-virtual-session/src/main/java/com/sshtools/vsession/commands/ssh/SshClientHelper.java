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
package com.sshtools.vsession.commands.ssh;

import java.io.IOException;
import java.util.Objects;

import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.PublicKeyAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.Utils;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.synergy.common.ssh.SshContext;

public class SshClientHelper {

	public static SshClientContext getSshContext(SshClientArguments arguments) throws IOException, SshException {
		if(CommandUtil.isNotEmpty(arguments.getSecurityLevel()) && 
				(CommandUtil.isNotEmpty(arguments.getCiphers()) || CommandUtil.isNotEmpty(arguments.getHmacs()))) {
			throw new IllegalArgumentException("Security level cannot be specified together with cipher or hmac spec.");
		}
		
		if (CommandUtil.isNotEmpty(arguments.getSecurityLevel())) {
			SecurityLevel securityLevel = SecurityLevel.valueOf(arguments.getSecurityLevel());
			return new SshClientContext(securityLevel);
		}
		
		if (CommandUtil.isNotEmpty(arguments.getCiphers()) || CommandUtil.isNotEmpty(arguments.getHmacs())) {
			return new SshClientContext(SecurityLevel.NONE);
		}
				
		return new SshClientContext();
	}
	
	public static void setUpCipherSpecs(SshClientArguments arguments, SshClientContext ctx)
			throws IOException, SshException {
		if (CommandUtil.isNotEmpty(arguments.getCiphers())) {
			String[] cipherSpecs = arguments.getCiphers();
			
			for (int i = cipherSpecs.length - 1; i >= 0; --i) {
				ctx.setPreferredCipherCS(cipherSpecs[i]); 
				ctx.setPreferredCipherSC(cipherSpecs[i]);
			}
		}
	}
	
	
	public static void setUpMacSpecs(SshClientArguments arguments, SshClientContext ctx)
			throws IOException, SshException {
		if (CommandUtil.isNotEmpty(arguments.getHmacs())) {
			String[] macSpecs = arguments.getHmacs();
			
			for (int i = macSpecs.length - 1; i >= 0; --i) {
				ctx.setPreferredMacCS(macSpecs[i]); 
				ctx.setPreferredMacSC(macSpecs[i]);
			}
		}
	}
	
	public static void setUpCompression(SshClientArguments arguments, SshClientContext ctx) 
			throws IOException, SshException {
		if (arguments.isCompression()) {
			ctx.setPreferredCompressionCS(SshContext.COMPRESSION_ZLIB);
 			ctx.setPreferredCompressionSC(SshContext.COMPRESSION_ZLIB);
		}
	}

	public static SshClient connectClient(SshClientArguments arguments, VirtualConsole console) throws IOException, SshException, PermissionDeniedException {
		

		SshClientContext ctx = getSshContext(arguments);
		
		setUpCipherSpecs(arguments, ctx);
		setUpMacSpecs(arguments, ctx);
		setUpCompression(arguments, ctx);
		
		SshClient sshClient = new SshClient(arguments.getDestination(), arguments.getPort(), arguments.getLoginName(), ctx);
		ClientAuthenticator auth;

		if (Objects.nonNull(arguments.getIdentityFile())) {
			
			AbstractFile identityFileTarget =  arguments.getIdentityFile();
			SshPrivateKeyFile pkf = SshPrivateKeyFileFactory.parse(identityFileTarget.getInputStream());
			
			String passphrase = null;
			if (pkf.isPassphraseProtected()) {
				SshKeyPair pair = null;
				for(int i=0;i<3 && sshClient.isConnected();i++) {
					try {
						passphrase = console.getLineReader().readLine("Passphrase :", '\0');
						pair = pkf.toKeyPair(passphrase);
					} catch (InvalidPassphraseException e) {
						continue;
					}
					
					auth = new PublicKeyAuthenticator(pair);

					if(!sshClient.authenticate(auth, 30000)) {
						console.println("Public key authentication failed");
					} else {
						console.println("Public key authentication succeeded");
					}
				}
			}
		} 
		
		if(!sshClient.isAuthenticated() && Objects.nonNull(arguments.getIdentity())) {
			auth = new PublicKeyAuthenticator(arguments.getIdentity());

			if(!sshClient.authenticate(auth, 30000)) {
				console.println("Public key authentication failed");
			} else {
				console.println("Public key authentication succeeded");
			}
		}

		if(!sshClient.isAuthenticated() && Utils.isNotBlank(arguments.getPassword())) {
			sshClient.authenticate(new PasswordAuthenticator(arguments.getPassword()), 30000);
		}

		if(!sshClient.isAuthenticated()) {
			do {
				auth = new PasswordAuthenticator(console.getLineReader().readLine("Password :", '*'));
				if (sshClient.authenticate(auth, 30000)) {
					break;
				}
			} while (sshClient.isConnected());
		}
		
		if(!sshClient.isAuthenticated()) {
			throw new IOException(String.format("Could not authenticate with %s", arguments.getDestination()));
		}
		return sshClient;
	}
}
