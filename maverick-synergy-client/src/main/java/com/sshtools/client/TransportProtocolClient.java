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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.net.HttpRequest;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.synergy.common.nio.ConnectRequestFuture;
import com.sshtools.synergy.common.nio.LicenseException;
import com.sshtools.synergy.common.nio.SocketConnection;
import com.sshtools.synergy.common.ssh.ConnectionTaskWrapper;
import com.sshtools.synergy.common.ssh.Service;
import com.sshtools.synergy.common.ssh.TransportProtocol;
import com.sshtools.synergy.common.ssh.components.SshKeyExchange;

//#ifdef LICENSE
//import com.sshtools.synergy.common.nio.LicenseManager;
//#endif

/**
 * The implementation of the client side of the SSH transport protocol.
 */
public class TransportProtocolClient extends TransportProtocol<SshClientContext> {

	
	
	Service pendingService; 
	boolean proxyDone;
	
	//#ifdef LICENSE
	//static final LicenseVerification license = new LicenseVerification();
	//#endif
	public TransportProtocolClient(SshClientContext sshContext, ConnectRequestFuture connectFuture) throws LicenseException {
		super(sshContext,connectFuture);
		//#ifdef LICENSE
		//checkLicensing();
		//#endif
	}

	//#ifdef LICENSE
	/*
	 	private final void checkLicensing() throws LicenseException {
		
		if(!license.isLicensed()) {
			license.verifyLicense();
			
			if(license.isValid()) {
				if(Log.isInfoEnabled()) {
					Log.info("This Maverick NG API product is licensed to " + license.getLicensee());
				}
			}
		}
		
		switch (license.getStatus() & LicenseVerification.LICENSE_VERIFICATION_MASK) {
			case LicenseVerification.EXPIRED:
				throw new LicenseException("Your license has expired! visit http://www.sshtools.com to obtain an update version of the software.");
			case LicenseVerification.OK:
				break;
			case LicenseVerification.INVALID:
				throw new LicenseException("Your license is invalid!");
			case LicenseVerification.NOT_LICENSED:
				throw new LicenseException("NOT_LICENSED_TEXT");
			case LicenseVerification.EXPIRED_MAINTENANCE:
				throw new LicenseException(
						"Your support and maintenance has expired! visit http://www.sshtools.com to purchase a subscription");
			default:
				throw new LicenseException("An unexpected license status was received.");
		}
	}
	*/
	//#endif

	@Override
	protected boolean canConnect(SocketConnection connection) {
		return true;
	}
	
	@Override
	public void onSocketConnect(SocketConnection connection) {
		
		this.socketConnection = connection;
		
		if(sshContext.isProxyEnabled()) {
			switch(sshContext.getProxyType()) {
			case HTTP:
				sendHTTPProxyRequest();
				break;
			case SOCKS4:
				
				break;
			case SOCKS5:
				
				break;
			default:
				throw new IllegalStateException("Proxy NONE should not mean that a isProxyEnabled returns true");
			}
		} else {
			super.onSocketConnect(connection);
		}
	}

	public boolean onSocketRead(ByteBuffer incomingData) {
		if(sshContext.isProxyEnabled() && !proxyDone) {
			return super.onSocketRead(incomingData);
		} else {
			return super.onSocketRead(incomingData);
		}
	}
	
