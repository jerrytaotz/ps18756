package NetworkElements;

import DataTypes.*;

import java.util.*;

public class LSRNIC {
	private LSR parent; // The router or computer that this nic is in
	private OtoOLink link; // The link connected to this nic
	private boolean trace = false; // should we print out debug statements?
	private ArrayList<Packet> inputBuffer = new ArrayList<Packet>(); // Where packets are put between the parent and nic
	private ArrayList<Packet> outputBuffer = new ArrayList<Packet>(); // Where packets are put to be sent
	private String id = null;
	private int linerate = 50;
	/**
	 * Default constructor for an ATM NIC
	 * @param parent
	 * @param id the initial ID for this NIC
	 * @since 1.0
	 */
	public LSRNIC(LSR parent,String id){
		this.parent = parent;
		this.parent.addNIC(this);
		this.id = id;
	}
	/**
	 * use this constructor if you don't want to specifiy an ID for this NIC
	 * @param parent
	 */
	public LSRNIC(LSR parent){
		this.parent = parent;
		this.parent.addNIC(this);
	}
	
	/**
	 * This method is called when a packet is passed to this nic to be sent. The packet is placed
	 * in an output buffer until a time unit passes
	 * @param currentPacket the packet to be sent (placed in the buffer)
	 * @param parent the router the packet came from
	 * @since 1.0
	 */
	public void sendPacket(Packet currentPacket, LSR parent){
		if(this.trace){
			System.out.println("Trace (LSR NIC): Received packet");
			if(this.link==null)
				System.out.println("Error (LSR NIC): You are trying to send a packet through a nic not connected to anything");
			if(this.parent!=parent)
				System.out.println("Error (LSR NIC): You are sending data through a nic that this router is not connected to");
			if(currentPacket==null)
				System.out.println("Warning (LSR NIC): You are sending a null packet");
		}
		
		
		parent.sendPacket(currentPacket);
		
	}
	
	/**
	 * This method connects a link to this nic
	 * @param link the link to connect to this nic
	 * @since 1.0
	 */
	public void connectOtoOLink(OtoOLink link){
		this.link = link;
	}
	
	/**
	 * This method is called when a packet is received over the link that this nic is connected to
	 * @param currentPacket the packet that was received
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket){
		this.inputBuffer.add(currentPacket);

	}
	
	/**
	 * Moves the packets from the output buffer to the line (then they get moved to the next nic's input buffer)
	 * @since 1.0
	 */
	public void sendPackets(){
		for(int i=0; i<this.outputBuffer.size(); i++)
			this.link.sendPacket(this.outputBuffer.get(i), this);
		
		this.outputBuffer.clear();
	}
	
	/**
	 * Moves packets from this nics input buffer to its output buffer
	 * @since 1.0
	 */
	public void receivePackets(){
		for(int i=0; i<this.inputBuffer.size(); i++)
			this.parent.receivePacket(this.inputBuffer.get(i), this);
		this.inputBuffer.clear();
	}
	
	/**
	 * Returns this nic's parent router
	 * @return the parent router for this NIC
	 */
	public LSR getParent(){
		return this.parent;
	}
	
	/**
	 * Returns the address of the router on the other end of the link.
	 * @return the address of the machine on the other end.
	 */
	public int getNeighborAddress(){
		return link.getOtherNIC(this).getParent().getAddress();
	}
	
	/**
	 * Return the available bandwidth for this NIC.  Since we are assuming infinite linerate
	 * for this project, we will always return the linerate.
	 * @return the linerate for this NIC
	 */
	public int getAvailableBW(){
		return this.linerate;
	}
	
	/**
	 * set the ID of this NIC.  Simulates a MAC address.
	 * @param id the new ID for this NIC
	 */
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * return the ID of this NIC
	 * @return the ID for this NIC
	 */
	public String getId(){
		return this.id;
	}
}
