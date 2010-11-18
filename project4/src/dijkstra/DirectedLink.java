package dijkstra;

public class DirectedLink {
	private Node node; //The node at the end of the link
	private float cost;
	
	/**
	 * Initializes a new DirectedLink with a specified end node and
	 * a cost.
	 * @param endNode
	 * @param initialCost
	 */
	public DirectedLink(Node endNode, float newCost){
		cost = newCost;
		node = endNode;
	}
	
	/**
	 * Get the cost associated with this link.
	 * @return the cost of the link.
	 */
	public float getCost(){
		return cost;
	}
	
	/**
	 * Return the node at the end of this link.
	 * @return the node at the end of the link
	 */
	public Node getNode(){
		return node;
	}
}