	private void sendHTTPProxyRequest() {
		
		postMessage(new SshMessage() {
			
			HttpRequest request = new HttpRequest();
			
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {

		        request.setHeaderBegin("CONNECT " 
		        			+ sshContext.getRemoteHostname() 
		        			+ ":" 
		        			+ sshContext.getRemotePort() 
		        			+ " HTTP/1.0");
		        request.setHeaderField("User-Agent", sshContext.getUserAgent());
		        request.setHeaderField("Pragma", "No-Cache");
		        request.setHeaderField("Host", sshContext.getProxyHostname());
		        request.setHeaderField("Proxy-Connection", "Keep-Alive");
		        
		        if(sshContext.getProxyUsername()!=null && !"".equals(sshContext.getProxyUsername().trim())
		        		&& sshContext.getProxyPassword()!=null && !"".equals(sshContext.getProxyPassword().trim())) {
		        	request.setBasicAuthentication(sshContext.getProxyUsername(), sshContext.getProxyPassword());
		        }
		        
		        try {
					buf.put(request.toString().getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
				return true;
			}

			public void messageSent(Long sequenceNo) {
				if(Log.isDebugEnabled())
					Log.debug("Sent HTTP Proxy Request");
					Log.debug(request.toString());
				}
		});
	}

	@Override
	protected void initializeKeyExchange(SshKeyExchange<SshClientContext> keyExchange, boolean firstPacketFollows,
			boolean useFirstPacket) throws IOException, SshException {
	
		keyExchange.init(this, localIdentification.toString().trim(),
				remoteIdentification.toString().trim(), localkex, remotekex,
				null, null,
				firstPacketFollows, useFirstPacket);
	}
	
	protected void onKeyExchangeInit() {
		
	}

	@Override
	protected void completeKeyExchange(SshKeyExchange<SshClientContext> keyExchange) {

		try {
			
			hostKey = SshPublicKeyFileFactory.decodeSSH2PublicKey(keyExchange.getHostKey());
			
			if(getContext().getHostKeyVerification()!=null) {
				
				String host = getConnectFuture().getHost();
				
				if(!Boolean.getBoolean("maverick.knownHosts.disablePortValidate")) {
					if (getConnectFuture().getPort() != 22) {
						host = "[" + host + "]:" + getConnectFuture().getPort();
					}
				}
				
				if (!getContext().getHostKeyVerification()
						.verifyHost(host, hostKey)) {
					EventServiceImplementation
							.getInstance()
							.fireEvent(
									new Event(
											this,
											EventCodes.EVENT_HOSTKEY_REJECTED,
											false)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											getConnection())
									.addAttribute(EventCodes.ATTRIBUTE_HOST_KEY,
											new String(keyExchange.getHostKey()))
									.addAttribute(EventCodes.ATTRIBUTE_HOST_PUBLIC_KEY,
											hostKey));
					disconnect(
							TransportProtocol.HOST_KEY_NOT_VERIFIABLE,
							"Host key not accepted");
					throw new SshException(
							"The host key was not accepted",
							SshException.CANCELLED_CONNECTION);
				}
	
				if (!hostKey.verifySignature(
						keyExchange.getSignature(),
						keyExchange.getExchangeHash())) {
					EventServiceImplementation
							.getInstance()
							.fireEvent(
									new Event(this, EventCodes.EVENT_HOSTKEY_REJECTED, false)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											getConnection())
									.addAttribute(EventCodes.ATTRIBUTE_HOST_KEY, new String(keyExchange.getHostKey()))
									.addAttribute(EventCodes.ATTRIBUTE_HOST_PUBLIC_KEY, hostKey));
					disconnect(
							TransportProtocol.HOST_KEY_NOT_VERIFIABLE,
							"Invalid host key signature");
	
					throw new SshException(
							"The host key signature is invalid",
							SshException.PROTOCOL_VIOLATION);
				}
			}
			
			EventServiceImplementation.getInstance().fireEvent(
					new Event(this,
							EventCodes.EVENT_HOSTKEY_ACCEPTED,
							true).addAttribute(
									EventCodes.ATTRIBUTE_CONNECTION,
									getConnection())
					             .addAttribute(EventCodes.ATTRIBUTE_HOST_KEY, new String(keyExchange.getHostKey()))
								.addAttribute(EventCodes.ATTRIBUTE_HOST_PUBLIC_KEY, hostKey));
		
			boolean first = !completedFirstKeyExchange;
			super.completeKeyExchange(keyExchange);
			sshContext.keysExchanged(first);
			
		} catch (SshException | IOException e) {
			if(Log.isErrorEnabled()) {
				Log.error("Could not verify host key", e);
			}
			getConnectFuture().done(false);
			if(disconnectStarted != null)
				disconnect(HOST_KEY_NOT_VERIFIABLE, "The host key could not be verified.");
		}
		
	}
	@Override
	protected void onNewKeysReceived() {
		generateNewKeysClientIn();
	}
	
	@Override
	protected void onNewKeysSent() {
		generateNewKeysClientOut();
	}

	@Override
	protected String selectNegotiatedComponent(String remotelist, String locallist)
			throws IOException {
		/**
		 * This switches the lists around so that we are selected identically to the
		 * server
		 */
		return super.selectNegotiatedComponent(locallist, remotelist);
	}
	
	@Override
	protected boolean processTransportMessage(int msgid, byte[] msg)
			throws IOException, SshException {
		
		switch(msgid) {
		case SSH_MSG_SERVICE_ACCEPT:
			if(pendingService!=null) {
				activeService = pendingService;
				pendingService = null;
				activeService.start();
			}
			return true;
		}
		return false;
	}

	public void setActiveService(Service service) {
		this.activeService = service;
	}
	
	public Service getActiveService() {
		return this.activeService;
	}
	
	public void startService(final Service service) {
		
		pendingService = service;
		
		postMessage(new SshMessage() {

			byte[] serviceNameBytes = getBytes(service.getName(), CHARSET_ENCODING);

			@Override
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {
				
				buf.put((byte)SSH_MSG_SERVICE_REQUEST);
				buf.putInt(serviceNameBytes.length);
				buf.put(serviceNameBytes);
				
				return true;
			}

			@Override
			public void messageSent(Long sequenceNo) {
				if(Log.isDebugEnabled()) {
					Log.debug("Sent SSH_MSG_SERVICE_REQUEST {}", service.getName());
				}
			}
		});
	}

	@Override
	protected void disconnected() {
		
		addTask(EVENTS, new ConnectionTaskWrapper(getConnection(), new Runnable() {
			public void run() {
				for(ClientStateListener stateListener : sshContext.getStateListeners()) {
					stateListener.disconnected(con);
				}
			}
		}));
		
	}

	@Override
	protected void onConnected() {
		this.con = getContext().getConnectionManager().registerTransport(this, sshContext);
	}

	@Override
	protected void onDisconnected() {
		getContext().getConnectionManager().unregisterTransport(this);
	}
	
	public String getName() {
		return "transport-client";
	}

	@Override
	protected SocketAddress getConnectionAddress() {
		return getLocalAddress();
	}
}
