package NetworkElements;
import java.util.*;

import dijkstra.DirectedGraph;
import dijkstra.PathCalculator;

public class PscLSR extends LSR {

	private HashMap<Integer,LSRNIC> routingTable;
	
	public PscLSR(int address) {
		super(address);
		this.routingTable = new HashMap<Integer,LSRNIC>();
	}
	
	/**
	 * Updates the routing tables in this router using
	 * Dijkstra's algorithm.
	 * @param map the network map to use.  This simulates receiving
	 * a link state message.
	 */
	public void updateRoutingTable(DirectedGraph map){
		//path calculator only works on a symbolic level, doesn't know about NICs
		HashMap<Integer, Integer> symbolicRoutingTable; 
		pc.dijkstrasAlgorithm(map, this.address);
		symbolicRoutingTable = pc.findBestPaths(map, this.address);

		this.routingTable.clear();
		//Populate the actual routing table with the symbolic info.
		for(Integer dest:symbolicRoutingTable.keySet()){
			for(LSRNIC nic:nics){
				if(nic.getNeighborAddress() == symbolicRoutingTable.get(dest)){
					this.routingTable.put(dest, nic);
					break;
				}
			}
		}
	}

	/**
	 * Print an error message containing the type of this LSR and it's address.
	 * @param errorMsg the error message you would like printed.
	 */
	@Override
	public void errorPrint(String errorMsg) {
		System.out.println("(PSC Router " + this.getAddress() + "): ERROR " + errorMsg);
	}

	@Override
	public LSRNIC getDestNICviaIP(int dest, boolean control) {
		if(!routingTable.containsKey(dest)){
			errorPrint("This router does not contain a path to " + dest);
			System.exit(0);
		}
		return routingTable.get(dest);
	}
}
