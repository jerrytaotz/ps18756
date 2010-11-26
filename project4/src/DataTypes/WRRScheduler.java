package DataTypes;
/**
 * This class will be responsible for taking a set of DiffServ
 * queues and scheduling them for output.  It will do so in 
 * such a fashion that EF will experience low delay, low jitter,
 * and low loss.
 * @author btello
 *
 */

import java.util.*;

import NetworkElements.LSRNIC;

public class WRRScheduler {
	
	LSRNIC parent;
	
	public WRRScheduler(LSRNIC parent){
		this.parent = parent;
	}
	
	/**
	 * The main scheduling algorithm for the DiffServ enabled LSR.  Meets EF contract by meeting
	 * each of the required EF requirements:
	 * low delay: forwards every packet in a reservation stream every round
	 * low jitter: forwards no more than the requested b/w for a stream.
	 * low loss: will not drop packets
	 * Meets AF requirements by harvesting unused bandwidth from all queues and passing it on
	 * to AF queues in top down order.  For example, if there are 10 packets/t of unused BW 
	 * and AF1 needs an extra 5 p/t, AF2 needs an extra 4, AF3 needs 2, and AF4 needs 6 then 
	 * AF1 gets 5, AF2 gets 4, AF3 gets 1 and AF4 gets 0 (5+4+1 = 10).
	 * BE traffic is serviced only if there is some leftover bandwidth after servicing all the
	 * other queues.
	 * @param DSQueues - the diffserv queues
	 * @param outBuf - the main output queue for the LSR
	 */
	public void nextRound(ArrayList<FIFOQueue> DSQueues,ArrayList<Packet> outBuf){
		int yield = 0; //Unused bandwidth counter
		int extra = 0;
		int numToSend = 0,tempToSend = 0;
		Packet nextP;
		FIFOQueue nextQueue;
		
		/*find the total unused bandwidth*/
		for(FIFOQueue q:DSQueues){
			extra = q.getWeight() - q.getNumPackets();
			if(extra > 0) yield += extra;
		}
		
		/*service EF queue*/
		nextQueue = DSQueues.get(LSRNIC.EF);
		nextQueue.incrementDelays();
		numToSend = Math.min(nextQueue.getWeight(), nextQueue.getNumPackets());
		for(int i = 0;i<numToSend;i++){
			nextP = nextQueue.remove();
			outBuf.add(nextP);
			sentData(nextP);
		}
		
		/*service the remaining queues*/
		for(int i = LSRNIC.AF1;i<DSQueues.size();i++){
			nextQueue = DSQueues.get(i);
			nextQueue.incrementDelays();
			
			numToSend = Math.min(nextQueue.getWeight(),nextQueue.getNumPackets());
			/*find out if this queue needs extra bandwidth*/
			if(nextQueue.getNumPackets() > nextQueue.getWeight()){
				/*ok it could use extra but do we have any available?*/
				if(yield > 0){
					tempToSend = numToSend;
					/*arg 1: yield > needed; arg 2: yield < needed*/
					numToSend += Math.min(nextQueue.getNumPackets() - nextQueue.getWeight(),
							nextQueue.getWeight() + yield);
					yield -= numToSend - tempToSend;
				}
			}
			/*send the packets*/
			for(int j = 0;j < numToSend; j++){
				nextP = nextQueue.remove();
				outBuf.add(nextP);
				sentData(nextP);
			}
		}
	}
	
	public void sentData(Packet p){
		int address = parent.getParent().getAddress();
		System.out.println("DATA: Router " + address + "): Transmitted data to " + p.getDest() 
				+ ": " + p.getID());
	}
}
