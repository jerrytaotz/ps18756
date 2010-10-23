
import java.util.ArrayList;
import org.junit.*;
import NetworkElements.*;


/**
 * A Junit testing class which should make testing a little easier.
 * @author Brady
 */
public class TestNet1 {

	// This object will be used to move time forward on all objects
	private ArrayList<IATMCellConsumer> allConsumers;// = new ArrayList<IATMCellConsumer>();
	private int time;
	
	/*The setup method.  Just zeros everything out for now.*/
	@Before
	public void setUp() throws Exception {
		time = 0;
		allConsumers = new ArrayList<IATMCellConsumer>();
	}

	/*the teardown method*/
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * moves time forward in all of the networks objects, so that cells take some amount of time to
	 * travel from once place to another
	 * @since 1.0
	 */
	public void tock(){
		System.out.println("** TIME = " + time + " **");
		time++;
		
		// Move the cells in the output buffers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).clearOutputBuffers();
		
		// Move the cells from the input buffers to the output buffers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).clearInputBuffers();
	}
	
	@Test
	public void testNet1(){
		System.out.println("**Test 1 SYSTEM SETUP **");
		
		// Create some new ATM Routers
		ATMRouter r1 = new ATMRouter(9);
		ATMRouter r2 = new ATMRouter(3);
		ATMRouter r3 = new ATMRouter(11);
		ATMRouter r4 = new ATMRouter(13);
		ATMRouter r5 = new ATMRouter(14);
		
		// give the routers interfaces
		ATMNIC r1n1 = new ATMNIC(r1);
		ATMNIC r2n1 = new ATMNIC(r2);
		ATMNIC r2n2 = new ATMNIC(r2);
		ATMNIC r2n3 = new ATMNIC(r2);
		ATMNIC r3n1 = new ATMNIC(r3);
		ATMNIC r4n1 = new ATMNIC(r4);
		ATMNIC r4n2 = new ATMNIC(r4);
		ATMNIC r5n1 = new ATMNIC(r5);
		
		// physically connect the router's nics
		OtoOLink l1 = new OtoOLink(r1n1, r2n1);
		OtoOLink l2 = new OtoOLink(r2n2, r3n1);
		OtoOLink l3 = new OtoOLink(r2n3, r4n1);
		OtoOLink l4 = new OtoOLink(r4n2, r5n1);
		
		// Create the forwarding tables so each Router knows what interface to use
		// to connect to another ATM router
		// router 1 - AS9
		r1.addNextHopInterface(3, r1n1);
		r1.addNextHopInterface(11, r1n1);
		r1.addNextHopInterface(13, r1n1);
		r1.addNextHopInterface(14, r1n1);
		
		// router 2 - AS3
		r2.addNextHopInterface(9, r2n1);
		r2.addNextHopInterface(11, r2n2);
		r2.addNextHopInterface(13, r2n3);
		r2.addNextHopInterface(14, r2n3);
		
		// router 3 - AS11
		r3.addNextHopInterface(3, r3n1);
		r3.addNextHopInterface(9, r3n1);
		r3.addNextHopInterface(13, r3n1);
		r3.addNextHopInterface(14, r3n1);
		
		// router 4 - AS13
		r4.addNextHopInterface(3, r4n1);
		r4.addNextHopInterface(9, r4n1);
		r4.addNextHopInterface(11, r4n1);
		r4.addNextHopInterface(14, r4n2);
		
		// router 5 - AS14
		r5.addNextHopInterface(3, r5n1);
		r5.addNextHopInterface(9, r5n1);
		r5.addNextHopInterface(11, r5n1);
		r5.addNextHopInterface(12, r5n1);
		
		// Connect a computer to r1
		Computer comp1 = new Computer("1");
		ATMNIC comp1n1 = new ATMNIC(comp1);
		ATMNIC r1n101 = new ATMNIC(r1);
		OtoOLink l101 = new OtoOLink(comp1n1, r1n101);
		
		// Connect a computer to r2
		Computer comp2 = new Computer("2");
		ATMNIC comp2n1 = new ATMNIC(comp2);
		ATMNIC r2n101 = new ATMNIC(r2);
		OtoOLink l201 = new OtoOLink(comp2n1, r2n101);
		
		// Add the objects that need to move in time to an array
		this.allConsumers.add(r1);
		this.allConsumers.add(r2);
		this.allConsumers.add(r3);
		this.allConsumers.add(r4);
		this.allConsumers.add(r5);
		this.allConsumers.add(comp1);
		this.allConsumers.add(comp2);
		
		// set the drop mechanism if we want to try them
		for(int i=0; i<this.allConsumers.size(); i++)
			this.allConsumers.get(i).useTailDrop();
		
		// Setup a connection from comp1 to router 13 and comp2 to 14
		tock();
		//comp1.sendPacket(500);
		comp1.setupConnection(13);
		comp2.setupConnection(14);
		for(int i=0; i<12; i++)
			this.tock();
		//comp1.sendPacket(500);
		//for(int i=0; i<8; i++)
			//this.tock();
		Assert.assertTrue(true);
	}
}
