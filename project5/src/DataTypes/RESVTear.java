package DataTypes;

public class RESVTear extends rsvpPacket {

	public RESVTear(int source, int dest) {
		super(source, dest);
		this.type = "RESVTEAR";
	}

}
