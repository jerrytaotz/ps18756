package DataTypes;

import java.util.*;

public class FIFOQueue{
	private int weight=1;
	private int bitsRoutedSinceLastPacketSend=0;
	private ArrayList<IPPacket> packets = new ArrayList<IPPacket>();
	private ArrayList<IPPacket> readyPackets = new ArrayList<IPPacket>();
	
	/**
	 * Default constructor for a FIFO Queue
	 */
	public FIFOQueue(){}
	
	/**
	 * Sets the weight of this queue
	 * @param weight the weight of this queue
	 */
	public void setWeight(int weight){
		this.weight = weight;
	}
	
	/**
	 * Gets the weight of this queue
	 * @return the weight of this queue
	 */
	public int getWeight(){
		return this.weight;
	}
	
	/**
	 * Adds one to the number of bits that have moved from this input queue to the 'output queue'
	 */
	public void routeBit(){
		//don't do anything if this queue is empty.
		if(this.packets.size()!=0){
			//route a single bit. (just increments the count of bits routed)
			//returns true when a packet has had all of its bits routed
			if(packets.get(0).routeBit() == true){
				//if the packet is ready to go, put it in the chute.
				this.readyPackets.add(this.remove());
			}
			this.bitsRoutedSinceLastPacketSend+=1;
		}
	}
	
	public IPPacket ready(){
		if(this.readyPackets.size()>0){
			return readyPackets.remove(0);
		}
		return null;
	}
	
	/**
	 * returns the number of bits that have been routed since a packet was last removed from the queue
	 * @return the number of bits that have been routed since a packet was last removed from the queue
	 */
	public int getBitsRoutedSinceLastPacketSent(){
		return this.bitsRoutedSinceLastPacketSend;
	}
	
	/**
	 * Returns the packet that is second to last in the queue
	 * @return the packet that is second to last in the queue
	 */
	public IPPacket secondLastPeek(){
		if(this.packets.size()>1)
			return this.packets.get(this.packets.size()-2);
		else if(this.packets.size()==1)
			return this.packets.get(this.packets.size()-1);
		else return null;
	}
	
	/**
	 * Increases the time the packet has been delayed
	 */
	public void tock(){
		for(IPPacket packet:packets)
			packet.addDelay(1);
	}
	
	// http://java.sun.com/j2se/1.5.0/docs/api/java/util/Queue.html
	public IPPacket element(){
		if(this.peek()!=null)
			return this.peek();
		else
			throw new NoSuchElementException();
	}
	
	/**
	 * Add a packet to the end of the queue.
	 */
	public boolean offer(IPPacket packet){
		this.packets.add(packet);
		return true;
	}
	
	public IPPacket peek(){
		if(this.packets.size()>0)
			return this.packets.get(0);
		else return null;
	}
	
	public IPPacket remove(){
		if(this.packets.size()>0){
			IPPacket ret = this.packets.get(0);
			this.packets.remove(0);
			this.bitsRoutedSinceLastPacketSend = 0;
			return ret;
		}
		else return null;
	}
}
