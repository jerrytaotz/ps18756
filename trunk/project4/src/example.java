import DataTypes.Constants;
import DataTypes.QoSMonitor;
import NetworkElements.*;
import java.util.*;

import dijkstra.DirectedGraph;

public class example {
	// This object will be used to move time forward on all objects
	private int time = 0;
	private ArrayList<LSR> allConsumers = new ArrayList<LSR>();
	private DirectedGraph map = new DirectedGraph();
	private QoSMonitor monitor = new QoSMonitor();
	/**
	 * Create a network and creates connections
	 * @since 1.0
	 */
	public void go(){
	
		System.out.println("** SYSTEM SETUP **");
		
		/* Create some new ATM Routers*/
		LSR r1 = new LSR(1);
		LSR r2 = new LSR(2);
		
		/* give the routers interfaces*/
		LSRNIC r1n1 = new LSRNIC(r1);
		LSRNIC r2n1 = new LSRNIC(r2);
		
		/* physically connect the router's nics */
		OtoOLink l1 = new OtoOLink(r1n1, r2n1);
		
		//Update the network map to reflect any new links
		l1.updateNetworkMap(map);

		//update all the routing tables
		r1.updateRoutingTable(map);
		r2.updateRoutingTable(map);
		
		// Add the objects that need to move in time to an array
		this.allConsumers.add(r1);
		this.allConsumers.add(r2);

		r1.setQoSMonitor(monitor);
		r2.setQoSMonitor(monitor);
		
		r1.allocateBandwidth(2, Constants.PHB_EF, 0, 5);
		r1.allocateBandwidth(2, Constants.PHB_AF, 1, 5);
		r1.allocateBandwidth(2, Constants.PHB_AF, 2, 4);
		r1.allocateBandwidth(2, Constants.PHB_AF, 3, 8);
		r1.allocateBandwidth(2, Constants.PHB_BE, 0, 0);
		/*tock() until all the LSPs are set up*/
		for(int i = 0;i<4;i++) tock();
		
		/*start sending traffic as required*/
		for(int i = 0; i<15;i++){
			for(int j = 0; j<5;j++){
				r1.createPacket(2, Constants.DSCP_EF);
			}
			for(int j = 0; j<3;j++){
				r1.createPacket(2, Constants.DSCP_AF11);
			}
			for(int j = 0; j<2;j++){
				r1.createPacket(2, Constants.DSCP_AF12);
			}
			for(int j = 0; j<3;j++){
				r1.createPacket(2, Constants.DSCP_AF21);
			}
			for(int j = 0; j<2;j++){
				r1.createPacket(2, Constants.DSCP_AF22);
			}
			for(int j = 0; j<4;j++){
				r1.createPacket(2, Constants.DSCP_AF31);
			}
			for(int j = 0; j<5;j++){
				r1.createPacket(2, Constants.DSCP_AF32);
			}
			for(int j = 0; j<40;j++){
				r1.createPacket(2, Constants.DSCP_BE);
			}
			tock();
		}
		
		monitor.printQoSData();
	}
	
	public void tock(){
		System.out.println("** TIME = " + time + " **");
		time++;
		
		
		// Send packets between routers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).sendPackets();

		// Move packets from input buffers to output buffers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).recievePackets();
		
	}
	public static void main(String args[]){
		example go = new example();
		go.go();
	}
}