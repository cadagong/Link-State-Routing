package socs.network.node;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Link {

  RouterDescription localRouter;
  RouterDescription remoteRouter;
  int weight;
  Socket socket;
  PrintWriter outgoing;
  BufferedReader incoming;
  ConnectionStatus cStatus;

  public Link(RouterDescription r1, RouterDescription r2, int weight) {
    localRouter = r1;
    remoteRouter = r2;
    this.weight = weight;
    this.cStatus = ConnectionStatus.NONE;
  }

  public Link(RouterDescription r1, RouterDescription r2, int weight, Socket socket, PrintWriter outgoing, BufferedReader incoming) {
    localRouter = r1;
    remoteRouter = r2;
    this.weight = weight;
    this.cStatus = ConnectionStatus.NONE;
    this.socket = socket;
    this.outgoing = outgoing;
    this.incoming = incoming;
  }

  public synchronized void setCommunicationDetails(Socket socket, PrintWriter outgoing, BufferedReader incoming) {
    this.socket = socket;
    this.outgoing = outgoing;
    this.incoming = incoming;
  }

  public synchronized void setConnectionStatus(Link.ConnectionStatus cStatus) {
    this.cStatus = cStatus;
  }

  public enum ConnectionStatus {
    NONE,
		INIT,
		TWO_WAY,
	}
}
