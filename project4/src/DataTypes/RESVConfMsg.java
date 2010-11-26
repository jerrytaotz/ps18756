package DataTypes;
/**
 * An RSVP RESVConf message
 * @author Brady
 *
 */

public class RESVConfMsg extends rsvpPacket {

	public RESVConfMsg(int source, int dest) {
		super(source, dest);
		this.type = "RESVCONF";
	}
}
