package socs.network.message;

import java.io.*;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class SOSPFPacket implements Serializable {

  //for inter-process communication
  public String srcProcessIP;
  public int srcProcessPort;

  //simulated IP address
  public String srcIP;
  public String dstIP;

  //common header
  public int sospfType; //0 - HELLO, 1 - LinkState Update
  public String routerID;

  //used by HELLO message to identify the sender of the message
  //e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
  //simulated IP address
  public String neighborID; //neighbor's simulated IP address
  public String message;

  //used by LSAUPDATE
  public Vector<LSA> lsaArray = null;
  
  public SOSPFPacket(String message) {
	  this.sospfType = 0;
	  this.message = message;
  }
  
  public SOSPFPacket(Vector<LSA> lsaVector, String srcIP) {
	  this.sospfType = 1;
	  this.lsaArray = lsaVector;
	  this.srcIP = srcIP;
  }
  

}
