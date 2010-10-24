
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
		System.out.println("**Test Net 2 Test 1 **");
		
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
	
}
