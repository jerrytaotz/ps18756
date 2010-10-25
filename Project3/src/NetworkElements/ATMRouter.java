/**
 * @author andy modified by btello
 * @since 1.0
 * @version 1.2
 * @date 24-10-2008
 */

package NetworkElements;

import java.util.*;
import DataTypes.*;

public class ATMRouter implements IATMCellConsumer{
	private int address; // The AS address of this router
	private ArrayList<ATMNIC> nics = new ArrayList<ATMNIC>(); // all of the nics in this router
	private TreeMap<Integer, ATMNIC> nextHop = new TreeMap<Integer, ATMNIC>(); // a map of which interface to use to get to a given router on the network
	private TreeMap<Integer, NICVCPair> VCtoVC = new TreeMap<Integer, NICVCPair>(); // a map of input VC to output nic and new VC number
	private boolean trace=false; // should we print out debug code?
	private int traceID = (int) (Math.random() * 100000); // create a random trace id for cells
	private ATMNIC currentConnAttemptNIC = null; // The nic that is currently trying to setup a connection
	private boolean displayCommands = true; // should we output the commands that are received?
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public ATMRouter(int address){
		this.address = address;
	}
	
	/**
	 * Adds a nic to this router
	 * @param nic the nic to be added
	 * @since 1.0
	 */
	public void addNIC(ATMNIC nic){
		this.nics.add(nic);
	}
	
	/**
	 * sends a call proceeding signal to a downstream nic.  Prints the 
	 * message corresponding to the signal before transmission. 
	 * @param nic - the nic on which the signal will be sent
	 * @param signal - a string representation of the signal to be sent.
	 * valid values for signal:
	 * "call proceeding"
	 * "setup <dest. address>"
	 * "wait"
	 * "connect <vc number>"
	 * "connect ack"
	 * "end <vc number>"
	 * "end ack"
	 * @param oldCell - the cell which this signal is being sent in response to
	 * @remarks if the value of signal is not on the list above, a packet 
	 * will still be sent to nic, it will just need to be dealt with on
	 * the receiving end as a bad signal.
	 */
	private void sendSignal(ATMNIC nic, String signal, ATMCell oldCell){
		//Create the cell to send
		ATMCell sigCell = null; 
		
		if(signal.contains("call proceeding")){
			sigCell = new ATMCell(0, signal, this.traceID++);
			sigCell.setIsOAM(true);
			sentCallProceeding(sigCell);
		}
		else if(signal.contains("setup")){
			sigCell = new ATMCell(0, signal, oldCell.getTraceID());
			sigCell.setIsOAM(true);
			sentSetup(sigCell);
		}
		else if(signal.contains("wait")){
			sigCell = new ATMCell(0, signal, this.traceID++);
			sigCell.setIsOAM(true);
			sentWait(sigCell);
		}
		else if(signal.contains("connect")){
			sigCell = new ATMCell(0, signal, this.traceID++);
			sigCell.setIsOAM(true);
			if(signal.contains("ack")) sentConnectAck(sigCell);
			else sentConnect(sigCell);
		}
		else if(signal.contains("end")){
			sigCell = new ATMCell(0,signal,oldCell.getTraceID());
			sigCell.setIsOAM(true);
			if(signal.contains("ack")) sentEndAck(sigCell);
			else sentEnd(sigCell);
		}
		else{
			System.err.println("Router " + address + "tried to send an unknown signal");
			return;
		}
		nic.sendCell(sigCell, this);
	}
	
	/**
	 * Process a setup signal.
	 * @param cell - the cell containing the setup signal
	 * @param nic - the nic which the cell came from
	 */
	private void processSetup(ATMCell cell, ATMNIC nic){
		String cellData = cell.getData();
		ATMNIC nextHopNIC;
		int destAddress;
		
		destAddress =getIntFromEndOfString(cellData);
		/*check for bad setup message format*/
		if(destAddress == -1){
			this.receivedBadCell(cell);
			return;
		}
		
		receivedSetup(cell);
		//check whether this router is busy or not and send wait() if it is
		if(currentConnAttemptNIC != null){
			sendSignal(nic,"wait " + destAddress,cell);
		}
		//set up the outgoing NIC
		else{
			currentConnAttemptNIC = nic;
			sendSignal(currentConnAttemptNIC,"call proceeding",cell);
			nextHopNIC = nextHop.get(destAddress);
			/*check if the dest. address has been entered into this router's tables*/
			if(nextHopNIC != null){
				sendSignal(nextHopNIC, "setup " + destAddress, cell);
			}
			//if this is the destination router.
			else if(destAddress == this.address){
				int inVC = calcInVC();
				System.out.println("Trace (ATMRouter): First free VC = " + inVC);
				//set up a VC to use and forward that back down the way.
				sendSignal(currentConnAttemptNIC,"connect " + inVC,cell);
				currentConnAttemptNIC = null;
				//the null entry signifies that this is a terminal point for a VC
				VCtoVC.put(inVC, null);
			}
			else{
				gotUnknownAddress(cell);
				return;
			}
		}
	}
	
