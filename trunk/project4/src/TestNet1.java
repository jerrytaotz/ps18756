
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
	
	@Before
	public void setUp() throws Exception {
		map = new DirectedGraph();
		allConsumers = new ArrayList<LSR>();
		
		//Create new machines
		LSR r1 = new LSR(1);
		LSR r2 = new LSR(2);
		LSR r3 = new LSR(3);
		LSR r4 = new LSR(4);
		LSR r5 = new LSR(5);
		LSR r6 = new LSR(6);
		
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

	@Test
	public void testRoutingTableSetup(){
		for(LSR r: allConsumers){
			r.printRoutingTable();
		}
	}
}
