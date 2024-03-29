import NetworkElements.*;

import java.util.*;

public class example {
	// This object will be used to move time forward on all objects
	private int time = 0;
	private ArrayList<LSR> allConsumers = new ArrayList<LSR>();
	/**
	 * Create a network and creates connections
	 * @since 1.0
	 */
	public void go(){
		System.out.println("** SYSTEM SETUP **");
		
		// Create some new ATM Routers
		LSR r1 = new PscLSR(9);
		LSR r2 = new PscLSR(3);
		LSR r3 = new PscLSR(11);
		LSR r4 = new PscLSR(13);
		LSR r5 = new PscLSR(15);
		
		// give the routers interfaces
		LSRNIC r1n1 = new LSRNIC(r1);
		LSRNIC r2n1 = new LSRNIC(r2);
		LSRNIC r2n2 = new LSRNIC(r2);
		LSRNIC r2n3 = new LSRNIC(r2);
		LSRNIC r3n1 = new LSRNIC(r3);
		LSRNIC r3n2 = new LSRNIC(r3);
		LSRNIC r3n3 = new LSRNIC(r3);
		LSRNIC r3n4 = new LSRNIC(r3);
		LSRNIC r4n1 = new LSRNIC(r4);
		LSRNIC r4n2 = new LSRNIC(r4);
		LSRNIC r4n3 = new LSRNIC(r4);
		LSRNIC r5n1 = new LSRNIC(r5);
		
		// physically connect the router's nics
		OtoOLink l1 = new OtoOLink(r1n1, r2n1);
		OtoOLink l2 = new OtoOLink(r2n2, r3n1);
		OtoOLink l2opt = new OtoOLink(r2n3, r3n2, true); // optical link
		OtoOLink l3 = new OtoOLink(r3n3, r4n1);
		OtoOLink l3opt = new OtoOLink(r3n4, r4n2, true); // optical link
		OtoOLink l4 = new OtoOLink(r4n3, r5n1);
		
		// Add the objects that need to move in time to an array
		this.allConsumers.add(r1);
		this.allConsumers.add(r2);
		this.allConsumers.add(r3);
		this.allConsumers.add(r4);
		
		//send packets from router 1 to the other routers...
		r1.createPacket(13);
		
		tock();
		
	}
	
	public void tock(){
		System.out.println("** TIME = " + time + " **");
		time++;		
		
		// Send packets between routers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).sendPackets();

		// Move packets from input buffers to output buffers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).receivePackets();
		
	}
	public static void main(String args[]){
		example go = new example();
		go.go();
	}
}