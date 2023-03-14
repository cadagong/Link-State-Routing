# Link-State-Routing

In this project, we developped a pure user-space program which simulates the major functionalities of a routing device running a simplified Link State Routing protocol. To simulate the real-world network environment, we start multiple instances of the program, each of which connects with (some of) the other routers via sockets. Each program instance represents a router or host in the simulated network space. 
The links connecting the routers/hosts and the IP addresses identifying the routers/hosts are simulated by in-memory data structures.

### Command-Line Client Commands
1. **Attach**
- R1 sends an attach request to R2. 
- R2 accepts the request if it's ports are not full.

2. **Start**
- R1 sets connection status with R2 to INIT and sends HELLO to R2.
- R2 recieves HELLO from R1 and sets connection status with R1 to INIT.
- R2 sends HELLO back to R1.
- R1 recieves HELLO from R2 and sets connection status with R2 to TWO_WAY.
- R1 sends HELLO to R2 again.
- R2 receives HELLO from R1 and sets connection status to TWO_WAY.
- R1 now initiates LSAUPDATE procedure by sending it's LSD (which at this point is empty) to R2.
- R2 receives LSAUPDATE message from R1 and updates it's LSD.
- R2 forwards the LSD it recieved from R1 to any other routers it is attached to, which in turn do the same.
- R2 also sends it's LSD back to R1 so that R1 can update it's information (since it just started and doesn't have the latest routing information).

3. **Detect**
- Router runs Dijkstra's Algorithm on it's LSD to determine the shortest path to the router with the simulated IP address of interest.
- No network communication occurs as a result of this command.



