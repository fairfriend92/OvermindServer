
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
			
			for (int i = 0; i < VirtualLayerManager.nodeClients.size(); i++) {				
				
				if (VirtualLayerManager.nodeClients.get(i).terminalFrame.shutdown) {
					VirtualLayerManager.removeNode(VirtualLayerManager.nodeClients.get(i).terminalFrame.localUpdatedNode);
				}				
								
			}
			
		}
		
	}
	
}