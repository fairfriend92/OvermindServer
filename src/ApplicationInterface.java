import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Class used by the server to interface with applications that leverage
 * the OverMind network. 
 */

public class ApplicationInterface {	
	
	
	/**
	 * Class holding all the necessary info about a removed node for an application
	 * to decide how to handle the deletion. 
	 */
	
	public static class RemovedNode {
		private Node removedNode, shadowNode; // The node that has been removed and the proposed replacement.
		
		public void RemoveNode(Node removedNode, Node shadowNode) {
			this.removedNode = removedNode;
			this.shadowNode = shadowNode;
		}		
	}
	
}
