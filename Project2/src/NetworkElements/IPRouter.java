package NetworkElements;

import java.util.*;
import java.awt.image.LookupTable;
import java.net.*;
import DataTypes.*;

/**
 * The IPRouter class is the abstraction of a physical IP router.
 * It maintains an input queue for each of its interfaces and has the capability
 * to be configured to use several different queuing algorithms.
 * @author Prof. Hyong Kim, modified by Brady Tello
 */
public class IPRouter implements IPConsumer{
	private ArrayList<IPNIC> nics = new ArrayList<IPNIC>();
	private HashMap<Inet4Address, IPNIC> forwardingTable = new HashMap<Inet4Address, IPNIC>();
	private HashMap<IPNIC, FIFOQueue> inputQueues = new HashMap<IPNIC, FIFOQueue>();
	private double virtualTime = 0.0;
	private int time = 0,currNIC = 0;
	private int lastNicServiced=-1, weightFulfilled=0;
	private Boolean fifo=true, rr=false, wrr=false, wfq=false, routeEntirePacket=true;
	private Boolean fulfillingPacket = false, packetFulfilled = false;
	// remembering the queue rather than the interface number is useful for wfq
	private FIFOQueue lastServicedQueue = null;
	private FIFOQueue centralFIFOQueue = null;

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
		FIFOQueue q; 
		
		//If we are using FIFO, move the packets to the central queue and return.
		if(this.fifo){
			this.centralFIFOQueue.offer(packet);
			return;
		}
		
		q = inputQueues.get(nic);
		//Place the packet in it's input queue.
		q.offer(packet);

		// If wfq set the expected finish time
		if(this.wfq){
			//Let's see if there are any packets ahead of this guy in the queue.
			IPPacket lastPacket = q.secondLastPeek();
			//0.0 will allow the virtualTime to dominate 
			//in max() if no other packets are present
			double lastFinishTime = 0.0;
			if(lastPacket != null)  lastFinishTime = lastPacket.getFinishTime();
			packet.setFinishTime(
					(double)packet.getSize()/(double)q.getWeight()
					+ Math.max(virtualTime, lastFinishTime));
		}
	}
	
	/**
	 * Forwards a packet on the interface mapped by its destination address
	 * @param packet the packet to be forwarded
	 */
	public void forwardPacket(IPPacket packet){
		forwardingTable.get(packet.getDest()).sendIPPacket(packet);
	}
	
	/**
	 * Route a single bit from a queue.  This method will behave differently
	 * depending on which of the various queueing disciplines has been selected.
	 * The various bit routing methods routeBit() can use are fifo() rr() wrr() 
	 * and wfq() 
	 */
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
	 * Perform packetwise round robin
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
	 * Do bit by bit round robin queuing
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
	 * Perform weighted round robin on the queue.  This function is used as a wrapper
	 * for either the bitwise or the packetwise versions of the weighted round robin 
	 * queuing methods.
	 */
	private void wrr(){
		if(this.routeEntirePacket == false){
			this.bwrr();
		}
		else this.pwrr();
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
	 * Perform packetwise weighted round robin
	 */
	private void pwrr(){
		IPPacket readyPacket = null;
		
		if(this.lastServicedQueue == null){
			lastServicedQueue = this.findNextServiceableQueue();
		}
		//make sure we actually found a queue with a packet to send
		if(this.lastServicedQueue != null){
			lastServicedQueue.routeBit();
			//increment the weight fulfilled if we are still working on bitwise obligation.
			if(!this.fulfillingPacket) this.weightFulfilled++;
			readyPacket = lastServicedQueue.ready();
			if(readyPacket != null){
				this.forwardPacket(readyPacket);
				this.packetFulfilled = true;
			}
			else this.packetFulfilled = false;
			//Check if we the weight obligation for this queue is fulfilled.
			if(this.weightFulfilled == lastServicedQueue.getWeight()){
				//if we have completed a packet this round, move on to the next queue
				if(this.packetFulfilled){
					this.weightFulfilled = 0;
					this.fulfillingPacket = false;
					//update the active queue.
					currNIC = (currNIC + 1) % nics.size();
					lastServicedQueue = this.findNextServiceableQueue();
				}
				else this.fulfillingPacket = true;
			}
		}
	}
	
	/**
	 * Perform weighted fair queuing on the queue
	 */
	private void wfq(){
		IPPacket readyPacket = null,headPacket = null;
		//Will be used to determine which queue gets to go next
		double minFin = Double.MAX_VALUE;
		
		//If the router is not currently in the middle of another packet
		if(!fulfillingPacket){
			//pick the queue containing the packet with the lowest finish time.
			for(FIFOQueue q:inputQueues.values()){
				headPacket = q.peek();
				if(headPacket != null){
					if(headPacket.getFinishTime() < minFin){
						minFin = headPacket.getFinishTime();
						this.lastServicedQueue = q;
					}
				}
			}
			//Tell the router it is busy with a packet again.
			fulfillingPacket = true;
		}
		//send a bit
		lastServicedQueue.routeBit();
		//check for a ready packet
		readyPacket = lastServicedQueue.ready();
		//If there was a packet ready for routing then send it
		if(readyPacket != null){
			forwardPacket(readyPacket);
			//tell the router it can pick the next available queue
			fulfillingPacket = false;
		}
	}
	
	/**
	 * Calculate the sum over the weights of each input queue
	 * @return the sum of the weights of each input queue.
	 */
	private double sumOfWeights(){
		double sumOfWeights = 0;
		for(FIFOQueue q:inputQueues.values()){
			if(q.peek() != null)
				sumOfWeights += q.getWeight();
		}
		return sumOfWeights;
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
			//and update the virtual time.
			//(NOTE: line speed is 1 bit per clock cycle)
			virtualTime += 1/sumOfWeights();
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
		//set up an input queue on each NIC
		for(Iterator<IPNIC> it = nics.iterator();it.hasNext();)
		{
			//Add a new queue to the NIC
			inputQueues.put(it.next(), new FIFOQueue());
		}	
	}
	
	/**
	 * sets if the router should route bit-by-bit, or entire packets at a time
	 * @param	routeEntirePacket if the entire packet should be routed
	 */
	public void setRouteEntirePacket(Boolean routeEntirePacket){
		this.routeEntirePacket=routeEntirePacket;
	}
}
