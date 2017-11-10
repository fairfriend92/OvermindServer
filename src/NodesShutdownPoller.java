import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class NodesShutdownPoller extends Thread {
	
	boolean shutdown = false;
	
	static BlockingQueue<Node> nodesToBeRemoved = new ArrayBlockingQueue<>(32);


	@Override
	public void run() {		
		super.run();
		
		while (!shutdown) {			

			Node tmpNode = new Node(null, null, null);
			
			try {
				tmpNode = nodesToBeRemoved.take();
			} catch (InterruptedException e) {
				System.out.println("NodesShutdownPoller interrupted");
				break; 
			}
			
			if (tmpNode.isShadowNode)
				VirtualLayerManager.removeShadowNode(tmpNode);	
			else			
				VirtualLayerManager.removeNode(tmpNode, true);			
			
		}
		
	}
	
}