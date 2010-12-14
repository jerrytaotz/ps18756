package NetworkElements;
import java.util.*;

import DataTypes.*;

import dijkstra.DirectedGraph;
import dijkstra.PathCalculator;

public class PscLSR extends LSR {

	private HashMap<Integer,LSRNIC> routingTable;
	
	public PscLSR(int address) {
		super(address);
		this.routingTable = new HashMap<Integer,LSRNIC>();
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
	 * Print an error message containing the type of this LSR and it's address.
	 * @param errorMsg the error message you would like printed.
	 */
	@Override
	public void errorPrint(String errorMsg) {
		System.out.println("(PSC Router " + this.getAddress() + "): ERROR " + errorMsg);
	}

	/**
	 * returns the destination NIC for a given destination using only the 
	 * destination address.  This should be used to determine which NIC a packet
	 * should travel across if it does not have an LSP setup.
	 * @param dest the destination address for the node
	 * @param control true if you want a control path for the dest 
	 * false if you want a data path
	 * @remarks the control parameter has no effect on the result of this method
	 * for a PSC router since control plane == data plane
	 */
	@Override
	public LSRNIC getDestNICviaIP(int dest, boolean control) {
		if(!routingTable.containsKey(dest)){
			errorPrint("This router does not contain a path to " + dest);
			System.exit(0);
		}
		return routingTable.get(dest);
	}

	/**
	 * This method will determine what type of RSVP message has been received and forward it
	 * to the appropriate handler.
	 * @param p the packet which was received
	 * @param nic the nic on which the packet was received.
	 */
	private void processRSVP(rsvpPacket p, LSRNIC nic){
		if(p.getType().equals("PATH")){
			processPATH((PATHMsg)p,nic);
		}
		if(p.getType().equals("RESV")){
			processRESV((RESVMsg)p,nic);
		}
	}
	
	/**
	 * Process a RESV message.  Confirms the downstream label contained in the RESV message 
	 * and enters the appropriate entries into the labelTable.
	 * @param p the RESV message
	 * @param nic the nic 'p' was received on
	 */
	private void processRESV(RESVMsg p, LSRNIC nic) {
		RESVMsg resvMsg;
		Label upInLabel = labelTable.getInLabel(p.getSource(), p.getDest());
		NICLabelPair upOutPair;
		LSRNIC pscForwardNIC;
		Label downInLabel = new Label(calcInLabel());
		
		traceMsg("Received PSC RESV message " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest() + " DL:" + p.getDL());
		
		/*confirm the downstream label*/
		labelTable.getPSCOutPairWithDest(p.getSource()).setLabel(p.getDL());

		if(p.getDest() == this.address){
			traceMsg("Terminated RESV Msg " + p.getId() + " releasing packets destined for " +
					p.getSource());
			releaseWaitingPackets(p);		
		}
		else{
			/*create a PSC RESV message and forward it*/
			upOutPair = labelTable.getPSCOutPairWithDest(p.getDest());
			pscForwardNIC = upOutPair.getNIC();
			resvMsg = new RESVMsg(p.getSource(),p.getDest(),downInLabel);
			resvMsg.setId(p.getId());
			pscForwardNIC.sendPacket(resvMsg, this);
			sentRESV(resvMsg);
		}
		
	}

	/**
	 * Releases all packets waiting on a specific LSP to be setup.  
	 * NOTE: does not release ALL packets from the queue, just the ones waiting for the
	 * LSP being setup by the RESV message p
	 * @param p the RESV message which triggered the release
	 */
	private void releaseWaitingPackets(RESVMsg p) {
		NICLabelPair outPair =  labelTable.getOutPair(p.getSource());
		LSRNIC outNIC = outPair.getNIC();
		MPLS header = new MPLS(outPair.getLabel());
		
		for(Packet waiting:waitingPackets){
			if(waiting.getDest() == p.getSource()){
				waiting.addMPLSheader(header);
				outNIC.sendPacket(waiting, this);
				sentData(p,outPair.getLabel());
			}
		}
	}

	/**
	 * This method will determine the type of packet received and forward it to the appropriate
	 * handler method.
	 * @param p the packet which was received
	 * @param nic the nic on which p was received
	 */
	@Override
	public void receivePacket(Packet p, LSRNIC nic) {
		if(p.isRSVP()){
			processRSVP((rsvpPacket)p,nic);
		}
		else 
			processDataPacket(p,nic);
	}
	
	/**
	 * This message should be printed whenever a PATH message is received.
	 * @param p
	 */
	private void processPATH(PATHMsg p,LSRNIC nic){
		Label upOutLabel = p.getUL();
		Label upInLabel = new Label(calcInLabel());
		Label downInLabel = new Label(calcInLabel());
		Label downOutLabel;
		LSRNIC forwardNIC;
		NICLabelPair downOutPair;
		NICLabelPair upOutPair = new NICLabelPair(nic,upOutLabel);
		PATHMsg pathMsg;
		RESVMsg resvMsg;
		
		if(p.getDest() == this.address){
			traceMsg("Terminated PSC PATH message " + p.getId() + " from:" + p.getSource() + 
					" to:" + p.getDest() + " UL: " + p.getUL());
			/*add a null entry to signify that this is the destination node.*/
			downOutPair = null; 
			labelTable.put(p.getDest(), p.getSource(), upInLabel, upOutPair); //upstream
			labelTable.put(p.getSource(), p.getDest(), downInLabel, downOutPair); //downstream
			
			/*Generate and forward new RESV message.*/
			resvMsg = new RESVMsg(this.address,p.getSource(),downInLabel);
			upOutPair.getNIC().sendPacket(resvMsg, this);
			sentRESV(resvMsg);
		}
		else{
			traceMsg("Received PSC PATH message " + p.getId() + " from:" + p.getSource() + 
					" to:" + p.getDest() + " UL: " + p.getUL());
			/*add upstream + downstream entries*/
			downOutLabel = new Label(Label.RESV_PENDING);
			downOutLabel.setIsPending(true);
			forwardNIC = routingTable.get(p.getDest());
			downOutPair = new NICLabelPair(forwardNIC,downOutLabel);
			labelTable.put(p.getDest(), p.getSource(), upInLabel, upOutPair); //upstream
			labelTable.put(p.getSource(), p.getDest(), downInLabel, downOutPair); //downstream
			
			/*Forward new PATH message.*/
			pathMsg = new PATHMsg(p.getSource(),p.getDest(),upInLabel);
			pathMsg.setId(p.getId());
			forwardNIC.sendPacket(pathMsg, this);
			sentPATH(pathMsg);
		}
	}
	
	/**
	 * prints a user supplied trace message with the format (PSC Router <this.address>): <msg>
	 * @param msg the message to be printed
	 */
	protected void traceMsg(String msg){
		System.out.println("(PSC Router " + this.address + "): " +msg);
	}
	
	/**
	 * calculates the next available input label.  Uses a linear scan over the values in the labelTable.
	 * @return the output label if one can be found.
	 * -1 if no input label could be found
	 */
	protected int calcInLabel(){
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
	protected int calcOutLabel(LSRNIC nic){
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
	 * Will initiate LSP setup to the destination address in 'newPacket'. 
	 * @param the packet which triggered this LSP setup 
	 */
	@Override
	protected void setupLSPOnTheFly(Packet newPacket) {
		NICLabelPair upstreamPair,downStreamPair;
		Label upstreamLabel,downStreamInLabel,downStreamOutLabel;
		LSRNIC forwardNIC;
		int dest = newPacket.getDest();
		PATHMsg pathMsg;
		forwardNIC = this.routingTable.get(dest);
		
		if(forwardNIC == null){
			noIPPath(newPacket);
		}
		
		/*Add the new upstream Label Table Entry*/
		upstreamLabel = new Label(calcInLabel());
		upstreamPair = new NICLabelPair(null,null); //null signifies this node is the initiator.
		labelTable.put(newPacket.getDest(),this.address,upstreamLabel, upstreamPair);
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + upstreamLabel +
				"\\local\\local");
		
		
		/*Add the new downstream Label Table entry.  The labels are not confirmed yet*/
		downStreamInLabel = new Label(calcInLabel());
		downStreamInLabel.setIsPending(true);
		downStreamOutLabel = new Label(Label.RESV_PENDING);
		downStreamOutLabel.setIsPending(true);
		downStreamPair = new NICLabelPair(forwardNIC,downStreamOutLabel);
		labelTable.put(this.address, newPacket.getDest(), downStreamInLabel, downStreamPair);		
		System.out.println("LSR " + this.address + ", ROUTE ADD, Input: " + downStreamInLabel +
				"\\" + dest + "\\RESV Pending");
		
		//Place a new PATH message in the output buffer
		pathMsg = new PATHMsg(this.address, dest, upstreamLabel.clone());
		sentPATH(pathMsg);
		pathMsg.addPrevHop(this.address);
		forwardNIC.sendPacket(pathMsg,this);
		
		/*enqueue the packet which triggered the LSP setup.*/
		waitingPackets.add(newPacket);
		
	}

	/**
	 * Will either store or forward a data packet received on the data plane.
	 * @param p the data packet
	 * @param nic the LSRNIC this packet was received on.
	 */
	public void processDataPacket(Packet p,LSRNIC nic){
		NICLabelPair outPair;
		LSRNIC outNIC;
		MPLS header;
		
		traceMsg("Received DATA packet " + p.getId() + " from:" + p.getSource() + 
				" to:" + p.getDest());
		
		if(p.getDest() != this.address){
			 outPair = labelTable.getOutPair(p.popMPLSheader().getLabel().getIntVal());
			 outNIC = outPair.getNIC();
			 header = new MPLS(outPair.getLabel());
			 p.addMPLSheader(header);
			 outNIC.sendPacket(p, this);
			 sentData(p,outPair.getLabel());
		}
		else{
			traceMsg("Terminated DATA packet " + p.getId());
		}
	}
	
	/**
	 * Print a confirmation message that a data packet was sent from this router.
	 * @param p
	 * @param label
	 */
	private void sentData(Packet p,Label label){
		 traceMsg("Sent DATA packet " + p.getId() + " from:" + p.getSource() + " to:" +
				 p.getDest() + " label:" + label);
	}
}
