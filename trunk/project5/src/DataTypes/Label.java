package DataTypes;
/**
 * This class represents the generalized labels of GMPLS.
 * It can contain either an integer label or an optical label.
 * It can never contain both.
 * @author Brady
 */

public class Label {
	private int label;
	private OpticalLabel oLabel;
	private boolean isOpticalLabel;
	private boolean lspPending; //is this label pending on the receipt of a RESV msg.?
	
	/**
	 * Use this constructor if you would like to create a new Optical label 
	 * @param label
	 */
	public Label(OpticalLabel label){
		this.label = 0;
		this.isOpticalLabel = true;
		this.oLabel = label;
		lspPending = false;
	}
	
	/**
	 * Use this constructor if you would like to create a new Integer
	 * MPLS label.
	 * @param label
	 */
	public Label(int label){
		this.label = label;
		this.isOpticalLabel = false;
		this.oLabel = OpticalLabel.NA;
		lspPending = false;
	}
	
	/**
	 * Tells you whether or not this is an optical label.
	 * @return true if this is an optical label
	 * @return false if this is an integer label
	 */
	public boolean isOptical(){
		return this.isOpticalLabel;
	}
	
	/**
	 * returns the integer label if this is indeed an integer label. Otherwise
	 * it will return -1.
	 * @return the integer label for this class if it is an integer label
	 * -1 otherwise
	 */
	public int getIntVal(){
		if(this.isOpticalLabel){
			return -1;
		}
		else return this.label;
	}
	
	/**
	 * returns the optical label if this is indeed an optical label. Otherwise
	 * it will return -1.
	 * @return the optical label.  Returns OpticalLabel.NA if this is not an Optical label
	 */
	public OpticalLabel getOptVal(){
		return this.oLabel;
	}
	
	/**
	 * Determine whether this label signifies a pending LSP setup
	 * @return true if this label denotes a pending LSP setup
	 * false otherwise
	 */
	public boolean isPending(){
		return this.lspPending;
	}
	
	/**
	 * Set whether or not this label is pending on the receipt of a RESV message.
	 * @param pending true if this label is not confirmed yet. False if this label is 
	 * being confirmed
	 */
	public void setIsPending(boolean pending){
		this.lspPending = pending;
	}
	
	/**
	 * Make a deep copy of this label
	 */
	@Override
	public Label clone(){
		Label cloneLabel;
		if(this.isOpticalLabel){
			cloneLabel = new Label(this.oLabel);
			cloneLabel.setIsPending(this.lspPending);
		}
		else{
			cloneLabel = new Label(this.label);
			cloneLabel.setIsPending(this.lspPending);
		}
		return cloneLabel;
	}
	
	@Override
	public String toString(){
		if(this.isOpticalLabel){
			if(this.isPending()) return this.oLabel.toString().concat("(Pending)");
			else return this.oLabel.toString();
		}
		else{
			if(this.isPending()) return String.valueOf(this.label).concat("(Pending)");
			return String.valueOf(this.label);
		}
	}
	
	/**
	 * Determines the equality of two Label objects based on their contained label values.
	 * @param compareLabel the label you would like to test for equality against this label.
	 * @return true if the values in the two labels are identical
	 * false otherwise
	 */
	public boolean equals(Label compareLabel){
		if(this.isOpticalLabel){
			if(!compareLabel.isOptical()){
				return false;
			}
			else if(this.oLabel.equals(compareLabel.getOptVal())){
				return true;
			}
			else return false;
		}
		else{
			if(compareLabel.isOptical()){
				return false;
			}
			else if(this.label == compareLabel.getIntVal()){
				return true;
			}
			else return false;
		}
	}
}
