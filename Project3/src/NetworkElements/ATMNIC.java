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
	
	private int REDMinThresh = 10; //The minimum number of cells in the output buffer before we start dropping cells
	private int REDMaxThresh = 20; //The maximum average number of cells to allow in the output buffer.
	private int REDSumOfSizes;
	private int count; //count is total # of packets ever in queue
	private double REDAvg; //average queue size for RED
	
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
		else if(this.red) this.runRED(cell);
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
		else cellDropped = true;
		
		// Output to the console what happened
		if(cellDropped)
			System.out.println("The cell " + cell.getTraceID() + " was tail dropped");
		else
			if(this.trace)
			System.out.println("The cell " + cell.getTraceID() + " was added to the output queue");
	}
	
	/**
	 * Runs Random early detection on the cell
	 * @param cell the cell to be added/dropped from the queue
	 * @since 1.0
	 */
	private void runRED(ATMCell cell){
		Random rnd = new Random();
		boolean cellDropped = false;
		double dropProbability = 0.0;
		double aveInt;
		int sidesOfADie; //used to "roll a die" to see if packet gets dropped or not
		
		//calculate the average queue size
		System.out.println("REDSumOfSizes" + REDSumOfSizes);
		this.REDAvg = (double)REDSumOfSizes/(double)++count;
		aveInt = Math.rint(REDAvg);
		
		//If the average is between the min and max thresh
		if(REDMaxThresh>aveInt && REDMinThresh < aveInt){
			//calculate the drop probability
			dropProbability = (aveInt - (double)REDMinThresh)/
				(double)(REDMaxThresh - REDMinThresh);
			//Create a "die" to roll for this packet
			sidesOfADie = (int)Math.rint(1.0/dropProbability);
			System.out.println("Ave Queue Size: " + aveInt + " (in prob. drop zone).");
			System.out.println("Drop Prob: 1/" + sidesOfADie);
			//1 in sidesOfADie probability that packet gets dropped.
			if((rnd.nextInt(sidesOfADie) + 1) == 1){
				cellDropped = true;
			}
			else{
				outputBuffer.add(cell);
				REDSumOfSizes += outputBuffer.size();
			}
		}
		else if(REDMaxThresh < aveInt){
			System.out.println("Ave Queue Size: " + aveInt + " (> RED Max Threshold)");
			cellDropped = true;
		}
		else{
			System.out.println("Ave Queue Size: " + aveInt + " (< RED Min Threshold)");
			outputBuffer.add(cell);
			REDSumOfSizes += outputBuffer.size();
		}
		
		// Output to the console what happened
		if(cellDropped)
			System.out.println("The cell " + cell.getTraceID() + " was dropped with probability " + dropProbability);
		else
			if(this.trace)
			System.out.println("The cell " + cell.getTraceID() + " was added to the output queue");
	}
	
	/**
	 * Runs Partial packet drop on the cell
	 * @param cell the cell to be added/dropped from the queue
	 * @since 1.0
	 */
	private void runPPD(ATMCell cell){
		boolean cellDropped = false;
		
		outputBuffer.add(cell);
		
		// Output to the console what happened
		if(cellDropped)
			System.out.println("The cell " + cell.getTraceID() + " was dropped");
		else
			if(this.trace)
			System.out.println("The cell " + cell.getTraceID() + " was added to the output queue");
	}
	
	/**
	 * Runs Early packet drop on the cell
	 * @param cell the cell to be added/dropped from the queue
	 * @since 1.0
	 */
	private void runEPD(ATMCell cell){
		boolean cellDropped = false;
		
		outputBuffer.add(cell);
		
		// Output to the console what happened
		if(cellDropped)
			System.out.println("The cell " + cell.getTraceID() + " was dropped");
		else
			if(this.trace)
			System.out.println("The cell " + cell.getTraceID() + " was added to the output queue");
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
		this.count = 0;
		this.REDSumOfSizes = 0;
		
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
