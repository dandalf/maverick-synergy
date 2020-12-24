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
package com.sshtools.common.auth;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.sshtools.common.ssh.SshConnection;

public class KeyboardInteractiveAuthenticator implements KeyboardInteractiveAuthenticationProvider {

	Class<? extends KeyboardInteractiveProvider> clz;
	
	public KeyboardInteractiveAuthenticator(Class<? extends KeyboardInteractiveProvider> clz) {
		this.clz = clz;
	}
	
	@Override
	public KeyboardInteractiveProvider createInstance(SshConnection con) throws IOException {
		try {
			return clz.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
