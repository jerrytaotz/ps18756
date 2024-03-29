package DataTypes;
/**
 * A table containing rows for every LSP in a given LSR.  A row has the following format:
 * int source | int dest | int input label | NICLabelPair output nic and label
 * Once an LSP is added, a new row is added to the table.  Lookups can be done in a variety
 * of fashions (see method documentation).
 * @author Brady
 */

import java.util.*;

import NetworkElements.LSRNIC;

public class LabelTable {

	private ArrayList<ltEntry> entries = new ArrayList<ltEntry>();
	
	/**
	 * Determine whether or not a label already exists for the specified source/dest. LSRs
	 * @param source the address of the LSR which is the source of traffic
	 * @param dest the address of the LSR which is the receiver of traffic
	 * @return true if there is an existing label between these two points. false otherwise
	 */
	public boolean labelExists(int source, int dest){
		for(ltEntry e:entries){
			/*scan the table*/
			if(e.getSource() == source && e.getDest() == dest){
				return true;
			}
		}
		/*no label exists for these end points*/
		return false;
	}

	/**
	 * Get a NICLabel pair for a specific input label.  Transmitting nodes should use this
	 * version of the function since the input label will be unique across all NICS.  Other
	 * methods should use the version which also takes a NIC since labels may not be
	 * unique across all nics.
	 * @param inLabel the input label you are interested in.
	 * @return the corresponding NICLabelPair, null if one did not exist for inLabel
	 */
	public NICLabelPair get(int inLabel){
		for(ltEntry e:entries){
			if(e.getInLabel() == inLabel){
				return e.getNLPair();
			}
		}
		return null;
	}
	
	/**
	 * Get a NICLabel pair for a specific destination.
	 * @param dest the destination you are interested in.
	 * @return the corresponding NICLabelPair, null if one did not exist for inLabel
	 */
	public NICLabelPair getOutPair(int dest){
		for(ltEntry e:entries){
			if(e.getDest() == dest){
				return e.getNLPair();
			}
		}
		return null;
	}
	
	/**
	 * Get a NICLabel pair for a specific input label.
	 * @param inLabel the input label you are interested in.
	 * @return the corresponding NICLabelPair, null if one did not exist for inLabel
	 */
	public NICLabelPair get(int inLabel,LSRNIC nic){
		for(ltEntry e:entries){
			if(e.getNLPair().getNIC() == nic && e.getInLabel() == inLabel){
				return e.getNLPair();
			}
		}
		return null;
	}
	
	/**
	 * Insert a new row into this table.
	 * @param source the transmitting end of the LSP
	 * @param dest the receiving end of the LSP
	 * @param inLabel the input label for this node
	 * @param nlPair the output label and nic corresponding to the inLabel
	 */
	public void put(int source, int dest, int inLabel, NICLabelPair nlPair){
		ltEntry newEntry = new ltEntry(source, dest, inLabel, nlPair);
		entries.add(newEntry);
	}
	
	/**
	 * Returns the input label for a given source/destination pair.
	 * @param source the transmitting end of the LSP
	 * @param dest the receiving end of the LSP
	 * @return the label corresponding to <source,dest>.  -1 if none exists
	 */
	public int getInLabel(int source, int dest){
		for(ltEntry e:entries){
			if(e.getSource() == source && e.getDest() == dest){
				return e.getInLabel();
			}
		}
		return -1;
	}
	
	/**
	 * Determine whether or not a specific inLabel is in this table.
	 * @param inLabel the label to be queried for
	 * @return true if the label existed in the table
	 * @return false otherwise
	 */
	public boolean containsInLabel(int inLabel){
		for(ltEntry e:entries){
			if(e.getInLabel() == inLabel){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Tells you if there is an LSP set up to a destination or not.
	 * @param dest the destination node of interest
	 * @return true if there is an LSP leading to dest.  false otherwise
	 */
	public boolean containsLSP(int dest){
		for(ltEntry e:entries){
			if(e.getDest() == dest){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * A private class which represents a row in the table.
	 * @author Brady
	 */
	private class ltEntry{
		private int source;
		private int dest;
		private int inLabel;
		private NICLabelPair nlPair;
		
		/**
		 * Create a new row in the label table.
		 * @param source 
		 * @param dest
		 * @param inLabel
		 * @param nlPair
		 */
		public ltEntry(int source, int dest, int inLabel, NICLabelPair nlPair){
			this.source = source;
			this.dest = dest;
			this.inLabel = inLabel;
			this.nlPair = nlPair;
		}
		
		/**
		 * =============================
		 * accessors and mutators
		 * =============================
		 */
		public int getSource(){
			return this.source;
		}
		
		public int getDest(){
			return this.dest;
		}
		
		public int getInLabel(){
			return this.inLabel;
		}
		
		public NICLabelPair getNLPair(){
			return this.nlPair;
		}
	}
}
