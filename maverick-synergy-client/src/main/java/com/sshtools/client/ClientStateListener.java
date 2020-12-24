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
package com.sshtools.client;

import java.util.List;
import java.util.Set;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.common.ssh.ConnectionStateListener;

public interface ClientStateListener extends ConnectionStateListener {

	default public void authenticate(AuthenticationProtocolClient auth, 
			SshConnection con, Set<String> supportedAuths, 
				boolean moreRequired, List<ClientAuthenticator> authsToTry) { 
	}

	default public void authenticationStarted(AuthenticationProtocolClient authenticationProtocolClient,
			SshConnection connection) {
		
	}
}
