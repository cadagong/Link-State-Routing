package socs.network.node;
import java.util.*;

public class WeightedGraph {
    private Map<Node, List<Edge>> graph;

    public WeightedGraph() {
        this.graph = new HashMap<>();
    }

    public void addNode(Node node) {
        graph.put(node, new ArrayList<>());
    }

    public void addEdge(Node from, Node to, int weight) {
        Edge edge = new Edge(to, weight);
        graph.get(from).add(edge);
    }

    public String shortestPath(Node start, Node end) {
        Map<Node, Integer> distance = new HashMap<>();
        Map<Node, Node> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>((a, b) -> distance.getOrDefault(a, Integer.MAX_VALUE) - distance.getOrDefault(b, Integer.MAX_VALUE));
    
        distance.put(start, 0);
        queue.offer(start);
    
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            for (Edge edge : graph.get(current)) {
                int dist = distance.get(current) + edge.weight;
                Node neighbor = edge.to;
                if (dist < distance.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distance.put(neighbor, dist);
                    previous.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }
    
        List<Node> path = new ArrayList<>();
        Node current = end;
        while (previous.containsKey(current)) {
            path.add(current);
            current = previous.get(current);
        }
        path.add(start);
        Collections.reverse(path);
    
        StringBuilder sb = new StringBuilder();
        int totalWeight = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Node node = path.get(i);
            Node nextNode = path.get(i+1);
            int weight = 0;
            for (Edge edge : graph.get(node)) {
                if (edge.to.equals(nextNode)) {
                    weight = edge.weight;
                    break;
                }
            }
            sb.append(node.getId()).append(" ->(").append(weight).append(") ");
            totalWeight += weight;
        }
        sb.append(path.get(path.size()-1).getId());
//        sb.append("\nTotal weight: ").append(totalWeight);
    
        return sb.toString();
    }

    private static class Edge {
        private final Node to;
        private final int weight;

        private Edge(Node to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    public static class Node {
        private final String id;

        public Node(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}

// WeightedGraph graph = new WeightedGraph();

// WeightedGraph.Node node1 = new WeightedGraph.Node("1");
// WeightedGraph.Node node2 = new WeightedGraph.Node("2");
// WeightedGraph.Node node3 = new WeightedGraph.Node("3");
// WeightedGraph.Node node4 = new WeightedGraph.Node("4");
// WeightedGraph.Node node5 = new WeightedGraph.Node("5");

// graph.addNode(node1);
// graph.addNode(node2);
// graph.addNode(node3);
// graph.addNode(node4);
// graph.addNode(node5);

// graph.addEdge(node1, node2, 3);
// graph.addEdge(node1, node3, 2);
// graph.addEdge(node2, node3, 1);
// graph.addEdge(node2, node4, 1);
// graph.addEdge(node3, node4, 4);
// graph.addEdge(node4, node5, 3);

// String shortestPath = graph.shortestPath(node1, node5);
// System.out.println(shortestPath); // prints "1 --(3)--> 2 --(1)--> 4 --(3)--> 5\nTotal weight: 7"