	/**
	 * calculates the next available input VC.  Uses a linear scan over the values in the VCtoVC map.
	 * @return the endpoint VC if one can be found. -1 if all integers are already in use.
	 */
	private int calcInVC() {
		//find the next available input VC
		for(int i = 1;i < Integer.MAX_VALUE;i++){
			if(!VCtoVC.containsKey(i)){
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * calculates the next available output VC.  Uses a linear scan over the values in the VCtoVC map.
	 * @return the endpoint VC if one can be found. -1 if all integers are already in use.
	 */
	private int calcOutVC(ATMNIC nic) {
		for(int i = 1; i< Integer.MAX_VALUE;i++){
			if(!VCtoVC.containsValue(new NICVCPair(nic, i))){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Processes the "call proceeding" signal.
	 * @param cell
	 */
	private void processCallProceeding(ATMCell cell){
		//TODO:implement this method, I don't think it really needs to do much
		receivedCallProceeding(cell);
	}
	
	/**
	 * TODO fill in the comments for this method
	 * @param cell
	 * @param nic
	 */
	private void processConnect(ATMCell cell, ATMNIC nic){
		String cellData = cell.getData();
		int proposedVC = getIntFromEndOfString(cellData);
		NICVCPair nicVCPair = new NICVCPair(nic, proposedVC);
		int nextInVC;
		
		receivedConnect(cell);
		
		//if the nic/VC pairing is already in the map
		if(VCtoVC.containsValue(nicVCPair)){
			//need to come up with a new output VC
			nicVCPair = new NICVCPair(nic,calcOutVC(nic));		
		}
		nextInVC = calcInVC();
		VCtoVC.put(nextInVC, nicVCPair);
		
		//print a message indicating choice of VC->VC mapping
		labelsSelected(nextInVC,VCtoVC.get(nextInVC));
		
		//send a connect ack message
		sendSignal(nic,"connect ack",cell);
		
		//forward the connect to the next router/computer
		sendSignal(currentConnAttemptNIC,"connect " + nextInVC,cell);
		
		//currentConnAttemptNIC being null indicates this router is free to process
		//setup messages again.
		currentConnAttemptNIC = null;
	}
	
	/**
	 * 
	 * @param cell
	 * @param nic
	 */
	private void processWait(ATMCell cell, ATMNIC nic){
		String cellData;
		int destAddress;
		
		if(cell != null && nic != null){
			cellData = cell.getData();
			destAddress = getIntFromEndOfString(cellData);
			//check if destAdress was screwed up
			if(destAddress != -1){
				receivedWait(cell);
				sendSignal(nic,"setup " + destAddress,cell);
			}
			else receivedBadCell(cell);
		}
		else{
			System.err.println("processWait received a null arg in Router " + this.address);
		}
	}
	
	/**
	 * Process the "connect ack" signal.  Prints to terminal to verify to user that
	 * the signal was received.
	 * @param cell - the cell containing the signal
	 */
	private void processConnectAck(ATMCell cell) {
		receiveConnectAck(cell);
	}
	
	/**
	 * Process the "end <VC number>" signal.  Tears down the input VC number specified in 
	 * the signal and passes the signal to the next router on the corresponding output VC.
	 * Once the VC has been deleted, an "end ack" signal is sent back to the originating router.
	 * If the input VC does not exist at this router, cellNoVC() is called.  If this router
	 * is the terminal point for this VC, the signal is not forwarded. 
	 * @param cell - the cell containing the signal
	 * @param nic - the NIC from which the signal was received
	 */
	private void processEnd(ATMCell cell, ATMNIC nic){
		String cellData = cell.getData();
		int endVC = getIntFromEndOfString(cellData);
		int outVC; //The VC which endVC is mapped to (if any)
		NICVCPair outPair; //The outgoing NIC/VC pairing (if it exists)
		
		//error checking
		if(endVC < 0){
			cellNoVC(cell);
			return;
		}
		
		if(VCtoVC.containsKey(endVC)){
			//The "end" signal was for a valid VC number
			sendSignal(nic, "end ack", cell);
			
			outPair = VCtoVC.get(endVC);
			
			if(outPair != null){ //if outPair == null, this is the end of the circuit
				outVC = outPair.getVC();
				sendSignal(outPair.getNIC(), "end " + outVC, cell);
				System.out.println("Trace (ATMRouter): Router " + address + 
				" removing entry <" + endVC + "," + outVC + ">");
			}
			else{
				System.out.println("Trace (ATMRouter): VC " + endVC + " torn down.");
			}
			//evict the entry from the VC table.
			VCtoVC.remove(endVC);
			
		}
		else{
			//This router doesn't have an entry for the received VC
			cellNoVC(cell);
		}
	}
	
	/**
	 * Process the "end ack" signal.  Prints to terminal to verify to user that the 
	 * message was received.
	 * @param cell - the cell containing the signal
	 */
	private void processEndAck(ATMCell cell){
		receivedEndAck(cell);
	}
	
	/**
	 * Handle the various OAM signals appropriately. 
	 * @param cell - the cell containing the OAM data.
	 * @param nic - the nic which the cell came from.
	 */
	private void processOAMSignal(ATMCell cell, ATMNIC nic){
		String cellData = cell.getData();
		
		/*check for bad OAM cell format*/
		if(cellData.compareTo("") == 0){
			this.receivedBadCell(cell);
			return;
		}
		
		if(cellData.contains("setup")){
			processSetup(cell,nic);
		}
		else if(cellData.contains("call proceeding")){
			processCallProceeding(cell);
		}
		else if(cellData.contains("wait")){
			processWait(cell,nic);
		}
		else if(cellData.contains("connect")){
			if(cellData.contains("ack")) processConnectAck(cell);
			else processConnect(cell,nic);
		}
		else if(cellData.contains("end")){
			if(cellData.contains("ack")) processEndAck(cell);
			else processEnd(cell,nic);
		}
		else{
			receivedUnknownSignal(cell);
		}
	}

	/**
	 * This method processes data and OAM cells that arrive from any nic in the router
	 * @param cell the cell that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receiveCell(ATMCell cell, ATMNIC nic){
		
		if(trace)
			System.out.println("Trace (ATMRouter): Received a cell " + cell.getTraceID());
		
		if(cell.getIsOAM()){
			// OAM is used for signaling so here we just process the signals
			this.processOAMSignal(cell,nic);
		}
		else{
			// find the nic and new VC number to forward the cell on
			// otherwise the cell has nowhere to go. output to the console and drop the cell
		}		
	}
	
	/**
	 * Gets the number from the end of a string
	 * @param string the sting to try and get a number from
	 * @return the number from the end of the string, or -1 if the end of the string is not a number
	 * @since 1.0
	 */
	private int getIntFromEndOfString(String string){
		// Try getting the number from the end of the string
		try{
			String num = string.split(" ")[string.split(" ").length-1];
			return Integer.parseInt(num);
		}
		// Couldn't do it, so return -1
		catch(Exception e){
			if(trace)
				System.out.println("Could not get int from end of string");
			return -1;
		}
	}
	
	/**
	 * This method returns a sequentially increasing random trace ID, so that we can
	 * differentiate cells in the network
	 * @return the trace id for the next cell
	 * @since 1.0
	 */
	public int getTraceID(){
		int ret = this.traceID;
		this.traceID++;
		return ret;
	}
	
	/**
	 * Tells the router the nic to use to get towards a given router on the network
	 * @param destAddress the destination address of the ATM router
	 * @param outInterface the interface to use to connect to that router
	 * @since 1.0
	 */
	public void addNextHopInterface(int destAddress, ATMNIC outInterface){
		this.nextHop.put(destAddress, outInterface);
	}
	
	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void clearOutputBuffers(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).clearOutputBuffers();
	}
	
	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void clearInputBuffers(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).clearInputBuffers();
	}
	
	/**
	 * Sets the nics in the router to use tail drop as their drop mechanism
	 * @since 1.0
	 */
	public void useTailDrop(){
		for(int i=0; i<this.nics.size(); i++)
			nics.get(i).setIsTailDrop();
	}
	
	/**
	 * Sets the nics in the router to use RED as their drop mechanism
	 * @since 1.0
	 */
	public void useRED(){
		for(int i=0; i<this.nics.size(); i++)
			nics.get(i).setIsRED();
	}
	
	/**
	 * Sets the nics in the router to use PPD as their drop mechanism
	 * @since 1.0
	 */
	public void usePPD(){
		for(int i=0; i<this.nics.size(); i++)
			nics.get(i).setIsPPD();
	}
	
	/**
	 * Sets the nics in the router to use EPD as their drop mechanism
	 * @since 1.0
	 */
	public void useEPD(){
		for(int i=0; i<this.nics.size(); i++)
			nics.get(i).setIsEPD();
	}
	
	/**
	 * Sets if the commands should be displayed from the router in the console
	 * @param displayComments should the commands be displayed or not?
	 * @since 1.0
	 */
	public void displayCommands(boolean displayCommands){
		this.displayCommands = displayCommands;
	}
	
	/**
	 * Outputs to the console that a cell has been dropped because it reached its destination
	 * @since 1.0
	 */
	public void cellDeadEnd(ATMCell cell){
		if(this.displayCommands)
		System.out.println("The cell is destined for this router (" + 
				this.address + "), taken off network " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a cell has been dropped as no such VC exists
	 * @since 1.0
	 */
	public void cellNoVC(ATMCell cell){
		if(this.displayCommands)
		System.out.println("The cell is trying to be sent on an incorrect VC " 
				+ cell.getTraceID());
	}
	
	/**
	 * Error Handling function to handle unknown (bad) cell formats.
	 * @param cell - the cell which was received
	 */
	private void receivedBadCell(ATMCell cell){
		if(this.displayCommands)
			System.err.println("Router " + this.address + 
					" received a malformed cell." + cell.getTraceID() +
					"\nCell Data: " + cell.getData());
	}
	
	/**
	 * Error handling function to handle unknown OAM signal types.
	 * @param cell - the cell containing the unknown OAM signal
	 */
	private void receivedUnknownSignal(ATMCell cell){
		if(this.displayCommands)
			System.err.println("Router " + this.address +
					" received an unknown OAM signal." + cell.getTraceID());
	}
	
	/**
	 * Error Handling function which can be called when a cell contains a 
	 * destination address which does not currently exist in this router's
	 * routing tables.
	 * @param cell - the offending cell
	 */
	private void gotUnknownAddress(ATMCell cell){
		if(this.displayCommands)
			System.out.println("Router " + this.address +
					"received a cell containing an unknown address. " + 
					cell.getTraceID()) ;
		
	}
	
	/**
	 * Outputs to the console that a connect message has been sent
	 * @since 1.0
	 */
	private void sentSetup(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND SETUP: Router " +this.address+ 
				" sent a setup " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a setup message has been sent
	 * @since 1.0
	 */
	private void receivedSetup(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC SETUP: Router " +this.address+ 
				" received a setup message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a call proceeding message has been received
	 * @since 1.0
	 */
	private void receivedCallProceeding(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC CALLPRO: Router " +this.address+ 
				" received a call proceeding message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect message has been sent
	 * @since 1.0
	 */
	private void sentConnect(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND CONN: Router " +this.address+ 
				" sent a connect message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect message has been received
	 * @since 1.0
	 */
	private void receivedConnect(ATMCell cell){
int VCNum = getIntFromEndOfString(cell.getData());
		
		//TODO remove the VC in parens below before submission.
		if(this.displayCommands)
		System.out.println("REC CONN: Router " +this.address+ 
				" received a connect message " + "(" + VCNum + ") " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect ack message has been sent
	 * @since 1.0
	 * @version 1.2
	 */
	private void sentConnectAck(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND CALLACK: Router " +this.address+ 
				" sent a connect ack message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect ack message has been received
	 * @since 1.0
	 */
	private void receiveConnectAck(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC CALLACK: Router " +this.address+ 
				" received a connect ack message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an call proceeding message has been received
	 * @since 1.0
	 */
	private void sentCallProceeding(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND CALLPRO: Router " +this.address+ 
				" sent a call proceeding message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an end message has been sent
	 * @since 1.0
	 */
	private void sentEnd(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND ENDACK: Router " +this.address+ " sent an end message " 
				+ cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an end message has been received
	 * @since 1.0
	 */
	private void recieveEnd(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC ENDACK: Router " +this.address+ " received an end message "
				+ cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an end ack message has been received
	 * @since 1.0
	 */
	private void receivedEndAck(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC ENDACK: Router " +this.address+ " received an end ack message " 
				+ cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an end ack message has been sent
	 * @since 1.0
	 */
	private void sentEndAck(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND ENDACK: Router " +this.address+ " sent an end ack message " 
				+ cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a wait message has been sent
	 * @since 1.0
	 */
	private void sentWait(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND WAIT: Router " +this.address+ " sent a wait message " 
				+ cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a wait message has been received
	 * @since 1.0
	 */
	private void receivedWait(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC WAIT: Router " +this.address+ " received a wait message " 
				+ cell.getTraceID());
	}
	
	/**
	 * Prints a message to the console indicating which input/output VCs were selected for
	 * for a connection
	 * @param inVC - the input VC for the connection
	 * @param outPair - the output NIC/VC pair for the connection
	 * @remarks the pair should be established before a call to this method
	 */
	private void labelsSelected(int inVC, NICVCPair outPair){
		if(this.displayCommands)
			System.out.println("Label mapping (Router " + this.address + "): <" +
					inVC + "," + outPair.getVC() + ">");
	}
}
