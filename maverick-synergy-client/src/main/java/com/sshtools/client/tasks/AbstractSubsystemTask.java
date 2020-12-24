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
package com.sshtools.client.tasks;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.common.ssh.ByteArrays;
import com.sshtools.synergy.common.ssh.PacketPool;

/**
 * An abstract task for connecting to an SSH subsystem.
 */
public abstract class AbstractSubsystemTask extends ConnectionAwareTask {
	
	
	protected long timeout = 60000;
	DataInputStream in;
	UUID taskUUID = UUID.randomUUID();
	
	public AbstractSubsystemTask(SshConnection con) {
		super(con);
	}
	
	protected void doTask() {
		
		try {
			SessionChannelNG session = getSession();
			if(session==null) {
				session = createSession();
				in = new DataInputStream(session.getInputStream());
			}
			doSubsystemTask(session);
		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	protected SessionChannelNG createSession() throws SshException {
		
		SessionChannelNG session = new SessionChannelNG(
				getMaximumPacketSize(),
				getMaximumWindowSize(), 
				getMaximumWindowSize(),
				getMinimumWindowSize(),
				false);
		
		session.addEventListener(new ChannelEventListener() {

			@Override
			public void onChannelClose(Channel channel) {
				IOUtils.closeStream(in);
				onCloseSession((SessionChannelNG) channel);
			}

		});

		con.openChannel(session);
		if(!session.getOpenFuture().waitFor(timeout).isSuccess()) {
			throw new IllegalStateException(
					"Could not open session channel");
		}
		
		con.setProperty(taskUUID.toString(), session);
		return session;
	}

	protected abstract int getMinimumWindowSize();

	protected abstract int getMaximumWindowSize();

	protected abstract int getMaximumPacketSize();

	protected SessionChannelNG getSession() {
		return (SessionChannelNG) con.getProperty(taskUUID.toString());
	}
	
	public byte[] nextMessage() throws SshException {
		
		int len = -1; 
		  try {
	        len = in.readInt();

	        if(len < 0)
	            throw new SshException("Negative message length in SFTP protocol.",
	                                   SshException.PROTOCOL_VIOLATION);

	        if(len > con.getContext().getMaximumPacketLength())
	            throw new SshException("Invalid message length in SFTP protocol [" + len + "]",
	                                   SshException.PROTOCOL_VIOLATION);

	        byte[] msg = ByteArrays.getInstance().getByteArray();
	        in.readFully(msg, 0, len);

	        return msg;
	    } catch(OutOfMemoryError ex) {
	        throw new SshException("Invalid message length in SFTP protocol [" + len + "]",
	                                   SshException.PROTOCOL_VIOLATION);
	    } catch (EOFException ex) {
	        getSession().close();
	        throw new SshException("The channel unexpectedly terminated",
	                               SshException.CHANNEL_FAILURE);
	    } catch (IOException ex) {

	        if(ex instanceof SshIOException)
	            throw ((SshIOException)ex).getRealException();

	        getSession().close();
	        
	        throw new SshException(SshException.CHANNEL_FAILURE, ex);
	    }
	}
	
	protected void onCloseSession(SessionChannelNG session) {
		try {
			in.close();
		} catch (IOException e) {
		}
	}
	
	protected abstract void doSubsystemTask(SessionChannelNG session) throws SshException;
	
	public void sendMessage(Packet msg) throws SshException {
		
		SessionChannelNG session = getSession();
		msg.finish();
		try {
			session.sendChannelDataAndBlock(msg.array(), 0, msg.size(), new PacketReturner(msg));
		} catch (IOException e) {
			Log.error("Channel I/O error", e);
		}
	}

	class PacketReturner implements Runnable {
		Packet msg;
		
		PacketReturner(Packet msg) {
			this.msg = msg;
		}
		
		public void run() {
			PacketPool.getInstance().putPacket(msg);
			msg = null;
		}
	}
}
