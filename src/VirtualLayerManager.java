import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualLayerManager extends Thread{
	
	// TODO put these constants in Constants
	
	public final static int SERVER_PORT_TCP = 4195;
	public final static int SERVER_PORT_UDP = 4196;
	private final static int IPTOS_RELIABILITY = 0x04;
	
	static boolean shutdown = false;	
	
	static ArrayList<com.example.overmind.LocalNetwork> unsyncNodes = new ArrayList<>();
	static ArrayList<com.example.overmind.LocalNetwork> syncNodes = new ArrayList<>();
	static ArrayList<LocalNetworkFrame> syncFrames = new ArrayList<>();
	static ArrayList<Node> nodeClients = new ArrayList<>();
	static ArrayList<com.example.overmind.LocalNetwork> availableNodes = new ArrayList<>();	
	
	static public String serverIP = null;
			
	@Override
	public void run() {
		super.run();
		
        try (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
            serverIP = s.next();
        } catch (java.io.IOException e) {
        	e.printStackTrace();
        }
        
        assert serverIP != null;
        
        System.out.println("This server has IP " + serverIP);
		
		BlockingQueue<Socket> clientSocketsQueue = new ArrayBlockingQueue<>(16);
		
		/**
		 * Build the TCP server socket which listens for physical devices ready to connect
		 */
		
		ServerSocket serverSocket = null;		
		
		try {
			serverSocket = new ServerSocket(SERVER_PORT_TCP);
		} catch (IOException e) {
        	e.printStackTrace();
		}
		
		assert serverSocket != null;
		
		/**
		 * Build the datagram input socket which listens for test packets from which the nat port can be retrieved
		 */
				
		DatagramSocket inputSocket = null;

        try {
            inputSocket = new DatagramSocket(SERVER_PORT_UDP);
        } catch (SocketException e) {
        	e.printStackTrace();
        }

        assert inputSocket != null;
        
        /**
         * Start the worker thread which reads LocalNetwork objects from the streams established by the clients
         */
        
        Socket clientSocket = null;		
        
        com.example.overmind.LocalNetwork thisServer = new com.example.overmind.LocalNetwork();
        thisServer.ip = serverIP;
        thisServer.natPort = 4194;        
						
		while (!shutdown) {
			
			/**
			 * Open new connections with the clients
			 */
		
			// Accept connections from the clients			
			try {				
				clientSocket = serverSocket.accept();
				clientSocketsQueue.put(clientSocket);
			} catch (IOException|InterruptedException e) {
	        	e.printStackTrace();
			}		
			
			com.example.overmind.LocalNetwork localNetwork = null; 					
			ObjectInputStream input = null;
			
			// Receive data stream from the client
			try {					
				input = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
	        	e.printStackTrace();
			}
			
			assert input != null;
			
			// Read the localNetwork class from the data stream
			try {
				localNetwork = (com.example.overmind.LocalNetwork) input.readObject();					
			} catch (IOException | ClassNotFoundException e) {
	        	e.printStackTrace();
			}
			
			assert localNetwork != null;
				
			/**
			 * Retrieve nat port of the current device 
			 */			
			
			try {				
						
				byte[] testPacketBuffer = new byte[1];
				
				DatagramPacket testPacket = new DatagramPacket(testPacketBuffer, 1);				
			
				inputSocket.receive(testPacket);				
				
				localNetwork.natPort = testPacket.getPort();
				
				localNetwork.ip = testPacket.getAddress().toString().substring(1);
			
				System.out.println("Nat port for device with IP " + localNetwork.ip + " is " + localNetwork.natPort);

				
			} catch (IOException e) {
	        	e.printStackTrace();
			}			
			
			localNetwork.postsynapticNodes.add(thisServer);
				
			Node newNode = new Node(localNetwork.ip, clientSocket);
			newNode.initialize();
			
			nodeClients.add(newNode);			
			
			assert localNetwork != null;	
			
			connectDevices(localNetwork);
								
		}
		/* [End of while(!shutdown)] */
							
	}
	/* [End of run() method] */	
	
	public synchronized static void connectDevices(com.example.overmind.LocalNetwork localNetwork) {
		
		/**
		 * Populate and update the list of terminals available for connection
		 */
		
		// The algorithm starts only if the list has at least one element
		if (!availableNodes.isEmpty() && !availableNodes.contains(localNetwork)) {
		
			// Iterate over all the available terminals
			for (int i = 0; i < availableNodes.size()
					|| (localNetwork.numOfDendrites == 0 && localNetwork.numOfSynapses == 0); i++) {

				com.example.overmind.LocalNetwork currentNode = availableNodes.get(i);

				// Branch depending on whether either the synapses or the dendrites of the current node are saturated
				if (currentNode.numOfDendrites - localNetwork.numOfNeurons >= 0
						&& localNetwork.numOfSynapses - currentNode.numOfNeurons >= 0 
						&& currentNode.postsynapticNodes.size() >= currentNode.presynapticNodes.size()) {

					// Update the number of dendrites and synapses for the current node and the local network
					currentNode.numOfDendrites -= localNetwork.numOfNeurons;
					localNetwork.numOfSynapses -= currentNode.numOfNeurons;

					// Update the list of connected terminals for both the node and the local network
					currentNode.presynapticNodes.add(localNetwork);
					localNetwork.postsynapticNodes.add(currentNode);

					// Send to the list of terminals which need to be updated the current node
					unsyncNodes.set(unsyncNodes.indexOf(currentNode), currentNode);
					
					// Update the current node in the list availableNodes
					availableNodes.set(i, currentNode);

				} else if (currentNode.numOfSynapses - localNetwork.numOfNeurons >= 0
						&& localNetwork.numOfDendrites - currentNode.numOfNeurons >= 0) {

					/**
					 * Just as before but now synapses and dendrites are exchanged
					 */

					currentNode.numOfSynapses -= localNetwork.numOfNeurons;
					localNetwork.numOfDendrites -= currentNode.numOfNeurons;

					currentNode.postsynapticNodes.add(localNetwork);
					localNetwork.presynapticNodes.add(currentNode);

					unsyncNodes.set(unsyncNodes.indexOf(currentNode), currentNode);
					availableNodes.set(i, currentNode);

				} else if (currentNode.numOfSynapses == 0 && currentNode.numOfDendrites == 0) {
					// If BOTH the synapses and the dendrites of the current node are saturated it can be removed
					availableNodes.remove(i);
				}
				/* [End of the inner if] */

			}
			/* [End of for loop] */
			
			// If either the dendrites or the synapses of the local network are not saturated it can be added to the list
			if (localNetwork.numOfDendrites > 0 || localNetwork.numOfSynapses > 0) {
				availableNodes.add(localNetwork);
			} 
			
			unsyncNodes.add(localNetwork);	

			
		} else if (availableNodes.isEmpty()) {
			
			// Add the local network automatically if the list is empty
			availableNodes.add(localNetwork);		
			unsyncNodes.add(localNetwork);	
			  
		} else if (availableNodes.contains(localNetwork)) {
			
			// If availableNodes contains the localNetwork it needs only to update its reference
			availableNodes.set(availableNodes.indexOf(localNetwork), localNetwork);					
			unsyncNodes.add(localNetwork);	
			
		}
		/* [End of the outer if] */		
									
		MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), syncNodes.size()));
		
	}

	public synchronized static void syncNodes() {		
		
		/**
		 * Sync the GUI with the updated info about the nodes
		 */
		
		if (!unsyncNodes.isEmpty()) {		
			
			for (int i = 0; i < unsyncNodes.size(); i++) {
				
				LocalNetworkFrame tmp;
				
				// Branch depending on whether the node is new or not
				if (!syncNodes.contains(unsyncNodes.get(i))) {					
			
					tmp = new LocalNetworkFrame();
					tmp.update(unsyncNodes.get(i));
					
					// The node is new so a new frame needs to be created
					tmp.display();
					
					// Add the new window to the list of frames 
					syncFrames.add(tmp);
					
					// Add the new node to the list of sync nodes
					syncNodes.add(unsyncNodes.get(i));
										
					
				} else {					
					
					int index = syncNodes.indexOf(unsyncNodes.get(i));					
					
					// Since the node is not new its already existing window must be retrieved from the list
					// TODO instead of using index to retrieve frame we could write a method with argument the LocalNetwork itself
					tmp = syncFrames.get(index);
					
					// The retrieved window needs only to be updated 
					tmp.update(unsyncNodes.get(i));
					
					// The old node is substituted with the new one in the list of sync nodes
					syncNodes.set(index, unsyncNodes.get(i));
					
				}
				
				/**
				 * Updated info regarding the current node are sent back to the physical device
				 */
					
				try {
					
					// Temporary object holding the info regarding the local network of the current node
					com.example.overmind.LocalNetwork tmpLN = unsyncNodes.get(i);
					
					// Use the indexOf method to retrieve the current node from the nodeClients list
					int index = nodeClients.indexOf(new Node(tmpLN.ip, null));
					
					// The node whose informations need to be sent back to the physical device
					Node pendingNode = nodeClients.get(index);	
					
					com.example.overmind.LocalNetwork tmpLocalNetwork = new com.example.overmind.LocalNetwork();
					tmpLocalNetwork.update(unsyncNodes.get(i));
									
					// Write the info in the steam
					pendingNode.output.writeObject(tmpLocalNetwork);						
								
					//pendingNode.output.reset();										
										
				} catch (IOException e) {
		        	e.printStackTrace();
				}				
								
			}				
			
			unsyncNodes.clear();	
			
			SpikesSorter.updateNodeFrames(syncFrames);
			MainFrame.updateMainFrame(new MainFrameInfo(0, syncNodes.size()));
			
		}
		
	}

}
/* [End of VirtualLayerManager class] */
