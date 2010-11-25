package DataTypes;

import java.util.ArrayList;

public class RESVMsg extends rsvpPacket {
	int tspec, PHB, tClass, label;	
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
	public RESVMsg(int source, int dest, int PHB, 
			int tClass, int tspec, ArrayList<Integer> hops){
		super(source,dest);
		this.type = "RESV";
		this.prevHops = hops;
		this.PHB = PHB;
		this.tClass = tClass;
		this.tspec = tspec;
		this.label = 0;
	}
	
	/**
	 * Return the tspec for this PATH message.
	 * @return the tspec
	 */
	public int getTspec(){
		return tspec;
	}
	
	/**
	 * get the PHB for this reservation request
	 * @return the requested PHB
	 */
	public int getPHB() {
		return PHB;
	}

	/**
	 * get the AF class for this message.  NOTE: this will be 0 if this is not an AF request. 
	 * @return the AF class
	 */
	public int gettClass() {
		return tClass;
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
	
	public RESVMsg Clone(){
		RESVMsg clone = new RESVMsg(this.source,this.dest,this.PHB,this.tClass,
				this.tspec, (ArrayList<Integer>)this.prevHops.clone());
		clone.setID(this.getID());
		clone.setLabel(this.label);
		return clone;
	}
}
