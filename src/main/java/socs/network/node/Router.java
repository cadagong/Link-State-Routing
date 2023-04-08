package socs.network.node;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.node.Link.ConnectionStatus;
import socs.network.util.Configuration;

public class Router {

	RouterDescription rd = new RouterDescription();
	protected LinkStateDatabase lsd;

	// assuming that all routers are with 4 ports
	// I changed this to HashMap so it's easier to use
	// Link[] ports = new Link[4];
	LinkedHashMap<String, Link> ports = new LinkedHashMap<String, Link>();
	ServerSocket serverSocket;

	public Router(Configuration config) {

		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = config.getint("socs.network.router.port");
		lsd = new LinkStateDatabase(rd);

		// Create a new thread for the router client
		(new Thread() {
			public void run() {
				terminal();
			}
		}).start();

		// Main thread is listening for incoming requests
		try {
			System.out.println("Server ready!");
			serverSocket = new ServerSocket(this.rd.processPortNumber);
			while (!serverSocket.isClosed()) {
				Socket socket = serverSocket.accept();
				System.out.println("\nAccepted connection request...");
				System.out.print(">> ");
				(new Thread() {
					public void run() {
						ObjectOutputStream outgoing = null;
						ObjectInputStream incoming = null;
						try {
							incoming = new ObjectInputStream(socket.getInputStream());
							outgoing = new ObjectOutputStream(socket.getOutputStream());
						} catch (IOException e) {
							e.printStackTrace();
						}
						requestHandler(socket, outgoing, incoming);
					}
				}).start();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private synchronized void lsaReceive(SOSPFPacket packet) {
		boolean updateOccured = false;
		for (LSA incomingLSA : packet.lsaArray) {
			String simIP = incomingLSA.linkStateID;
			int incomingSeqNum = incomingLSA.lsaSeqNumber;
			// System.out.println("New " + simIP + incomingLSA.lsaSeqNumber);
			if ((!lsd._store.containsKey(simIP))
					|| (lsd._store.containsKey(simIP) && (lsd._store.get(simIP).lsaSeqNumber < incomingSeqNum))) {
//				int oldSeqNum = 0;	
//				if (lsd._store.containsKey(simIP)) {	
//					oldSeqNum = lsd._store.get(simIP).lsaSeqNumber;	
//				}	
				lsd._store.put(simIP, incomingLSA);
				System.out.println("\nUpdated LSA of " + simIP + ". ");
				// System.out.println("Old sequence number: " + oldSeqNum);
				System.out.println("New sequence number: " + incomingSeqNum);
				System.out.print(">> ");
				updateOccured = true;
			} else {
				System.out.println("\nIncoming LSA sequence number for " + simIP + " is "
						+ "smaller than or equal to current sequence number --> not updating.");
				System.out.print(">> ");
			}
		}
		if (updateOccured) {
			lsaForward(packet.srcIP);
		}
		ArrayList<LSA> lsaList = new ArrayList<LSA>(lsd._store.values());
		for (LSA lsa : lsaList) {
			if (lsa.links.size() == 1 && !lsa.linkStateID.equals(rd.simulatedIPAddress)) {
				lsd._store.remove(lsa.linkStateID);
//				System.out.println("REMOVED DISCONNECTED ROUTER: " + lsa.linkStateID);
				lsaUpdate();
			}
		}
	}

	/**
	 * process request from the remote router. For example: when router2 tries to
	 * attach router1. Router1 can decide whether it will accept this request. The
	 * intuition is that if router2 is an unknown/anomaly router, it is always safe
	 * to reject the attached request from router2.
	 */
	private void requestHandler(Socket socket, ObjectOutputStream outgoing, ObjectInputStream incoming) {
		try {
			SOSPFPacket packet = null;
//			boolean disconnected = false;
			try {
				packet = (SOSPFPacket) incoming.readObject();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}
			System.out.println(socket.isClosed());
			while (packet != null && socket.isClosed() == false) {
				if (packet.sospfType == 1) { // type 1 = lsaupdate
					System.out.println("\nhandling: LSAUPDATE message from "
							+ socket.getRemoteSocketAddress().toString() + " with simulated IP of " + packet.srcIP);
					System.out.print(">> ");
					lsaReceive(packet);

				} else if (packet.sospfType == 0) {
					String message = packet.message;
					System.out.println(
							"\nhandling: " + "\"" + message + "\" from " + socket.getRemoteSocketAddress().toString());
					System.out.print(">> ");

					String[] messageParts = message.split(" ");

					String task = messageParts[0].toLowerCase();
					String remote_sIP = messageParts[1];

					switch (task) {
					case "attach":
						// if all ports are occupied
						if (this.ports.size() == 4) {
							outgoing.writeObject(new SOSPFPacket("full"));
						}
						// can attach successfully
						else {
							String remote_address = socket.getInetAddress().toString().substring(1);
							int remote_port = Integer.valueOf(messageParts[2]);
							int linkWeight = Integer.valueOf(messageParts[3]);

							RouterDescription remote_rd = new RouterDescription(remote_address, remote_port,
									remote_sIP);
							Link link = addLink(remote_rd, linkWeight);
							link.setCommunicationDetails(socket, outgoing, incoming);

							System.out.println("\nLocal Socket Address: " + socket.getLocalPort());

							System.out
									.println("\nNow attached to router " + remote_sIP + ". Link weight: " + linkWeight);
							System.out.print(">> ");

							outgoing.writeObject(new SOSPFPacket("success"));
						}
						break;

					case "hello":
						Link link = this.ports.get(remote_sIP);
						String remote_cStatus = messageParts[2];
						if (link != null) {
							if (link.cStatus.equals(Link.ConnectionStatus.NONE)) {
								link.setConnectionStatus(Link.ConnectionStatus.INIT);

								System.out.println("\nSetting connection status to INIT");
								System.out.println("Sending HELLO to " + remote_sIP);
								System.out.print(">> ");

								// send HELLO to remote server and piggyback this router simulated IP and
								// connection status
								link.outgoing.writeObject(new SOSPFPacket("HELLO " + this.rd.simulatedIPAddress + " "
										+ Link.ConnectionStatus.INIT.toString()));
							} else if (link.cStatus.equals(Link.ConnectionStatus.INIT)) {
								link.setConnectionStatus(Link.ConnectionStatus.TWO_WAY);
								System.out.println("\nSetting connection status to TWO_WAY");
								System.out.println("Communication channel established with " + remote_sIP);

								// if remote connection status is INIT, send back another HELLO
								if (remote_cStatus.equalsIgnoreCase("init")) {
									System.out.println("Sending HELLO to " + remote_sIP);
									link.outgoing.writeObject(new SOSPFPacket("HELLO " + this.rd.simulatedIPAddress
											+ " " + Link.ConnectionStatus.TWO_WAY.toString()));
								}

								LinkDescription ld = new LinkDescription(link.remoteRouter.simulatedIPAddress,
										link.remoteRouter.processPortNumber, link.weight);
								System.out.println();
								lsd._store.get(rd.simulatedIPAddress).links.add(ld);
								// Send LSAUpdate to update new link with our router's LSA
								lsaUpdate();

								System.out.print(">> ");
							}
						}

						break;

					case "disconnect":
						Link linkDisconnect = this.ports.get(remote_sIP);
						for (LinkDescription i : lsd._store.get(rd.simulatedIPAddress).links) {
							if (i.linkID.equals(remote_sIP)) {
								lsd._store.get(rd.simulatedIPAddress).links.remove(i);
								break;
							}
						}
						System.out.println("HERE");
						lsaUpdate();
						System.out.println("HERE");

						System.out.println("\nSending disconnect acknowledgement to " + remote_sIP);
						System.out.print(">> ");
						try {
							linkDisconnect.outgoing
									.writeObject(new SOSPFPacket("acknowledge " + rd.simulatedIPAddress + " "));
						} catch (IOException e4) {
							e4.printStackTrace();
						}
						this.ports.remove(remote_sIP);
						return;
//						try {
//							Thread.sleep(3000);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						socket.close();
//						System.out.println("\nSocket connection closed.");
//						System.out.print(">> ");
//						disconnected = true;

					case "acknowledge":
						System.out.println("\nReceived disconnect acknowledgement from " + remote_sIP);
						System.out.print(">> ");
						this.ports.remove(remote_sIP);
						socket.close();
						System.out.println("\nSocket connection closed.");
						System.out.print(">> ");
						return;
					}
				}
//				outgoing.reset();
				try {
					System.out.println("CLosed here: " + socket.isClosed() );
					if (socket.isClosed() == false) {
						packet = (SOSPFPacket) incoming.readObject();
					} else {
						break;
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
//				} catch (EOFException e5) {
//					e5.printStackTrace();
				}
			}
//			socket.close();
			System.out.println("\nSocket connection closed.");
			System.out.print(">> ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * output the intest path to the given destination ip
	 * <p/>
	 * format: source ip address -> ip address -> ... -> destination ip
	 *
	 * @param destinationIP the ip adderss of the destination simulated router
	 */
	private void processDetect(String destinationIP) {
		System.out.println(lsd.getShortestPath(destinationIP));
	}

	private synchronized void lsaForward(String receivedFrom) {
		for (String s : ports.keySet()) {
			// Don't send new packet to the same router you received original packet from
			if (ports.get(s).cStatus != Link.ConnectionStatus.TWO_WAY) {
				continue;
			}
			if (!s.equals(receivedFrom)) {
				Link tmp = ports.get(s);
				Vector<LSA> lsaArray = new Vector<LSA>();
				for (LSA lsa : lsd._store.values()) {
					lsaArray.add(lsa);
				}
				// Send packet with srcIP of current router
				SOSPFPacket outgoingLSA = new SOSPFPacket(lsaArray, rd.simulatedIPAddress);
				try {
					tmp.outgoing.writeObject(outgoingLSA);
					tmp.outgoing.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private synchronized void lsaUpdate() {
		// Construct outgoing SOSPFPacket containing Vector of LSA
		Vector<LSA> lsaArray = new Vector<LSA>();
		lsd._store.get(rd.simulatedIPAddress).lsaSeqNumber++;

		for (LSA lsa : lsd._store.values()) {
			lsaArray.add(lsa);
		}

		SOSPFPacket outgoingLSA = new SOSPFPacket(lsaArray, rd.simulatedIPAddress);

		// Loops through all the links this router is connected to
		// and sends the SOSPFPacket
		for (Link l : ports.values()) {
			if (l.cStatus != ConnectionStatus.TWO_WAY) {
				continue;
			}
			try {
				l.outgoing.writeObject(outgoingLSA);
				l.outgoing.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 *
	 * @param portNumber the port number which the link attaches at
	 */
	private void processDisconnect(int portNumber) {
		if (portNumber < 0 || portNumber > 3) {
			System.out.println("Invalid range. Only ports 0 to 3 are valid");
			return;
		}
		if (ports.size() - 1 < portNumber) {
			System.out.println("Port " + portNumber + "does not exist");
			return;
		}
		List<Link> linkList = new ArrayList<Link>(ports.values());
		Link link = linkList.get(portNumber);
		String remoteIP = link.remoteRouter.simulatedIPAddress;
//		System.out.println("DISCONNECTING FROM " + remoteIP);
		if (link.cStatus == Link.ConnectionStatus.TWO_WAY) {
			portNumber = portNumber + 1;
			lsd._store.get(rd.simulatedIPAddress).links.remove(portNumber);
			lsaUpdate();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e3) {
				e3.printStackTrace();
			}
		}
		System.out.println("\nSending disconnect request");
		System.out.print(">> ");
		String message = "disconnect " + this.rd.simulatedIPAddress + " ";
		try {
			System.out.println(ports.get(remoteIP));
			System.out.println(ports.get(remoteIP).outgoing);
			ports.get(remoteIP).outgoing.writeObject(new SOSPFPacket(message));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("OBJECT SENT");

	}

	/**
	 * Attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * 
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, int processPort, String simulatedIP, int weight,
			boolean connect) {
		if (ports.size() < 4) {
			try {
				if (this.ports.containsKey(simulatedIP)) {
					System.out.println("\nERROR: This router is already attached to router " + simulatedIP);
					System.out.print(">> ");
					return;
				}

				// "Client" requesting attach to "Server"
				RouterDescription targetRD = new RouterDescription(processIP, processPort, simulatedIP);
				Socket socket = new Socket(processIP, processPort);

				ObjectOutputStream outgoing = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream incoming = new ObjectInputStream(socket.getInputStream());

				System.out.println("\nSending attach request");
				System.out.print(">> ");
				String message = "attach " + this.rd.simulatedIPAddress + " " + this.rd.processPortNumber + " "
						+ weight;
				outgoing.writeObject(new SOSPFPacket(message));

				SOSPFPacket packet = null;
				try {
					packet = (SOSPFPacket) incoming.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				// Assuming packet can ONLY be String
				String response = packet.message;

				System.out.println("\nResponse: " + response);
				System.out.print(">> ");

				if (response.equals("success")) {
					Link link = addLink(targetRD, weight);
					link.setCommunicationDetails(socket, outgoing, incoming);
					link.setConnectionStatus(Link.ConnectionStatus.INIT);
					System.out.println("\nSuccessfully attached to router " + simulatedIP);
					System.out.print(">> ");
				} else if (response.equals("full")) {
					System.out
							.println("\nERROR: All ports at router " + processIP + ":" + processPort + " are occupied");
					System.out.print(">> ");
				}

				// call request handler for all future communication with remote router
				(new Thread() {
					public void run() {
						requestHandler(socket, outgoing, incoming);
					}
				}).start();

				if (connect) {
					try {
						Thread.sleep(200);
						processStart();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.out.println("\nHost unknown");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("\nAll 4 ports are full, cannot attach");
			System.out.print(">> ");
		}
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {
		int newConnectionsCount = 0;
		for (Link link : this.ports.values()) {
			if (link.cStatus.equals(Link.ConnectionStatus.INIT)) {
				initHelloProtocol(link);
				newConnectionsCount++;
			} else if (link.cStatus.equals(Link.ConnectionStatus.NONE)) {
				link.setConnectionStatus(Link.ConnectionStatus.INIT);
				initHelloProtocol(link);
				newConnectionsCount++;
			}
		}
		if (newConnectionsCount == 0) {
			System.out.println("\nTwo-way communication already established with all attached routers.");
			System.out.print(">> ");
		}
	}

	private void initHelloProtocol(Link link) {
		String remoteSIP = link.remoteRouter.simulatedIPAddress;

		System.out.println("\nSetting connection status to INIT");
		System.out.println("Sending HELLO to " + remoteSIP);
		System.out.print(">> ");

		String message = "HELLO " + this.rd.simulatedIPAddress + " " + Link.ConnectionStatus.INIT.toString();
		try {
			link.outgoing.writeObject(new SOSPFPacket(message));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, int processPort, String simulatedIP, int weight) {
		// processAttach(String processIP, int processPort, String simulatedIP, int
		// weight)
	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		System.out.println("\n\nROUTER NEIGHBORS:");
		System.out.println("\n---------------------------------\n");
		for (Link link : ports.values()) {
			System.out.println("Router address: " + link.remoteRouter.processIPAddress + ":"
					+ link.remoteRouter.processPortNumber);
			System.out.println("Simulated IP: " + link.remoteRouter.simulatedIPAddress);
			System.out.println("Link weight: " + link.weight);
			System.out.println("Connection status: " + link.cStatus.toString());
			System.out.println("\n---------------------------------\n");
		}
		System.out.print(">> ");
	}

	private void processInfo() {
		System.out.println("\nRouter Information:");
		System.out.println("\nRouter IP Address: " + this.rd.processIPAddress);
		System.out.println("Router Listening Port: " + this.rd.processPortNumber);
		System.out.println("Router Simulated IP Address: " + this.rd.simulatedIPAddress);
		System.out.print(">> ");
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {
		int portSize = ports.size();
//		lsd.

//		System.out.println("PORT SIZE: " + portSize);
		for (int i = 0; i < portSize; i++) {
//			int currentSize = ports.size();
//			System.out.println("CURRENT PORT SIZE: " + ports.size());
			int curSize = ports.size();
			int loopSize = ports.size();
			System.out.println("CURSIZE: " + curSize);
			processDisconnect(0);
			while (loopSize == curSize)  {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				loopSize = ports.size();
				System.out.println("LOOPSIZE: " + loopSize);
			}
		}
	}

	private void printLSA() {
		for (LSA i : lsd._store.values()) {
			System.out.println("LSA for " + i.linkStateID);
			System.out.println("Seq num: " + i.lsaSeqNumber);
			for (LinkDescription j : i.links) {
				System.out.println(j.linkID + ", " + j.tosMetrics);
			}
		}
	}

	/**
	 * update the weight of an attached link
	 */
	private void updateWeight(String processIP, int processPort, String simulatedIP, int weight) {

	}

	// ------------------------------------------------//
	// UTILITY FUNCTIONS
	// ------------------------------------------------//

	private void printToTerminal(String string) {
		System.out.println("\n" + string);
		System.out.print(">> ");
	}

	// Created a method to add Links to the HashMap
	public synchronized Link addLink(RouterDescription r2, int weight) {
		Link link = new Link(this.rd, r2, weight);
		this.ports.put(r2.simulatedIPAddress, link);

		return link;
	}

	// ------------------------------------------------//
	// ROUTER CLIENT
	// ------------------------------------------------//

	public void terminal() {
		try {
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);

			System.out.print(">> ");
			String command = br.readLine();

			while (true) {
				if (command.startsWith("attach ")) {
					String[] cmdLine = command.split(" ");
					(new Thread() {
						public void run() {
							String pIP = cmdLine[1];
							int pPort = Integer.parseInt(cmdLine[2]);
							String sIP = cmdLine[3];
							int weight = Integer.parseInt(cmdLine[4]);
							processAttach(pIP, pPort, sIP, weight, false);
						}
					}).start();

				} else if (command.equals("start")) {
					(new Thread() {
						public void run() {
							processStart();
						}
					}).start();

				} else if (command.equals("neighbors")) {
					(new Thread() {
						public void run() {
							processNeighbors();
						}
					}).start();
				} else if (command.equals("info")) {
					(new Thread() {
						public void run() {
							processInfo();
						}
					}).start();
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Integer.parseInt(cmdLine[1]));
				} else if (command.startsWith("detect ")) {
					String[] cmdLine = command.split(" ");
					processDetect(cmdLine[1]);
				} else if (command.equals("lsaupdate")) {
					lsaUpdate();
				} else if (command.equals("lsa")) {
					printLSA();
				} else if (command.startsWith("connect ")) {
					String[] cmdLine = command.split(" ");
					(new Thread() {
						public void run() {
							String pIP = cmdLine[1];
							int pPort = Integer.parseInt(cmdLine[2]);
							String sIP = cmdLine[3];
							int weight = Integer.parseInt(cmdLine[4]);
							processAttach(pIP, pPort, sIP, weight, true);
						}
					}).start();
				} else if (command.equals("quit")) {
					processQuit();
//					serverSocket.close();
					System.exit(0);
//					break;
				} else {
					System.out.println("Please enter a valid command.");
				}
				System.out.print(">> ");
				command = br.readLine();
			}
//			isReader.close();
//			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
