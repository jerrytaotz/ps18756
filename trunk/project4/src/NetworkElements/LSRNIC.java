package NetworkElements;

import DataTypes.*;
import java.util.*;

public class LSRNIC {
	private int REDMaxThresh = 100; // the maximum number of packets in the output buffer
	private int REDMinThresh = 20; // the minimum number of packets in the output buffer before we start dropping packets
	private int lineRate = 50;  //number of packets we can send during each time interval
	private int availableBW = lineRate;
	private boolean trace = false; // should we print out debug statements?
	private LSR parent; // The router or computer that this nic is in
	private OtoOLink link; // The link connected to this nic
	private ArrayList<Packet> inputBuffer = new ArrayList<Packet>(); // Where packets are put between the parent and nic
	private ArrayList<Packet> outputBuffer = new ArrayList<Packet>(); // Where packets are put to be sent
	private ArrayList<FIFOQueue> DSQueues;
	private WRRScheduler scheduler;
	private QoSMonitor monitor = null;
	
	public final static int EF = 0;
	public final static int AF1 = 1;
	public final static int AF2 = 2;
	public final static int AF3 = 3;
	public final static int AF4 = 4;
	public final static int BE = 5;
	
	/**
	 * Default constructor for an ATM NIC
	 * @param parent
	 * @since 1.0
	 */
	public LSRNIC(LSR parent){
		this.parent = parent;
		this.parent.addNIC(this);
		DSQueues = new ArrayList<FIFOQueue>();
		/*EF/AF don't get any weight until it is requested.*/
		DSQueues.add(EF,new FIFOQueue(0));
		DSQueues.add(AF1,new FIFOQueue(0));
		DSQueues.add(AF2,new FIFOQueue(0));
		DSQueues.add(AF3,new FIFOQueue(0));
		DSQueues.add(AF4,new FIFOQueue(0));
		/*start off by giving the BE queue all the bandwidth*/
		DSQueues.add(BE,new FIFOQueue(lineRate));
		scheduler = new WRRScheduler(this);
	}
	
	/**
	 * This method is called when a packet is passed to this nic to be sent. The packet is placed
	 * in an output buffer until a time unit passes
	 * @param currentPacket the packet to be sent (placed in the buffer)
	 * @param parent the router the packet came from
	 * @since 1.0
	 */
	public void sendPacket(Packet currentPacket, LSR parent){
		FIFOQueue DSQueue;
		
		if(this.trace){
			System.out.println("Trace (LSR NIC): Received packet");
			if(this.link==null)
				System.out.println("Error (LSR NIC): You are trying to send a packet through a nic not connected to anything");
			if(this.parent!=parent)
				System.out.println("Error (LSR NIC): You are sending data through a nic that this router is not connected to");
			if(currentPacket==null)
				System.out.println("Warning (LSR NIC): You are sending a null packet");
		}
		/*determine which queue this packet needs to go to*/
		DSQueue = DSQueues.get(classifyPacket(currentPacket));
		this.runRED(currentPacket,DSQueue);
	}
	
	/**
	 * Will reserve the specified resources 
	 * @param PHB
	 * @param afClass
	 * @param tspec
	 * @return true if the reservation was successful
	 * false if the reservation failed.
	 */
	public boolean reserveBW(int PHB,int afClass,int tspec){
		/*are there enough resources to accomodate this request?*/
		if(tspec > this.availableBW){
			System.out.println("PATHERR: Router " + parent.getAddress() +
			"cannot accomodate RESV request. {Requested: " + tspec + ",Available: " + availableBW +
			"}");
			System.exit(0);
		}
		
		switch(PHB){
		case Constants.PHB_EF:
			DSQueues.get(EF).increaseWeight(tspec);
			break;
		case Constants.PHB_AF:
			if(afClass == 1){
				DSQueues.get(AF1).increaseWeight(tspec);
				break;
			}
			else if(afClass == 2){
				DSQueues.get(AF2).increaseWeight(tspec);
				break;
			}
			else if(afClass == 3){
				DSQueues.get(AF3).increaseWeight(tspec);
				break;
			}
			else if(afClass == 4){
				DSQueues.get(AF4).increaseWeight(tspec);
				break;
			}
			else{
				System.out.println("(Router " + parent.getAddress() + "): " +
				"received an invalid PHB AF class.");
				return false;
			}
		case Constants.PHB_BE:
			/*do nothing.  BE doesn't get to reserve BW*/
			System.out.println("RESV: NIC on router " + parent.getAddress() +
					" fulfilled RESV request. {Requested: " + tspec + ",Now Available: " + availableBW +
					"}");
			return true;
		default:
			System.out.println("(Router " + parent.getAddress() + "): " +
					"received an invalid PHB.");
		}
		/*siphon off the BW from the BE class and update the available BW*/
		DSQueues.get(BE).decreaseWeight(tspec);
		availableBW -= tspec;
		System.out.println("RESV: NIC on router " + parent.getAddress() +
				" fulfilled RESV request. {Requested: " + tspec + ",Now Available: " + availableBW +
				"}");
		return true;
	}
	
