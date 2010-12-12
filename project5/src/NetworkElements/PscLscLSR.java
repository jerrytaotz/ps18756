package NetworkElements;
import java.util.*;

import dijkstra.DirectedGraph;
import DataTypes.*;

/**
 * This is a class which represents an LSR which is both PSC and LSC capable.
 * @author Brady
 *
 */

public class PscLscLSR extends LSR {

	/*maintains pointers to all the PSC only links.*/
	private ArrayList<LSRNIC> PSCLinks; 
	/*maintains pointers to all of the LSC data links.*/
	private ArrayList<LSRNIC> LSCDataLinks; /*keep track of which
	/*maintains pointers to all of the LSC control links.*/
	private ArrayList<LSRNIC> LSCControlLinks;
	
	/*The "IP" routing tables for the data and control plane.*/
	private HashMap<Integer,LSRNIC> dataRoutingTable;
	private HashMap<Integer,LSRNIC> controlRoutingTable;
	
	/**
	 * Default constructor.
	 * @param address
	 */
	public PscLscLSR(int address) {
		super(address);
		this.LSCControlLinks = new ArrayList<LSRNIC>();
		this.LSCDataLinks = new ArrayList<LSRNIC>();
		this.PSCLinks = new ArrayList<LSRNIC>();
		this.dataRoutingTable = new HashMap<Integer, LSRNIC>();
		this.controlRoutingTable = new HashMap<Integer, LSRNIC>();
	}
	
	/**
	 * Adds the NIC 'n1' to the set of PSC capable links
	 * @param pscNIC NIC to be tracked as a PSC NIC
	 */
	public void setPSCLink(LSRNIC pscNIC){
		this.PSCLinks.add(pscNIC);
	}
	
	/**
	 * Adds the link 'data' to the set of LSC data links and the link 'control to the set of
	 * LSC control links.
	 * @param data
	 * @param control
	 */
	public void setDataAndControlLinkPair(LSRNIC data,LSRNIC control){
		this.LSCDataLinks.add(data);
		this.LSCControlLinks.add(control);
	}
	
	/**
	 * This method processes data and OAM cells that arrive from any nic with this router as a 
	 * destination.  
	 * @param p the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @remarks It is the responsibility of the programmer to ensure that any messages
	 * which are transmitted to a PscLscLSR are in the appropriate format.  For example,
	 * if a node will be sending a packet to a PSC interface, it should not have an 
	 * optical Label.  Or, if an adjacent LSC router is forwarding a PSC data packet
	 * to this router, it must not forward it to the control plane link but rather to the
	 * data plane link.  Packets received on the control plane link will be assumed to be in
	 * LSC format.
	 * @since 1.0
	 */
	public void receivePacket(Packet p, LSRNIC nic){
		if(LSCControlLinks.contains(nic)){
			this.processLSCRSVP((rsvpPacket)p, nic);
		}
		else if(LSCDataLinks.contains(nic)){
			this.processDataPacket(p,nic);
		}
		else if(PSCLinks.contains(nic)){
			this.processPSCPacket(p, nic);
		}
		else{
			errorPrint("Detected a nic which is not classified as PSC,LSCData, or LSCControl");
			System.exit(0);
		}
	}
	
	/*
	 * =============================================================================
	 * PSC methods
	 * =============================================================================
	 */
	
	/**
	 * Process a packet which was received on one of the PSC links.
	 * @param currentPacket the received packet
	 * @param nic the nic the packet was received on
	 */
	private void processPSCPacket(Packet p,LSRNIC nic){
		if(p.isRSVP()){
			processPSCRSVP((rsvpPacket)p,nic);
		}
		else{
			processDataPacket(p, nic);
		}
	}
	
	/**
	 * This method will parse a PSC RSVP method to determine the type of RSVP message and
	 * pass control to the appropriate handler.
	 * @param p the packet which was received
	 * @param nic the nic on which this packet was received.
	 */
	private void processPSCRSVP(rsvpPacket p, LSRNIC nic) {
		if(p.getType().compareTo("PATH") == 0){
			receivedPSCPATH((PATHMsg)p,nic);
		}
		//TODO add cases for the other messages.
	}

