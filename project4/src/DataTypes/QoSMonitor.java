package DataTypes;
import java.util.*;
/**
 * This class will be 'plugged in' to all LSRs and LSRNICs in a network in
 * order to gather all relevant QoS data.  Collects dropped packets
 * and latency numbers and uses those figures to calculate average latency,
 * drop rate, average jitter, and average throughput in a DiffServ enabled
 * MPLS network.  This is not an active monitor.  Elements must inform it of
 * events such as packet drop and receipt.
 * @author Brady
 */

public class QoSMonitor {
	HashMap<Integer,Integer> dropCounts;
	HashMap<Integer,Integer> numSent;
	HashMap<Integer,ArrayList<Integer>> delays; //for measuring jitter
	
	public QoSMonitor(){
		dropCounts = new HashMap<Integer,Integer>();
		numSent = new HashMap<Integer,Integer>();
		delays = new HashMap<Integer, ArrayList<Integer>>();
		/*provide a list for each of the DSCPs we are concerned with.*/
		delays.put(Constants.DSCP_EF, new ArrayList<Integer>());
		delays.put(Constants.DSCP_AF11, new ArrayList<Integer>());
		delays.put(Constants.DSCP_AF12, new ArrayList<Integer>());
		delays.put(Constants.DSCP_AF21, new ArrayList<Integer>());
		delays.put(Constants.DSCP_AF22, new ArrayList<Integer>());
		delays.put(Constants.DSCP_AF31, new ArrayList<Integer>());
		delays.put(Constants.DSCP_AF32, new ArrayList<Integer>());
		delays.put(Constants.DSCP_BE, new ArrayList<Integer>());
		
		dropCounts.put(Constants.DSCP_EF,0);
		dropCounts.put(Constants.DSCP_AF11, 0);
		dropCounts.put(Constants.DSCP_AF12, 0);
		dropCounts.put(Constants.DSCP_AF21, 0);
		dropCounts.put(Constants.DSCP_AF22, 0);
		dropCounts.put(Constants.DSCP_AF31, 0);
		dropCounts.put(Constants.DSCP_AF32, 0);
		dropCounts.put(Constants.DSCP_BE, 0);
		
		numSent.put(Constants.DSCP_EF,0);
		numSent.put(Constants.DSCP_AF11,0);
		numSent.put(Constants.DSCP_AF12,0);
		numSent.put(Constants.DSCP_AF21,0);
		numSent.put(Constants.DSCP_AF22,0);
		numSent.put(Constants.DSCP_AF31,0);
		numSent.put(Constants.DSCP_AF32,0);
		numSent.put(Constants.DSCP_BE,0);	
	}
	
	/**
	 * notify this monitor that a packet was received by the destination
	 * @param p
	 */
	public void notifyReceive(Packet p){
		ArrayList<Integer> delayList = delays.get(p.getDSCP());
		delayList.add(p.getDelay());
	}
	
	/**
	 * notify this monitor that a packet was sent
	 * @param p
	 */
	public void notifySend(Packet p){
		int currNumSent = numSent.get(p.getDSCP());
		currNumSent++;
	}
	
	/**
	 * Notifies this monitor about a packet drop occurring in the network.
	 * @param p
	 */
	public void notifyDrop(Packet p){
		int currDropCount = dropCounts.get(p.getDSCP());
		currDropCount++;
	}
	
	/**
	 * Runs all of the functions which calculate the various network metrics and
	 * prints the results to the terminal.
	 */
	public void calculateQoS(){
	
		printQoSLine("EF",Constants.DSCP_EF);
		printQoSLine("AF11",Constants.DSCP_AF11);
		printQoSLine("AF12",Constants.DSCP_AF12);
		printQoSLine("AF21",Constants.DSCP_AF21);
		printQoSLine("AF22",Constants.DSCP_AF22);
		printQoSLine("AF31",Constants.DSCP_AF31);
		printQoSLine("AF32",Constants.DSCP_AF32);
		printQoSLine("BE",Constants.DSCP_BE);
	}
	
	/**
	 * Prints a line of QoS information for the specified traffic class.  Should only
	 * be run after all data has been collected.
	 * @param tType string representation of the traffic class 'DSCP'.
	 * @param DSCP the actual traffic class.
	 */
	private void printQoSLine(String tType, int DSCP){
		double latency;
		double jitter;
		double tput;
		double dropRate;
		
		latency = calculateLatency(DSCP);
		jitter = calculateJitter(latency,DSCP);
		dropRate = calculateDropRate(DSCP);
		tput = calculateThroughput(dropRate, DSCP);
	
		System.out.println(tType + ":Latency - " + latency + " ms:Jitter - " + jitter + 
				" ms:Throughput - " + tput + " %:Drop Rate - " + dropRate + "%:");
	}
	
	/**
	 * calculate the average jitter for a specific traffic class
	 * @param meanLatency
	 * @return
	 */
	private double calculateJitter(double meanLatency,int DSCP){
		double meanJitter = 0.0;
		double sum = 0.0;
		ArrayList<Integer> classDelayList = delays.get(DSCP);
		
		/*calculate all the jitters*/
		for(int delay:classDelayList){
			sum += Math.pow((double)(delay-meanLatency), 2.0);
		}
		return meanJitter/classDelayList.size();
	}
	
	/**
	 * calculate the average throughput for a given traffic class
	 * @param meanDropRate the mean drop rate for the traffic class.
	 * @param DSCP the traffic class you are interested in
	 * @return the average throughput for class 'DSCP'
	 */
	private double calculateThroughput(double meanDropRate,int DSCP){
		return (1.0 - meanDropRate);
	}
	
	/**
	 * calculate the average latency for a specific traffic class.
	 * @param DSCP the traffic class you are interested in
	 * @return the average latency for class 'DSCP'
	 */
	private double calculateLatency(int DSCP){
		double sum = 0.0;
		double meanLatency = 0.0;
		ArrayList<Integer> classDelayList = delays.get(DSCP);
		
		/*calculate all the latencies*/
		for(int delay:classDelayList){
			sum += delay;
		}
		return meanLatency/classDelayList.size();
	}
	
	/**
	 * Calculate the drop rate for a given traffic class.
	 * @param DSCP the traffic class you are interested in
	 * @return
	 */
	private double calculateDropRate(int DSCP){
		return dropCounts.get(DSCP)/numSent.get(DSCP);
	}
}
