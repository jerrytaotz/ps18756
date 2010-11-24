package NetworkElements;

import java.util.*;

import dijkstra.*;
import DataTypes.*;

public class LSR{

	private int address; // The AS address of this router
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	private PathCalculator pc;
	private HashMap<Integer, LSRNIC> routingTable; //The IP routing table for this LSR
	private TreeMap<Integer, NICLabelPair> labelTable; // a map of input label to output nic and label
	private ArrayList<Packet> waitingPackets;

	//TODO: Write a smoother
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
		pc = new PathCalculator();
		labelTable = new TreeMap<Integer, NICLabelPair>();
		waitingPackets = new ArrayList<Packet>();
		this.routingTable = new HashMap<Integer,LSRNIC>();
	}

	/**
	 * Return this router's address
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
	 * This method processes data and RSVP cells that arrive from any nic
	 * @param currentPacket the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket, LSRNIC nic){
		if(currentPacket.isRSVP()){
			processRSVP(currentPacket);
		}
		//if destined for this router, hold on to it
		else if(currentPacket.getDest() == this.address){

		}
		//else forward it
		else{
			this.sendPacket(currentPacket);
		}
	}

	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the destination router
	 * @param DSCP the differentiated services code point field
	 * @since 1.0
	 */
	public void createPacket(int destination, int DSCP){
		Packet newPacket= new Packet(this.getAddress(), destination, DSCP);
		this.sendPacket(newPacket);
	}

	/**
	 * This method allocates bandwidth for a specific traffic class from the current router
	 * to the destination router
	 * @param dest destination router id
	 * @param PHB 0=EF, 1=AF, 2=BE
	 * @param Class AF classes 1,2,3,4. (0 if EF or BE)
	 * @param Bandwidth number of packets per time unit for this PHB/Class
	 * @since 1.0
	 */
	public void allocateBandwidth(int dest, int PHB, int Class, int Bandwidth){
		PATHMsg pathMsg = new PATHMsg(this.address,dest,PHB,Class);

		//Place this message in the output buffer
		pathMsg.addPrevHop(this.address);
		this.routingTable.get(dest).receivePacket(pathMsg);	
	}

	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		LSRNIC forwardNIC;
		NICLabelPair newOutPair;

		//This method should send the packet to the correct NIC.
		if(!labelTable.containsKey(newPacket.getDest())){
			//NO LSP! Set one up on the fly with BE service
			allocateBandwidth(newPacket.getDest(),Constants.PHB_BE,0,0);
			//LSP_PENDING signals that we should queue incoming packets until RESV is received 
			newOutPair = new NICLabelPair(routingTable.get(newPacket.getDest()),
					NICLabelPair.LSP_PENDING);
			labelTable.put(newPacket.getDest(), newOutPair);
			waitingPackets.add(newPacket);
		}
		else if(labelTable.get(newPacket.getDest()).getLabel() == NICLabelPair.LSP_PENDING){
			//We are waiting on a RESV message for this LSP.  Just queue up until it's received
			waitingPackets.add(newPacket);
		}
		else{
			//clear the waiting packets queue
			for(Packet p:waitingPackets){
				forwardNIC = labelTable.get(p.getDest()).getNIC();
				forwardNIC.sendPacket(p, this);
			}
			forwardNIC = labelTable.get(newPacket.getDest()).getNIC();
			forwardNIC.sendPacket(newPacket, this);
		}

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
		//path calculator only works on a symbolic level, doesn't know about NICs
		HashMap<Integer, Integer> symbolicRoutingTable; 
		pc.dijkstrasAlgorithm(map, this.address);
		symbolicRoutingTable = pc.findBestPaths(map, this.address);

		this.routingTable.clear();
		//Populate the actual routing table with the symbolic info.
		for(Integer dest:symbolicRoutingTable.keySet()){
			for(LSRNIC nic:nics){
				if(nic.getNeighborAddress() == symbolicRoutingTable.get(dest)){
					this.routingTable.put(dest, nic);
					break;
				}
			}
		}
	}

	/**
	 * determines which router to forward this request to and
	 * pushes the address of this router onto the message's previous hops.
	 * @param p
	 */
	private void processPATH(PATHMsg p){
		LSRNIC forwardNIC;

		if(p.getDest() == this.address){
			System.out.println("(Router " + this.address + "): terminated PATH msg "
					+ p.getID() + " from router " + p.getSource());
		}
		else{ 
			receivedPATH(p); //print a message
			p.addPrevHop(this.address);
			forwardNIC = routingTable.get(p.getDest());
			this.sentPATH(p);
			forwardNIC.sendPacket(p, this);
		}
	}

	/**
	 * Determines what type of RSVP packet p is and hands control 
	 * off to the appropriate processing method.
	 * @param p the rsvp packet to be handled.
	 */
	private void processRSVP(Packet p){
		String pType = p.getType();
		if(pType.compareTo("PATH") == 0){
			processPATH((PATHMsg)p);
		}

	}

	/**
	 * Prints when a router receives an RSVP packet
	 */
	private void receivedRSVP(rsvpPacket p){
		System.out.println("(Router " + this.address +"): received an RSVP packet: " 
				+ p.getID());
	}

	/**
	 * prints when a router receives a PATH msg.
	 */
	private void receivedPATH(PATHMsg p){
		System.out.println("(Router " + this.address +"): received PATH message " 
				+ p.getID() + " from " + p.getSource());
	}

	private void sentPATH(PATHMsg p){
		System.out.println("(Router " + this.address +"): sent a PATH message: " 
				+ p.getID());
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
