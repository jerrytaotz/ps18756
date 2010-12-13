package NetworkElements;

import java.util.*;

import dijkstra.*;
import DataTypes.*;

public abstract class LSR{
	protected int address; // The AS address of this router
	protected ArrayList<LSRNIC> nics; // all of the nics in this router
	protected PathCalculator pc;
	protected LabelTable labelTable;
	protected ArrayList<Packet> waitingPackets; //where to queue up packets waiting for an LSP
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
		nics = new ArrayList<LSRNIC>();
		pc = new PathCalculator();
		labelTable = new LabelTable();
		waitingPackets = new ArrayList<Packet>();
	}
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address, boolean psc, boolean lsc ){
		this.address = address;
	}

	/**
	 * The return the router's address
	 * @since 1.0
	 */
	public int getAddress(){
		return this.address;
	}
	
	/**
	 * Adds a nic to this router
	 * @param nic the nic to be added
	 * @since 1.0
	 */
	public void addNIC(LSRNIC nic){
		this.nics.add(nic);
	}
	
	/**
	 * This method processes data and OAM cells that arrive from any nic with this router as a destination
	 * @param currentPacket the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	abstract public void receivePacket(Packet currentPacket, LSRNIC nic);
	
	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the destination router
	 * @since 1.0
	 */
	public void createPacket(int destination){
		Packet newPacket= new Packet(this.getAddress(), destination);
		this.sendPacket(newPacket);
	}

	/**
	 * This method is used to send packets across the data plane.  If an LSP does not exist at
	 * the time of transmission, one will be automatically created by sendPacket().  Until the
	 * RESV message has been received at this router, all packets will be queued.  Once the
	 * RESV message is received, all packets will be immediately transmitted.
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		LSRNIC forwardNIC;
		NICLabelPair newOutPair;
		Label outLabel;
		MPLS header;

		//This method should send the packet to the correct NIC.
		if(!labelTable.containsLSP(newPacket.getDest())){
			//NO LSP! Set one up on the fly.
			setupLSPOnTheFly(newPacket);
		}
		else if(labelTable.getOutPair(newPacket.getDest()).getLabel().isPending()){
			/*We are waiting on a RESV message for this LSP.  Just queue up until it's received*/
			waitingPackets.add(newPacket);
		}
		else{
			/*send the new packet*/
			forwardNIC = labelTable.getOutPair(newPacket.getDest()).getNIC();
			outLabel = labelTable.getOutPair(newPacket.getDest()).getLabel();
			header = new MPLS(outLabel,0,1);
			newPacket.addMPLSheader(header);
			forwardNIC.sendPacket(newPacket, this);
			//sentData(newPacket);
		}

	}

	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendKeepAlivePackets() {
		
		//This method should send the keep alive packets for routes for each the router is an inbound router
		
	}
	
	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void sendPackets(){
		sendKeepAlivePackets();
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).sendPackets();
	}
	
	/**
	 * Will either store or forward a data packet received on the data plane.
	 * @param p the data packet
	 * @param nic the LSRNIC this packet was received on.
	 */
	abstract protected void processDataPacket(Packet p,LSRNIC nic);
	
	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void receivePackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).receivePackets();
	}
	
	public void sendKeepAlive(int dest, OpticalLabel label){
			Packet p = new Packet(this.getAddress(), dest, label);
			p.setRSVP(true, "KeepAlive");
			this.sendPacket(p);
	}
	
	/**
	 * Print a notification 
	 * @param pathMsg
	 */
	protected void sentPATH(PATHMsg pathMsg) {
		traceMsg("Sent PATH message " + pathMsg.getId() + " to " 
				+ pathMsg.getDest());
	}
	
	/**
	 * Prints an error message and terminates application if a control packet was
	 * received for which there was no IP route.
	 * @param p the offending packet
	 */
	protected void noIPPath(Packet p){
		errorPrint("Error on PATH message (no IP route to " + p.getDest());
		System.exit(1);
	}
	
	/**
	 * Sets up a new LSP to the destination contained in 'newPacket'
	 * @param newPacket the packet for which we need a new LSP
	 */
	abstract protected void setupLSPOnTheFly(Packet newPacket);
	
	/**
	 * A method to test whether or not the IP routing tables are working correctly
	 * for both the data and control planes.
	 * @param dest the address of the router you would like to find the outgoing NIC for
	 * @param control true if you would like to find the control path NIC to dest
	 * @return the destination 
	 */
	abstract public LSRNIC getDestNICviaIP(int dest,boolean control);
	
	/**
	 * A method to print a customized error message including the concrete type of the 
	 * router and its address
	 * @param errorMsg
	 */
	abstract protected void errorPrint(String errorMsg);
	
	/**
	 * Prints a user supplied trace message containing the concrete type of the router.
	 * Should be used to print all log messages.
	 * @param msg the user supplied message
	 */
	abstract protected void traceMsg(String msg);
}
