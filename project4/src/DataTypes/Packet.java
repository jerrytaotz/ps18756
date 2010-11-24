package DataTypes;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Packet {
	protected int source; // The source and destination addresses
	protected int dest, DSCP;
	private int id;
	protected boolean RSVP = false; //is this an RSVP packet?
	private Queue<MPLS> MPLSheader = new LinkedList<MPLS>(); // all of the MPLS headers in this router
	protected String type;
	private Random rand;
	
	/**
	 * The default constructor for a packet
	 * @param source the source ip address of this packet
	 * @param dest the destination ip address of this packet
	 * @param DSCP Differential Services Code Point
	 * @since 1.0
	 */
	public Packet(int source, int dest, int DSCP){
		try{
			this.source = source;
			this.dest = dest;
			this.DSCP = DSCP;
			rand = new Random();
			id = rand.nextInt(100);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public String getType(){
		return type;
	}
	
	/**
	 * Return whether or not this packet is an RSVP packet
	 */
	public boolean isRSVP(){
		return RSVP;
	}
	
	/**
	 * Adds an MPLS header to a packet
	 * @since 1.0
	 */
	public void addMPLSheader(MPLS header){
		MPLSheader.add(header);
	}
	
	/**
	 * Pops an MPLS header from the packet
	 * @since 1.0
	 */
	public MPLS popMPLSheader(){
		return MPLSheader.poll();
	}
	
	/**
	 * Returns the source ip address of this packet
	 * @return the source ip address of this packet
	 * @since 1.0
	 */
	public int getSource(){
		return this.source;
	}
	
	/**
	 * Returns the destination ip address of this packet
	 * @return the destination ip address of this packet
	 * @since 1.0
	 */
	public int getDest(){
		return this.dest;
	}

	/**
	 * Set the DSCP field
	 * @param DSCP the DSCP field value
	 * @since 1.0
	 */
	public void setDSCP(int dSCP) {
		this.DSCP = dSCP;
	}

	/**
	 * Returns the DSCP field
	 * @return the DSCP field
	 * @since 1.0
	 */
	public int getDSCP() {
		return this.DSCP;
	}
	
	/**
	 * returns the packet ID associated with this packet.
	 * @return
	 */
	public int getID(){
		return this.id;
	}
	
	/**
	 * Set the ID of this packet.
	 * @param newID the new ID of this packet
	 */
	public void setID(int newID){
		this.id = newID;
	}
	
	/**
	 * Classifies DSCPs as PHBs.  Useful for placing things in the right queue.
	 * If the current DSCP is not a defined DSCP, this returns -1.
	 * @return the PHB corresponding to the DSCP of this packet. -1 if the current 
	 * DSCP is not valid.
	 */
	public int classifyDSCP(){
		if(this.DSCP == 46) return Constants.PHB_EF;
		else if(this.DSCP >= 11 && this.DSCP <= 43){
			return Constants.PHB_AF;
		}
		else if(this.DSCP == 0) return Constants.PHB_BE;
		else return -1;
	}
	
	/**
	 * Get the AF class of this packet.  If this packet is not an AF
	 * stream, this method will return -1.  
	 * TODO: this is sloppy.  Come back and see if you can do some bit
	 * manipulations if you have time.
	 * @return the AF class if this an AF packet.  If not, return -1.
	 */
	public int getAFClass(){
		switch(this.DSCP){
		case Constants.DSCP_AF11:
			return 1;
		case Constants.DSCP_AF21:
			return 2;
		case Constants.DSCP_AF31:
			return 3;
		case Constants.DSCP_AF41:
			return 4;
		case Constants.DSCP_AF12:
			return 1;
		case Constants.DSCP_AF22:
			return 2;
		case Constants.DSCP_AF32:
			return 3;
		case Constants.DSCP_AF42:
			return 4;
		case Constants.DSCP_AF13:
			return 1;
		case Constants.DSCP_AF23:
			return 2;
		case Constants.DSCP_AF33:
			return 3;
		case Constants.DSCP_AF43:
			return 4;
		default:
			return -1;
		}
	}
}
	
