/**
 * @author btello
 */

package DataTypes;

import NetworkElements.*;

public class NICLabelPair{
	private LSRNIC nic; // The nic of the pair
	private Label label; // the label of the pair
	private boolean lspPending;
	
	/**
	 * Constructor for a pair of (nic, label)
	 * @param nic the nic that is in the pair
	 * @param label the label that is in the pair
	 * @since 1.0
	 */
	public NICLabelPair(LSRNIC nic, Label label){
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
	public Label getLabel(){
		return this.label;
	}
	
	/**
	 * Sets the label for this pair to another value.
	 * @param label the new label to use.
	 */
	public void setLabel(Label label){
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
}
