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
	/*the list of all the available lambdas*/
	private ArrayList<OpticalLabel> availableLambdas;

	public LscLSR(int address) {
		super(address);
		this.LSCDataLinks = new ArrayList<LSRNIC>();
		this.LSCControlLinks = new ArrayList<LSRNIC>();
		this.dataRoutingTable = new HashMap<Integer, LSRNIC>();
		this.controlRoutingTable = new HashMap<Integer, LSRNIC>();
		availableLambdas = new ArrayList<OpticalLabel>(Arrays.asList(OpticalLabel.values()));
		availableLambdas.remove(OpticalLabel.NA);
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
			this.processLSCRSVP((rsvpPacket)p, nic);
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
	public void processLSCRSVP(rsvpPacket p,LSRNIC nic){
		if(p.getType().compareTo("PATH") == 0){
			receivedPATH((PATHMsg)p,nic);
		}
		if(p.getType().equals("RESV")){
			receivedRESV((RESVMsg)p,nic);
		}
		//TODO fill in the details for the other message types
	}

	/**
	 * Processes a RESV message.
	 * @param p the RESVmsg
	 * @param nic the nic 'p' was received on
	 */
	private void receivedRESV(RESVMsg p, LSRNIC nic) {
		Label dl = p.getDL();
		NICLabelPair downPair;
		LSRNIC upNIC; //The NIC to use to get one more hop upstream
		
		traceMsg("Received LSC RESV message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest() + " DL:" + p.getDL());
		
		/*verify that the DL is still available.*/
		if(getOpticalLabel(dl.getOptVal()) == false){
			errorPrint("RESV message (DL Lambda no longer available)");
			//TODO have these errors generate RESVERR
			System.exit(1);
		}
		
		/*confirm the DL*/
		downPair = labelTable.getWithInLabel(dl);
		downPair.getLabel().setIsPending(false);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + dl +
				"\\" + p.getSource() + "\\" + downPair.getLabel());
		
		/*forward the RESV message*/
		upNIC = getDestNICviaIP(p.getDest(),true);
		upNIC.sendPacket(p.Clone(), this);
		sentRESV(p);
	}

	/**
	 * Processes a PATH message.
	 * @param p the packet containing the PATH message
	 * @param nic the nic p was received on
	 */
	private void receivedPATH(PATHMsg p, LSRNIC nic) {
		Label upstreamIn, downstreamIn, downstreamOut;
		NICLabelPair upPair, downPair;
		LSRNIC forwardNIC = controlRoutingTable.get(p.getDest());
		LSRNIC dataForwardNIC  = dataRoutingTable.get(p.getDest());
		PATHMsg pathMsg;
		boolean lambdaAvailable = false;
		
		traceMsg("Received LSC PATH message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest() + " UL:" + p.getUL() + " SL:" + p.getSL());
		
		if(forwardNIC == null || dataForwardNIC == null){
			noIPPath(p);
		}
		
		/*check whether our lambdas are available*/
		lambdaAvailable = isLambdaAvailable(p.getSL().getOptVal());
		if(!lambdaAvailable){
			errorPrint("PATH message (SL Lambda not available)");
			//TODO have these errors generate PATHERR
			System.exit(1);
		}
		/*using getOpticalLabel because the UL is confirmed*/
		lambdaAvailable = getOpticalLabel(p.getUL().getOptVal());
		if(!lambdaAvailable){
			errorPrint("Error on PATH message (UL Lambda not available)");
			System.exit(1);
		}
		
		/*Add the upstream entry to the table*/
		upstreamIn = new Label(p.getUL().getOptVal());
		upPair = new NICLabelPair(nic, p.getUL().clone());
		labelTable.put(p.getDest(),p.getSource(),upstreamIn,upPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + upstreamIn +
				"\\" + p.getSource() + "\\" + upPair.getLabel());
		
		/*Add the downstream entry to the table*/
		downstreamIn = new Label(p.getSL().getOptVal());
		downstreamIn.setIsPending(true);
		downstreamOut = downstreamIn.clone();
		downPair = new NICLabelPair(dataForwardNIC,downstreamOut);
		labelTable.put(p.getSource(), p.getDest(), downstreamIn, downPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + downstreamIn +
				"\\" + p.getDest() + "\\" + downPair.getLabel());
		
		/*Forward the PATH message to the appropriate output buffer*/
		pathMsg = new PATHMsg(p.getSource(),p.getDest(),
				upstreamIn.clone(),downPair.getLabel().clone());
		pathMsg.setPrevHops((ArrayList<Integer>)p.getPrevHops().clone());
		pathMsg.addPrevHop(this.address);
		pathMsg.setId(p.getId());
		sentPATH(pathMsg);
		forwardNIC.sendPacket(pathMsg,this);
	}

	/**
	 * The side effect free version of getOpticalLabel.  Simply determines whether 
	 * a wavelength is available or not.
	 * @param ol
	 * @return
	 */
	private boolean isLambdaAvailable(OpticalLabel ol){
		if(this.availableLambdas.contains(ol)){
			return true;
		}
		return false;
	}
	
	/**
	 * Query the list of available wavelengths to determine whether or not the wavelength
	 * specified by 'ol' is available for use or not.  
	 * SIDE EFFECT: If 'ol' IS in the list, it will be removed.
	 * @param ol the optical label specifying the desired wavelength
	 * @return true if the wavelength was available for use
	 * false otherwise
	 */
	private boolean getOpticalLabel(OpticalLabel ol){
		if(availableLambdas.contains(ol)){
			availableLambdas.remove(ol);
			return true;
		}
		else return false;
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
	
	/**
	 * Print a confirmation message that a RESV message was sent.
	 * @param resv the RESV message itself
	 */
	private void sentRESV(RESVMsg resv) {
		traceMsg("Sent RESV message " + resv.getId() + " from:" + resv.getSource() +
				"to:"+ resv.getDest() + " DL:" + resv.getDL());
	}

	/**
	 * Will forward a data packet received on the data plane.
	 * @param p the data packet
	 * @param nic the LSRNIC this packet was received on.
	 */
	public void processDataPacket(Packet p,LSRNIC nic){
		Label pLabel = new Label(p.getOpticalLabel());//wrap the optical label for table lookup
		NICLabelPair forwardPair;
		LSRNIC forwardNIC;
		
		traceMsg("Received DATA packet " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest());
		/*check to see if the LSP has been set up yet*/
		forwardPair = labelTable.getWithInLabel(pLabel);
		if(forwardPair != null){
			/*LSP exists.  Forward the packet*/
			forwardNIC = forwardPair.getNIC();
			forwardNIC.sendPacket(p, this);
			traceMsg("Sent DATA packet " + p.getId() + " from:" + p.getSource() + 
					" to:" + p.getDest());
		}
		else{
			errorPrint("Received a packet for a non existing LSP");
			System.exit(1);
		}
	}
	
}
