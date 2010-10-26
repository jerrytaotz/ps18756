
import java.util.ArrayList;
import org.junit.*;
import NetworkElements.*;


/**
 * A Junit testing class which sets up the sample network from the project 
 * writeup and runs some tests against it.
 * @author Brady Tello
 */
public class TestNet1 {

	// This object will be used to move time forward on all objects
	private ArrayList<IATMCellConsumer> allConsumers;// = new ArrayList<IATMCellConsumer>();
	private int time;
	ATMRouter r1,r2,r3,r4,r5;
	Computer comp1,comp2;
	
	/*The setup method.  Just zeros everything out for now.*/
	@Before
	public void setUp() throws Exception {
		time = 0;
		allConsumers = new ArrayList<IATMCellConsumer>();
		
		// Create some new ATM Routers
		r1 = new ATMRouter(9);
		r2 = new ATMRouter(3);
		r3 = new ATMRouter(11);
		r4 = new ATMRouter(13);
		r5 = new ATMRouter(14);
		
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
		comp1 = new Computer("1");
		ATMNIC comp1n1 = new ATMNIC(comp1);
		ATMNIC r1n101 = new ATMNIC(r1);
		OtoOLink l101 = new OtoOLink(comp1n1, r1n101);
		
		// Connect a computer to r2
		comp2 = new Computer("2");
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
	public void testNet1Test1(){
		System.out.println("**TestNet1 Test 1**");

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
	
	/**
	 * Similar to test 1 but tears down the VCs after they are set up.
	 */
	@Test
	public void testNet1Test2(){
		System.out.println("**TestNet1: Test \"end\" signal**");

		// Setup a connection from comp1 to router 13 and comp2 to 14
		tock();
		comp1.setupConnection(13);
		comp2.setupConnection(14);
		for(int i=0; i<6; i++)
			this.tock();
		//Try to tear down a VC before one is setup
		comp1.endConnection();
		for(int i=0; i<7; i++)
			this.tock();
		//VCs will be established at this point.  Try to tear them down for real now.
		comp2.endConnection();
		comp1.endConnection();
		for(int i=0;i<10;i++)
			tock();
		Assert.assertTrue(true);
	}
	
	/**
	 * Test the tail drop mechanism by bombarding a NIC with packets and seeing
	 * if it starts dropping after it receives maximumBufferCells packets.
	 */
	@Test
	public void TestTailDropIngress(){
		System.out.println("**Test Net 1: Test Tail Drop at Ingress**");
		
		//Start out setting up connections normally.
		tock();
		comp2.setupConnection(11);
		for(int i = 0; i<3;i++)
			tock();
		//bombard comp 2's NIC with packets.  30-maxBufferCells should get dropped.
		for(int i= 0; i<30;i++){
			comp2.sendPacket(5);
		}
	}
	
	/**
	 * Test the tail drop mechanism at an intermediate router by overloading
	 * router 3 with traffic from router 9 and computer 2.
	 */
	@Test
	public void TestTailDropIntermediate(){
		System.out.println("**Test Net 1: Test Tail Drop at Intermediate**");
		
		//Start out setting up connections normally.
		tock();
		comp2.setupConnection(13);
		comp1.setupConnection(14);
		for(int i = 0; i<12;i++)
			tock();
		//fill comp1's NIC with packets.
		for(int i = 0;i<20;i++){
			comp1.sendPacket(5);
		}
		//load router 9 up with these 20 packets
		tock();
		//fill comp2's NIC with packets.
		for(int i= 0; i<20;i++){
			comp2.sendPacket(5);
		}
		//Now send everything to router 3
		tock();
		
	}
	
	/**
	 * Test the tail drop mechanism by bombarding a NIC with packets and seeing
	 * if it starts dropping after it receives maximumBufferCells packets.
	 */
	@Test
	public void TestRED(){
		System.out.println("**Test Net 1: Test RED**");
		
		for(int i= 0; i< this.allConsumers.size();i++){
			allConsumers.get(i).useRED();
		}
			
		//bombard comp 2's NIC with packets.
		for(int i= 0; i<70;i++){
			comp2.sendPacket(5);
		}
	}
	
	/**
	 * Tests PPD functionality by sending a few short packets, then a really long one, 
	 * then an OAM cell just to see if it is forced into the queue.  All this is followed
	 * by a single small packet which should go through to RED processing rather than being
	 * immediately dropped by PPD.
	 */
	@Test
	public void TestPPDIngress(){
		System.out.println("**Test Net 1: Test PPD");
		
		for(int i = 0; i< this.allConsumers.size();i++){
			allConsumers.get(i).usePPD();
		}
		
		comp1.setupConnection(13);
		for(int i = 0;i < 10; i++){
			tock();
		}
		
		//Bring the queue close to it's threshold
		System.out.println("***SENDING 18 SMALL PACKETS***");
		for(int i = 0;i<18; i++){
			comp1.sendPacket(30);
		}
		System.out.println("***SENDING A BIG PACKET***");
		comp1.sendPacket(3000);
		System.out.println("***SENDING AN OAM SIGNAL***");
		comp1.endConnection();
		tock();
		System.out.println("***SENDING ONE LAST SMALL PACKET***");
		comp1.sendPacket(30);
	}
}
