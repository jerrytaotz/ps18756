package DataTypes;

import java.util.*;

public class PATHMsg extends rsvpPacket {
	int tspec, PHB, tClass;	
	ArrayList<Integer> prevHops; //So RESV messages come back on the same route
	
	/**
	 * Default constructor for a PATH message
	 * @param source the address of the original sender (NOT the prev. hop)
	 * @param dest the address of the ultimate receiver (NOT the next hop)
	 * @param PHB the DiffServ PHB which will be reserved for this path
	 * @param tClass if this is to reserve an AF stream this will be the AF class.  0 otherwise
	 * @param tspec the amount of bandwidth the receiver should request in its RESV message
	 */
	public PATHMsg(int source, int dest, int PHB, int tClass, int tspec){
		super(source,dest);
		this.type = "PATH";
		this.prevHops = new ArrayList<Integer>();
		this.PHB = PHB;
		this.tClass = tClass;
		this.tspec = tspec;
	}
		
	/**
	 * Add the next prevHop to this message.
	 * @param prevHop
	 */
	public void addPrevHop(int prevHop){
		this.prevHops.add(0,prevHop);
	}
		
	/**
	 * Return the tspec for this PATH message.
	 * @return the tspec
	 */
	public int getTspec(){
		return tspec;
	}
	
	public int getPHB() {
		return PHB;
	}

	public void setPHB(int pHB) {
		PHB = pHB;
	}

	public int gettClass() {
		return tClass;
	}

	public void settClass(int tClass) {
		this.tClass = tClass;
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
		PATHMsg clone = new PATHMsg(this.source,this.dest,this.PHB,this.tClass,this.tspec);
		clone.setPrevHops((ArrayList<Integer>)this.prevHops.clone());
		clone.setID(this.getID());
		return clone;
	}
	
}
