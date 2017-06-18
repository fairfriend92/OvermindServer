import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class NodesShutdownPoller extends Thread {
	
	static boolean shutdown = false;
	
	static BlockingQueue<Node> nodesToBeRemoved = new ArrayBlockingQueue<>(4);


	@Override
	public void run() {		
		super.run();
		
		while (!shutdown) {			

			Node tmpNode = new Node(null, null, null);
			
			try {
				tmpNode = nodesToBeRemoved.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			VirtualLayerManager.removeNode(tmpNode);			
			
		}
		
	}
	
}