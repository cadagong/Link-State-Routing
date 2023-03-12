// package socs.network.node;

// import java.io.BufferedReader;
// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.io.OutputStreamWriter;
// import java.io.PrintWriter;
// import java.net.Socket;
// import java.util.ArrayList;
// import java.util.Scanner;

// public class RouterThread extends Thread {
// 	private Socket socket;
// 	private BufferedReader in = null;
// 	private PrintWriter out = null;
// 	private boolean client;
// 	private Router router;
// 	private RouterDescription rd;
// 	private int weight;
// 	private Task task;

// 	// Incoming tasks from other routers
// 	public RouterThread(Router router, Socket socket) {

// 	}


// 	// Locally-originating tasks without network calls
// 	public RouterThread(Router router, Socket socket, Task task, RouterDescription thisRD) {
// 		//super();
// 		// System.out.println("running inside thread constructor");
// 		// System.out.println("client: " + client);
// 		// this.socket = socket;
// 		// this.client = client;
// 		// this.router = router;
// 		// this.rd = rd;
// 		// this.weight = weight;
// 	}

// 	// Locally-originating tasks with network calls
// 	public RouterThread(Router router, Socket socket, Task task, RouterDescription thisRD, RouterDescription targetRD) {
// 		//super();
// 		// System.out.println("running inside thread constructor");
// 		// System.out.println("client: " + client);
// 		// this.socket = socket;
// 		// this.client = client;
// 		// this.router = router;
// 		// this.rd = rd;
// 		// this.weight = weight;
// 	}

// 	@Override
// 	public void run() {
// 		// System.out.println("running inside thread run method");
// 		// If server, then prompt user to accept/deny request

// 		try {
// 			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
// 			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
// 		} catch (IOException e) {
// 			e.printStackTrace();
// 		}
// 		if (!client) {
// 			Scanner scanner = new Scanner(System.in);
// 			System.out.println("received HELLO from " + socket.getInetAddress().getHostName());
// 			System.out.println("Do you accept this request? (Y/N)");
// 			String input = scanner.nextLine();
// 			scanner.close();
// 			// Close connection if rejected
// 			if (input.equals("N")) {
// 				try {
// 					// Send to client "N" to notify it that the request has been rejected
// 					out.println("N");
// 					System.out.println("You rejected the attach request");
// 					socket.close();
// 					in.close();
// 					out.close();
// 					return;
// 				} catch (IOException e) {
// 					e.printStackTrace();
// 				}
// 			// SERVER: If request accepted, start listening to receive messages from client
// 			} else {
// 				receive();
// 			}
// 		// CLIENT: start listening to receive messages from server
// 		} else {
// 			receive();
// 		}
// 	}

// 	private void receive() {
// 		//System.out.println("We are running receive in RouterThread!");
// 		// If client, send simulated IP and weight
// 		if (client) {
// 			// Combine the two with a ',' in between
// 			out.println(rd.simulatedIPAddress + "," + weight);
// 			try {
// 				// Checks if attach request has been accepted or not
// 				// Maybe add print statements if attached successfully to test?
// 				String accept = in.readLine();
// 				if (accept.equals("N")) {
// 					System.out.println("Your attach request has been rejected");
// 					socket.close();
// 					in.close();
// 					out.close();
// 					return;
// 				}
// 				// Create a new RouterDescription for server and add it to the HashMap using addLink()
// 				String serverIP = in.readLine();
// 				RouterDescription r2 = new RouterDescription(socket.getInetAddress().getHostName(), (int) socket.getPort(), serverIP);
// 				//router.addLink(r2, weight);
// 			} catch (IOException e) {
// 				e.printStackTrace();
// 			}
// 		// If server, send simulated IP
// 		} else {
// 			out.println(rd.simulatedIPAddress);
// 			try {
// 				// Read client simulated IP and link weight
// 				String ipAndWeight = in.readLine();
// 				// Split the two values with ','
// 				String[] parts = ipAndWeight.split(",");
// 				String clientIP = parts[0];
// 				int weightFromClient = int.parseint(parts[1]);
// 				// Create a new RouterDescription for client and add it to the HashMap using addLink()
// 				RouterDescription r2 = new RouterDescription(socket.getInetAddress().getHostName(), (int) socket.getPort(), clientIP);
// 				//router.addLink(r2, weightFromClient);
// 			} catch (IOException e) {
// 				e.printStackTrace();
// 			}
// 		}
// 		// TO DO: START() command
// 		// When receive HELLO, change state of router description, and send back hello, and more
// 		while (true) {
// 			String clientCommand;
// 			try {
// 				clientCommand = in.readLine();
// 				System.out.println("Client Says :" + clientCommand);
// 			} catch (IOException e) {
// 				e.printStackTrace();
// 			}
// 		}
// 	}

// 	//TO DO: START() command
// 	// public void start() {
// 	// 	out.println();
// 	// 	out.flush();
// 	// }
// }
