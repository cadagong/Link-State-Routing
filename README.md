# Link-State-Routing

In this project, we developpd a pure user-space program which simulates the major functionalities of a routing device running a simplified 
Link State Routing protocol. To simulate the real-world network environment, we start multiple instances of the program, each of which 
connects with (some of) the other routers via sockets. Each program instance represents a router or host in the simulated network space. 
The links connecting the routers/hosts and the IP addresses identifying the routers/hosts are simulated by in-memory data structures.
