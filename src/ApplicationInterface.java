import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Class used by the server to interface with applications that leverage
 * the Overmind network. 
 */

public class ApplicationInterface {
	
	private static ArrayList<RegisteredApp> registeredApps = new ArrayList<>(Constants.MAX_REGISTERED_APPS);
	
	/**
	 * Method used by external applications to register and commence interaction with
	 * the Overmind. RegisteredApp objects are used to signal every change of note that occurs
	 * to the network, so that each registered application can take the necessary steps to account
	 * for it. 
	 */
	
	public static RegisteredApp registerApplication(int maxRemoveNodes, String appName) {		
		RegisteredApp registeredApp;
		
		if (registeredApps.size() == Constants.MAX_REGISTERED_APPS) {
			return null;
		}
		else {
			registeredApp = new RegisteredApp(maxRemoveNodes, appName);
			registeredApps.add(registeredApp);
			
			/*
			 * Update the GUI with the registered apps.
			 */
			
			if (MainFrame.registeredAppsListModel.contains("No app registered")) {
				MainFrame.registeredAppsListModel.clear();
			}			
			MainFrame.registeredAppsListModel.addElement(appName);
			MainFrame.mainPanel.revalidate();
			MainFrame.mainPanel.repaint();
		}
		
		return registeredApp;
	}
	
	/**
	 * Method used to put in the registeredApps' queues the latest RemovedNode. 
	 */
	
	public static void addRemovedNode(RemovedNode removedNode) {
		for (RegisteredApp registeredApp : registeredApps) {
			if (registeredApp.removedNodes.size() < registeredApp.maxRemovedNodes) {
				registeredApp.removedNodes.add(removedNode);
			}
		}
	}
	
	
	/**
	 * Class holding all the necessary info about a removed node for an application
	 * to decide how to handle the deletion. 
	 */
	
	public static class RemovedNode {
		Node removedNode, shadowNode; // The node that has been removed and the proposed replacement.
		
		public RemovedNode(Node removedNode, Node shadowNode) {
			this.removedNode = removedNode;
			this.shadowNode = shadowNode;
		}		
		
		public RemovedNode(Node removedNode) {
			this.removedNode = removedNode;
			this.shadowNode = null;
		}
	}
	
	/**
	 * Class that represents an application which is interfacing with the Overmind network.
	 */
	
	public static class RegisteredApp {
		private int maxRemovedNodes; // Capacity of the removedNodes queue.
		String appName;
		BlockingQueue<RemovedNode> removedNodes;
		
		 private RegisteredApp(int maxRemovedNodes, String appName) {
			this.maxRemovedNodes = maxRemovedNodes;
			this.appName = appName;
			removedNodes  = new ArrayBlockingQueue<>(maxRemovedNodes);
		}
	}
	
}
