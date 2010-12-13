package DataTypes;

/**
 * An RSVP signaling packet.  This class should be extended to represent the
 * various RSVP messages.
 * @author Brady
 */
public class rsvpPacket extends Packet {
	
	/**
	 * Constructor for a PSC RSVP packet
	 * @param source
	 * @param dest
	 */
	public rsvpPacket(int source, int dest){
		super(source, dest);
		this.RSVP = true;
	}	

	/**
	 * Constructor for a PSC RSVP packet capable of being transmitted over LSC data channels.
	 * @param source
	 * @param dest
	 * @param ol the optical label which will be used to forward this message across the
	 * LSC data plane.
	 */
	public rsvpPacket(int source, int dest, OpticalLabel ol){
		super(source,dest,ol);
		this.RSVP = true;
	}
	
}
