package DataTypes;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Packet {
	protected int source, dest, DSCP; // The source and destination addresses
	protected boolean RSVP = false;
	protected String type; //used to classify the various types of packets
	private String RSVPMsg = null; 
	private int id;
	private OpticalLabel opticalLabel = OpticalLabel.NA;
	private Queue<MPLS> MPLSheader = new LinkedList<MPLS>(); // all of the MPLS headers in this router
	/**
	 * The default constructor for a packet
	 * @param source the source ip address of this packet
	 * @param dest the destination ip address of this packet
	 * @since 1.0
	 */
	public Packet(int source, int dest){
		try{
			this.source = source;
			this.dest = dest;
			Random rand = new Random();
			id = rand.nextInt(1000);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * The default constructor for a packet
	 * @param source the source ip address of this packet
	 * @param dest the destination ip address of this packet
	 * @since 1.0
	 */
	public Packet(int source, int dest, OpticalLabel label){
		try{
			this.source = source;
			this.dest = dest;
			this.opticalLabel = label;
			Random rand = new Random();
			id = rand.nextInt(1000);
		}
		catch(Exception e){
			e.printStackTrace();
		}
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

	public OpticalLabel getOpticalLabel() {
		return opticalLabel;
	}

	public boolean isRSVP() {
		return RSVP;
	}

	public void setRSVP(boolean rsvp, String msg) {
		this.RSVP = rsvp;
		RSVPMsg = msg;
	}

	public String getRSVPMsg() {
		return RSVPMsg;
	}
	
	/**
	 * Get the value stored in this packet's ID field.
	 * @return this packet's id
	 */
	public int getId(){
		return this.id;
	}
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * set this packet's ID field
	 * @param id the new ID
	 */
	public void setId(int id){
		this.id = id;
	}

}
	
