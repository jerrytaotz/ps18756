package NetworkElements;

import dijkstra.*;
import DataTypes.*;

public class OtoOLink {
	private LSRNIC r1NIC=null, r2NIC=null;
	private Boolean trace=false;
	
	/**
	 * The default constructor for a OtoOLink.
	 * @param computerNIC
	 * @param routerNIC
	 * @since 1.0
	 */
	public OtoOLink(LSRNIC r1NIC, LSRNIC r2NIC){	
		this.r1NIC = r1NIC;
		this.r1NIC.connectOtoOLink(this);
		this.r2NIC = r2NIC;
		this.r2NIC.connectOtoOLink(this);
		
		if(this.trace){
			if(r1NIC==null)
				System.err.println("Error (OtoOLink): R1 nic is null");
			if(r1NIC==null)
				System.err.println("Error (OtoOLink): R2 nic is null");
		}
	}
	
	public void updateNetworkMap(DirectedGraph map){
		LSR lsr1 = r1NIC.getParent();
		LSR lsr2 = r2NIC.getParent();
		float linkCost = 1/(float)r1NIC.getAvailableBW();

		//Update the network map to reflect this new link.
		Node node1;
		Node node2; 
		if(map.containsNode(lsr1.getAddress())){
			node1 = map.getNode(lsr1.getAddress());
		}
		else node1 = new Node(lsr1.getAddress());
		if(map.containsNode(lsr2.getAddress())){
			node2 = map.getNode(lsr2.getAddress());
		}
		else node2 = new Node(lsr2.getAddress());
		
		node1.addLink(linkCost,node2);
		node2.addLink(linkCost,node1);
		
		map.addNode(node1);
		map.addNode(node2);
	}
	
	/**
	 * Sends a packet from one end of the link to the other
	 * @param currentPacket the packet to be sent
	 * @param nic the nic the packet is being sent from
	 * @since 1.0
	 */
	public void sendPacket(Packet currentPacket, LSRNIC nic){
		if(this.r1NIC.equals(nic)){
			if(this.trace)
				System.out.println("(OtoOLink) Trace: sending packet from router A to router B");
			this.r2NIC.receivePacket(currentPacket);
		}
		else if(this.r2NIC.equals(nic)){
			if(this.trace)
				System.out.println("(OtoOLink) Trace: sending packet from router B to router A");
			this.r1NIC.receivePacket(currentPacket);
		}
		else
			System.err.println("(OtoOLink) Error: You are trying to send a packet down a link that you are not connected to");
	}
	
	/**
	 * Returns the address of the node on the opposite end of the link from a caller.
	 * @param the calling node.
	 * @return the address of the node on the opposite end of the link from the caller.
	 */
	public int getNeighborAddress(LSRNIC caller){
		if(caller == r1NIC) return r2NIC.getParent().getAddress();
		else return r1NIC.getParent().getAddress();
	}
}
