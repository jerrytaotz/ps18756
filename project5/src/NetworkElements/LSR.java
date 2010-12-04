package NetworkElements;

import java.util.*;
import DataTypes.*;

public class LSR{
	private int address; // The AS address of this router
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
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
	public void receivePacket(Packet currentPacket, LSRNIC nic){
		System.out.println("packet: " + currentPacket.getSource() + ", " + currentPacket.getDest());
		System.out.println("\tOAM: " + currentPacket.isOAM());
		if (currentPacket.isOAM())
		{
			System.out.println("\tOAM: " + currentPacket.getOAMMsg() + ", " + currentPacket.getOpticalLabel().toString());
		}
	}
	
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
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		
		//This method should send the packet to the correct NIC (and wavelength if LSC router).

		
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
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void receivePackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).receivePackets();
	}
	
	public void sendKeepAlive(int dest, OpticalLabel label){
			Packet p = new Packet(this.getAddress(), dest, label);
			p.setOAM(true, "KeepAlive");
			this.sendPacket(p);
	}
}
