package socs.network.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import socs.network.util.Configuration;

public class Router {

	
	RouterDescription rd = new RouterDescription();
	protected LinkStateDatabase lsd = new LinkStateDatabase(rd);

	// assuming that all routers are with 4 ports
	// I changed this to HashMap so it's easier to use
	// Link[] ports = new Link[4];
	HashMap<String, Link> ports = new HashMap<String,Link>();


	public Router(Configuration config) {
		// import configuration settings
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = config.getint("socs.network.router.port");

		// Create a new thread for the router client
		(new Thread() {
			public void run() {
				terminal();
			}
		}).start();

		// Main thread is listening for incoming requests
		try {
			System.out.println("Server ready!");
			ServerSocket serverSocket = new ServerSocket(this.rd.processPortNumber);
			while(true) {				
				Socket socket = serverSocket.accept();
				System.out.println("\nAccepted connection request...");
				System.out.print(">> ");
				(new Thread() {
					public void run() {
						requestHandler(socket);
					}
				}).start();				
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} // 	finally {
		// 	try {
		// 		serverSocket.close();
		// 	} catch (IOException e) {
		// 		e.printStackTrace();
		// 	}
		// }
	}

	/**
	 * process request from the remote router. For example: when router2 tries to
	 * attach router1. Router1 can decide whether it will accept this request. The
	 * intuition is that if router2 is an unknown/anomaly router, it is always safe
	 * to reject the attached request from router2.
	 */
	private void requestHandler(Socket socket) {
		try {
			BufferedReader incoming = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter outgoing = new PrintWriter(socket.getOutputStream(), true);

			String message = incoming.readLine();
			while (message != null) {
                    System.out.println("\nhandling: " + "\"" + message + "\" from " + socket.getRemoteSocketAddress().toString());
					System.out.print(">> ");

					String[] messageParts = message.split(" ");

					String task = messageParts[0].toLowerCase();
					String remote_sIP = messageParts[1];

					switch(task) {
						case "attach":						
							// if all ports are occupied
							if(this.ports.size() == 4) {
								outgoing.println("full");
							}
							// can attach successfully
							else {
								String remote_address = socket.getInetAddress().toString().substring(1);
								int remote_port = Integer.valueOf(messageParts[2]);
								int linkWeight = Integer.valueOf(messageParts[3]);

								RouterDescription remote_rd = new RouterDescription(remote_address, remote_port, remote_sIP);
								addLink(remote_rd, linkWeight);

								System.out.println("\nNow attached to router " + remote_sIP);
								System.out.println("Link weight: " + linkWeight);
								System.out.print(">> ");

								outgoing.println("success");
							}
							break;

						case "hello":
							Link link = this.ports.get(remote_sIP);
							if (link != null) {
								if (link.cStatus.equals(Link.ConnectionStatus.NONE)) {

									// create socket and input/output streams for two-way communication
									Socket two_way_socket = new Socket(link.remoteRouter.processIPAddress, link.remoteRouter.processPortNumber);
									BufferedReader two_way_incoming = new BufferedReader(new InputStreamReader(two_way_socket.getInputStream()));
									PrintWriter two_way_outgoing = new PrintWriter(two_way_socket.getOutputStream(), true);

									link.setCommunicationDetails(two_way_socket, two_way_outgoing, two_way_incoming);

									link.setConnectionStatus(Link.ConnectionStatus.INIT);

									System.out.println("\nSetting connection status to INIT");
									System.out.println("Sending HELLO to " + remote_sIP);
									System.out.println(">> ");

									link.outgoing.println("HELLO " + this.rd.simulatedIPAddress);
								}
								else if (link.cStatus.equals(Link.ConnectionStatus.INIT)) {
									link.setConnectionStatus(Link.ConnectionStatus.TWO_WAY);

									System.out.println("\nReceived HELLO from " + remote_sIP);
									System.out.println("Setting connection status to TWO_WAY");
									System.out.println("Sending HELLO to " + remote_sIP);
									System.out.println(">> ");

									link.outgoing.println("HELLO " + this.rd.simulatedIPAddress);
								}
							}

							break;
					}
					message = incoming.readLine();
            }
			socket.close();
			System.out.println("\nSocket connection closed.");
			System.out.print(">> ");
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	// Created a method to add Links to the HashMap
	public synchronized void addLink(RouterDescription r2, int weight) {
		Link link = new Link(this.rd, r2, weight);
		this.ports.put(r2.simulatedIPAddress, link);
	}

	/**
	 * output the intest path to the given destination ip
	 * <p/>
	 * format: source ip address -> ip address -> ... -> destination ip
	 *
	 * @param destinationIP the ip adderss of the destination simulated router
	 */
	private void processDetect(String destinationIP) {

	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 *
	 * @param portNumber the port number which the link attaches at
	 */
	private void processDisconnect(int portNumber) {

	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * <p/>
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, int processPort, String simulatedIP, int weight) {
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

				PrintWriter outgoing = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader incoming = new BufferedReader(new InputStreamReader(socket.getInputStream()));			

				System.out.println("\nSending attach request");
				System.out.print(">> ");
				String message = "attach " + this.rd.simulatedIPAddress + " " + this.rd.processPortNumber + " " + weight;
				outgoing.println(message);

				String response = incoming.readLine();

				System.out.println("\nResponse: " + response);
				System.out.print(">> ");

				if (response.equals("success")) {
					addLink(targetRD, weight);
					System.out.println("\nSuccessfully attached to router " + simulatedIP);
					System.out.print(">> ");
				}
				else if (response.equals("full")) {
					System.out.println("\nERROR: All ports at router " + processIP + ":" + processPort + "are occupied");
					System.out.print(">> ");
				}
				socket.close();
				System.out.println("\nSocket connection closed.");
				System.out.print(">> ");
			} 
			catch (UnknownHostException e) {
				e.printStackTrace();
				System.out.println("\nHost unknown");
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("\nAll 4 ports are full, cannot attach");
		}
	}


	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {
		//Collection<Link> allLinks = this.ports.values();

		for (Link link : this.ports.values()) {
			// MIGHT WANT TO RUN ALL THIS IN SEPARATE THREAD!!
			//sendHello(link);
			if (link.cStatus.equals(Link.ConnectionStatus.NONE)) {
				initHelloProtocol(link);
			}
		}
	}

	private void initHelloProtocol(Link link) {
		try {

			String remoteSIP = link.remoteRouter.simulatedIPAddress;

			// create socket and input/output streams for two-way communication
			Socket two_way_socket = new Socket(link.remoteRouter.processIPAddress, link.remoteRouter.processPortNumber);
			BufferedReader two_way_incoming = new BufferedReader(new InputStreamReader(two_way_socket.getInputStream()));
			PrintWriter two_way_outgoing = new PrintWriter(two_way_socket.getOutputStream(), true);

			link.setCommunicationDetails(two_way_socket, two_way_outgoing, two_way_incoming);


			link.setConnectionStatus(Link.ConnectionStatus.INIT);	

			System.out.println("\nSetting connection status to INIT");		
			System.out.println("Sending HELLO to " + remoteSIP);
			System.out.print(">> ");
			
			String message = "HELLO " + this.rd.simulatedIPAddress;
			link.outgoing.println(message);
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
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

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		System.out.println("\n\nROUTER NEIGHBORS:");
		System.out.println("\n---------------------------------\n");
		for (Link link : ports.values()) {
			System.out.println("Router address: " + link.remoteRouter.processIPAddress + ":" + link.remoteRouter.processPortNumber);
			System.out.println("Simulated IP: " + link.remoteRouter.simulatedIPAddress);
			System.out.println("Link weight: " + link.weight);
			System.out.println("Connection status: " + link.cStatus.toString());
			System.out.println("\n---------------------------------\n");
		}
		System.out.print(">> ");
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {

	}

	/**
	 * update the weight of an attached link
	 */
	private void updateWeight(String processIP, int processPort, String simulatedIP, int weight) {

	}

	private void printToTerminal(String string) {
		System.out.println("\n" + string);
		System.out.print(">> ");
	}

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
							processAttach(pIP, pPort, sIP, weight);
						}
					}).start();
					
				}
				else if (command.equals("start")) {
					(new Thread() {
						public void run() {
							processStart();
						}
					}).start();
					
				} 
				else if (command.equals("neighbors")) {
					(new Thread() {
						public void run() {
							processNeighbors();
						}
					}).start();	
				} 
				// else if (command.startsWith("disconnect ")) {
				// 	String[] cmdLine = command.split(" ");
				// 	processDisconnect(int.parseint(cmdLine[1]));
				// } 
				// else if (command.startsWith("detect ")) {
				// 	String[] cmdLine = command.split(" ");
				// 	processDetect(cmdLine[1]);
				// }  
				// else if (command.equals("connect")) {
				// 	String[] cmdLine = command.split(" ");
				// 	processConnect(cmdLine[1], int.parseint(cmdLine[2]), cmdLine[3], int.parseint(cmdLine[4]));
				// } 
				else if (command.startsWith("quit ")) {
					processQuit();
					break;
				} 
				else {
					System.out.println("Please enter a valid command.");
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
