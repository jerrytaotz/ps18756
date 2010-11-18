package dijkstra;


import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestDirectedGraph {
	private DirectedGraph graph;
	private PathCalculator pc = new PathCalculator();
	
	@Before
	public void setUp() throws Exception {
		graph = new DirectedGraph();
		
		Node node1 = new Node(1);
		Node node2 = new Node(2);
		Node node3 = new Node(3);
		Node node4 = new Node(4);
		Node node5 = new Node(5);
		Node node6 = new Node(6);
		
		node1.addLink(1/(float)1000, node2);
		node2.addLink(1/(float)1000, node1);
		node6.addLink(1/(float)100, node2);
		node2.addLink(1/(float)100, node6);
		node3.addLink(1/(float)500, node2);
		node2.addLink(1/(float)500, node3);
		node5.addLink(1/(float)1000, node2);
		node2.addLink(1/(float)1000, node5);
		node5.addLink(1/(float)300, node4);
		node4.addLink(1/(float)300, node5);
		node4.addLink(1/(float)1000, node3);
		node3.addLink(1/(float)1000, node4);
		
		graph.addNode(node1);
		graph.addNode(node2);
		graph.addNode(node3);
		graph.addNode(node4);
		graph.addNode(node5);
		graph.addNode(node6);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testDirectedGraph(){
		System.out.println(graph);
	}

	@Test
	public void testDijkstras(){
		HashMap<Integer, Integer> bestPaths;
		System.out.println(graph);
		//For each node in the graph run:
		for(Node n:graph.getNodes()){
			System.out.println("Best paths for node " + n.getAddress());
			//shortest path calculation
			pc.dijkstrasAlgorithm(graph, n.getAddress());
			//find Best Paths
			bestPaths = pc.findBestPaths(graph, n.getAddress());
			for(Integer i:bestPaths.keySet()){
				System.out.println(i + ": " + bestPaths.get(i));
			}
		}
	}
}
