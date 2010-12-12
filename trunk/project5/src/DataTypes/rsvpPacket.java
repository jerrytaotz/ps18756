package DataTypes;

/**
 * An RSVP signaling packet.
 * @author Brady
 */
public class rsvpPacket extends Packet {
	
	public rsvpPacket(int source, int dest){
		super(source, dest);
		this.RSVP = true;
	}	

}
