/**
 * @author andy modified by btello
 * @version 1.0
 * @date 24-10-2008
 * @since 1.0
 */

package NetworkElements;

import DataTypes.*;

import java.util.*;

public class ATMNIC {
	private IATMCellConsumer parent; // The router or computer that this nic is in
	private OtoOLink link; // The link connected to this nic
	private boolean trace = false; // should we print out debug statements?
	private ArrayList<ATMCell> inputBuffer = new ArrayList<ATMCell>(); // Where cells are put between the parent and nic
	private ArrayList<ATMCell> outputBuffer = new ArrayList<ATMCell>(); // Where cells are put to be outputted
	private boolean tail=true, red=false, ppd=false, epd=false; // set what type of drop mechanism
	private int maximumBufferCells = 20; // the maximum number of cells in the output buffer
	/*RED state parameters*/
	private int REDMinThresh = 10; //The minimum number of cells in the output buffer before we start dropping cells
	private int REDMaxThresh = 20; //The maximum average number of cells to allow in the output buffer.
	private double REDAvg; //average queue size for RED
	/*PPD state parameters*/
	private boolean droppingAPacket; //is PPD currently dropping an IP packet?

	/**
	 * Default constructor for an ATM NIC
	 * @param parent
	 * @since 1.0
	 */
	public ATMNIC(IATMCellConsumer parent){
		this.parent = parent;
		this.parent.addNIC(this);
	}

	/**
	 * This method is called when a cell is passed to this nic to be sent. The cell is placed
	 * in an output buffer until a time unit passes
	 * @param cell the cell to be sent (placed in the buffer)
	 * @param parent the router the cell came from
	 * @since 1.0
	 */
	public void sendCell(ATMCell cell, IATMCellConsumer parent){
		if(this.trace){
			System.out.println("Trace (ATM NIC): Received cell");
			if(this.link==null)
				System.out.println("Error (ATM NIC): You are trying to send a cell through a nic not connected to anything");
			if(this.parent!=parent)
				System.out.println("Error (ATM NIC): You are sending data through a nic that this router is not connected to");
			if(cell==null)
				System.out.println("Warning (ATM NIC): You are sending a null cell");
		}


		if(this.tail) this.runTailDrop(cell);
		else if(this.red) this.runRED(cell,false,false);
		else if(this.ppd) this.runPPD(cell);
		else if(this.epd) this.runEPD(cell);
	}

	/**
	 * Runs tail drop on the cell
	 * @param cell the cell to be added/dropped
	 * @since 1.0
	 */
	private void runTailDrop(ATMCell cell){
		boolean cellDropped = false;

		if(outputBuffer.size() < maximumBufferCells){
			outputBuffer.add(cell);
		}
		//Never drop an OAM cell.
		else if(cell.getIsOAM()){
			forceOAM(cell);
		}
		else cellDropped = true;

		// Output to the console what happened
		if(cellDropped)
			System.out.println("The cell " + cell.getTraceID() + " was tail dropped");
		else
			if(this.trace)
				System.out.println("The cell " + cell.getTraceID() + " was added to the output queue");
	}

	/**
	 * Runs Random early detection on the cell.  Computes the average queue length over time
	 * and drops 
	 * @param cell the cell to be added/dropped from the queue
	 * @param withPPD set this to true if using RED for PPD
	 * @param withEPD set this to true if using RED for EPD
	 * @return true if the cell was admitted, false otherwise;
	 * @since 1.0
	 */
	private boolean runRED(ATMCell cell, boolean withPPD, boolean withEPD){
		Random rnd = new Random();
		boolean cellDropped = false;
		double dropProbability = 0.0;
		double aveInt;
		int sidesOfADie; //used to "roll a die" to see if packet gets dropped or not

		//calculate the average queue size
		//System.out.println("REDSumOfSizes " + REDSumOfSizes);
		//this.REDAvg = (double)REDSumOfSizes/(double)++count;
		//aveInt = Math.rint(REDAvg);
		
		aveInt = (REDAvg + (outputBuffer.size() + 1))/2;
		
		//If the average is between the min and max thresh
		if(REDMaxThresh>aveInt && REDMinThresh < aveInt){
			//Create a "die" to roll for this packet.
			//As ave. approaches REDMaxThresh, the die gets smaller.
			sidesOfADie = (REDMaxThresh + 1) - (int)aveInt;
			dropProbability = 1.0/(double)sidesOfADie;
			System.out.println("(RED) Ave Queue Size: " + aveInt + " (in prob. drop zone).");
			System.out.println("(RED) Drop Prob: " + dropProbability + " = 1/" + sidesOfADie);
			//roll the die. Drop packet on a 1.
			if((rnd.nextInt(sidesOfADie) + 1) == 1){
				cellDropped = true;
			}
			else{
				outputBuffer.add(cell);
				//REDSumOfSizes += outputBuffer.size();
			}
		}
		else if(REDMaxThresh < aveInt){
			System.out.println("(RED) Ave Queue Size: " + aveInt + " (> RED Max Threshold)");
			//If this is an OAM cell we need to force it in by evicting some other cell
			if(cell.getIsOAM()){
				forceOAM(cell);
				return true;
			}
			else
				cellDropped = true;
		}
		else{
			System.out.println("(RED) Ave Queue Size: " + aveInt + " (< RED Min Threshold)");
			outputBuffer.add(cell);
			//REDSumOfSizes += outputBuffer.size();
		}

		// Output to the console if not using RED for PPD or EPD, otherwise they will
		// handle it on their own.
		if(cellDropped){
			if(!withPPD && !withEPD)
				System.out.println("The cell " + cell.getTraceID() + 
						" was dropped with probability " + dropProbability);
			return false;
		}
		else{
			if(this.trace && !withPPD && !withEPD)
				System.out.println("The cell " + cell.getTraceID() + " was added to the output queue");
			return true;
		}
	}

