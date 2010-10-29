
import java.util.ArrayList;

import NetworkElements.*;
import org.junit.*;

/**
 * A test network using an extended star topology in which some nodes hang off of 
 * a center node (5) and then a couple nodes hang off of those as well.  A little
 * more complex than the one provided in the notes so hopefully it will route out 
 * some of the edge cases.
 * @author Brady Tello
 *
 */
public class TestNet2 {

	// This object will be used to move time forward on all objects
	private ArrayList<IATMCellConsumer> allConsumers;// = new ArrayList<IATMCellConsumer>();
	private int time;

	ATMRouter r1,r2,r3,r4,r5,r6,r7;
	Computer comp1,comp2,comp3,comp4,comp5;
	
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
	
	@Before
	public void setUp() throws Exception {
		time = 0;
		allConsumers = new ArrayList<IATMCellConsumer>();
		
		// Create some new ATM Routers
		r1 = new ATMRouter(1);
		r2 = new ATMRouter(2);
		r3 = new ATMRouter(3);
		r4 = new ATMRouter(4);
		r5 = new ATMRouter(5);
		r6 = new ATMRouter(6);
		r7 = new ATMRouter(7);
		
		// give the routers interfaces
		ATMNIC r1n3 = new ATMNIC(r1);
		ATMNIC r2n4 = new ATMNIC(r2);
		ATMNIC r3n1 = new ATMNIC(r3);
		ATMNIC r3n5 = new ATMNIC(r3);
		ATMNIC r4n2 = new ATMNIC(r4);
		ATMNIC r4n5 = new ATMNIC(r4);
		ATMNIC r5n3 = new ATMNIC(r5);
		ATMNIC r5n4 = new ATMNIC(r5);
		ATMNIC r5n6 = new ATMNIC(r5);
		ATMNIC r5n7 = new ATMNIC(r5);
		ATMNIC r6n5 = new ATMNIC(r6);
		ATMNIC r7n5 = new ATMNIC(r7);
		
		// physically connect the router's nics
		OtoOLink l13 = new OtoOLink(r1n3,r3n1);
		OtoOLink l24 = new OtoOLink(r2n4,r4n2);
		OtoOLink l35 = new OtoOLink(r3n5,r5n3);
		OtoOLink l45 = new OtoOLink(r4n5,r5n4);
		OtoOLink l56 = new OtoOLink(r5n6, r6n5);
		OtoOLink l57 = new OtoOLink(r5n7,r7n5);
		
		// Create the forwarding tables so each Router knows what interface to use
		// to connect to another ATM router
		// router 1 - AS1
		r1.addNextHopInterface(2, r1n3);
		r1.addNextHopInterface(3, r1n3);
		r1.addNextHopInterface(4, r1n3);
		r1.addNextHopInterface(5, r1n3);
		r1.addNextHopInterface(6, r1n3);
		r1.addNextHopInterface(7, r1n3);
		
		// router 2 - AS2
		r2.addNextHopInterface(1, r2n4);
		r2.addNextHopInterface(3, r2n4);
		r2.addNextHopInterface(4, r2n4);
		r2.addNextHopInterface(5, r2n4);
		r2.addNextHopInterface(6, r2n4);
		r2.addNextHopInterface(7, r2n4);
		
		// router 3 - AS3
		r3.addNextHopInterface(1, r3n1);
		r3.addNextHopInterface(2, r3n5);
		r3.addNextHopInterface(4, r3n5);
		r3.addNextHopInterface(5, r3n5);
		r3.addNextHopInterface(6, r3n5);
		r3.addNextHopInterface(7, r3n5);
		
		// router 4 - AS4
		r4.addNextHopInterface(1, r4n5);
		r4.addNextHopInterface(2, r4n2);
		r4.addNextHopInterface(3, r3n5);
		r4.addNextHopInterface(5, r4n5);
		r4.addNextHopInterface(6, r4n5);
		r4.addNextHopInterface(7, r4n5);
		
		// router 5 - AS5
		r5.addNextHopInterface(1, r5n3);
		r5.addNextHopInterface(2, r5n4);
		r5.addNextHopInterface(3, r5n3);
		r5.addNextHopInterface(4, r5n4);
		r5.addNextHopInterface(6, r5n6);
		r5.addNextHopInterface(7, r5n7);

		//router 6 - AS6
		r6.addNextHopInterface(1, r6n5);
		r6.addNextHopInterface(2, r6n5);
		r6.addNextHopInterface(3, r6n5);
		r6.addNextHopInterface(4, r6n5);
		r6.addNextHopInterface(5, r6n5);
		r6.addNextHopInterface(7, r6n5);

		//router 7 - AS7
		r7.addNextHopInterface(1, r7n5);
		r7.addNextHopInterface(2, r7n5);
		r7.addNextHopInterface(3, r7n5);
		r7.addNextHopInterface(4, r7n5);
		r7.addNextHopInterface(5, r7n5);
		r7.addNextHopInterface(6, r7n5);
		
		// Connect a computer to r1
		comp1 = new Computer("1");
		ATMNIC comp1nr1 = new ATMNIC(comp1);
		ATMNIC r1ncomp1 = new ATMNIC(r1);
		OtoOLink lcomp1r1 = new OtoOLink(r1ncomp1, comp1nr1);
		
		// Connect two computers to r2
		comp2 = new Computer("2");
		ATMNIC comp2nr2 = new ATMNIC(comp2);
		ATMNIC r2ncomp2 = new ATMNIC(r2);
		OtoOLink lcomp2r2 = new OtoOLink(comp2nr2, r2ncomp2);
		
		comp3 = new Computer("3");
		ATMNIC comp3nr2 = new ATMNIC(comp3);
		ATMNIC r2ncomp3 = new ATMNIC(r2);
		OtoOLink lcomp3r2 = new OtoOLink(comp3nr2, r2ncomp3);
		
		//Connect a computer to r6
		comp4 = new Computer("4");
		ATMNIC comp4nr6 = new ATMNIC(comp4);
		ATMNIC r6ncomp4 = new ATMNIC(r6);
		OtoOLink lcomp4r6 = new OtoOLink(comp4nr6,r6ncomp4);
		
		//Connect a computer to r5
		comp5 = new Computer("5");
		ATMNIC comp5nr5 = new ATMNIC(comp5);
		ATMNIC r5ncomp5 = new ATMNIC(r5);
		OtoOLink lcomp5r5 = new OtoOLink(comp5nr5,r5ncomp5);
		
		// Add the objects that need to move in time to an array
		this.allConsumers.add(r1);
		this.allConsumers.add(r2);
		this.allConsumers.add(r3);
		this.allConsumers.add(r4);
		this.allConsumers.add(r5);
		this.allConsumers.add(r6);
		this.allConsumers.add(r7);
		this.allConsumers.add(comp1);
		this.allConsumers.add(comp2);
		this.allConsumers.add(comp3);
		this.allConsumers.add(comp4);
		this.allConsumers.add(comp5);
		
		// set the drop mechanism if we want to try them
		for(int i=0; i<this.allConsumers.size(); i++)
			this.allConsumers.get(i).useTailDrop();
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void TestNet2Test1(){
		System.out.println("**Test Net 2: Test Connectivity **");
		
		// Setup a connection from comp1 to router 7
		tock();
		comp1.setupConnection(7);
		comp4.setupConnection(2);
		for(int i=0; i<12; i++)
			this.tock();
		comp3.setupConnection(5);
		for(int i = 0; i<5; i++)
			this.tock();
		comp2.setupConnection(7);
		for(int i = 0; i<11; i++)
			this.tock();
	}
	
	/**
	 * A test to ensure that the end message works the way its supposed to.
	 * Connections are established as they are in Test 1 and then the 'end'
	 * signal is sent across each connection.  All connections should
	 * be torn down.
	 */
	@Test
	public void TestNet2TestEndNormal(){
		System.out.println("***Test Net 2: Testing the 'end' signal.***");
		// Setup a connection from comp1 to router 7
		tock();
		comp1.setupConnection(7);
		comp4.setupConnection(2);
		//comp4 should get VC 1
		//comp1 should get VC 1
		for(int i=0; i<12; i++)
			this.tock();
		comp3.setupConnection(5);
		//comp3 should get VC 2
		for(int i = 0; i<5; i++)
			this.tock();
		//Comp2 should get VC 3
		comp2.setupConnection(7);
		for(int i = 0; i<11; i++)
			this.tock();
		System.out.println("***TEARING ALL ROUTES DOWN***");
		comp1.endConnection();
		comp2.endConnection();
		comp3.endConnection();
		comp4.endConnection();
		for(int i = 0; i<9;i++) tock();
	}
	
	/**
	 * This test tries to tear down a connection before it has been set up.
	 * comp1 starts to set up a connection, lets the 'setup' signal get half way
	 * and then sends an end message.  The first router (r1) should deny the message.
	 */
	@Test
	public void TestNet2TestEndNoVC(){
		System.out.println("**Test Net 2: Testing 'end' signal with no VC.");
		comp1.endConnection(); //this should fail.
		comp1.setupConnection(7);
		tock();
		tock();
		comp1.endConnection();//this too
		tock();
		comp1.endConnection();//and this
		for(int i = 0; i < 6;i++) tock();
		comp1.endConnection();//this should go through
		for(int i =0;i<10;i++) tock();
		comp1.sendPacket(5);
		tock();
	}
	
	/**
	 * A test to make sure tail drop works in this second network.
	 * Overloads comp1's NIC to see if the packets exceeding the threshold
	 * are actually dropped.
	 */
	@Test
	public void TestNet2TestTailDropIngress(){
		// Setup a connection from comp1 to router 7
		System.out.println("**Test Net 2: Test Tail Drop**");
		tock();
		comp1.setupConnection(5);
		for(int i=0; i<12; i++)
			this.tock();
		for(int i = 0;i<30;i++)
			comp1.sendPacket(5);
		this.tock();
		for(int i = 0; i<25; i++)
			comp1.sendPacket(5);
		
	}
	
	/**
	 * Tests tail drop on an intermediate router to ensure that packets
	 * are actually dropped.  Does this by bombarding router 5 with packets.
	 */
	@Test 
	public void TestTailDropIntermediate(){
		System.out.println("***TEST NET 2: TEST TAIL DROP ON INT. ROUTER***");
		comp1.setupConnection(7);
		for(int i = 0;i<12;i++){
			tock();
		}
		comp4.setupConnection(7);
		for(int i = 0;i<12;i++){
			tock();
		}
		for(int i = 0;i<20;i++){
			comp1.sendPacket(5);
		}
		tock();
		for(int i = 0;i<5;i++){
			comp4.sendPacket(5);
		}
		tock();
		tock();//5 packets should be dropped by router 5.
	}
	
	/**
	 * A test to make sure RED works in this second network.  It sets up a connection
	 * between computer2 and router 6 and then overloads computer 2's NIC with packets.
	 * Comp2's NIC should start dropping packets probabilistically after a while.
	 * It should also drop all packets once the RED threshold is reached.  It then
	 * clears the NIC and overloads the NIC again just to make sure everything clears out
	 * and works properly after a buffer flush.
	 */
	@Test
	public void TestNet2TestREDIngress(){
		// Setup a connection from comp2 to router 6
		System.out.println("**Test Net 2: Test RED**");
		
		for(int i = 0; i<this.allConsumers.size();i++){
			allConsumers.get(i).useRED();
		}
		tock();
		//Setup a connection just to test RED in presence of other traffic.
		System.out.println("***SETTING UP CONNECTION BETW. COMP2 AND R6***");
		comp2.setupConnection(6);
		for(int i=0; i<8; i++)
			this.tock();
		System.out.println("***SENDING 30 PACKETS***");
		for(int i = 0;i<30;i++)
			comp2.sendPacket(5);
		this.tock();
		System.out.println("***SENDING ANOTHER 30***");
		for(int i = 0; i<30; i++)
			comp2.sendPacket(5);
		
	}
	
	/**
	 * Tests RED at an intermediate router.  Does this by overwhelming router
	 * 5 with packets from both router 6 and router 3.
	 */
	@Test
	public void TestREDIntermediate(){
		System.out.println("***TEST NET 2: TEST RED ON INT. ROUTER***");
		for(int i = 0;i<this.allConsumers.size();i++){
			allConsumers.get(i).useRED();
		}
		
		comp1.setupConnection(7);
		for(int i = 0;i<12;i++){
			tock();
		}
		comp4.setupConnection(7);
		for(int i = 0;i<12;i++){
			tock();
		}
		for(int i = 0;i<20;i++){
			comp1.sendPacket(5);
		}
		tock();
		for(int i = 0;i<20;i++){
			comp4.sendPacket(5);
		}
		tock();
		tock();
	}
	
	/**
	 * Tests PPD at an ingress router.
	 */
	@Test public void TestPPDIngress(){
		System.out.println("***TEST NET 2: TEST PPD ON INGRESS ROUTER***");
		for(int i = 0;i<this.allConsumers.size();i++){
			allConsumers.get(i).usePPD();
		}
		comp1.setupConnection(3);
		for(int i = 0;i<6;i++){
			tock();
		}
		comp1.sendPacket(20000);
	}
	
	/**
	 * Tests PPD at an intermediate router.  Does this by overwhelming router
	 * 5 with packets from both router 6 and router 3.
	 */
	@Test
	public void TestPPDIntermediate(){
		System.out.println("***TEST NET 2: TEST PPD ON INT. ROUTER***");
		for(int i = 0;i<this.allConsumers.size();i++){
			allConsumers.get(i).usePPD();
		}
		
		comp1.setupConnection(7);
		for(int i = 0;i<12;i++){
			tock();
		}
		comp4.setupConnection(7);
		for(int i = 0;i<12;i++){
			tock();
		}
		for(int i = 0;i<30;i++){
			comp1.sendPacket(5);
		}
		tock();
		comp4.sendPacket(6000);//Make sure once first cell is dropped, no more get through.
		tock();
		tock();
	}
	
	/**
	 * Tests EPD at an ingress router.
	 */
	@Test public void TestEPDIngress(){
		System.out.println("***TEST NET 2: TEST EPD ON INGRESS ROUTER***");
		for(int i = 0;i<this.allConsumers.size();i++){
			allConsumers.get(i).useEPD();
		}
		comp1.setupConnection(3);
		for(int i = 0;i<6;i++){
			tock();
		}
		comp1.sendPacket(20000); //This packet should (with high p) get entirely dropped.
	}
	
	/**
	 * Tests EPD at an intermediate router.  Does this by overwhelming router
	 * 5 with packets from both router 6 and router 3.
	 */
	@Test
	public void TestEPDIntermediate(){
		System.out.println("***TEST NET 2: TEST EPD ON INT. ROUTER***");
		for(int i = 0;i<this.allConsumers.size();i++){
			allConsumers.get(i).useEPD();
		}
		
		comp1.setupConnection(7);
		for(int i = 0;i<12;i++){
			tock();
		}
		comp4.setupConnection(7);
		for(int i = 0;i<12;i++){
			tock();
		}
		for(int i = 0;i<30;i++){
			comp1.sendPacket(5);
		}
		tock();
		comp4.sendPacket(12000);//Make sure once first cell is dropped, no more get through.
		tock();
		tock();
	}

}
