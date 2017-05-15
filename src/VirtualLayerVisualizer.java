import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class VirtualLayerVisualizer extends Thread{
	
	static boolean shutdown = false; 
	
	static BlockingQueue<ArrayList<Node>> unsyncNodesQueue = new ArrayBlockingQueue<>(1);
	private ArrayList<Node> unsyncNodes = new ArrayList<>();
		
	@Override
	public void run () {
		super.run();
		
		while (!shutdown) {
			
			try {
				unsyncNodes = unsyncNodesQueue.take();
			} catch (InterruptedException e ) {
				e.printStackTrace();
			} 			
			
			for (int i = 0; i < unsyncNodes.size(); i++) {
				
				
				
			}
			
			
		}
		
	}
	
	static void insertVLNode (Node newNode) {
		
	}

}
