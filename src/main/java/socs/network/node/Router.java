package socs.network.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import socs.network.util.Configuration;

public class Router {

	protected LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	private ServerSocket serverSocket;
//	Link[] ports = new Link[4];

	// assuming that all routers are with 4 ports
	// I changed this to HashMap so it's easier to use
	HashMap<String,Link> ports;

	public Router(Configuration config) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		lsd = new LinkStateDatabase(rd);
		ports = new HashMap<String,Link>();
		// Create a new thread for the terminal
		(new Thread() {
			public void run() {
				terminal();
			}
		}).start();
		// Original main thread is just listening for incoming connections
		requestHandler();
	}
	
	// Created a method to add Links to the HashMap
	public void addLink(RouterDescription r2, short weight) {
		Link link = new Link(rd, r2, weight);
		ports.put(r2.simulatedIPAddress, link);
	}

	/**
	 * output the shortest path to the given destination ip
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
	private void processDisconnect(short portNumber) {

	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * <p/>
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
		if (ports.size() < 4) {
			try {
				// "Client" requesting attach to "Server"
				RouterDescription outgoingRD = new RouterDescription(processIP, processPort, simulatedIP);
				Socket clientSocket = new Socket(processIP, processPort);
				new RouterThread(clientSocket, true, this, outgoingRD, weight).start();
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.out.println("Host unknown");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("All 4 ports are full, cannot attach");
		}
	}

	/**
	 * process request from the remote router. For example: when router2 tries to
	 * attach router1. Router1 can decide whether it will accept this request. The
	 * intuition is that if router2 is an unknown/anomaly router, it is always safe
	 * to reject the attached request from router2.
	 */
	private void requestHandler() {
		try {
			serverSocket = new ServerSocket(3000);
			// Might need to change this in the future
			while (ports.size() < 4) {
				Socket socket = serverSocket.accept();
				// (short) -1 because as a "server" receiving attach requests,
				// it doesn't have access to the link weight
				new RouterThread(socket, true, this, rd, (short) -1).start();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * broadcast Hello to neighbors
	 */
	// ****TO DO*****
	private void processStart() {

	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		for (String i : ports.keySet()) {
			System.out.println(i);
		}
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {

	}

	/**
	 * update the weight of an attached link
	 */
	private void updateWeight(String processIP, short processPort, String simulatedIP, short weight) {

	}

	public void terminal() {
		try {
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();
			while (true) {
				if (command.startsWith("detect ")) {
					String[] cmdLine = command.split(" ");
					processDetect(cmdLine[1]);
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Short.parseShort(cmdLine[1]));
				} else if (command.startsWith("quit")) {
					processQuit();
					// Theoretically done
				} else if (command.startsWith("attach ")) {
					String[] cmdLine = command.split(" ");
					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				// TO DO: START()
				} else if (command.equals("start")) {
					processStart();
				} else if (command.equals("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					// output neighbors
					// DONE: theoretically
					processNeighbors();
				} else {
					// invalid command
					// Why break here? does this need to be changed? worry after
					break;
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