	/**
	 * Determines the DiffServ type of a packet based on the DSCP.
	 * @param p
	 * @return EF for EF packets
	 * AF1 for AF1 packets
	 * AF2 for AF2 packets
	 * AF3 for AF3 packets
	 * AF4 for AF4 packets
	 * BE for BE packets
	 * -1 if the packet has an unknown DSCP type.
	 */
	private int classifyPacket(Packet p){
		int afClass;
		
		if(p.isRSVP()){
			/*place signaling packets in AF1*/
			return AF1;
		}
		else{
			switch(p.classifyDSCP()){
			case Constants.PHB_EF:	
				return EF;
			case Constants.PHB_AF:
				afClass = p.getAFClass();
				if(afClass == 1) return AF1;
				if(afClass == 2) return AF2;
				if(afClass == 3) return AF3;
				if(afClass == 4) return AF4;
			case Constants.PHB_BE:
				return BE;
			default:
				System.out.println("(Router " + parent.getAddress() + "): " +
						"Received an packet with an unknown DSCP type.");
				return -1;
			}
		}
	}
	
	/**
	 * Runs Random early detection on the packet
	 * @param currentPacket the packet to be added/dropped from the queue
	 * @since 1.0
	 */
	private void runRED(Packet currentPacket,FIFOQueue q){
		boolean packetDropped = false;
		double dropProbability = 0.0;
		Random rnd = new Random();
		double aveInt;
		int sidesOfADie; //used to "roll a die" to see if packet gets dropped or not
		Packet droppedPacket;
		
		/*just use the actual queue size rather than an ave.*/
		aveInt = q.getNumPackets();
		
		/*If the average is between the min and max thresh*/
		if(aveInt > REDMinThresh && aveInt < REDMaxThresh){
			/*
			 *Create a "die" to roll for this packet.
			 *the die has less sides the closer we get to the max thresh.
			 */
			sidesOfADie = (REDMaxThresh + 1) - (int)aveInt;
			dropProbability = 1.0/(double)sidesOfADie;
			/*roll the die. Drop packet on a 1*/
			if((rnd.nextInt(sidesOfADie) + 1) == 1){
				packetDropped = true;
			}
			else{
				outputBuffer.add(currentPacket);
			}
		}
		else if(aveInt > REDMaxThresh){
			/*If this is an RSVP cell we need to force it in by evicting some other cell*/
			if(!currentPacket.isRSVP()){
				packetDropped = true;
				dropProbability = 1.0;
			}
		}
		
		/*insert the packet in it's queue if it passed*/
		if(packetDropped == false){
			q.insert(currentPacket);
			return;
		}
		else{
			/*if RED says we need to drop something, see if any lesser priority can be dropped*/
			droppedPacket = q.tryToEvict(currentPacket);
			if(this.monitor != null) monitor.notifyDrop(currentPacket);
			System.out.println("RED: NIC on Router " + this.parent.getAddress() + " dropped packet " +
					droppedPacket.getID() + " DSCP " + currentPacket.getDSCP() + " with probability " 
					+ dropProbability);
		}
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
	 * This method is called when a packet is received over the link that this nic is connected to
	 * @param currentPacket the packet that was received
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket){
		this.inputBuffer.add(currentPacket);

	}
	
	/**
	 * Moves the packets from the output buffer to the line 
	 * (then they get moved to the next nic's input buffer)
	 * @since 1.0
	 */
	public void sendPackets(){
		/*run the scheduler on the DiffServ queues*/
		this.scheduler.nextRound(DSQueues, outputBuffer);
		
		for(int i=0; i<Math.min(lineRate,this.outputBuffer.size()); i++)
			this.link.sendPacket(this.outputBuffer.get(i), this);
		ArrayList<Packet> temp = new ArrayList<Packet>();
		for(int i=Math.min(lineRate,this.outputBuffer.size()); i<this.outputBuffer.size(); i++)
			temp.add((Packet)this.outputBuffer.get(i));
		this.outputBuffer.clear();
		this.outputBuffer=temp;
	}
	
	/**
	 * Moves packets from this nic's input buffer to its output buffer
	 * @since 1.0
	 */
	public void recievePackets(){
		for(int i=0; i<this.inputBuffer.size(); i++)
			this.parent.receivePacket(this.inputBuffer.get(i), this);
		this.inputBuffer.clear();
	}
	
	/**
	 * Retrieve the parent LSR of this NIC.  Useful for updating the network map
	 * automatically whenever two nodes are linked.
	 * @return this nodes parent.
	 */
	public LSR getParent(){
		return this.parent;
	}
	
	/**
	 * Tells you how much bandwidth is available for allocation
	 * @return the current remaining bandwidth
	 */
	public int getAvailableBW(){
		return availableBW;
	}
	
	/**
	 * Returns the address of the router on the other end of the link.
	 * @return the address of the machine on the other end.
	 */
	public int getNeighborAddress(){
		return link.getNeighborAddress(this);
	}
	
	/**
	 * Sets the QoS monitor for this NIC.
	 * @param monitor
	 */
	public void setQoSMonitor(QoSMonitor monitor){
		this.monitor = monitor;
	}
}
