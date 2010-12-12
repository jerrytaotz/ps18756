package DataTypes;
/**
 * An RSVP PATH message.
 * @author Brady Tello
 */
import java.util.*;

public class PATHMsg extends rsvpPacket {	
	ArrayList<Integer> prevHops; //So RESV messages come back on the same route
	
	/**
	 * Default constructor for a PATH message
	 * @param source the address of the original sender (NOT the prev. hop)
	 * @param dest the address of the ultimate receiver (NOT the next hop)
	 * @param PHB the DiffServ PHB which will be reserved for this path
	 * @param tClass if this is to reserve an AF stream this will be the AF class.  0 otherwise
	 * @param tspec the amount of bandwidth the receiver should request in its RESV message
	 */
	public PATHMsg(int source, int dest){
		super(source,dest);
		this.type = "PATH";
		this.prevHops = new ArrayList<Integer>();
	}
		
	/**
	 * Add the next prevHop to this message.
	 * @param prevHop
	 */
	public void addPrevHop(int prevHop){
		this.prevHops.add(0,prevHop);
	}

	/**
	 * return the hops used by this message to get where it currently is.
	 * @return the hops taken by this message so far.
	 */
	public ArrayList<Integer> getPrevHops(){
		return this.prevHops;
	}
	
	public void setPrevHops(ArrayList<Integer> prevHops) {
		this.prevHops = prevHops;
	}

	public PATHMsg Clone(){
		PATHMsg clone = new PATHMsg(this.source,this.dest);
		clone.setPrevHops((ArrayList<Integer>)this.prevHops.clone());
		clone.setId(this.getId());
		return clone;
	}
	
}
