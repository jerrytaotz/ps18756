
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import NetworkElements.*;
import DataTypes.*;
import dijkstra.*;
import java.util.*;


public class TestNet1 {

	private DirectedGraph map;
	private ArrayList<LSR> allConsumers;
	private int time;
	private LSR r1;
	private LSR r2;
	private LSR r3;
	private LSR r4;
	private LSR r5;
	private LSR r6;
	
	@Before
	public void setUp() throws Exception {
		map = new DirectedGraph();
		allConsumers = new ArrayList<LSR>();
		time = 0;
		
		//Create new machines
		r1 = new LSR(1);
		r2 = new LSR(2);
		r3 = new LSR(3);
		r4 = new LSR(4);
		r5 = new LSR(5);
		r6 = new LSR(6);
		
		//Give them NICS
		LSRNIC r1r2 = new LSRNIC(r1);
		LSRNIC r2r1 = new LSRNIC(r2);
		LSRNIC r2r3 = new LSRNIC(r2);
		LSRNIC r2r5 = new LSRNIC(r2);
		LSRNIC r2r6 = new LSRNIC(r2);
		LSRNIC r3r2 = new LSRNIC(r3);
		LSRNIC r3r4 = new LSRNIC(r3);
		LSRNIC r4r3 = new LSRNIC(r4);
		LSRNIC r4r5 = new LSRNIC(r4);
		LSRNIC r5r4 = new LSRNIC(r5);
		LSRNIC r5r2 = new LSRNIC(r5);
		LSRNIC r6r2 = new LSRNIC(r6);
		
		//Connect the NICs
		OtoOLink l1 = new OtoOLink(r1r2,r2r1);
		OtoOLink l2 = new OtoOLink(r2r6,r6r2);
		OtoOLink l3 = new OtoOLink(r2r5,r5r2);
		OtoOLink l4 = new OtoOLink(r2r3,r3r2);
		OtoOLink l5 = new OtoOLink(r3r4,r4r3);
		OtoOLink l6 = new OtoOLink(r4r5,r5r4);
		
		//Update the view of the network
		l1.updateNetworkMap(map);
		l2.updateNetworkMap(map);
		l3.updateNetworkMap(map);
		l4.updateNetworkMap(map);
		l5.updateNetworkMap(map);
		l6.updateNetworkMap(map);
		
		//now use the new view to create routing tables
		r1.updateRoutingTable(map);
		r2.updateRoutingTable(map);
		r3.updateRoutingTable(map);
		r4.updateRoutingTable(map);
		r5.updateRoutingTable(map);
		r6.updateRoutingTable(map);
		
		allConsumers.add(r1);
		allConsumers.add(r2);
		allConsumers.add(r3);
		allConsumers.add(r4);
		allConsumers.add(r5);
		allConsumers.add(r6);
	}

	@After
	public void tearDown() throws Exception {
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
	
	@Test
	public void testRoutingTableSetup(){
		for(LSR r: allConsumers){
			r.printRoutingTable();
		}
	}
	
	@Test public void testPATHReceipt(){
		r1.allocateBandwidth(4, Constants.PHB_BE, 0, 0);
		r1.allocateBandwidth(3, Constants.PHB_AF, 2, 10);
		r1.allocateBandwidth(6, Constants.PHB_EF, 0, 20);
		r6.allocateBandwidth(1, Constants.PHB_BE, 0, 0);
		for(int i = 0;i<20;i++){
			tock();
		}
	}
}