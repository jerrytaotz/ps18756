package NetworkElements;

import java.util.*;
import java.awt.image.LookupTable;
import java.net.*;
import DataTypes.*;

public class IPRouter implements IPConsumer{
	private ArrayList<IPNIC> nics = new ArrayList<IPNIC>();
	private HashMap<Inet4Address, IPNIC> forwardingTable = new HashMap<Inet4Address, IPNIC>();
	private int time = 0,currNIC = 0;
	private Boolean fifo=true, rr=false, wrr=false, wfq=false, routeEntirePacket=true;
	private HashMap<IPNIC, FIFOQueue> inputQueues = new HashMap<IPNIC, FIFOQueue>();
	private int lastNicServiced=-1, weightFulfilled=0;
	// remembering the queue rather than the interface number is useful for wfq
	private FIFOQueue lastServicedQueue = null;
	private FIFOQueue centralFIFOQueue = null;
	private double virtualTime = 0.0;
	
	/**
	 * The default constructor of a router
	 */
	public IPRouter(){
		
	}
	
	/**
	 * adds a forwarding address in the forwarding table
	 * @param destAddress the address of the destination
	 * @param nic the nic the packet should be sent on if the destination address matches
	 */
	public void addForwardingAddress(Inet4Address destAddress, IPNIC nic){
		forwardingTable.put(destAddress, nic);
	}
	
	/**
	 * receives a packet from the NIC
	 * @param packet the packet received
	 * @param nic the nic the packet was received on
	 */
	public void receivePacket(IPPacket packet, IPNIC nic){
		//this.forwardPacket(packet);

		//Place the packet in it's input queue.
		inputQueues.get(nic).offer(packet);
		
		//If we are using FIFO, move the packets to the central queue.
		if(this.fifo){
			this.centralFIFOQueue.offer(packet);
		}
		
		// If wfq set the expected finish time
		if(this.wfq){
			
		}
	}
	
	public void forwardPacket(IPPacket packet){
		forwardingTable.get(packet.getDest()).sendIPPacket(packet);
	}
	
	public void routeBit(){
		/*
		 *  FIFO scheduler
		 */
		if(this.fifo) this.fifo();
			
		
		/*
		 *  RR scheduler
		 */
		if(this.rr) this.rr();
			
		
		/*
		 *  WRR scheduler
		 */
		if(this.wrr) this.wrr();
			
		
		/*
		 * WFQ scheduler
		 */
		if(this.wfq) this.wfq();
	}
	
	/**
	 * Perform FIFO scheduling on the queue
	 */
	private void fifo(){
		IPPacket readyPacket;
		this.centralFIFOQueue.tock();
		//route a single bit from the central queue
		
		this.centralFIFOQueue.routeBit();
		//check if the queue has any ready packets.
		readyPacket = centralFIFOQueue.ready();
		if(readyPacket != null){
			this.forwardPacket(readyPacket);
		}
	}
	
	/**
	 * Perform packet based round robin scheduling on the input queues
	 */
	private void rr(){
		if(nics.size() > 0)
		{
			if(this.routeEntirePacket == false)
				bbrr(); //perform bit by bit round robin
			else{
				packetRoundRobin();
			}
		}	
	}
	
	/**
	 * perform packetwise round robin
	 */
	private void packetRoundRobin(){
		IPPacket readyPacket = null;
		if(lastServicedQueue == null){
			lastServicedQueue = findNextServiceableQueue();
		}
		//check if we actually found a queue with something to send
		if(lastServicedQueue != null){
			//route the next bit in the current packet
			lastServicedQueue.routeBit();
			//see if the current packet is ready
			readyPacket = lastServicedQueue.ready();
			if(readyPacket != null){
				 this.forwardPacket(readyPacket);
				//move on to the next queue
				 this.currNIC = (currNIC + 1) % nics.size();
				 lastServicedQueue = findNextServiceableQueue();
			}
		}
		//if no queue had any packets, silently exit
	}
	
	/**
	 * do bit by bit round robin
	 */
	private void bbrr(){
		FIFOQueue nextQueue = null;
		IPNIC nextNIC = null;
		
		//get the next occupied queue and skip over the others
		for(int i = 0; i < nics.size(); i++ ){
			//Get the next NIC which has a packet to route.
			nextNIC = nics.get(currNIC);
			nextQueue = inputQueues.get(nextNIC);
			//if we have a non empty queue we can route the bit and break out
			if(nextQueue.peek() != null){
				nextQueue.routeBit();
				break;
			}
			else currNIC = (currNIC + 1) % nics.size();
		}
		
		//if we were able to find a bit to route, see if it completed a packet.
		if(nextQueue != null){
			//check if the current queue has any ready packets.
			IPPacket readyPacket = nextQueue.ready();
			if(readyPacket != null){
				this.forwardPacket(readyPacket);
			}
		}
		
		//Move on to next NIC or loop back to the beginning of the list.
		currNIC = (currNIC + 1) % nics.size();
	}
	
	/**
	 * Perform weighted round robin on the queue
	 */
	private void wrr(){
		if(this.routeEntirePacket == false){
			this.bwrr();
		}
	}
	
