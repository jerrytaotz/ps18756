package dijkstra;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * An abstraction a directed graph to be used in Dijkstra's algorithm.
 * The terms nodes and links will be used rather than vertices and edges.
 * @author Brady
 *
 */
public class DirectedGraph {
	private HashMap<Integer, Node> graph;
	
	public DirectedGraph(){
		graph = new HashMap<Integer, Node>();
	}
	/**
	 * Add a new node to this graph.
	 * @param node the node to be added.
	 */
	public void addNode(Node node){
		graph.put(node.getAddress(),node);
	}
	
	/**
	 * Retrieve a node in this graph
	 * @param address the address of the node to be retrieved
	 * @return the node corresponding to the supplied address
	 */
	public Node getNode(int address){
		if(graph.containsKey(address)){
			return (Node)graph.get(address);
		}
		else return null;
	}
	
	/**
	 * Determine whether this graph contains a node with a specific id or not.
	 * @param address the node address to be searched for
	 * @return true if the graph contains the node false otherwise
	 */
	public boolean containsNode(int address){
		if(graph.containsKey(address)){
			return true;
		}
		else return false;
	}
	
	/**
	 * Retrieve the set of all nodes for this graph.
	 */
	public Collection<Node> getNodes(){
		return graph.values();
	}
	
	public String toString(){
		String description = new String("NODE: {<next hop>,<link cost>,<next node cost>}...\n");
		Integer address;
		Float nodeCost;
		Float linkCost;
		Node nextNode;
		
		Collection<Node> nodes = graph.values();
		for(Iterator<Node> it = nodes.iterator();it.hasNext();){
			nextNode = it.next();
			address = nextNode.getAddress();
			description = description.concat(address.toString() + ": ");
			for(DirectedLink link:nextNode.getLinks()){
				address = link.getNode().getAddress();
				nodeCost = link.getNode().getCurrCost();
				linkCost = link.getCost();
				//if the current cost to the node is Infinity then mark it as such
				if(nodeCost == Float.MAX_VALUE){
					description = 
						description.concat("{" + address + "," + linkCost +",INF} ");
				}
				else description = 
					description.concat("{" + address + "," + linkCost + "," +
							link.getNode().getCurrCost() + "} ");
			}
			description = description.concat("\n");
		}
		return description;
	}
}
