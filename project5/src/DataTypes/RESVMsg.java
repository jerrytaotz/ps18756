package DataTypes;
/**
 * An RSVP RESV message
 * @author Brady Tello
 */

import java.util.ArrayList;

public class RESVMsg extends rsvpPacket {
	Label dl;	
	ArrayList<Integer> prevHops; //So RESV messages come back on the same route
	
	/**
	 * Allocate space for a new RESVMsg.
	 * @param source the address of the receiver of the traffic (the node which created this message)
	 * @param dest the address of the traffic sender
	 * @param dl the downstream label to be confirmed
	 */
	public RESVMsg(int source, int dest, ArrayList<Integer> hops,Label dl){
		super(source,dest);
		this.type = "RESV";
		this.prevHops = hops;
		this.dl = dl;
	}
	
	/**
	 * Allocate space for a new PSC RESV message
	 * @param source the address of the receiver of the traffic (the node which created this message)
	 * @param dest the address of the traffic sender
	 * @param dl the downstream label to be confirmed
	 */
	public RESVMsg(int source, int dest,Label dl){
		super(source,dest);
		this.type = "RESV";
		this.dl = dl;
	}
	
	/**
	 * Creates a new RESV message with identical fields to a given RESV message but capable of
	 * being transported across the optical network
	 * @param m the message to be encapsulated in an wrapper
	 * @param ol the optical label to be used.
	 */
	public RESVMsg(RESVMsg m,OpticalLabel ol){
		super(m.source,m.dest,ol);
		this.type = "RESV";
		this.dl = m.getDL();
		this.setId(m.getId());
	}
	
	/**
	 * Return the next hop on the list of hops to get back to sender.
	 * Simulates the routers keeping state about the PATH message.
	 */
	public int getNextHop(){
		return this.prevHops.remove(0);
	}
	
	/**
	 * Set the GMPLS Downstream Label in this message.  This will be the label used by an upstream
	 * router to forward data to the LSR which created this message.
	 * @param dl the new label to use.
	 */
	public void setDL(Label dl){
		this.dl = dl;
	}
	
	/**
	 * Return the downstream label in this RESV message
	 * @return
	 */
	public Label getDL(){
		return this.dl;
	}
	
	
	/**
	 * make a deep copy of this message
	 * @return a deep copy of this message.
	 */
	public RESVMsg Clone(){
		RESVMsg clone = new RESVMsg(this.source,this.dest, 
				(ArrayList<Integer>)this.prevHops.clone(),this.dl.clone());
		clone.setId(this.getId());
		clone.setDL(this.dl);
		return clone;
	}
}
