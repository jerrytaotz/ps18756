package DataTypes;

import java.util.*;

public class PATHMsg extends rsvpPacket {
	int tspec, PHB, tClass;	
	ArrayList<Integer> prevHops; //So RESV messages come back on the same route
	
	public PATHMsg(int source, int dest, int PHB, int tClass){
		super(source,dest);
		this.type = "PATH";
		this.prevHops = new ArrayList<Integer>();
		this.PHB = PHB;
		this.tClass = tClass;
	}
		
	/**
	 * Add the next prevHop to this message.
	 * @param prevHop
	 */
	public void addPrevHop(int prevHop){
		this.prevHops.add(0,prevHop);
	}
	
	/**
	 * return the hops used by this message
	 * @return the hops taken by this message so far.
	 */
	public ArrayList<Integer> getPrevHops(){
		return this.getPrevHops();
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

	public void setPrevHops(ArrayList<Integer> prevHops) {
		this.prevHops = prevHops;
	}

}
