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
package com.sshtools.synergy.common.ssh;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ForwardingDataWindow extends CachingDataWindow {

	ForwardingDataWindow(int maximumWindowSpace) {
		super(maximumWindowSpace, true);
	}

	public synchronized int write(SocketChannel socketChannel) throws IOException {
		if(Boolean.getBoolean("maverick.disableMaximumWrite")) {
			return socketChannel.write(cache);
		} else {
			int c = 0;
			while(true) {
				int r = socketChannel.write(cache);
				if(r<=0) {
					break;
				}
				c+=r;
			}
			return c;
		}
	}
	
	public synchronized int read(SocketChannel socketChannel) throws IOException {
		
		cache.compact();
		
		try {
			return socketChannel.read(cache);
		} finally {
			cache.flip();
		}
	}
}
