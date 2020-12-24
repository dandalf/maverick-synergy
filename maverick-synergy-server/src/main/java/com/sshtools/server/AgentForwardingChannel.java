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
package com.sshtools.server;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.common.ssh.ChannelNG;
import com.sshtools.synergy.common.ssh.ChannelOutputStream;

public class AgentForwardingChannel extends ChannelNG<SshServerContext> implements Closeable {

	PipedInputStream in = new PipedInputStream();
	PipedOutputStream out = new PipedOutputStream(in);
	boolean active = false;
	Object agent;
	SessionChannelServer session;

	public static final String SSH_AGENT_CLIENT = "ssh-agent";
	
	public AgentForwardingChannel(String type, SessionChannelServer session) throws IOException {
		this(type, session, null);
	}
	
	public AgentForwardingChannel(String type, SessionChannelServer session, ChannelEventListener listener) throws IOException {
		super(type, 32768, 1024000, 1024000, 65536);
		this.session = session;
		if(listener!=null) {
			addEventListener(listener);
		}
	}

	@Override
	protected void onChannelFree() {
	}

	@Override
	protected byte[] createChannel() throws IOException {
		return null;
	}

	@Override
	protected byte[] openChannel(byte[] requestdata) throws WriteOperationRequest, ChannelOpenException {
		return null;
	}

	public boolean isActive() {
		return active;
	}
	
	@Override
	protected void onChannelOpenConfirmation() {
		
		// Don't use addTask because we are already waiting on this
		getContext().getExecutorService().execute(new Runnable() {
			public void run() {
				try {
					Class<?> clz = Class.forName("com.maverick.agent.client.SshAgentClient");
					Constructor<?> c = clz.getConstructor(boolean.class, String.class, Closeable.class, InputStream.class, OutputStream.class, boolean.class);
					
					SshConnection con = getConnection();
					con.setProperty(SSH_AGENT_CLIENT, agent = c.newInstance(true, 
							"MaverickSSHD", 
							AgentForwardingChannel.this,
							in, 
							new ChannelOutputStream(AgentForwardingChannel.this),
							getChannelType().equals("auth-agent")));
					active = true;
					
					onAgentConnected(con, agent);
				} catch (Exception e) {
					Log.error("Could not start agent", e);
					close();
				}
			}
		});
		
	}

	protected void onAgentConnected(SshConnection con, Object agent) {
		
	}
	
	@Override
	protected void onChannelClosed() {
		try {
			Method m = agent.getClass().getMethod("close");
			m.invoke(agent);
		} catch (Throwable e) {
			Log.error("Could not invoke close method on SshAgentClient");
		} 
	}

	@Override
	protected void onChannelOpen() {
	}

	@Override
	protected void onChannelClosing() {

	}

	@Override
	protected void onChannelRequest(String type, boolean wantreply, byte[] requestdata) {
	}

	@Override
	protected void onRemoteEOF() {
	}

	@Override
	protected void onLocalEOF() {
	}

	@Override
	protected void onChannelData(ByteBuffer data) {
		try {
			byte[] t = new byte[data.remaining()];
			data.get(t);
			out.write(t);
		} catch (IOException e) {
			Log.error("Error passing incoming data to agent InputStream", e);
			close();
		}
	}

	@Override
	protected void onExtendedData(ByteBuffer data, int type) {
		
	}

	@Override
	protected boolean checkWindowSpace() {
		throw new UnsupportedOperationException();
	}

	
}
