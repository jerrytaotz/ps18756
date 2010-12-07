package dijkstra;

import java.util.ArrayList;
import java.util.HashMap;

public class PathCalculator {

	/**
	 * My implementation of dijkstra's best path algorithm.  This 
	 * assumes a fully connected graph.  If g is a non-fully connected
	 * graph, some nodes will not be accounted for.
	 * @param g
	 * @param startNode
	 */
	public void dijkstrasAlgorithm(DirectedGraph g, int startNode){
		Node currNode;
		boolean existsNextNode = true;
		float min = Float.MAX_VALUE;
		ArrayList<Node> reachable = new ArrayList<Node>();
		
		//initialize the costs to infinity
		for(Node n: g.getNodes()){
			n.setCurrCost(Float.MAX_VALUE);
			n.setPrevNode(-1);
			n.setVisited(false);
		}
		
		currNode = g.getNode(startNode);
		currNode.setCurrCost(0);
		currNode.setPrevNode(0);
		currNode.setVisited(true);
		
		while(existsNextNode){
			existsNextNode = false;
			for(DirectedLink l:currNode.getLinks()){
				//If the adjacent node has not been visited, check its cost
				if(!l.getNode().wasVisited()){
					if(!reachable.contains(l.getNode())) reachable.add(l.getNode());
					if((l.getNode().getCurrCost()) > (currNode.getCurrCost() + l.getCost())){
						//update the cost and the previous node
						l.getNode().setCurrCost(currNode.getCurrCost() + l.getCost());	
						l.getNode().setPrevNode(currNode.getAddress());
					}
				}
			}
			//Now that all the costs have been updated, check for the lowest one.
			for(Node n:reachable){
				if(n.getCurrCost() < min){
					currNode = n;
					min = n.getCurrCost();
					existsNextNode = true;
				}
			}
			if(existsNextNode){
				currNode.setVisited(true);
				reachable.remove(currNode);
			}
			min = Float.MAX_VALUE; //reset the min for the next round
		}//while(existsNextNode)
	}//findShortestPaths()
	
	/**
	 * Calculates all the best paths between a source node and the rest of the graph.
	 * In the context of a network, this function calculates routing tables.
	 * This function assumes that dijkstra's algorithm has already been run on this graph. 
	 * @param g the graph to find the paths for
	 * @param source the node from which your best paths will be referenced
	 * @return a hash map containing the next hop for each destination. null on error.
	 */
	public HashMap<Integer, Integer> findBestPaths(DirectedGraph g, int source){
		HashMap<Integer, Integer> bestPaths = new HashMap<Integer, Integer>();
		int prevHop,currHop;
		
		if(g.getNode(source) == null) return null;
		for(Node dest:g.getNodes()){
			//we're not concerned with the source node
			if(dest.getAddress() != source){
				currHop = dest.getAddress();
				prevHop = dest.getPrevNode();
				//trace our way back to the source
				while(prevHop != -1){
					//If we haven't traced back to the source, get the next hop
					if(prevHop != source){
						currHop = prevHop;
						prevHop = g.getNode(prevHop).getPrevNode();
					}
					//otherwise we have found the best next hop for the destination
					else{
						bestPaths.put(dest.getAddress(), currHop);
						break;
					}
				}
			}
		}
		return bestPaths;
	}
}
