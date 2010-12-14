package DataTypes;
/**
 * An RSVP PATH message.
 * @author Brady Tello
 */
import java.util.*;

public class PATHMsg extends rsvpPacket {	
	ArrayList<Integer> prevHops; //So RESV messages come back on the same route
	Label upstreamLabel, suggestedLabel;
	
	/**
	 * constructor for an LSC PATH message
	 * @param source the address of the original sender (NOT the prev. hop)
	 * @param dest the address of the ultimate receiver (NOT the next hop)
	 * @param ul the upstream label to use to connect to the transmitting node
	 * @param sl the label which the transmitting node would like to use to connect to its
	 * next hop
	 */
	public PATHMsg(int source, int dest, Label ul, Label sl){
		super(source,dest);
		this.type = "PATH";
		this.prevHops = new ArrayList<Integer>();
		this.upstreamLabel = ul;
		this.suggestedLabel = sl;
	}
	
	/**
	 * constructor for a PSC PATH message
	 * @param source the address of the original sender (NOT the prev. hop)
	 * @param dest the address of the ultimate receiver (NOT the next hop)
	 * @param ul the upstream label to use to connect to the transmitting node
	 */
	public PATHMsg(int source, int dest, Label ul){
		super(source,dest);
		this.type = "PATH";
		this.prevHops = new ArrayList<Integer>();
		this.upstreamLabel = ul;
		this.suggestedLabel = null;
	}
	
	/**
	 * Default constructor for a PATH message capable of being forwarded across the LSC data plane.
	 * @param source the address of the original sender (NOT the prev. hop)
	 * @param dest the address of the ultimate receiver (NOT the next hop)
	 * @param ol the optical label which will be used to forward this message across the LSC network
	 * @param ul the upstream label to be used on the new LSP
	 */
	public PATHMsg(int source, int dest,OpticalLabel ol, Label ul){
		super(source,dest,ol);
		this.type = "PATH";
		this.prevHops = new ArrayList<Integer>();
		this.upstreamLabel = ul;
		this.suggestedLabel = null;
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

	public Label getUL(){
		return upstreamLabel;
	}
	
	public Label getSL(){
		return suggestedLabel;
	}
	
	public PATHMsg Clone(){
		PATHMsg clone = new PATHMsg(this.source,this.dest,
				upstreamLabel.clone(),suggestedLabel.clone());
		clone.setPrevHops((ArrayList<Integer>)this.prevHops.clone());
		clone.setId(this.getId());
		return clone;
	}
	
}