	/**
	 * Force an OAM cell into the queue by evicting the first non-OAM cell
	 * in the queue and replacing it with the OAM cell.  Only call this method
	 * when the queue is full (or the average queue size is too high.)
	 * @param cell - the OAM cell which is causing the eviction
	 * @remarks This method was implemented under the assumption that the 
	 * output buffer will never be filled with only OAM cells (as posted on discussion board)
	 * Thus, there will always be at least one non-OAM cell to evict.
	 */
	private void forceOAM(ATMCell cell) {
		ATMCell victim;
		for(int i = 0;i < this.outputBuffer.size();i++){
			victim = outputBuffer.get(i);
			if(!victim.getIsOAM()){
				outputBuffer.remove(i);
				outputBuffer.add(cell);
				System.out.println("Trace (ATMNIC): Evicted a packet in the queue" +
				"to make room for an OAM cell.");
				return;
			}
		}
	}

	/**
	 * Runs Partial packet drop on the cell.  Uses the existing RED method to determine
	 * whether a packet is dropped or not.  Once a packet gets 
	 * @param cell the cell to be added/dropped from the queue
	 * @since 1.0
	 */
	private void runPPD(ATMCell cell){
		boolean cellDropped = false;

		//If in the process of dropping a packet, check if this is part of
		//that same packet.
		if(this.droppingAPacket){
			//FIRST check OAM.  If it is OAM, just let it go through to RED processing.
			if(!cell.getIsOAM()){
				//If it is part of packet being dropped, drop it.
				if(cell.getPacketData() == null){
					cellDropped = true;
				}
				//If new IP packet allow it to try it's luck with RED
				else this.droppingAPacket = false;
			}
		}

		if(!this.droppingAPacket || cell.getIsOAM()){
			//If RED doesn't admit the packet, start dropping all the cells.
			if(!runRED(cell,true,false)){
				cellDropped = true;
				this.droppingAPacket = true;
			}
		}

		// Output to the console what happened
		if(cellDropped)
			System.out.println("The cell " + cell.getTraceID() + " was dropped");
		else
			if(this.trace)
				System.out.println("The cell " + cell.getTraceID() + 
				" was added to the output queue");
	}

	/**
	 * Runs Early packet drop on the cell
	 * @param cell the cell to be added/dropped from the queue
	 * @since 1.0
	 */
	private void runEPD(ATMCell cell){
		boolean cellDropped = false;
		
		//if cell is not an IP header, check if packet is being dropped.
		if(cell.getPacketData() == null){
			//if so, drop it as long as it's not an OAM cell.
			if(this.droppingAPacket && !cell.getIsOAM()) cellDropped = true;
			//if not dropping, let it go through RED (which will also handle OAM).
			else cellDropped = !this.runRED(cell,false,true);
		}
		//if cell IS an IP header, we need to run a RED simulation on it.
		else{
			System.out.println("(EPD) Running RED simulation.");
			cellDropped = !this.runREDSimulation(cell);
			this.droppingAPacket = cellDropped;
			if(cellDropped = false)
				cellDropped = this.runRED(cell, false, true);
		}		
		// Output to the console what happened
		if(cellDropped)
			System.out.println("The cell " + cell.getTraceID() + " was dropped");
		else
			if(this.trace)
				System.out.println("The cell " + cell.getTraceID() + " was added to the output queue");
	}