	/**
	 * Processes a PSC PATH message.
	 * @param p
	 * @param nic
	 */
	private void receivedPSCPATH(PATHMsg p, LSRNIC nic){
		traceMsg("Received PSC PATH message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest());
	}
	/*
	 * ==========================================================================
	 * LSC methods
	 * ==========================================================================
	 */
	
	/**
	 * Determines what type of control message was received and forwards it to the appropriate
	 * handler.
	 */
	private void processLSCRSVP(rsvpPacket p,LSRNIC nic){
		if(p.getRSVPMsg().compareTo("PATH") == 0){
			receivedLSCPATH((PATHMsg)p,nic);
		}
		//TODO add cases for the other messages
	}
	
	private void receivedLSCPATH(PATHMsg p, LSRNIC nic) {
		traceMsg("Received LSC PATH message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest());
		//TODO implement this method
	}

	/**
	 * Updates the routing tables in this router using
	 * Dijkstra's algorithm.
	 * @param dataMap the graph to use in creating a routing table for the data plane
	 * @param controlMap the graph to use in creating a routing table for the control plane
	 */
	public void updateRoutingTable(DirectedGraph dataMap,DirectedGraph controlMap){
		updateRoutingTable(dataMap,this.dataRoutingTable);
		updateRoutingTable(controlMap,this.controlRoutingTable);
	}
	
	/**
	 * A helper method for the updateRoutingTable function which updates the given routing 
	 * table 'rt' with the data contained in the 'map'.
	 * @param map the map to be used
	 * @param rt the routing table you want to update (should only be one of this.dataRoutingTable
	 * or this.controlRoutingTable.
	 */
	private void updateRoutingTable(DirectedGraph map,HashMap<Integer,LSRNIC> rt){
		//path calculator only works on a symbolic level, doesn't know about NICs
		HashMap<Integer, Integer> symbolicRoutingTable;
		
		if(rt == null || map == null){
			String badArg = (map == null) ? "map" : "rt";
			errorPrint("Passed a null " + badArg + " argument to updateRoutingTable(map,rt)");
		}
		
		pc.dijkstrasAlgorithm(map, this.address);
		symbolicRoutingTable = pc.findBestPaths(map, this.address);

		rt.clear();
		//Populate the actual routing table with the symbolic info.
		for(Integer dest:symbolicRoutingTable.keySet()){
			//check whether this update is for the data or the control set
			if(rt == this.dataRoutingTable){
				for(LSRNIC nic:nics){
					//if the NIC is not in the LSC control set AND it is the next hop link
					if(!LSCControlLinks.contains(nic) &&
							nic.getNeighborAddress() == symbolicRoutingTable.get(dest)){
						this.dataRoutingTable.put(dest, nic);
						break;
					}
				}
			}
			else if(rt == this.controlRoutingTable){
				for(LSRNIC nic:nics){
					//if the NIC is in the control set AND it is the next hop link 
					if(!LSCDataLinks.contains(nic) &&
							nic.getNeighborAddress() == symbolicRoutingTable.get(dest)){
						this.controlRoutingTable.put(dest, nic);
						break;
					}
				}
			}
			else{
				errorPrint("updateRoutingTable was given an invalid routing table.");
				System.exit(0);
			}
		}
	}
	
	/**
	 * prints a user supplied trace message with the format (PSC+LSC Router <this.address>): <msg>
	 * @param msg the message to be printed
	 */
	protected void traceMsg(String msg){
		System.out.println("(LSC+PSC Router " + this.address + "): " +msg);
	}
	
	/**
	 * Print an error message containing the type of this LSR and it's address.
	 * @param errorMsg the error message you would like printed.
	 */
	@Override
	public void errorPrint(String errorMsg){
		traceMsg("ERROR " + errorMsg);
	}

	/**
	 * This method will be used to determine the outgoing NIC for a destination node
	 * using it's destination address rather than an MPLS label.  This method should be 
	 * used to forward messages on the control plane since labels will never be set up
	 * for control paths.  It should also be used to determine the outgoing NIC for
	 * label forwarding before the labelTable entry is created.
	 * @param dest the destination node
	 * @control true if you would like to find the control route to the destination
	 * false if you would like to find the data route to the destination
	 */
	@Override
	public LSRNIC getDestNICviaIP(int dest, boolean control) {
		if(control){
			if(!controlRoutingTable.containsKey(dest)){
				errorPrint("This router does not contain a control path to " + dest);
				System.exit(0);
			}
			return controlRoutingTable.get(dest);
		}
		else{
			if(!dataRoutingTable.containsKey(dest)){
				errorPrint("This router does not contain a data path to " + dest);
				System.exit(0);
			}
			return dataRoutingTable.get(dest);
		}
	}

	@Override
	protected void setupLSPOnTheFly(Packet newPacket) {
		// TODO Auto-generated method stub
		
	}
	
}
