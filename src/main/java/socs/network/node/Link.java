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

  public Link(RouterDescription r1, RouterDescription r2, int weight, Socket socket, PrintWriter outgoing, BufferedReader incoming) {
    localRouter = r1;
    remoteRouter = r2;
    this.weight = weight;
    this.cStatus = ConnectionStatus.NONE;
    this.socket = socket;
    this.outgoing = outgoing;
    this.incoming = incoming;
  }

  public void setConnectionStatus(ConnectionStatus cStatus) {
		this.cStatus = cStatus;
	}

	public ConnectionStatus getConnectionStatus() {
		return this.cStatus;
	}

  public enum ConnectionStatus {
    NONE,
		INIT,
		TWO_WAY,
	}
}