	/**
	 * Run a simulation of the RED algorithm of an IP cell to determine if any of its
	 * cells will be dropped or not.
	 * @param cell the cell containing the IP header we want to analyze
	 * @return true if the entire packet would make it through, false otherwise.
	 */
	private boolean runREDSimulation(ATMCell cell) {
		boolean packetAdmitted = false;
		int packetLen = 0,numCells = 0;
		int sidesOfADie;
		ArrayList<ATMCell> virtualOutputBuffer = (ArrayList<ATMCell>)this.outputBuffer.clone();
		double aveInt,dropProbability = 0.0;
		Random rnd = new Random();

		//1. get the length of the IP packet
		packetLen = cell.getPacketData().getSize();
		//2. Divide the length across x ATM cells
		numCells = packetLen/ATMCell.CELL_SIZE + 1;
		System.out.println("(REDSIM) A packet of length " + packetLen + " = " + 
				numCells + " ATM cells.");

		//3. Run RED simulation using a virtual buffer and virtual counter for each cell
		for(int i = 0;i<numCells;i++){
			//calculate the average queue size
			aveInt = (REDAvg + (virtualOutputBuffer.size() + 1))/2;
			
			//If the average is between the min and max thresh
			if(REDMaxThresh>aveInt && REDMinThresh < aveInt){
				//Create a "die" to roll for this packet.
				//As ave. approaches REDMaxThresh, the die gets smaller.
				sidesOfADie = (REDMaxThresh + 1) - (int)aveInt;
				dropProbability = 1.0/(double)sidesOfADie;
				
				System.out.println("(REDSIM) Ave Queue Size: " + aveInt + " (in prob. drop zone).");
				System.out.println("(REDSIM) Drop Prob: 1/" + sidesOfADie);
				//roll the die. Drop packet on a 1.
				if((rnd.nextInt(sidesOfADie) + 1) == 1){
					System.out.println("(REDSIM) Simulation detected a cell drop.");
					return false;
				}
				else{
					virtualOutputBuffer.add(cell);
				}
			}
			else if(REDMaxThresh < aveInt){
				System.out.println("(REDSIM) Simulation detected a cell drop.");
				return false;
			}
			else{
				System.out.println("(REDSIM)Ave Queue Size: " + aveInt + " (< RED Min Threshold)");
				virtualOutputBuffer.add(cell);
			}
		}
		
		//if we made it here, the packet is good to go.
		System.out.println("(REDSIM) Packet passed RED simulation.");
		packetAdmitted = true;
		return packetAdmitted;
	}

	/**
	 * Sets that the nic should use Tail drop when deciding weather or not to add cells to the queue
	 * @since 1.0
	 */
	public void setIsTailDrop(){
		this.red=false;
		this.tail=true;
		this.ppd=false;
		this.epd=false;
	}

	/**
	 * Sets that the nic should use RED when deciding weather or not to add cells to the queue
	 * @since 1.0
	 */
	public void setIsRED(){

		this.REDAvg = 0;

		this.red=true;
		this.tail=false;
		this.ppd=false;
		this.epd=false;
	}

	/**
	 * Sets that the nic should use PPD when deciding weather or not to add cells to the queue
	 * @since 1.0
	 */
	public void setIsPPD(){
		this.droppingAPacket = false;
		this.red=false;
		this.tail=false;
		this.ppd=true;
		this.epd=false;
	}

	/**
	 * Sets that the nic should use EPD when deciding weather or not to add cells to the queue
	 * @since 1.0
	 */
	public void setIsEPD(){
		this.red=false;
		this.tail=false;
		this.ppd=false;
		this.epd=true;
	}

	/**
	 * This method connects a link to this nic
	 * @param link the link to connect to this nic
	 * @since 1.0
	 */
	public void connectOtoOLink(OtoOLink link){
		this.link = link;
	}

	/**
	 * This method is called when a cell is received over the link that this nic is connected to
	 * @param cell the cell that was received
	 * @since 1.0
	 */
	public void receiveCell(ATMCell cell){
		this.inputBuffer.add(cell);

	}

	/**
	 * Moves the cells from the output buffer to the line (then they get moved to the next nic's input buffer)
	 * @since 1.0
	 */
	public void clearOutputBuffers(){
		for(int i=0; i<this.outputBuffer.size(); i++)
			this.link.sendCell(this.outputBuffer.get(i), this);
		this.outputBuffer.clear();
	}

	/**
	 * Moves cells from this nics input buffer to its output buffer
	 * @since 1.0
	 */
	public void clearInputBuffers(){
		for(int i=0; i<this.inputBuffer.size(); i++)
			this.parent.receiveCell(this.inputBuffer.get(i), this);
		this.inputBuffer.clear();
	}
}
