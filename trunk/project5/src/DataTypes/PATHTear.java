package DataTypes;

public class PATHTear extends rsvpPacket {

	public PATHTear(int source, int dest) {
		super(source, dest);
		this.type = "PATHTEAR";
	}

}
