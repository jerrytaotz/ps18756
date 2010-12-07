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
	 * This method processes data and OAM cells that arrive from any nic with this router as a destination
	 * @param currentPacket the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket, LSRNIC nic){
		if(LSCControlLinks.contains(nic)){
			this.processLSCControl(currentPacket, nic);
		}
		else if(LSCDataLinks.contains(nic)){
			this.processLSCData(currentPacket,nic);
		}
		else if(PSCLinks.contains(nic)){
			this.processPSCPacket(currentPacket, nic);
		}
		else{
			errorPrint("Detected a nic which is not classified as PSC,LSCData, or LSCControl");
			System.exit(0);
		}
	}
	
	/**
	 * Process a packet which was received on one of the PSC links.
	 * @param currentPacket the received packet
	 * @param nic the nic the packet was received on
	 */
	public void processPSCPacket(Packet currentPacket,LSRNIC nic){
		System.out.println("PSC packet: " + currentPacket.getSource() + ", " + currentPacket.getDest());
		System.out.println("\tOAM: " + currentPacket.isOAM());
		if (currentPacket.isOAM())
		{
			System.out.println("\tOAM: " + currentPacket.getOAMMsg() + ", " + currentPacket.getOpticalLabel().toString());
		}
	}
	
	public void processLSCData(Packet currentPacket,LSRNIC nic){
		System.out.println("LSC Data packet: " + currentPacket.getSource() + ", " + currentPacket.getDest());
		System.out.println("\tOAM: " + currentPacket.isOAM());
		if (currentPacket.isOAM())
		{
			System.out.println("\tOAM: " + currentPacket.getOAMMsg() + ", " + currentPacket.getOpticalLabel().toString());
		}
	}
	public void processLSCControl(Packet currentPacket,LSRNIC nic){
		System.out.println("LSC Control packet: " + currentPacket.getSource() + ", " + currentPacket.getDest());
		System.out.println("\tOAM: " + currentPacket.isOAM());
		if (currentPacket.isOAM())
		{
			System.out.println("\tOAM: " + currentPacket.getOAMMsg() + ", " + currentPacket.getOpticalLabel().toString());
		}
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
	 * Print an error message containing the type of this LSR and it's address.
	 * @param errorMsg the error message you would like printed.
	 */
	@Override
	public void errorPrint(String errorMsg){
		System.out.println("(PSC/LSC Router " + this.getAddress() + "): ERROR " + errorMsg);
	}

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
	
}