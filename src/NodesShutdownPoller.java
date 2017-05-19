
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
			
			Node tmpNode;
			
			for (int i = 0; i < VirtualLayerManager.nodeClients.size(); i++) {				
				
				tmpNode = VirtualLayerManager.nodeClients.get(i);
				
				if (tmpNode.terminalFrame.shutdown && tmpNode.isActive) {
					VirtualLayerManager.removeNode(tmpNode.terminalFrame.localUpdatedNode);
				}				
								
			}
			
		}
		
	}
	
}