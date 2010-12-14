import java.util.ArrayList;
/**
 * This file sets up the example GMPLS network from the project 5 writeup.
 * @author Brady Tello
 */
import org.junit.*;
import NetworkElements.*;
import dijkstra.*;


public class TestNet1 {

	// This object will be used to move time forward on all objects
	private int time = 0;
	private ArrayList<LSR> allConsumers = new ArrayList<LSR>();
	DirectedGraph dataMap;
	DirectedGraph controlMap;
	
	private PscLSR lsrA;
	private PscLscLSR lsrB;
	private LscLSR lsrC;
	private LscLSR lsrD;
	private LscLSR lsrE;
	private PscLscLSR lsrF;
	private PscLSR lsrG;
	
	@Before
	public void setUp() throws Exception {
		lsrA = new PscLSR(1);
		lsrB = new PscLscLSR(2);
		lsrC = new LscLSR(3);
		lsrD = new LscLSR(4);
		lsrE = new LscLSR(5);
		lsrF = new PscLscLSR(6);
		lsrG = new PscLSR(7);
		
		dataMap = new DirectedGraph();
		controlMap = new DirectedGraph();
		
		//setup A's PSC link to B (don't need to characterize any links since it's PSC only)
		LSRNIC lsrAlsrB = new LSRNIC(lsrA,"lsrAlsrB");
		
		//B is a PSC+LSC router so it needs a PSC link to A as well as data+control channels to C 
		LSRNIC lsrBlsrA = new LSRNIC(lsrB,"lsrBlsrA");
		lsrB.setPSCLink(lsrBlsrA); //characterize PSC link
		LSRNIC lsrBlsrCData = new LSRNIC(lsrB,"lsrBlsrCData");
		LSRNIC lsrBlsrCControl = new LSRNIC(lsrB,"lsrBlsrCControl");
		lsrB.setDataAndControlLinkPair(lsrBlsrCData, lsrBlsrCControl);//characterize contr.+data pair		
		
		// Setup the LSC routers so that they get a control channel for each data channel.
		LSRNIC lsrClsrBData = new LSRNIC(lsrC,"lsrClsrBData");
		LSRNIC lsrClsrBControl = new LSRNIC(lsrC,"lsrClsrBControl");
		lsrC.setDataAndControlLinkPair(lsrClsrBData, lsrClsrBControl); 
		LSRNIC lsrClsrDData = new LSRNIC(lsrC,"lsrClsrDData");
		LSRNIC lsrClsrDControl = new LSRNIC(lsrC,"lsrClsrDControl");
		lsrC.setDataAndControlLinkPair(lsrClsrDData, lsrClsrDControl);

		LSRNIC lsrDlsrCData = new LSRNIC(lsrD,"lsrDlsrCData");
		LSRNIC lsrDlsrCControl = new LSRNIC(lsrD,"lsrDlsrCControl");
		lsrD.setDataAndControlLinkPair(lsrDlsrCData, lsrDlsrCControl);
		LSRNIC lsrDlsrEData = new LSRNIC(lsrD,"lsrDlsrEData");
		LSRNIC lsrDlsrEControl = new LSRNIC(lsrD,"lsrDlsrEControl");
		lsrD.setDataAndControlLinkPair(lsrDlsrEData, lsrDlsrEControl);

		LSRNIC lsrElsrDData = new LSRNIC(lsrE,"lsrElsrDData");
		LSRNIC lsrElsrDControl = new LSRNIC(lsrE,"lsrElsrDControl");
		lsrE.setDataAndControlLinkPair(lsrElsrDData, lsrElsrDControl);
		LSRNIC lsrElsrFData = new LSRNIC(lsrE,"lsrElsrFData");
		LSRNIC lsrElsrFControl = new LSRNIC(lsrE,"lsrElsrFControl");
		lsrE.setDataAndControlLinkPair(lsrElsrFData, lsrElsrFControl);

		//Again, LSR F is LSC+PSC capable so get a PSC link AND a control/data pair
		LSRNIC lsrFlsrEData = new LSRNIC(lsrF,"lsrFlsrEData");
		LSRNIC lsrFlsrEControl = new LSRNIC(lsrF,"lsrFlsrEControl");
		lsrF.setDataAndControlLinkPair(lsrFlsrEData, lsrFlsrEControl);
		LSRNIC lsrFlsrG = new LSRNIC(lsrF,"lsrFlsrG");
		lsrF.setPSCLink(lsrFlsrG);
		
		//LSR G is PSC only (don't need to characterize any links)
		LSRNIC lsrGlsrF = new LSRNIC(lsrG,"lsrGlsrF");
		
		//Connect all of the NICS
		OtoOLink lAB = new OtoOLink(lsrAlsrB,lsrBlsrA);
		OtoOLink lBCData = new OtoOLink(lsrBlsrCData,lsrClsrBData);
		OtoOLink lBCControl = new OtoOLink(lsrBlsrCControl,lsrClsrBControl);
		OtoOLink lCDData = new OtoOLink(lsrClsrDData,lsrDlsrCData);
		OtoOLink lCDControl = new OtoOLink(lsrClsrDControl,lsrDlsrCControl);
		OtoOLink lDEData = new OtoOLink(lsrDlsrEData,lsrElsrDData);
		OtoOLink lDEControl = new OtoOLink(lsrDlsrEControl,lsrElsrDControl);
		OtoOLink lEFData = new OtoOLink(lsrElsrFData,lsrFlsrEData);
		OtoOLink lEFControl = new OtoOLink(lsrElsrFControl,lsrFlsrEControl);
		OtoOLink lFG = new OtoOLink(lsrFlsrG,lsrGlsrF);
		
		/*
		 * The way I have decided to implement the data and control planes is to create 
		 * separate routing tables for data and signaling packets.  I feel like this is what 
		 * would be done in a real GMPLS router since the control signal may take a different 
		 * route than the data if it is based on IP forwarding.
		 */
		//the PSC link between A and B is on the data plane as well as the control plane
		lAB.updateNetworkMap(dataMap);
		lAB.updateNetworkMap(controlMap);
		//the middle links need to be split into the data plane or the control plane
		lBCData.updateNetworkMap(dataMap);
		lBCControl.updateNetworkMap(controlMap);
		
		lCDData.updateNetworkMap(dataMap);
		lCDControl.updateNetworkMap(controlMap);
		
		lDEData.updateNetworkMap(dataMap);
		lDEControl.updateNetworkMap(controlMap);
		
		lEFData.updateNetworkMap(dataMap);
		lEFControl.updateNetworkMap(controlMap);
		
		//the PSC link between F and G is on the data plane as well as the control plane
		lFG.updateNetworkMap(dataMap);
		lFG.updateNetworkMap(controlMap);
		
		//For the PSC routers we should be able to use either the data or control map
		lsrA.updateRoutingTable(dataMap);
		lsrB.updateRoutingTable(dataMap, controlMap);
		lsrC.updateRoutingTable(dataMap, controlMap);
		lsrD.updateRoutingTable(dataMap, controlMap);
		lsrE.updateRoutingTable(dataMap, controlMap);
		lsrF.updateRoutingTable(dataMap, controlMap);
		lsrG.updateRoutingTable(dataMap);
		
		allConsumers.add(lsrA);
		allConsumers.add(lsrB);
		allConsumers.add(lsrC);
		allConsumers.add(lsrD);
		allConsumers.add(lsrE);
		allConsumers.add(lsrF);
		allConsumers.add(lsrG);
		
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
			allConsumers.get(i).receivePackets();
		
	}
	
