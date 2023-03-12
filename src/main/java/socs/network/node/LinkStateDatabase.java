package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.HashMap;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  String getShortestPath(String destinationIP) {
//    System.out.println(rd.simulatedIPAddress);
//    System.out.println(_store);

    HashMap<String, WeightedGraph.Node> nodes = new HashMap<String, WeightedGraph.Node>();
    System.out.println(destinationIP);
	System.out.println(nodes.get(destinationIP));
    WeightedGraph graph = new WeightedGraph();
    for (String linkID : _store.keySet()) {
//      System.out.println(linkID);
      WeightedGraph.Node tmpNode = new WeightedGraph.Node(linkID);
      nodes.put(linkID, tmpNode);
      graph.addNode(tmpNode);
    }
    if (nodes.get(destinationIP) == null) {
    	return "destinationIP (" + destinationIP + ") does not exist in the network";
    }
    for (String linkID : _store.keySet()) {
      LSA lsa = _store.get(linkID);
      for (LinkDescription ld : lsa.links) {
//        System.out.println(linkID + " " + ld.linkID + " " + ld.tosMetrics);
        graph.addEdge(nodes.get(linkID), nodes.get(ld.linkID), ld.tosMetrics);
      }
    }
    return graph.shortestPath(nodes.get(rd.simulatedIPAddress), nodes.get(destinationIP));
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
//    System.out.println("here" + rd.simulatedIPAddress);
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.tosMetrics).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
