/**
 * @author andy modified by btello
 * @version 1.0
 * @date 24-10-2008
 * @since 1.0
 */

package DataTypes;

import NetworkElements.*;

public class NICLabelPair implements Comparable<NICLabelPair>{
	private LSRNIC nic; // The nic of the pair
	private int label; // the label of the pair
	
	/*
	 * set the label of this pair to this value when a PATH message has been sent 
	 * but the RESV has not been received yet.
	 */
	public final static int LSP_PENDING = -1;
	
	/**
	 * Constructor for a pair of (nic, label)
	 * @param nic the nic that is in the pair
	 * @param label the label that is in the pair
	 * @since 1.0
	 */
	public NICLabelPair(LSRNIC nic, int label){
		this.nic = nic;
		this.label = label;
	}
	
	/**
	 * Returns the nic that makes up half of the pair
	 * @return the nic that makes up half of the pair
	 * @since 1.0
	 */
	public LSRNIC getNIC(){
		return this.nic;
	}
	
	/**
	 * Returns the nic that makes up half of the pair
	 * @return the nic that makes up half of the pair
	 * @since 1.0
	 */
	public int getLabel(){
		return this.label;
	}
	
	/**
	 * Sets the label for this pair to another value.
	 * @param label the new label to use.
	 */
	public void setLabel(int label){
		this.label = label;
	}
	
	/**
	 * Returns whether or not a given object is the same as this pair. I.e.
	 * is it a pair containing the nic and label.
	 * @return true/false the given object of the same as this object
	 * @since 1.0
	 */
	public boolean equals(Object o){
		if(o instanceof NICLabelPair){
			NICLabelPair other = (NICLabelPair) o;
			
			if(other.getNIC()==this.getNIC() && other.getLabel()==this.getLabel())
				return true;
		}
		
		return false;
	}
	
	/**
	 * Allows this object to be used in a TreeMap
	 * @returns if this object is less than, equal to, or greater than a given object
	 * @since 1.0
	 */
	public int compareTo(NICLabelPair o){
		return this.getLabel()-o.getLabel();
	}
}
