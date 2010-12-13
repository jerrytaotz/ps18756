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
	/*the list of all the available lambdas*/
	private ArrayList<OpticalLabel> availableLambdas;
	
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
		availableLambdas = new ArrayList<OpticalLabel>(Arrays.asList(OpticalLabel.values()));
		availableLambdas.remove(OpticalLabel.NA);
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
			processPSCPATH((PATHMsg)p,nic);
		}
		//TODO add cases for the other messages.
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
	 * calculates the next available integer input label.  Uses a linear scan over the values 
	 * in the labelTable.
	 * @return the input label if one can be found.
	 * -1 if no input label could be found
	 */
	protected int calcIntInLabel(){
		//find the next available input label
		for(int i = 1;i < Integer.MAX_VALUE;i++){
			if(!labelTable.containsIntInLabel(i)){
				return i;
			}
		}
		return -1;
	}

	/**
	 * calculates the next available input label.  Uses a linear scan over the values in the labelTable.
	 * @return the output label if one can be found.
	 * -1 otherwise  
	 */
	protected int calcIntOutLabel(LSRNIC nic){
		/*Get all of the labels associated with the 'nic'*/
		ArrayList<Label> outLabels = labelTable.getOutLabels(nic);
		ArrayList<Integer> intOutLabels = new ArrayList<Integer>();
		
		for(Label l:outLabels){
			intOutLabels.add(l.getIntVal());
		}
		//find the next available input label
		for(int i = 1;i < Integer.MAX_VALUE;i++){
			if(!intOutLabels.contains(i)){
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Processes a PSC PATH message.
	 * @param p
	 * @param nic
	 */
	private void processPSCPATH(PATHMsg p, LSRNIC nic){
		Label upstreamIn, downStreamIn, downstreamOut;
		OpticalLabel downOutLambda, upInLambda;
		NICLabelPair upPair, downPair;
		LSRNIC forwardNIC = controlRoutingTable.get(p.getDest());
		LSRNIC dataForwardNIC = dataRoutingTable.get(p.getDest());
		PATHMsg pathMsg;
		
		traceMsg("Received PSC PATH message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest() + " UL: " + p.getUL() + " SL: " + p.getSL());
				
		if(forwardNIC == null || dataForwardNIC == null){
			noIPPath(p);
		}
		
		/*calculate the suggested upstream optical label*/
		upInLambda = getNextOptLabel();
		getOpticalLabel(upInLambda); //Remove the upstream lambda from the list since it's confirmed
		if(upInLambda == null){
			//TODO send a PATHErr, tear down everything we've done (delete entries, release lambdas)
			errorPrint("Upstream LSP setup failed (no available wavelength)");
			System.exit(1);
		}
		upstreamIn = new Label(upInLambda);
		
		/*calculate the suggested downstream optical label*/
		downOutLambda = getNextOptLabel();
		if(downOutLambda == null){
			//TODO send a PATHErr
			errorPrint("Downstream LSP setup failed (no available wavelength)");
			System.exit(1);
		}
		downstreamOut = new Label(downOutLambda);
		downstreamOut.setIsPending(true);
		
		/*add the upstream entry to the label table.*/
		upPair = new NICLabelPair(nic,p.getUL());
		labelTable.put(p.getDest(), p.getSource(), upstreamIn, upPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + upstreamIn +
		"\\" + p.getSource() + "\\PSI");
		
		/*add the downstream entry to the label table*/
		downStreamIn = p.getSL().clone();
		downPair = new NICLabelPair(dataForwardNIC,downstreamOut);
		labelTable.put(p.getSource(),p.getDest(),downStreamIn,downPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: PSI" +
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
		if(p.getType().equals("PATH")){
			processLSCPATH((PATHMsg)p,nic);
		}
		else if(p.getType().equals("RESV")){
			processLSCRESV((RESVMsg)p,nic);
		}
		//TODO add cases for the other messages
	}
	
	/**
	 * processes a RESV message received from an LSC interface
	 * @param p the RESV message
	 * @param nic the nic on which 'p' was received
	 */
	private void processLSCRESV(RESVMsg p, LSRNIC nic) {
		Label dl = p.getDL(),pscUpLabel,pscDownOutLabel,pscDownInLabel;
		NICLabelPair lscDownPair,pscUpPair,pscDownPair;
		PATHMsg pscPathMsg;
		LSRNIC pscPathNIC;
		
		traceMsg("Received LSC RESV message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest() + " DL:" + p.getDL());
		
		/*verify that the DL is still available.*/
		if(getOpticalLabel(dl.getOptVal()) == false){
			errorPrint("RESV message (DL Lambda no longer available)");
			//TODO have these errors generate RESVERR
			System.exit(1);
		}
		
		/*confirm the LSC DL*/
		lscDownPair = labelTable.getWithOutLabel(dl);
		lscDownPair.getLabel().setIsPending(false);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: LSI" +
				"\\" + p.getSource() + "\\" + lscDownPair.getLabel());
		
		/*Now the PSC side needs to forward a PSC PATH on the data plane*/
		/*Create new PSC upstream entry*/
		pscUpPair = labelTable.getOutPair(p.getDest());
		pscUpLabel = new Label(calcIntInLabel());
		pscUpLabel.setIsPending(false);
		labelTable.put(p.getSource(), p.getDest(), pscUpLabel, pscUpPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + pscUpLabel +
				"\\" + p.getDest() + "\\" + pscUpPair.getLabel());
		
		/*Create new PSC downstream entry*/
		pscPathNIC = lscDownPair.getNIC();
		pscDownInLabel = labelTable.getInLabel(p.getDest(),p.getSource());
		pscDownOutLabel = new Label(calcIntOutLabel(pscPathNIC));
		pscDownOutLabel.setIsPending(true); //the PSC downstream labels are not confirmed yet
		pscDownPair = new NICLabelPair(lscDownPair.getNIC(),pscDownOutLabel);
		labelTable.put(p.getDest(), p.getSource(), pscDownInLabel , pscDownPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + pscDownInLabel +
				"\\" + p.getSource() + "\\" + pscDownPair.getLabel());
		
		/*Create a LSC capable PATH message containing the new labels*/
		pscPathMsg = new PATHMsg(p.getDest(),p.getSource(),
				lscDownPair.getLabel().getOptVal(),pscUpLabel,pscDownOutLabel);	
		pscPathNIC.sendPacket(pscPathMsg, this);
		sentPATH(pscPathMsg);
	}

	/**
	 * Processes a PATH message received on an LSC link. Does the following:
	 * 1. Adds confirmed labelTable entries for the upstream and downstream 
	 * @param p
	 * @param nic
	 */
	private void processLSCPATH(PATHMsg p, LSRNIC nic) {
		LSRNIC forwardNIC = dataRoutingTable.get(p.getDest()); //dataForwardNIC == forwardNIC (PSC)
		NICLabelPair upPair, downPair;
		Label upstreamIn, downstreamIn, downstreamOut;
				
		if(forwardNIC == null) noIPPath(p);
		
		traceMsg("Received LSC PATH message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest() + " UL:" + p.getUL() + " SL: " + p.getSL());
		
		/*check if the upstream wavelength is available*/
		if(getOpticalLabel(p.getUL().getOptVal()) == false ||
				getOpticalLabel(p.getSL().getOptVal()) == false){
			//TODO send PATHErr
			errorPrint("Upstream LSP setup failed (SL/UL unavailable)");
			System.exit(1);
		}
		
		//Set the confirmed status of the labels.
		p.getUL().setIsPending(false);
		p.getSL().setIsPending(false);
		
		/*1. add the upstream+downstream entries to the label table*/
		upstreamIn = new Label(calcIntInLabel());
		
		/*Add the upstream entry to the table*/
		upstreamIn = new Label(p.getUL().getOptVal());
		upPair = new NICLabelPair(nic, p.getUL().clone());
		labelTable.put(p.getDest(),p.getSource(),upstreamIn,upPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: PSI" +
				"\\" + p.getSource() + "\\" + upPair.getLabel());
		
		/*Add the downstream entry to the table*/
		downstreamIn = new Label(p.getSL().getOptVal());
		downstreamOut = new Label(upstreamIn.clone().getIntVal());
		downPair = new NICLabelPair(forwardNIC,downstreamOut);
		labelTable.put(p.getSource(), p.getDest(), downstreamIn, downPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + downstreamIn +
				"\\" + p.getDest() + "\\PSI");
		
		/*forward a RESV message upstream*/
		initiateLSCRESV(p,nic);
	}

	/**
	 * Starts the RESV message chain at the LSC level.  Forward a new RESV message
	 * back upstream so that intermediate LSC routers can confirm the LSC LSP setup.
	 * @param p the LSC PATHMsg which triggered the RESV
	 * @param nic the nic 'p' was received on.
	 */
	private void initiateLSCRESV(PATHMsg p, LSRNIC nic) {
		RESVMsg resv = new RESVMsg(p.getDest(), p.getSource(),p.getPrevHops(),p.getSL());
		nic.sendPacket(resv, this);
		sentRESV(resv);
	}

	/**
	 * Gives you an optical label to use for a new PATH message
	 * @return the next available label if one exists.
	 * null if no input label could be found
	 */
	protected OpticalLabel getNextOptLabel(){
		if(availableLambdas.size() > 0){
			return availableLambdas.get(0);
		}
		else return null;
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
	
	/**
	 * prints a user supplied trace message with the format (PSC+LSC Router <this.address>): <msg>
	 * @param msg the message to be printed
	 */
	protected void traceMsg(String msg){
		System.out.println("(LSC+PSC Router " + this.address + "): " +msg);
	}
	
	/**
	 * Print a confirmation message that a RESV message was sent.
	 * @param resv the RESV message itself
	 */
	private void sentRESV(RESVMsg resv) {
		traceMsg("Sent RESV message " + resv.getId() + " from:" + resv.getSource() +
				"to:"+ resv.getSource() + " DL:" + resv.getDL());
	}
	
	/**
	 * Will either store or forward a data packet received on the data plane.
	 * @param p the data packet
	 * @param nic the LSRNIC this packet was received on.
	 */
	public void processDataPacket(Packet p,LSRNIC nic){
		traceMsg("Received DATA packet " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest());
		//TODO implement a check to see if the LSP has been set up yet
		//TODO fill in the details of how to store/forward messages
	}
	
}