	/**
	 * perform bitwise weighted round robin
	 */
	private void bwrr(){
		IPPacket readyPacket = null;
		
		if(this.lastServicedQueue == null){
			lastServicedQueue = this.findNextServiceableQueue();
		}
		//make sure we actually found a queue with a packet to send
		if(this.lastServicedQueue != null){
			lastServicedQueue.routeBit();
			this.weightFulfilled++;
			readyPacket = lastServicedQueue.ready();
			if(readyPacket != null){
				this.forwardPacket(readyPacket);
			}
			//Check if we the weight obligation for this queue is fulfilled.
			if(this.weightFulfilled == lastServicedQueue.getWeight()){
				this.weightFulfilled = 0;
				//update the active queue.
				currNIC = (currNIC + 1) % nics.size();
				lastServicedQueue = this.findNextServiceableQueue();
			}
		}
	}
	
	/**
	 * Perform weighted fair queuing on the queue
	 */
	private void wfq(){

	}
	
	/**
	 * adds a nic to the consumer 
	 * @param nic the nic to be added
	 */
	public void addNIC(IPNIC nic){
		this.nics.add(nic);
	}
	
	/**
	 * sets the weight of queues, used when a weighted algorithm is used.
	 * Example
	 * Nic A = 1
	 * Nic B = 4
	 * 
	 * For every 5 bits of service, A would get one, B would get 4.
	 * @param nic the nic queue to set the weight of
	 * @param weight the weight of the queue
	 */
	public void setQueueWeight(IPNIC nic, int weight){
		if(this.inputQueues.containsKey(nic))
			this.inputQueues.get(nic).setWeight(weight);
		
		else System.err.println("(IPRouter) Error: The given NIC does not have a queue associated with it");
	}
	
	/**
	 * moves time forward 1 millisecond
	 */
	public void tock(){
		this.time+=1;
		
		// Add 1 delay to all packets in queues
		ArrayList<FIFOQueue> delayedQueues = new ArrayList<FIFOQueue>();
		for(Iterator<FIFOQueue> queues = this.inputQueues.values().iterator(); queues.hasNext();){
			FIFOQueue queue = queues.next();
			if(!delayedQueues.contains(queue)){
				delayedQueues.add(queue);
				queue.tock();
			}
		}
		
		// calculate the new virtual time for the next round
		if(this.wfq){
			
		}
		
		// route bit for this round
		this.routeBit();
	}
	
	/**
	 * Finds the next queue which has something to transmit.
	 * @return the next queue which has a packet in it, if there is one. null otherwise
	 */
	private FIFOQueue findNextServiceableQueue(){
		FIFOQueue nextQueue = null;
		IPNIC nextNIC = null;
		
		//get the next occupied queue and skip over the others
		for(int i = 0; i < nics.size(); i++ ){
			//Get the next NIC which has a packet to route.
			nextNIC = nics.get(currNIC);
			nextQueue = inputQueues.get(nextNIC);
			//if we have a non empty queue we can route the bit and break out
			if(nextQueue.peek() != null){
				return nextQueue;
			}
			else currNIC = (currNIC + 1) % nics.size();
		}
		return nextQueue;
	}
	/**
	 * set the router to use FIFO service
	 */
	public void setIsFIFO(){
		this.fifo = true;
		this.rr = false;
		this.wrr = false;
		this.wfq = false;
		
		// Setup router for FIFO under here
		//Setup a single queue for all incoming packets to go to.
		this.centralFIFOQueue = new FIFOQueue();
	}
	
	/**
	 * set the router to use Round Robin service (packet based)
	 */
	public void setIsRoundRobin(){
		this.fifo = false;
		this.rr = true;
		this.wrr = false;
		this.wfq = false;
		
		// Setup router for Round Robin under here
		//set up an input queue on each NIC
		for(Iterator<IPNIC> it = nics.iterator();it.hasNext();)
		{
			//Add a new queue to the NIC
			inputQueues.put(it.next(), new FIFOQueue());
		}		
	}
	
	/**
	 * sets the router to use weighted round robin service
	 */
	public void setIsWeightedRoundRobin(){
		this.fifo = false;
		this.rr = false;
		this.wrr = true;
		this.wfq = false;
		
		// Setup router for Weighted Round Robin under here
		//set up an input queue on each NIC
		for(Iterator<IPNIC> it = nics.iterator();it.hasNext();)
		{
			//Add a new queue to the NIC
			inputQueues.put(it.next(), new FIFOQueue());
		}	
	}
	
	/**
	 * sets the router to use weighted fair queuing
	 */
	public void setIsWeightedFairQueuing(){
		this.fifo = false;
		this.rr = false;
		this.wrr = false;
		this.wfq = true;
		
		// Setup router for Weighted Fair Queuing under here
		
	}
	
	/**
	 * sets if the router should route bit-by-bit, or entire packets at a time
	 * @param	routeEntirePacket if the entire packet should be routed
	 */
	public void setRouteEntirePacket(Boolean routeEntirePacket){
		this.routeEntirePacket=routeEntirePacket;
	}
}
