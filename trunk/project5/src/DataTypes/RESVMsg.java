package DataTypes;
/**
 * An RSVP RESV message
 * @author Brady Tello
 */

import java.util.ArrayList;

public class RESVMsg extends rsvpPacket {
	int label;	
	ArrayList<Integer> prevHops; //So RESV messages come back on the same route
	
	/**
	 * Allocate space for a new RESVMsg.
	 * @param source the address of the receiver of the traffic (the node which created this message)
	 * @param dest the address of the traffic sender
	 * @param PHB the PHB to be reserved in each intermediate router
	 * @param tClass the AF class to be reserved if the PHB is AF
	 * @param tspec the amount of bandwidth to be reserved by each intermeiate node
	 * @param hops the hops to take to get back to the source
	 */
	public RESVMsg(int source, int dest, ArrayList<Integer> hops){
		super(source,dest);
		this.type = "RESV";
		this.prevHops = hops;
		this.label = 0;
	}
	
	/**
	 * Return the next hop on the list of hops to get back to sender.
	 * Simulates the routers keeping state about the PATH message.
	 */
	public int getNextHop(){
		return this.prevHops.remove(0);
	}
	
	/**
	 * Set the MPLS label in this message.  This will be the label used by a next
	 * hop router when forwarding this message back to the traffic source.
	 * @param label the new label to use.
	 */
	public void setLabel(int label){
		this.label = label;
	}
	
	/**
	 * Return the MPLS label in this RESV message
	 * @return
	 */
	public int getLabel(){
		return this.label;
	}
	
	
	/**
	 * make a deep copy of this message
	 * @return a deep copy of this message.
	 */
	public RESVMsg Clone(){
		RESVMsg clone = new RESVMsg(this.source,this.dest, 
				(ArrayList<Integer>)this.prevHops.clone());
		clone.setId(this.getId());
		clone.setLabel(this.label);
		return clone;
	}
}
