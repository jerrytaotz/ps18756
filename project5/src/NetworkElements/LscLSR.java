package NetworkElements;
/**
 * This is a class which represents an LSC capable OXC in GMPLS.
 * 
 * @author Brady Tello
 */
import java.util.*;

import dijkstra.DirectedGraph;
import DataTypes.*;

public class LscLSR extends LSR {
	
	private ArrayList<LSRNIC> LSCDataLinks;
	private ArrayList<LSRNIC> LSCControlLinks;
	
	private HashMap<Integer,LSRNIC> dataRoutingTable;
	private HashMap<Integer,LSRNIC> controlRoutingTable;

	public LscLSR(int address) {
		super(address);
		this.LSCDataLinks = new ArrayList<LSRNIC>();
		this.LSCControlLinks = new ArrayList<LSRNIC>();
		this.dataRoutingTable = new HashMap<Integer, LSRNIC>();
		this.controlRoutingTable = new HashMap<Integer, LSRNIC>();
	}
	
	/**
	 * Adds the link 'data' to the set of LSC data links and the link 'control to the set of
	 * LSC control links.
	 * @param data
	 * @param control
	 */
	public void setDataAndControlLinkPair(LSRNIC data, LSRNIC control){
		this.LSCDataLinks.add(data);
		this.LSCControlLinks.add(control);
	}
	
	/**
	 * This method processes data and OAM cells that arrive from any nic with this router as a destination
	 * @param p the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 * @override
	 */
	public void receivePacket(Packet p, LSRNIC nic){
		if(LSCControlLinks.contains(nic)){
			this.processLSCControl(p, nic);
		}
		else if(LSCDataLinks.contains(nic)){
			this.processDataPacket(p,nic);
		}
		else{
			errorPrint("Detected a nic which is not classified as LSCData, or LSCControl");
			System.exit(0);
		}
	}
	
	/**
	 * Parse a message received on a control link to determine what type of control message it
	 * was.  Passes control to the appropriate handler method.
	 * @param currentPacket the received packet
	 * @param nic the nic the packet was received on.
	 */
	public void processLSCControl(Packet p,LSRNIC nic){
		if(p.getRSVPMsg().compareTo("PATH") == 0){
			receivedPATH(p,nic);
		}
		//TODO fill in the details for the other message types
	}

	/**
	 * Processes a PATH message.
	 * @param p the packet containing the PATH message
	 * @param nic the nic p was received from
	 */
	private void receivedPATH(Packet p, LSRNIC nic) {
		traceMsg("Received PATH message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest());
		//TODO implement this method.
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
					if(LSCControlLinks.contains(nic) &&
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
		System.out.println("(LSC Router " + this.getAddress() + "): ERROR " + errorMsg);
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

	/**
	 * prints a user supplied trace message with the format (LSC Router <this.address>): <msg>
	 * @param msg the message to be printed
	 */
	@Override
	protected void traceMsg(String msg) {
		System.out.println("(LSC Router " + this.address + "): " +msg);
		
	}

	@Override
	protected void setupLSPOnTheFly(Packet newPacket) {
		// TODO Auto-generated method stub
		
	}
}
