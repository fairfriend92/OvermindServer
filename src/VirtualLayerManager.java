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
	
	static ArrayList<com.example.overmind.Terminal> unsyncTerminals = new ArrayList<>();
	static ArrayList<com.example.overmind.Terminal> syncTerminals = new ArrayList<>();
	static ArrayList<TerminalFrame> syncFrames = new ArrayList<>();
	static ArrayList<Node> nodeClients = new ArrayList<>();
	static ArrayList<com.example.overmind.Terminal> availableTerminals = new ArrayList<>();	
	
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
				
			Node newNode = new Node(terminal.ip, clientSocket);
			newNode.initialize();
			
			nodeClients.add(newNode);			
			
			assert terminal != null;	
			
			connectTerminals(terminal);
								
		}
		/* [End of while(!shutdown)] */
							
	}
	/* [End of run() method] */	
	
	public synchronized static void connectTerminals(com.example.overmind.Terminal disconnectedTerminal) {	
	
		/**
		 * Populate and update the list of terminals available for connection
		 */			
		
		// The algorithm starts only if the list has at least one element
		if (!availableTerminals.isEmpty() && !availableTerminals.contains(disconnectedTerminal)) {
		
			// Iterate over all the available terminals
			for (int i = 0; i < availableTerminals.size()
					|| (disconnectedTerminal.numOfDendrites == 0 && disconnectedTerminal.numOfSynapses == 0); i++) {

				com.example.overmind.Terminal currentTerminal = availableTerminals.get(i);
				
				// Branch depending on whether either the synapses or the dendrites of the current terminal are saturated
				if (currentTerminal.numOfSynapses - disconnectedTerminal.numOfNeurons >= 0
						&& disconnectedTerminal.numOfDendrites - currentTerminal.numOfNeurons >= 0
						&& currentTerminal.postsynapticTerminals.size() <= currentTerminal.presynapticTerminals.size()) {

					// Update the number of synapses and dendrites for both currentTerminal and disconnectedTerminal
					currentTerminal.numOfSynapses -= disconnectedTerminal.numOfNeurons;
					disconnectedTerminal.numOfDendrites -= currentTerminal.numOfNeurons;

					// Update the list of connected terminals
					currentTerminal.postsynapticTerminals.add(disconnectedTerminal);
					disconnectedTerminal.presynapticTerminals.add(currentTerminal);

					// Send to the list of terminals which need to be updated the current terminal
					if (unsyncTerminals.contains(currentTerminal)) {
						unsyncTerminals.set(unsyncTerminals.indexOf(currentTerminal), currentTerminal);
					} else {
						unsyncTerminals.add(currentTerminal);
					}
					
					// Update the current terminal in the list availableTerminals
					availableTerminals.set(i, currentTerminal);
					
				} else if (currentTerminal.numOfDendrites - disconnectedTerminal.numOfNeurons >= 0
						&& disconnectedTerminal.numOfSynapses - currentTerminal.numOfNeurons >= 0) {
					
					/**
					 * Just as before but now synapses and dendrites are exchanged
					 */

					currentTerminal.numOfDendrites -= disconnectedTerminal.numOfNeurons;
					disconnectedTerminal.numOfSynapses -= currentTerminal.numOfNeurons;

					currentTerminal.presynapticTerminals.add(disconnectedTerminal);
					disconnectedTerminal.postsynapticTerminals.add(currentTerminal);

					if (unsyncTerminals.contains(currentTerminal)) {
						unsyncTerminals.set(unsyncTerminals.indexOf(currentTerminal), currentTerminal);
					} else {
						unsyncTerminals.add(currentTerminal);
					}
					
					availableTerminals.set(i, currentTerminal);

				} else if (currentTerminal.numOfSynapses - disconnectedTerminal.numOfNeurons >= 0
						&& disconnectedTerminal.numOfDendrites - currentTerminal.numOfNeurons >= 0) {

					/**
					 * Just as before but now synapses and dendrites are exchanged
					 */

					currentTerminal.numOfSynapses -= disconnectedTerminal.numOfNeurons;
					disconnectedTerminal.numOfDendrites -= currentTerminal.numOfNeurons;

					currentTerminal.postsynapticTerminals.add(disconnectedTerminal);
					disconnectedTerminal.presynapticTerminals.add(currentTerminal);

					if (unsyncTerminals.contains(currentTerminal)) {
						unsyncTerminals.set(unsyncTerminals.indexOf(currentTerminal), currentTerminal);
					} else {
						unsyncTerminals.add(currentTerminal);
					}
					
					availableTerminals.set(i, currentTerminal);

				} else if (currentTerminal.numOfSynapses == 0 && currentTerminal.numOfDendrites == 0) {
					// If BOTH the synapses and the dendrites of the current terminal are saturated it can be removed
					availableTerminals.remove(i);
				}
				/* [End of the inner if] */

			}
			/* [End of for loop] */
			
			// If either the dendrites or the synapses of the disconnected terminal are not saturated it can be added to the list
			if (disconnectedTerminal.numOfDendrites > 0 || disconnectedTerminal.numOfSynapses > 0) {
				availableTerminals.add(disconnectedTerminal);
			} 
			
			unsyncTerminals.add(disconnectedTerminal);	

			
		} else if (availableTerminals.isEmpty()) {
			
			// Add the disconnected terminal automatically if the list is empty
			availableTerminals.add(disconnectedTerminal);		
			unsyncTerminals.add(disconnectedTerminal);	
			  
		} else if (availableTerminals.contains(disconnectedTerminal)) {
			
			// If availableTerminals contains the disconnectedTerminal it needs only to update its reference
			availableTerminals.set(availableTerminals.indexOf(disconnectedTerminal), disconnectedTerminal);					
			unsyncTerminals.add(disconnectedTerminal);	
			
		}
		/* [End of the outer if] */		
									
		MainFrame.updateMainFrame(new MainFrameInfo(unsyncTerminals.size(), syncTerminals.size()));
		
	}
	
	public synchronized static void removeTerminal(com.example.overmind.Terminal removableTerminal) {
		
		//syncNodes();		
		
		// If the method has been called unnecessarily exit without doing anything 
		// (However this should not happen...)
		if (!availableTerminals.contains(removableTerminal)) { return; }
		
		unsyncTerminals.remove(removableTerminal);
		
		int index = syncTerminals.indexOf(removableTerminal);
		
		availableTerminals.remove(removableTerminal); 	
		
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
			System.out.println("Failed to shutdown spikes monitor for device with ip " + removableTerminal.ip);	
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
			System.out.println("Failed to close the stimuli executor for device with ip " + removableTerminal.ip);	
		}		
		
		/**
		 * Close the frame associated to the terminal and remove its references from the list of clients
		 */
		
		syncFrames.get(index).frame.dispose();
		syncFrames.remove(index);			
		
		syncTerminals.remove(index);		
		
		nodeClients.get(index).close();
		nodeClients.remove(index);		
		
		/**
		 * Remove all references to the current terminal from the other terminal' lists
		 */
		
		for (int i = 0; i < availableTerminals.size(); i++) {
			
			boolean terminalIsModified = false;
			
			if (availableTerminals.get(i).postsynapticTerminals.contains(removableTerminal)) {
				availableTerminals.get(i).postsynapticTerminals.remove(removableTerminal);
				availableTerminals.get(i).numOfSynapses += removableTerminal.numOfNeurons;
				terminalIsModified = true;
			}
			
			if (availableTerminals.get(i).presynapticTerminals.contains(removableTerminal)) {
				availableTerminals.get(i).presynapticTerminals.remove(removableTerminal);
				availableTerminals.get(i).numOfDendrites += removableTerminal.numOfNeurons;
				terminalIsModified = true;
			}
			
			if (terminalIsModified) { unsyncTerminals.add(availableTerminals.get(i)); }			
			
		}	
		
		// Sync other nodes that have been eventually modified
		syncTerminals();
		
	}

	public synchronized static void syncTerminals() {		
		
		/**
		 * Sync the GUI with the updated info about the terminals
		 */
		
		if (!unsyncTerminals.isEmpty()) {		
			
			for (int i = 0; i < unsyncTerminals.size(); i++) {
				
				TerminalFrame tmp;
				
				// Branch depending on whether the terminal is new or not
				if (!syncTerminals.contains(unsyncTerminals.get(i))) {					
			
					tmp = new TerminalFrame();
					tmp.update(unsyncTerminals.get(i));
					
					// The terminal is new so a new frame needs to be created
					tmp.display();
															
					// Add the new window to the list of frames 
					syncFrames.add(tmp);
					
					// Add the new terminal to the list of sync terminals
					syncTerminals.add(unsyncTerminals.get(i));
										
					
				} else {					
					
					int index = syncTerminals.indexOf(unsyncTerminals.get(i));					
					
					// Since the terminal is not new its already existing window must be retrieved from the list
					// TODO instead of using index to retrieve frame we could write a method with argument the Terminal itself
					tmp = syncFrames.get(index);
					
					// The retrieved window needs only to be updated 
					tmp.update(unsyncTerminals.get(i));
					
					// The old terminal is substituted with the new one in the list of sync terminal
					syncTerminals.set(index, unsyncTerminals.get(i));
					
				}
				
				/**
				 * Updated info regarding the current terminal are sent back to the physical device
				 */
					
				try {
					
					// Temporary object holding the info regarding the local network of the current node
					com.example.overmind.Terminal tmpT = unsyncTerminals.get(i);
					
					// Use the indexOf method to retrieve the current node from the nodeClients list
					int index = nodeClients.indexOf(new Node(tmpT.ip, null));
					
					// The node whose informations need to be sent back to the physical device
					Node pendingNode = nodeClients.get(index);	
					
					com.example.overmind.Terminal tmpTerminal = new com.example.overmind.Terminal();
					tmpTerminal.update(unsyncTerminals.get(i));
									
					// Write the info in the steam
					pendingNode.output.writeObject(tmpTerminal);						
								
					//pendingNode.output.reset();										
										
				} catch (IOException e) {
		        	e.printStackTrace();
		        	removeTerminal(unsyncTerminals.get(i));
				}
							
			}				
			
			unsyncTerminals.clear();	
												
		}
		
		SpikesSorter.updateNodeFrames(syncFrames);
		MainFrame.updateMainFrame(new MainFrameInfo(0, syncTerminals.size()));
		
	}

}
/* [End of VirtualLayerManager class] */