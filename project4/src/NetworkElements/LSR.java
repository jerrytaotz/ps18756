package NetworkElements;

import java.util.*;

import dijkstra.*;
import DataTypes.*;

public class LSR{
	private int address; // The AS address of this router
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	private PathCalculator pc;
	private HashMap<Integer, Integer> routingTable;
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
		pc = new PathCalculator();
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
		
	}
	
	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the distination router
	 * @param DSCP the differentiated services code point field
	 * @since 1.0
	 */
	public void createPacket(int destination, int DSCP){
		Packet newPacket= new Packet(this.getAddress(), destination, DSCP);
		this.sendPacket(newPacket);
	}

	/**
	 * This method allocates bandwidth for a specific traffic class
	 * @param Label MPLS label for the LSP
	 * @param PHB 0=EF, 1=AF, 2=BE
	 * @param Class AF classes 1,2,3,4.  (0 if EF or BE)
	 * @param Bandwidth number of packets per time unit for this PHB/Class
	 * @since 1.0
	 */
	public void allocateBandwidth(int Label, int PHB, int Class, int Bandwidth){
		
	}
	
	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		
		//This method should send the packet to the correct NIC.
		
	}

	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void sendPackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).sendPackets();
	}
	
	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void recievePackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).recievePackets();
	}
	
	/**
	 * Updates the routing tables in this router using
	 * Dijkstra's algorithm.
	 * @param map the network map to use.  This simulates receiving
	 * a link state message.
	 */
	public void updateRoutingTable(DirectedGraph map){
		pc.dijkstrasAlgorithm(map, this.address);
		this.routingTable = pc.findBestPaths(map, this.address);
	}
	
	/**
	 * Prints a quick summary of the routing table.
	 */
	public void printRoutingTable(){
		System.out.println("Routing table for Router " + this.address + "\n===");
		for(int i:this.routingTable.keySet()){
			System.out.println(i + ": " + routingTable.get(i));
		}
	}
}
