package NetworkElements;

import java.util.*;

import dijkstra.*;
import DataTypes.*;

public class LSR{

	private int address; // The AS address of this router
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	private PathCalculator pc;
	private HashMap<Integer, LSRNIC> routingTable; //The IP routing table for this LSR
	private LabelTable labelTable = new LabelTable();
	//private TreeMap<Integer, NICLabelPair> labelTable; // a map of input label to output nic and label
	private ArrayList<Packet> waitingPackets;
	private QoSMonitor monitor = null;

	//TODO: Write a smoother
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
		pc = new PathCalculator();
		//labelTable = new TreeMap<Integer, NICLabelPair>();
		waitingPackets = new ArrayList<Packet>();
		this.routingTable = new HashMap<Integer,LSRNIC>();
	}

	/**
	 * Return this router's address
	 * @since 1.0
	 */
	public int getAddress(){
		return this.address;
	}

	/**
	 * Adds a nic to this router
	 * @param nic the nic to be added
	 * @since 1.0
	 */
	public void addNIC(LSRNIC nic){
		this.nics.add(nic);
	}

	/**
	 * This method processes data and RSVP cells that arrive from any nic
	 * @param currentPacket the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket, LSRNIC nic){
		if(currentPacket.isRSVP()){
			processRSVP(currentPacket,nic);
		}
		//if destined for this router, hold on to it
		else if(currentPacket.getDest() == this.address){
			if(monitor != null) monitor.notifyReceive(currentPacket);
			receivedData(currentPacket);
		}
		//else forward it
		else{
			this.sendPacket(currentPacket);
		}
	}

	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the destination router
	 * @param DSCP the differentiated services code point field
	 * @since 1.0
	 */
	public void createPacket(int destination, int DSCP){
		Packet newPacket= new Packet(this.getAddress(), destination, DSCP);
		this.sendPacket(newPacket);
		if(monitor!=null) monitor.notifySend(newPacket);
	}

	/**
	 * This method allocates bandwidth for a specific traffic class from the current router
	 * to the destination router
	 * @param dest destination router id
	 * @param PHB 0=EF, 1=AF, 2=BE
	 * @param Class AF classes 1,2,3,4. (0 if EF or BE)
	 * @param Bandwidth number of packets per time unit for this PHB/Class
	 * @since 1.0
	 */
	public void allocateBandwidth(int dest, int PHB, int Class, int Bandwidth){
		PATHMsg pathMsg = new PATHMsg(this.address,dest,PHB,Class,Bandwidth);

		//Place this message in the output buffer
		sentPATH(pathMsg);
		pathMsg.addPrevHop(this.address);
		this.routingTable.get(dest).sendPacket(pathMsg,this);	
	}

	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		LSRNIC forwardNIC;
		NICLabelPair newOutPair;
		int outLabel;
		MPLS header;

		//This method should send the packet to the correct NIC.
		if(!labelTable.containsLSP(newPacket.getDest())){
			//NO LSP! Set one up on the fly with BE service
			setupLSPOnTheFly(newPacket);
		}
		else if(labelTable.getOutPair(newPacket.getDest()).getLabel() == NICLabelPair.LSP_PENDING){
			/*We are waiting on a RESV message for this LSP.  Just queue up until it's received*/
			waitingPackets.add(newPacket);
		}
		else{
			/*send the new packet*/
			forwardNIC = labelTable.getOutPair(newPacket.getDest()).getNIC();
			outLabel = labelTable.getOutPair(newPacket.getDest()).getLabel();
			header = new MPLS(outLabel,newPacket.getDSCP(),1);
			newPacket.addMPLSheader(header);
			forwardNIC.sendPacket(newPacket, this);
			//sentData(newPacket);
		}

	}

	/**
	 * If a packet was sent to a destination for which there was no LSP, this method
	 * establishes one on the fly with the requested class of service.
	 * @param newPacket
	 */
	private void setupLSPOnTheFly(Packet newPacket){
		NICLabelPair newOutPair;
		
		newPacket.setDSCP(Constants.DSCP_BE);
		allocateBandwidth(newPacket.getDest(),Constants.PHB_BE,0,0);
		//LSP_PENDING signals that we should queue incoming packets until RESV is received 
		newOutPair = new NICLabelPair(routingTable.get(newPacket.getDest()),
				NICLabelPair.LSP_PENDING);
		labelTable.put(this.address,newPacket.getDest(),newPacket.getDest(), newOutPair);
		waitingPackets.add(newPacket);
	}
	
	/**
	 * Clear out any packets that may have been waiting on an LSP
	 * @param resv the resv message which triggered the release
	 */
	private void sendWaitingPackets(RESVMsg resv){
		int outLabel;
		LSRNIC forwardNIC;
		MPLS header;
		
		for(Packet p:waitingPackets){
			if(p.getDest() == resv.getSource()){
				forwardNIC = labelTable.getOutPair(p.getDest()).getNIC();
				/*add the MPLS info now that we have all the LSP setup*/
				outLabel = labelTable.getOutPair(p.getDest()).getLabel();
				header = new MPLS(outLabel,p.getDSCP(),1);
				p.addMPLSheader(header);
				forwardNIC.sendPacket(p, this);
			}
		}
	}
	
	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void sendPackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).sendPackets();
	}

	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void recievePackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).recievePackets();
	}

	/**
	 * calculates the next available input label.  Uses a linear scan over the values in the labelTable.
	 * @return the output label if one can be found. -1 if all integers are already in use.
	 */
	private int calcInLabel() {
		//find the next available input label
		for(int i = 1;i < Integer.MAX_VALUE;i++){
			if(!labelTable.containsInLabel(i)){
				return i;
			}
		}
		return -1;
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
	 * determines which router to forward this request to and
	 * pushes the address of this router onto the message's previous hops.
	 * @param p the packet containing the PATH message
	 */
	private void processPATH(PATHMsg p,LSRNIC nic){
		LSRNIC forwardNIC;
		PATHMsg fwdPATH;
		RESVMsg resv;
		int inLabel;
		int source = p.getSource(), dest = p.getDest();

		if(dest == this.address){
			if(labelTable.labelExists(source, dest)){
				/*use existing input label.  Don't modify label table*/
				inLabel = labelTable.getInLabel(source, dest);
				status("Terminated PATH msg " + p.getID() + ". Using existing MPLS label " 
						+ inLabel + " to router " + source);
			}
			/*setup a label mapping to null to indicate this is the receiving node*/
			else{
				inLabel = calcInLabel();
				labelTable.put(source,dest,inLabel, null);
				status("Terminated PATH msg " + p.getID() + " Established MPLS label " 
						+ inLabel + " to router " + source);
			}
			/*send back a reservation message*/
			resv = new RESVMsg(this.address,source,p.getPHB(),p.gettClass(),
					p.getTspec(),(ArrayList<Integer>)p.getPrevHops().clone());
			/*set the MPLS label so the next node knows which one to use.*/
			resv.setLabel(inLabel);
			forwardNIC = routingTable.get(resv.getNextHop());
			forwardNIC.sendPacket(resv, this);
			sentRESV(resv);
		}
		else{ 
			receivedPATH(p); //print a message
			fwdPATH = p.Clone();
			fwdPATH.addPrevHop(this.address);
			forwardNIC = routingTable.get(fwdPATH.getDest());
			this.sentPATH(fwdPATH); //print a message
			forwardNIC.sendPacket(fwdPATH, this);
		}
	}

	/**
	 * Processes a reserve message.  Checks reservation request against available
	 * resources.  If resources are available to fulfill the request, they are reserved
	 * and the RESV message is forwarded (unless this router is the destination). If
	 * sufficient resources are not available, a RESVERR message is forwarded back to 
	 * the sender.
	 * @param p the packet containing the RESV message.
	 * @param nic the NIC this RESV packet came in from
	 */
	private void processRESV(RESVMsg p,LSRNIC nic){
		LSRNIC forwardNIC;
		RESVMsg forwardRESV;
		int nextHop;
		
		if(p.getDest() == this.address){
			/*Reserve bandwidth*/
			if(nic.reserveBW(p.getPHB(), p.gettClass(), p.getTspec()) == true){
				/*set up label mapping*/
				createLabelMapping(nic,p,true);
				System.out.println("RESV: Router " + this.address + " terminated RESV msg "
						+ p.getID() + " from router " + p.getSource());
				sendResvConf(p.getSource());
			}
		}
		/*if this is not p's destination*/
		else{
			receivedRESV(p);
			forwardRESV = p.Clone();
			nextHop = forwardRESV.getNextHop();
			forwardNIC = routingTable.get(nextHop);
			/*check resources*/
			if(nic.reserveBW(forwardRESV.getPHB(), forwardRESV.gettClass(), 
					forwardRESV.getTspec()) == true){
				/*resources reserved. setup LSP labels + forward RESV to next node*/
				createLabelMapping(nic,forwardRESV,false);
				forwardNIC.sendPacket(forwardRESV, this);
				this.sentRESV(forwardRESV);
			}
			else{
				/*not enough resources for the reservation. send RESVERR. message printed by reserve()*/
				return;
			}
		}
	}
	
	/**
	 * send a reserve confirmation to a destination node.
	 * @param dest the node this message needs to go to.
	 */
	private void sendResvConf(int dest) {
		RESVConfMsg rc = new RESVConfMsg(this.address, dest);
		LSRNIC forwardNIC = this.labelTable.getOutPair(dest).getNIC();
		forwardNIC.sendPacket(rc, this);
		sentRESVConf(rc);
	}

	/**
	 * Adds a new entry to the MPLS label table after a successful resource reservation.
	 * @param nic the outgoing NIC for the mapping
	 * @param p the RESV packet to retrieve the outgoing label from
	 * @param isDest is the calling router the destination for the RESV message
	 * if it is then the input label will be set to the destination address rather than a label.
	 * @return true if the mapping could be created false otherwise
	 */
	private boolean createLabelMapping(LSRNIC nic, RESVMsg p,boolean isDest){
		NICLabelPair newPair;
		int inLabel,outLabel;
		int dest = p.getDest(), source = p.getSource();
		NICLabelPair outPair;
		
		/*first check if a mapping already exists*/
		if(labelTable.labelExists(dest, source)){
			/*check if the mapping was waiting for an LSP*/
			outPair = labelTable.getOutPair(source);
			if(outPair.getLabel() == NICLabelPair.LSP_PENDING){
				outPair.setLabel(p.getLabel());
				status("Releasing packet hold. Using label mapping {in:" 
						+ labelTable.getInLabel(dest, source) + ",out:" + outPair.getLabel() + "}");
				sendWaitingPackets(p);
				return true;
			}
			inLabel = labelTable.getInLabel(dest, source);
			outLabel = labelTable.get(inLabel).getLabel();
			status("Using existing label mapping {in: " + inLabel + ",out: " + outLabel + "}");
			p.setLabel(inLabel);
			return true;
		}
		
		inLabel = calcInLabel();
		
		outLabel = p.getLabel();
		/*no further checks since John stated we son't need to worry about label contention.*/
		if(inLabel > 0){
			newPair = new NICLabelPair(nic,outLabel);
			this.labelTable.put(dest,source,inLabel, newPair);
			System.out.println("(Router " + this.address + "): Created label mapping {in:" + inLabel + 
					",out:" + outLabel + "}");
			p.setLabel(inLabel);
			return true;
		}
		else{
			System.out.println("(Router " + this.address + "): Couldn't create new label mapping.");
			return false;
		}
	}
	/**
	 * Determines what type of RSVP packet p is and hands control 
	 * off to the appropriate processing method.
	 * @param p the rsvp packet to be handled.
	 * @param nic the nic this packet came in off of
	 */
	private void processRSVP(Packet p, LSRNIC nic){
		String pType = p.getType();
		if(pType.compareTo("PATH") == 0){
			processPATH((PATHMsg)p,nic);
		}
		else if(pType.compareTo("RESV") == 0){
			processRESV((RESVMsg)p,nic);
		}
		else if(pType.compareTo("RESVCONF") == 0){
			processRESVConf((RESVConfMsg)p,nic);
		}
	}

	private void processRESVConf(RESVConfMsg p, LSRNIC nic) {
		LSRNIC forwardNIC;
		receivedRESVConf(p);
		if(p.getDest() != this.address){
			/*forward if needed*/
			forwardNIC = this.labelTable.getOutPair(p.getDest()).getNIC();
			forwardNIC.sendPacket(p, this);
		}
	}

	/**
	 * set the QoS monitor for this router and all of its nics.
	 * @param monitor the QoSMonitor you want to hook up to this router.
	 */
	public void setQoSMonitor(QoSMonitor monitor){
		this.monitor = monitor;
		for(LSRNIC nic:nics){
			nic.setQoSMonitor(monitor);
		}
	}
	
	/**
	 * ==========================================
	 * Status printing methods
	 * ==========================================
	 */
	private void receivedRSVP(rsvpPacket p){
		System.out.println("(Router " + this.address +"): received an RSVP packet: " 
				+ p.getID());
	}

	private void receivedPATH(PATHMsg p){
		System.out.println("PATH: Router " + this.address +" received a PATH from router " 
				+ p.getSource() + ": " + p.getID());
	}

	private void sentPATH(PATHMsg p){
		System.out.println("PATH: Router " + this.address + " sent a PATH to router " 
				+ p.getDest() + ": " +p.getID());
	}

	private void receivedRESV(RESVMsg p){
		System.out.println("RESV: Router " + this.address + " received a RESV from router " 
				+ p.getSource() + ": " + p.getID());
	}

	private void sentRESV(RESVMsg p){
		System.out.println("RESV: Router " + this.address + " sent a RESV to router " 
				+ p.getDest() + ": " +p.getID());
	}
	
	private void receivedData(Packet p){
		System.out.println("DATA: Router " + this.address +" received data packet from router " 
				+ p.getSource() + " (delay = " + p.getDelay() + "): " + p.getID());
	}

	private void sentData(Packet p){
		System.out.println("DATA: Router " + this.address + " sent DATA to " + p.getDest() + ": " 
				+ p.getID());
	}
	
	private void sentRESVConf(Packet p){
		System.out.println("RESVCONF: Router " + this.address + " sent a RESVCONF to router " +
				p.getDest() + ": " + p.getID());
	}
	
	private void receivedRESVConf(Packet p){
		System.out.println("RESVCONF: Router " + this.address + " received a RESVCONF from router " +
				p.getSource() + ": " + p.getID());
	}
	
	private void status(String s){
		System.out.println("(Router " + this.address + "): " + s);
	}
	
	/**
	 * Prints a quick summary of the routing table.
	 */
	public void printRoutingTable(){
		System.out.println("Routing table for Router " + this.address + "\n===");
		for(int i:this.routingTable.keySet()){
			System.out.println(i + ": " + routingTable.get(i));
		}
	}
}
