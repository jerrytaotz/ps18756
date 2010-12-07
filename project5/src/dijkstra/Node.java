package dijkstra;

import java.util.ArrayList;

/**
 * An abstraction of a graph vertex to be used in a directed graph in 
 * Dijkstra's algorithm.  The term Node is used rather than vertex since
 * we are working on a network.
 * @author Brady
 *
 */
public class Node {
	private int address;
	private float currCost;
	private boolean visited;
	private ArrayList<DirectedLink> links;
	private int prevNodeAddress; //The previous node on the best path to this node.
	
	/**
	 * Sets the distance to infinity and the visited state to false.
	 */
	public Node(int nodeAddress){
		currCost = Float.MAX_VALUE;
		visited = false;
		address = nodeAddress;
		this.links = new ArrayList<DirectedLink>();
		prevNodeAddress = -1;
	}
	
	/**
	 * Set the current distance to this node.
	 * @param newWeight
	 */
	public void setCurrCost(float newCost){
		currCost = newCost;
	}
	
	/**
	 * Return the current cost to this node.
	 * @return the current cost to this node.
	 */ 
	public float getCurrCost(){
		return currCost;
	}
	
	/**
	 * Sets the visited state of this node to true
	 */
	public void setVisited(boolean newVisited){
		visited = newVisited;
	}
	
	/**
	 * Determine whether or not this node has been visited yet.
	 * @return has this node been visited yet?
	 */
	public boolean wasVisited(){
		return visited;
	}
	
	/**
	 * Get the address of this node.
	 * @return this nodes address
	 */
	public int getAddress(){
		return address;
	}
	
	/**
	 * Adds a new directed link to this node
	 * @param linkCost
	 * @param endNode
	 */
	public void addLink(float linkCost, Node endNode){
		DirectedLink newLink = new DirectedLink(endNode, linkCost);
		this.links.add(newLink);
	}
	
	/**
	 * returns the list of directed links for this node.
	 */
	public ArrayList<DirectedLink> getLinks(){
		return links;
	}
	
	/**
	 * Returns the previous node on the best path to this node
	 * Assumes that the best path calculator's findBestPaths has already been called
	 * on this node's parent graph.
	 * @return the previous node on the best path to this node.
	 */
	public int getPrevNode(){
		return prevNodeAddress;
	}
	
	/**
	 * Sets the address of the previous node on the best path to this node
	 * @param prevAddress
	 */
	public void setPrevNode(int prevAddress){
		prevNodeAddress = prevAddress;		
	}
}
