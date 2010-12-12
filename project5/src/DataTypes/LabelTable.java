package DataTypes;
/**
 * A table containing rows for every LSP in a given LSR.  A row has the following format:
 * int source | int dest | int input label | NICLabelPair output nic and label
 * Once an LSP is added, a new row is added to the table.  Lookups can be done in a variety
 * of fashions (see method documentation).
 * @author Brady
 */

import java.util.*;

import NetworkElements.LSR;
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
	public NICLabelPair get(Label inLabel){
		for(ltEntry e:entries){
			if(e.getInLabel().equals(inLabel)){
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
	public NICLabelPair get(Label inLabel,LSRNIC nic){
			for(ltEntry e:entries){
				if((e.getNLPair().getNIC() == nic) && e.getInLabel().equals(inLabel)){
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
	public void put(int source, int dest, Label inLabel, NICLabelPair nlPair){

		/*Add the entry to the table*/
		ltEntry newEntry = new ltEntry(source, dest, inLabel, nlPair);
		entries.add(newEntry);
		
	}
	
	/**
	 * Returns the input label for a given source/destination pair.
	 * @param source the transmitting end of the LSP
	 * @param dest the receiving end of the LSP
	 * @return the Label corresponding to <source,dest>.  null if none exists
	 */
	public Label getInLabel(int source, int dest){
		for(ltEntry e:entries){
			if(e.getSource() == source && e.getDest() == dest){
				return e.getInLabel();
			}
		}
		return null;
	}
	
	/**
	 * Determine whether or not a specific inLabel is in this table.
	 * @param inLabel the label to be queried for
	 * @return true if the label existed in the table
	 * @return false otherwise
	 */
	public boolean containsInLabel(Label inLabel){
		for(ltEntry e:entries){
			//Check if the label is actually an optical label and if it matches
			if(e.getInLabel().equals(inLabel)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determine whether an integer input label is already present in the table.
	 * @param inLabel the label to be queried for
	 * @return true if the label existed in the table
	 * @return false otherwise
	 */
	public boolean containsIntInLabel(int inLabel){
		for(ltEntry e:entries){
			//Check if the label is actually an optical label and if it matches
			if(!e.getInLabel().isOptical()){
				if(e.getInLabel().getIntVal() == inLabel){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns an array list containing all of the output labels associated with 'nic'
	 * @param nic the nic you want to find the output labels for.
	 */
	public ArrayList<Label> getOutLabels(LSRNIC nic){
		ArrayList<Label> outLabelList = new ArrayList<Label>();
		for(ltEntry e:this.entries){
			if(e.getNLPair().getNIC() == nic){
				outLabelList.add(e.getNLPair().getLabel());
			}
		}
		return outLabelList;
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
	 * Delete the row in the label table corresponding to the specified source and dest
	 * @param source the source node
	 * @param dest the destination node
	 * @return true if the entry was successfully deleted. false otherwise.
	 */
	public boolean delete(int source, int dest){
		ltEntry removeEntry = null;
		for(ltEntry entry:entries){
			if(entry.getSource() == source && entry.getDest() == dest){
				removeEntry = entry;
			}
		}
		if(removeEntry != null){
			entries.remove(removeEntry);
			return true;
		}
		else{
			System.out.println("ERROR: Tried to delete a non-existent LSP");
			return false;
		}
	}
	
	
	/**
	 * A private class which represents a row in the table.
	 * @author Brady
	 */
	private class ltEntry{
		private int source;
		private int dest;
		private Label inLabel;
		private NICLabelPair nlPair;
		
		/**
		 * Create a new row in the label table.
		 * @param source 
		 * @param dest
		 * @param inLabel
		 * @param nlPair
		 */
		public ltEntry(int source, int dest, Label inLabel, NICLabelPair nlPair){
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
		
		public Label getInLabel(){
			return this.inLabel;
		}
		
		public NICLabelPair getNLPair(){
			return this.nlPair;
		}
	}
}