	/**
	 * Tests whether the dijkstra's algorithm code will work for both the data plane and
	 * the control plane going from left to right.
	 */
	@Test
	public void testRoutingLeftToRight(){
		Assert.assertTrue(lsrA.getDestNICviaIP(7, true).getId().compareTo("lsrAlsrB") == 0);
		Assert.assertTrue(lsrA.getDestNICviaIP(7, false).getId().compareTo("lsrAlsrB") == 0);
		Assert.assertTrue(lsrB.getDestNICviaIP(7, true).getId().compareTo("lsrBlsrCControl") == 0);
		Assert.assertTrue(lsrB.getDestNICviaIP(7, false).getId().compareTo("lsrBlsrCData") == 0);
		Assert.assertTrue(lsrC.getDestNICviaIP(7, true).getId().compareTo("lsrClsrDControl") == 0);
		Assert.assertTrue(lsrC.getDestNICviaIP(7, false).getId().compareTo("lsrClsrDData") == 0);
		Assert.assertTrue(lsrD.getDestNICviaIP(7, true).getId().compareTo("lsrDlsrEControl") == 0);
		Assert.assertTrue(lsrD.getDestNICviaIP(7, false).getId().compareTo("lsrDlsrEData") == 0);
		Assert.assertTrue(lsrE.getDestNICviaIP(7, true).getId().compareTo("lsrElsrFControl") == 0);
		Assert.assertTrue(lsrE.getDestNICviaIP(7, false).getId().compareTo("lsrElsrFData") == 0);
		Assert.assertTrue(lsrF.getDestNICviaIP(7, true).getId().compareTo("lsrFlsrG") == 0);
		Assert.assertTrue(lsrF.getDestNICviaIP(7, false).getId().compareTo("lsrFlsrG") == 0);
	}
	
	/**
	 * Tests whether the dijkstra's algorithm code will work for both the data plane and
	 * the control plane going from right to left.
	 */
	@Test
	public void testRoutingRightToLeft(){
		Assert.assertTrue(lsrG.getDestNICviaIP(1, true).getId().compareTo("lsrGlsrF") == 0);
		Assert.assertTrue(lsrG.getDestNICviaIP(1, false).getId().compareTo("lsrGlsrF") == 0);
		Assert.assertTrue(lsrF.getDestNICviaIP(1, true).getId().compareTo("lsrFlsrEControl") == 0);
		Assert.assertTrue(lsrF.getDestNICviaIP(1, false).getId().compareTo("lsrFlsrEData") == 0);
		Assert.assertTrue(lsrE.getDestNICviaIP(1, true).getId().compareTo("lsrElsrDControl") == 0);
		Assert.assertTrue(lsrE.getDestNICviaIP(1, false).getId().compareTo("lsrElsrDData") == 0);
		Assert.assertTrue(lsrD.getDestNICviaIP(1, true).getId().compareTo("lsrDlsrCControl") == 0);
		Assert.assertTrue(lsrD.getDestNICviaIP(1, false).getId().compareTo("lsrDlsrCData") == 0);
		Assert.assertTrue(lsrC.getDestNICviaIP(1, true).getId().compareTo("lsrClsrBControl") == 0);
		Assert.assertTrue(lsrC.getDestNICviaIP(1, false).getId().compareTo("lsrClsrBData") == 0);
		Assert.assertTrue(lsrB.getDestNICviaIP(1, true).getId().compareTo("lsrBlsrA") == 0);
		Assert.assertTrue(lsrB.getDestNICviaIP(1, false).getId().compareTo("lsrBlsrA") == 0);
	}
	
	/**
	 * For now this method will test the createPacket method of a PSC to make sure
	 * it properly initiates the PATH message sequence.
	 */
	@Test
	public void testPATHInit(){
		lsrA.createPacket(7);
		for(int i = 0;i<30;i++){
			this.tock();
		}
	}
	
}
