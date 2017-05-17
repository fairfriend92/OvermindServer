/**
 * Called by my MainFrame to manage the connection and disconnection of the devices which make up the 
 * physical layer (PL). The connected devices form the virtual layer (VL) on top of which the Overmind is built.
 * 
 * TERMINOLOGY
 * 
 * a) Device: the physical terminal
 * b) Local network: the network hosted by the device
 * c) Node: the representation of the terminal in the virtual space
 * 
 * 1) Physical layer: the network of terminals 
 * 2) Virtual layer: the network of nodes managed by the server
 * 3) Overmind: the highest abstraction which implements the A.I. on top of the VL, which itself is
 * 		        a representation of the PL
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class VirtualLayerManager extends Thread{
	
	// TODO put these constants in Constants
	
	public final static int SERVER_PORT_TCP = 4195;
	public final static int SERVER_PORT_UDP = 4196;
	
	static boolean shutdown = false;	
	
	static ArrayList<Node> unsyncNodes = new ArrayList<>();
	static ArrayList<Node> syncNodes = new ArrayList<>();
	static ArrayList<TerminalFrame> syncFrames = new ArrayList<>();
	static ArrayList<Node> nodeClients = new ArrayList<>();
	static ArrayList<Node> availableNodes = new ArrayList<>();	
	
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
         * Start the worker thread which reads Terminal objects from the streams established by the clients
         */
        
        Socket clientSocket = null;		
        
        com.example.overmind.Terminal thisServer = new com.example.overmind.Terminal();
        thisServer.ip = serverIP;
        //thisServer.ip = "192.168.1.213";
        thisServer.natPort = 4194;        
						
		while (!shutdown) {
			
			/**
			 * Open new connections with the clients
			 */
		
			// Accept connections from the clients			
			try {				
				clientSocket = serverSocket.accept();
			} catch (IOException e) {
	        	e.printStackTrace();
			}		
			
			com.example.overmind.Terminal terminal = null; 					
			ObjectInputStream input = null;
			
			// Receive data stream from the client
			try {					
				input = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
	        	e.printStackTrace();
			}
			
			assert input != null;
			
			// Read the Terminal class from the data stream
			try {
				terminal = (com.example.overmind.Terminal) input.readObject();					
			} catch (IOException | ClassNotFoundException e) {
	        	e.printStackTrace();
			}
			
			assert terminal != null;
				
			/**
			 * Retrieve nat port of the current device 
			 */			
			
			try {				
						
				byte[] testPacketBuffer = new byte[1];
				
				DatagramPacket testPacket = new DatagramPacket(testPacketBuffer, 1);				
			
				inputSocket.receive(testPacket);				
				
				terminal.natPort = testPacket.getPort();
				
				terminal.ip = testPacket.getAddress().toString().substring(1);
			
				System.out.println("Nat port for device with IP " + terminal.ip + " is " + terminal.natPort);

				
			} catch (IOException e) {
	        	e.printStackTrace();
			}			
			
			terminal.postsynapticTerminals.add(thisServer);
				
			Node newNode = new Node(clientSocket, terminal);
			newNode.initialize();
			/*
			Node node2 = new Node();
			node2.initialize();
			node2.update(newNode);
			node2.terminal.numOfNeurons = 51;
			System.out.println(newNode.terminal.numOfNeurons);
			*/
			
			nodeClients.add(newNode);			
			
			assert terminal != null;	
			
			connectNodes(newNode);
								
		}
		/* [End of while(!shutdown)] */
							
	}
	/* [End of run() method] */	
	
	public synchronized static void connectNodes(Node disconnectedNode) {	
	
		/**
		 * Populate and update the list of terminals available for connection
		 */			
		
		// The algorithm starts only if the list has at least one element
		if (!availableNodes.isEmpty() && !availableNodes.contains(disconnectedNode)) {
		
			// Iterate over all the available terminals
			for (int i = 0; i < availableNodes.size()
					|| (disconnectedNode.terminal.numOfDendrites == 0 && disconnectedNode.terminal.numOfSynapses == 0); i++) {

				Node currentNode = availableNodes.get(i);
				
				// Branch depending on whether either the synapses or the dendrites of the current terminal are saturated
				if (currentNode.terminal.numOfSynapses - disconnectedNode.terminal.numOfNeurons >= 0
						&& disconnectedNode.terminal.numOfDendrites - currentNode.terminal.numOfNeurons >= 0
						&& currentNode.terminal.postsynapticTerminals.size() <= currentNode.terminal.presynapticTerminals.size()) {

					// Update the number of synapses and dendrites for both currentNode.terminal and disconnectedNode.terminal
					currentNode.terminal.numOfSynapses -= disconnectedNode.terminal.numOfNeurons;
					disconnectedNode.terminal.numOfDendrites -= currentNode.terminal.numOfNeurons;

					// Update the list of connected terminals
					currentNode.terminal.postsynapticTerminals.add(disconnectedNode.terminal);
					disconnectedNode.terminal.presynapticTerminals.add(currentNode.terminal);

					// Send to the list of terminals which need to be updated the current terminal
					if (unsyncNodes.contains(currentNode)) {
						unsyncNodes.set(unsyncNodes.indexOf(currentNode), currentNode);
					} else {
						unsyncNodes.add(currentNode);
					}
					
					// Update the current terminal in the list availableTerminals
					availableNodes.set(i, currentNode);
					
				} else if (currentNode.terminal.numOfDendrites - disconnectedNode.terminal.numOfNeurons >= 0
						&& disconnectedNode.terminal.numOfSynapses - currentNode.terminal.numOfNeurons >= 0) {
					
					/**
					 * Just as before but now synapses and dendrites are exchanged
					 */

					currentNode.terminal.numOfDendrites -= disconnectedNode.terminal.numOfNeurons;
					disconnectedNode.terminal.numOfSynapses -= currentNode.terminal.numOfNeurons;

					currentNode.terminal.presynapticTerminals.add(disconnectedNode.terminal);
					disconnectedNode.terminal.postsynapticTerminals.add(currentNode.terminal);

					if (unsyncNodes.contains(currentNode)) {
						unsyncNodes.set(unsyncNodes.indexOf(currentNode), currentNode);
					} else {
						unsyncNodes.add(currentNode);
					}
					
					availableNodes.set(i, currentNode);
				
				} else if (currentNode.terminal.numOfSynapses == 0 && currentNode.terminal.numOfDendrites == 0) {
					// If BOTH the synapses and the dendrites of the current terminal are saturated it can be removed
					availableNodes.remove(i);
				}
				/* [End of the inner if] */

			}
			/* [End of for loop] */
			
			// If either the dendrites or the synapses of the disconnected terminal are not saturated it can be added to the list
			if (disconnectedNode.terminal.numOfDendrites > 0 || disconnectedNode.terminal.numOfSynapses > 0) {
				availableNodes.add(disconnectedNode);
			} 
			
			unsyncNodes.add(disconnectedNode);	

			
		} else if (availableNodes.isEmpty()) {
			
			// Add the disconnected terminal automatically if the list is empty
			availableNodes.add(disconnectedNode);		
			unsyncNodes.add(disconnectedNode);	
			  
		} else if (availableNodes.contains(disconnectedNode)) {
			
			// If availableTerminals contains the disconnectedNode.terminal it needs only to update its reference
			availableNodes.set(availableNodes.indexOf(disconnectedNode), disconnectedNode);					
			unsyncNodes.add(disconnectedNode);	
			
		}
		/* [End of the outer if] */		
									
		MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), syncNodes.size()));
		
	}
	
	public synchronized static void removeTerminal(Node removableNode) {
		
		//syncNodes();		
		
		// If the method has been called unnecessarily exit without doing anything 
		// (However this should not happen...)
		if (!availableNodes.contains(removableNode)) { return; }
		
		unsyncNodes.remove(removableNode);
		
		int index = syncNodes.indexOf(removableNode);
		
		availableNodes.remove(removableNode); 	
		
		/**
		 * Shutdown the executor of the the spikes monitor 
		 */		
		
		boolean spikesMonitorIsShutdown = false;	
		
		syncFrames.get(index).shutdown = true;
	
		syncFrames.get(index).spikesMonitorExecutor.shutdown();	
		
		try {
			spikesMonitorIsShutdown = syncFrames.get(index).spikesMonitorExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!spikesMonitorIsShutdown) {
			System.out.println("Failed to shutdown spikes monitor for device with ip " + removableNode.terminal.ip);	
		} 			
		
		/**
		 * Shutdown the executor of the external stimuli
		 */
		
		syncFrames.get(index).thisTerminalRSG.shutdown = true;		
		syncFrames.get(index).thisTerminalRSS.shutdown = true;	
		
		syncFrames.get(index).stimulusExecutor.shutdown();	
		
		boolean stimulusExecIsShutdown = false;
		
		try {
			stimulusExecIsShutdown = syncFrames.get(index).stimulusExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!stimulusExecIsShutdown) {
			System.out.println("Failed to close the stimuli executor for device with ip " + removableNode.terminal.ip);	
		}		
		
		/**
		 * Close the frame associated to the terminal and remove its references from the list of clients
		 */
		
		syncFrames.get(index).frame.dispose();
		syncFrames.remove(index);			
		
		syncNodes.remove(index);		
		
		nodeClients.get(index).close();
		nodeClients.remove(index);		
		
		/**
		 * Remove all references to the current terminal from the other terminal' lists
		 */
		
		for (int i = 0; i < availableNodes.size(); i++) {
			
			boolean terminalIsModified = false;
			
			if (availableNodes.get(i).terminal.postsynapticTerminals.contains(removableNode.terminal)) {
				availableNodes.get(i).terminal.postsynapticTerminals.remove(removableNode.terminal);
				availableNodes.get(i).terminal.numOfSynapses += removableNode.terminal.numOfNeurons;
				terminalIsModified = true;
			}
			
			if (availableNodes.get(i).terminal.presynapticTerminals.contains(removableNode.terminal)) {
				availableNodes.get(i).terminal.presynapticTerminals.remove(removableNode.terminal);
				availableNodes.get(i).terminal.numOfDendrites += removableNode.terminal.numOfNeurons;
				terminalIsModified = true;
			}
			
			if (terminalIsModified) { unsyncNodes.add(availableNodes.get(i)); }			
			
		}	
		
		// Sync other nodes that have been eventually modified
		syncTerminals();
		
	}

	public synchronized static void syncTerminals() {		
		
		/**
		 * Sync the GUI with the updated info about the terminals
		 */
		
		if (!unsyncNodes.isEmpty()) {		
			
			for (int i = 0; i < unsyncNodes.size(); i++) {				
				
				// Branch depending on whether the terminal is new or not
				if (unsyncNodes.get(i).terminalFrame == null) {					
			
					unsyncNodes.get(i).terminalFrame = new TerminalFrame();
					unsyncNodes.get(i).terminalFrame.update(unsyncNodes.get(i));
					
					// The terminal is new so a new frame needs to be created
					unsyncNodes.get(i).terminalFrame.display();
															
					// Add the new window to the list of frames 
					syncFrames.add(unsyncNodes.get(i).terminalFrame);
					
					// Add the new terminal to the list of sync terminals
					syncNodes.add(unsyncNodes.get(i));															
					
				} else {					
					
					int index = syncNodes.indexOf(unsyncNodes.get(i));					
					
					// Since the terminal is not new its already existing window must be retrieved from the list
					// TODO instead of using index to retrieve frame we could write a method with argument the Terminal itself
					unsyncNodes.get(i).terminalFrame = syncFrames.get(index);
					
					// The retrieved window needs only to be updated 
					unsyncNodes.get(i).terminalFrame.update(unsyncNodes.get(i));
					
					// The old terminal is substituted with the new one in the list of sync terminal
					syncNodes.set(index, unsyncNodes.get(i));					
					
				}
				
				/**
				 * Updated info regarding the current terminal are sent back to the physical device
				 */
					
				try {
					
					com.example.overmind.Terminal tmpTerminal = new com.example.overmind.Terminal();
					tmpTerminal.update(unsyncNodes.get(i).terminal);
									
					// Write the info in the steam
					unsyncNodes.get(i).output.writeObject(tmpTerminal);					
																	
				} catch (IOException e) {
		        	e.printStackTrace();
		        	removeTerminal(unsyncNodes.get(i));
				}
							
			}				
			
			unsyncNodes.clear();	
												
		}
		
		SpikesSorter.updateNodeFrames(syncFrames);
		MainFrame.updateMainFrame(new MainFrameInfo(0, syncNodes.size()));
		
	}

}
/* [End of VirtualLayerManager class] */