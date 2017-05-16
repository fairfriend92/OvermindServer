
public class NodesShutdownPoller extends Thread {
	
	static boolean shutdown = false;

	@Override
	public void run() {		
		super.run();
		
		while (!shutdown) {			
			
			try {
				NodesShutdownPoller.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			for (int i = 0; i < VirtualLayerManager.syncFrames.size(); i++) {				
				
				if (VirtualLayerManager.syncFrames.get(i).shutdown) {
					VirtualLayerManager.removeNode(VirtualLayerManager.syncFrames.get(i).localUpdatedNode);
				}				
								
			}
			
		}
		
	}
	
}