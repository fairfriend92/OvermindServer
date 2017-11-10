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

import java.util.List;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class VirtualLayerManager extends Thread{			
	
	boolean shutdown = false;	
	
	static int numberOfSyncNodes = 0;	
	static int numberOfShadowNodes = 0;
	int totalNumberOfDevices = 0;
	static volatile int activeShadowNodesRatio = 2;
	static ConcurrentHashMap<Integer, Node> nodesTable = new ConcurrentHashMap<>(Constants.MAX_CONNECTED_TERMINALS);
	
	// TODO: perhaps weights could be char instead of float?
	static ConcurrentHashMap<Integer, float[]> weightsTable = new ConcurrentHashMap<>(Constants.MAX_CONNECTED_TERMINALS);	
	static ConcurrentHashMap<Integer, ArrayList<Node>> shadowNodesListsTable = new ConcurrentHashMap<>(Constants.MAX_CONNECTED_TERMINALS);
	
	static List<Node> unsyncNodes = Collections.synchronizedList(new ArrayList<Node>());
	static List<Node> availableNodes = Collections.synchronizedList(new ArrayList<Node>());	
	static List<Short> freeNodes = Collections.synchronizedList(new ArrayList<Short>());
	
	static public String serverIP = null;
	
	static VirtualLayerVisualizer VLVisualizer = new VirtualLayerVisualizer();
	
	/* Network related objects */
	
	DatagramSocket inputSocket = null;
    Socket clientSocket = null;		
	ServerSocket serverSocket = null;	
	ObjectInputStream input = null;
	ObjectOutputStream output = null;
			
	@Override
	public void run() {
		super.run();
		
		VLVisualizer.start();
		
        try (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
            serverIP = s.next();
        } catch (java.io.IOException e) {
        	e.printStackTrace();
        }
        
        assert serverIP != null;
        
        System.out.println("This server has IP " + serverIP);		
		
		/*
		 * Build the TCP server socket which listens for physical devices ready to connect
		 */
		
		
		try {
			serverSocket = new ServerSocket(Constants.SERVER_PORT_TCP);
		} catch (IOException e) {
        	e.printStackTrace();
		}
		
		assert serverSocket != null;
		
		/*
		 * Build the datagram input socket which listens for test packets from which the nat port can be retrieved
		 */
				
        try {
            inputSocket = new DatagramSocket(Constants.SERVER_PORT_UDP);
        } catch (SocketException e) {
        	e.printStackTrace();
        }

        assert inputSocket != null;
                
        com.example.overmind.Terminal thisServer = new com.example.overmind.Terminal();
        thisServer.ip = serverIP;
        //thisServer.ip = "192.168.1.213";
        thisServer.natPort = Constants.OUT_UDP_PORT;    
        
        Node[] disconnectedNode = new Node[1];
						
		while (!shutdown) {
		
			/*
			 * Open new connections with the clients
			 */	
		
			// Accept connections from the clients			
			try {				
				clientSocket = serverSocket.accept();
				clientSocket.setTrafficClass(0x04);
				clientSocket.setTcpNoDelay(true);
				//clientSocket.setTcpNoDelay(true);
			} catch (SocketException e) {
				
				/*
				 * serverSocket gets closed whenever the application is terminated
				 * by the user. At that point exit the loop and proceed to an 
				 * orderly shutdown.
				 */
				
				System.out.println("serverSocket is closed");
				break; // Exit while loop and shutdown this thread. 
			} catch (IOException e) {
	        	e.printStackTrace();
			}				
			
			com.example.overmind.Terminal terminal = null; 	
			
			// Create the output stream for the client socket			
			try {
				output = new ObjectOutputStream(clientSocket.getOutputStream());				
			} catch (IOException e) {
	        	e.printStackTrace();
			} 					
			
			// Receive data stream from the client
			try {					
				input = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
	        	e.printStackTrace();
			}
			
			
			assert input != null;
			
			// Read the Terminal class from the data stream
			
			Object obj = null;			
		
			try {
				obj = input.readObject();					
			} catch (IOException | ClassNotFoundException e) {
	        	e.printStackTrace();
			}			
		
			assert obj != null;			
			
			terminal = (com.example.overmind.Terminal) obj;				
			
			/*
			 * Retrieve nat port of the current device 
			 */			
			
			int ipHashCode = 0;
			
			try {				
						
				byte[] testPacketBuffer = new byte[1];
				
				DatagramPacket testPacket = new DatagramPacket(testPacketBuffer, 1);				
			
				inputSocket.receive(testPacket);				
				
				terminal.natPort = testPacket.getPort();			
				
				// Use the InetAddress hashcode to identify the node
				ipHashCode = testPacket.getAddress().hashCode();
				
				terminal.ip = testPacket.getAddress().toString().substring(1);
			
				System.out.println("Nat port for device with IP " + terminal.ip + " is " + terminal.natPort);				
						
			} catch (IOException e) {
	        	e.printStackTrace();
			} 
			
			terminal.postsynapticTerminals.add(thisServer);
			terminal.serverIP = serverIP;
			// TODO: Should the number of synapses of the terminal be decreased by terminal.numOfNeurons to account for the random spike generator?
			
			/*
			 * Create the node containing the terminal object just received
			 */
				
			Node newNode = new Node(clientSocket, terminal, output);
			newNode.physicalID = ipHashCode;
			newNode.virtualID = totalNumberOfDevices; // TODO Use more intelligent hashing for the virtualID 
			totalNumberOfDevices++;
			
			// If lateral connections have been enabled, to get the number of synapses the node started with the number
			// of neurons must be added to the current number of synapses
			if (newNode.hasLateralConnections())
				newNode.originalNumOfSynapses = (short)(terminal.numOfSynapses + terminal.numOfNeurons);
			else 
				newNode.originalNumOfSynapses = terminal.numOfSynapses;
		
			// Put the new node in the hashmap using the hashcode of the
			// InetAddress of the terminal contained in the node as key
			nodesTable.put(newNode.physicalID, newNode);	
		
			assert terminal != null;				
			
			/*
			 * Connect the new node to the network only if the number of backup Shadow Nodes is sufficient
			 */
			
			if ((numberOfSyncNodes <= activeShadowNodesRatio * numberOfShadowNodes) || !MainFrame.useShadowNodesFlag ) {
			
				System.out.println("Active node added");
				
				// If the terminal has lateral connections, the number of weights must account for the synapses occupied by those
				int numOfWeights = terminal.numOfNeurons * newNode.originalNumOfSynapses;
				
				weightsTable.put(newNode.virtualID, new float[numOfWeights]);
				disconnectedNode[0] = newNode;				
				connectNodes(disconnectedNode);
				
			
			} else {				
				
				System.out.println("Shadow node added");
				newNode.isShadowNode = true;
				numberOfShadowNodes++;
				MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), numberOfSyncNodes, numberOfShadowNodes));

				newNode.terminalFrame = new TerminalFrame();
				newNode.terminalFrame.update(newNode);
				newNode.terminalFrame.display();				
				
				// Retrieve the list of Shadow Nodes with a certain number of neurons, and if it
				// doesn't exist, create it
				if (shadowNodesListsTable.get((int)terminal.numOfNeurons) == null) {
					
					ArrayList<Node> shadowNodesList = new ArrayList<>(32);
					shadowNodesList.add(newNode);
					shadowNodesListsTable.put((int)terminal.numOfNeurons, shadowNodesList);
					
				} else {
					
					shadowNodesListsTable.get((int)terminal.numOfNeurons).add(newNode);
					
				}
				/* [End of inner if] */
								
			}
			/* [End of outer if] */
											
		}
		/* [End of while(!shutdown)] */	
		
		/* Shutdown operations. */
		
		inputSocket.close();	
		try {
			if (clientSocket != null)
				clientSocket.close();			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			VLVisualizer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// TODO: Close all the nodes client sockets. Use removeNode method perhaps?
							
	}
	/* [End of run() method] */	
	
	public synchronized static short modifyNode(Node[] nodeToModify) {			
		
		if (VirtualLayerVisualizer.cutLinkFlag) {
		
			if (nodeToModify[0].postsynapticNodes.contains(nodeToModify[1])) {
				nodeToModify[0].terminal.numOfSynapses += nodeToModify[1].terminal.numOfNeurons;
				nodeToModify[1].terminal.numOfDendrites += nodeToModify[0].terminal.numOfNeurons;
				nodeToModify[0].terminal.postsynapticTerminals.remove(nodeToModify[1].terminal);
				nodeToModify[1].terminal.presynapticTerminals.remove(nodeToModify[0].terminal);
				nodeToModify[0].postsynapticNodes.remove(nodeToModify[1]);
				nodeToModify[1].presynapticNodes.remove(nodeToModify[0]);
			} else if (nodeToModify[1].postsynapticNodes.contains(nodeToModify[0])) {
				nodeToModify[1].terminal.numOfSynapses += nodeToModify[0].terminal.numOfNeurons;
				nodeToModify[0].terminal.numOfDendrites += nodeToModify[1].terminal.numOfNeurons;
				nodeToModify[1].terminal.postsynapticTerminals.remove(nodeToModify[0].terminal);
				nodeToModify[0].terminal.presynapticTerminals.remove(nodeToModify[1].terminal);
				nodeToModify[1].postsynapticNodes.remove(nodeToModify[0]);
				nodeToModify[0].presynapticNodes.remove(nodeToModify[1]);
			} else
				return 1; // TODO: Use constants to return value with meaningful name
								
		}
		
		if (VirtualLayerVisualizer.createLinkFlag && 
				(nodeToModify[0].terminal.numOfSynapses - nodeToModify[1].terminal.numOfNeurons) >= 0 &&
				(nodeToModify[1].terminal.numOfDendrites - nodeToModify[0].terminal.numOfNeurons) >= 0) {
			
			if (!nodeToModify[1].presynapticNodes.contains(nodeToModify[0]) && 
					(!nodeToModify[1].postsynapticNodes.contains(nodeToModify[0]) || VLVisualizer.allowBidirectionalConn)) {
				nodeToModify[0].terminal.numOfSynapses -= nodeToModify[1].terminal.numOfNeurons;
				nodeToModify[1].terminal.numOfDendrites -= nodeToModify[0].terminal.numOfNeurons;
				nodeToModify[0].terminal.postsynapticTerminals.add(nodeToModify[1].terminal);
				nodeToModify[1].terminal.presynapticTerminals.add(nodeToModify[0].terminal);
				nodeToModify[0].postsynapticNodes.add(nodeToModify[1]);
				nodeToModify[1].presynapticNodes.add(nodeToModify[0]);
			} else
				return 2;
			
		} else if (VirtualLayerVisualizer.createLinkFlag) return 3;
		
		connectNodes(nodeToModify);
		
		return 0;
		
	}
	
	public synchronized static void connectNodes(Node[] disconnectedNodes) {			
	
		/*
		 * Populate and update the list of terminals available for connection
		 */		
	
		for (int j = 0; (j < disconnectedNodes.length) && (disconnectedNodes[j] != null); j++) {
			
			Node disconnectedNode = disconnectedNodes[j];
			
			// The algorithm starts only if the list has at least one element
			if (!availableNodes.isEmpty() && !availableNodes.contains(disconnectedNode)) {

				// Iterate over all the available terminals or until the connections of the new terminal are saturated
				for (int i = 0; i < availableNodes.size() || (disconnectedNode.terminal.numOfDendrites == 0
						&& disconnectedNode.terminal.numOfSynapses == 0); i++) {

					Node currentNode = availableNodes.get(i);

					// Branch depending on whether either the synapses or the dendrites of the current terminal are saturated
					// Try to increase the number of connections of the type which has the the fewest, but keep in mind
					// that each terminal must have at least one presynaptic connection
					if (currentNode.terminal.numOfSynapses - disconnectedNode.terminal.numOfNeurons >= 0
							&& disconnectedNode.terminal.numOfDendrites - currentNode.terminal.numOfNeurons >= 0
							&& (currentNode.terminal.postsynapticTerminals
									.size() <= currentNode.terminal.presynapticTerminals.size() 
									|| disconnectedNode.terminal.presynapticTerminals.size() == 0)) {

						// Update the number of synapses and dendrites for both currentNode.terminal and disconnectedNode.terminal
						currentNode.terminal.numOfSynapses -= disconnectedNode.terminal.numOfNeurons;
						disconnectedNode.terminal.numOfDendrites -= currentNode.terminal.numOfNeurons;

						// Update the list of connected terminals
						currentNode.terminal.postsynapticTerminals.add(disconnectedNode.terminal);
						disconnectedNode.terminal.presynapticTerminals.add(currentNode.terminal);

						// Update the info of the new node regarding the connections established by the associated terminal
						currentNode.postsynapticNodes.add(disconnectedNode);
						disconnectedNode.presynapticNodes.add(currentNode);

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

						currentNode.presynapticNodes.add(disconnectedNode);
						disconnectedNode.postsynapticNodes.add(currentNode);

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
				/* [End of inner for loop] */

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
				
				// TODO References to the disconnectedNode in its presynapticNodes and postsynapticNodes must be updated too

			}
			/* [End of the outer if] */
		}
		/* [End of outer for loop] */
		
		MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), numberOfSyncNodes, numberOfShadowNodes));
		
		syncNodes();
		
	}
	
	/*
	 * Activate a shadow node
	 */
	
	public synchronized static void activateNode(Node shadowNode) {
		
		System.out.println("Activating shadow node with ip " + shadowNode.terminal.ip);
		
		ArrayList<Node> shadowNodesList = shadowNodesListsTable.get((int)shadowNode.terminal.numOfNeurons);
		shadowNodesList.remove(shadowNode);
		weightsTable.put(shadowNode.virtualID, new float[shadowNode.terminal.numOfNeurons * shadowNode.terminal.numOfSynapses]);
		numberOfShadowNodes--;
		MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), numberOfSyncNodes, numberOfShadowNodes));
		connectNodes(new Node[]{shadowNode});		
		
	}
	
	/*
	 * Remove a shadow node
	 */
	
	public synchronized static void removeShadowNode(Node shadowNode) {
		
		System.out.println("Shadow node with ip " + shadowNode.terminal.ip + " is being removed");		
		
		ArrayList<Node> shadowNodesList = shadowNodesListsTable.get((int)shadowNode.terminal.numOfNeurons);
		shadowNodesList.remove(shadowNode);
		nodesTable.remove(shadowNode.physicalID);
		numberOfShadowNodes--;

		synchronized (shadowNode.terminalFrame.tcpKeepAliveLock) {
			
			shadowNode.terminalFrame.tcpKeepAliveLock.notify();
		
		}
		
		boolean sNodeMonitorIsShutdown = false;	
		
		shadowNode.terminalFrame.shutdown = true;		
	
		shadowNode.terminalFrame.spikesMonitorExecutor.shutdown();	
		
		try {
			sNodeMonitorIsShutdown = shadowNode.terminalFrame.spikesMonitorExecutor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!sNodeMonitorIsShutdown) {
			System.out.println("Failed to shutdown spikes monitor for device with ip " + shadowNode.terminal.ip);	
		} 			
		
		boolean sNodeKeepAliveShutdown = false;
		
		shadowNode.terminalFrame.tcpKeepAliveExecutor.shutdown();
		
		try {
			sNodeKeepAliveShutdown = shadowNode.terminalFrame.tcpKeepAliveExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!sNodeKeepAliveShutdown) {
			System.out.println("Failed to shutdown tcpKeepAlive package sender with ip " + shadowNode.terminal.ip);	
		} 	
				
		shadowNode.terminalFrame.frame.dispose();			
		shadowNode.terminalFrame.localUpdatedNode = null;
		shadowNode.terminalFrame = null;
		shadowNode.close();
		shadowNode = null;
		
		MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), numberOfSyncNodes, numberOfShadowNodes));
		
	}
	
	/*
	 * Remove an active node
	 */
	
	// TODO: use syncNodes instead of connectNodes
	
	public synchronized static void removeNode(Node removableNode, boolean unwantedDisconnection) {	
		
		// If the removable node was in line to be updated, drop its reference
		unsyncNodes.remove(removableNode);	
		
		// Remove the reference to the old node
		nodesTable.remove(removableNode.physicalID);
		numberOfSyncNodes--;
		
		/*
		 * Wake up the thread that sends the TCP keep alive packet
		 */
		
		synchronized (removableNode.terminalFrame.tcpKeepAliveLock) {
		
			removableNode.terminalFrame.tcpKeepAliveLock.notify();
		
		}
		
		/*
		 * Shutdown the executor of the the spikes monitor 
		 */		
		
		boolean spikesMonitorIsShutdown = false;	
		
		removableNode.terminalFrame.shutdown = true;		
	
		removableNode.terminalFrame.spikesMonitorExecutor.shutdown();	
		
		try {
			spikesMonitorIsShutdown = removableNode.terminalFrame.spikesMonitorExecutor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!spikesMonitorIsShutdown) {
			System.out.println("Failed to shutdown spikes monitor for device with ip " + removableNode.terminal.ip);	
		} 			
		
		/*
		 * Shutdown the executor of the external stimuli
		 */
		
		removableNode.terminalFrame.thisTerminalRSG.shutdown = true;		
		removableNode.terminalFrame.thisTerminalRSS.shutdown = true;	
		
		removableNode.terminalFrame.stimulusExecutor.shutdown();	
		
		boolean stimulusExecIsShutdown = false;
		
		try {
			stimulusExecIsShutdown = removableNode.terminalFrame.stimulusExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!stimulusExecIsShutdown) {
			System.out.println("Failed to close the stimuli executor for device with ip " + removableNode.terminal.ip);	
		}		
		
		/*
		 * Shutdown the executor of the tpcKeepAlive package sender
		 */
		
		boolean tcpKeepAliveExecutorIsShutdown = false;
		
		removableNode.terminalFrame.tcpKeepAliveExecutor.shutdown();
		
		try {
			tcpKeepAliveExecutorIsShutdown = removableNode.terminalFrame.tcpKeepAliveExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!tcpKeepAliveExecutorIsShutdown) {
			System.out.println("Failed to shutdown tcpKeepAlive package sender with ip " + removableNode.terminal.ip);	
		} 	
		
		/*
		 * Close the frame associated to the terminal and the socket as well
		 */
		
		removableNode.terminalFrame.frame.dispose();		
		
		removableNode.close();
		
		VLVisualizer.layeredPaneVL.removeNode(removableNode);			
		
		// TODO: accept shadow nodes with number of neurons greater than that of the removed node?
		ArrayList<Node> shadowNodesList = shadowNodesListsTable.get((int)removableNode.terminal.numOfNeurons);
		
		// If the disconnection is abrupt and there are shadow nodes available
		if (unwantedDisconnection && shadowNodesList != null && !shadowNodesList.isEmpty()) {		
						
			// Retrieve the first shadow node available 
			Node shadowNode = shadowNodesList.remove(shadowNodesList.size() - 1);			
			
			/*
			 * Create a RemovedNode object to notify the applications interfaced with the 
			 * Overmind that a node is being removed and that a shadow node is being inserted in 
			 * its place.
			 */
			
			// First make deep copies of the shadow node and the one that it's being deleted, 
			// since later on their info are going to be interchanged. 
			Node removableNodeCopy = new Node(null, null, null);	
			removableNodeCopy.update(removableNode);
			Node shadowNodeCopy = new Node(null, null, null);
			shadowNodeCopy.update(shadowNode);
			
			ApplicationInterface.RemovedNode removedNode = new ApplicationInterface.RemovedNode(removableNodeCopy, shadowNodeCopy);
			ApplicationInterface.addRemovedNode(removedNode);
			
			/*
			 * Shutdown the partial terminal frame of the shadow node
			 */
			
			synchronized (shadowNode.terminalFrame.tcpKeepAliveLock) {
				
				shadowNode.terminalFrame.tcpKeepAliveLock.notify();
			
			}
			
			boolean sNodeMonitorIsShutdown = false;	
			
			shadowNode.terminalFrame.shutdown = true;		
		
			shadowNode.terminalFrame.spikesMonitorExecutor.shutdown();	
			
			try {
				sNodeMonitorIsShutdown = shadowNode.terminalFrame.spikesMonitorExecutor.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (!sNodeMonitorIsShutdown) {
				System.out.println("Failed to shutdown spikes monitor for device with ip " + shadowNode.terminal.ip);	
			} 			
			
			boolean sNodeKeepAliveShutdown = false;
			
			shadowNode.terminalFrame.tcpKeepAliveExecutor.shutdown();
			
			try {
				sNodeKeepAliveShutdown = shadowNode.terminalFrame.tcpKeepAliveExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (!sNodeKeepAliveShutdown) {
				System.out.println("Failed to shutdown tcpKeepAlive package sender with ip " + shadowNode.terminal.ip);	
			} 	
					
			shadowNode.terminalFrame.frame.dispose();			
			shadowNode.terminalFrame.localUpdatedNode = null;
			shadowNode.terminalFrame = null;
			
			System.out.println("Node with ip " + removableNode.terminal.ip + " is being substituted with node with ip " + shadowNode.terminal.ip);		
			
			removableNode.terminalFrame.localUpdatedNode = null;	
						
			// Update the physical ID of the old node
			removableNode.physicalID = shadowNode.physicalID;
			
			// Reinsert the old node using the new physical ID as hash tag
			nodesTable.put(removableNode.physicalID, removableNode);
			
			// Assign the object output stream of the shadow node to the old node		
			removableNode.output = shadowNode.output;
			
			// Close the socket which connected the server to the old terminal, since it's not
			// available anymore
			removableNode.close();
			
			// Assign the socket of the shadow node to the old node
			removableNode.client = shadowNode.client;
			
			// Update the info regarding the physical terminal underlying the old node with that
			// of the terminal which is part of the shadow node
			removableNode.terminal.ip = shadowNode.terminal.ip;
			removableNode.terminal.natPort = shadowNode.terminal.natPort;
			
			// Retrieve the weights of the old node
			float[] weights = weightsTable.get(removableNode.virtualID);
			
			// Create the two arrays which represent the sparse array containing only those synaptic 
			// weights which have been modified
			float[] newWeights = new float[removableNode.terminal.numOfNeurons * removableNode.originalNumOfSynapses];
			int[] newWeightsIndexes = new int[removableNode.terminal.numOfNeurons * removableNode.originalNumOfSynapses];
			int index = 0;
			
			// Check which of the retrieve weights are different from the default value (0.0) 
			// and populate the sparse array consequently
			for (int i = 0; i < weights.length; i++) {
				if (weights[i] != 0) {
					newWeights[index] = weights[i];
					newWeightsIndexes[index] = i;
					index++;
				}					
			}
			
			// Resize the sparse array
			removableNode.terminal.newWeights = new float[index];
			System.arraycopy(weights, 0, removableNode.terminal.newWeights, 0, index);
			removableNode.terminal.newWeightsIndexes = new int[index];
			System.arraycopy(newWeightsIndexes, 0, removableNode.terminal.newWeightsIndexes, 0, index);		
						
			unsyncNodes.addAll(removableNode.presynapticNodes);
			unsyncNodes.addAll(removableNode.postsynapticNodes);
									
			// Update the number of shadow nodes
			numberOfShadowNodes--;
			
			unsyncNodes.add(removableNode);
			
			// Prepare shadow node for garbage collection
			shadowNode = null;			
			
			syncNodes();
			
		} else {
		
			System.out.println("Node with ip " + removableNode.terminal.ip + " is being removed");	
			
			/*
			 * Send the application interface a reference to the removable node so that all 
			 * the currently interfaced applications can handle the removal. No need to make a deep copy
			 * of removable node in this case since its info are left untouched. 
			 */
			
			ApplicationInterface.RemovedNode removedNode = new ApplicationInterface.RemovedNode(removableNode);
			ApplicationInterface.addRemovedNode(removedNode);
	
			availableNodes.remove(removableNode); 							
												
			/*
			 * Remove all references to the current terminal from the other terminals' lists.
			 */
			
			boolean nodeHasBeenModified = false;
			Node tmpNode;
			
			int numOfNodesAffected = removableNode.presynapticNodes.size() + removableNode.postsynapticNodes.size();
			
			ArrayList<Node> modifiedNodes = new ArrayList<>(numOfNodesAffected);
			
			for (int i = 0; i < removableNode.presynapticNodes.size(); i++) {
				
				tmpNode = removableNode.presynapticNodes.get(i);
				
				nodeHasBeenModified = tmpNode.postsynapticNodes.remove(removableNode);
				tmpNode.terminal.postsynapticTerminals.remove(removableNode.terminal);
				
				if (nodeHasBeenModified) {
					tmpNode.terminal.numOfSynapses += removableNode.terminal.numOfNeurons;
					//unsyncNodes.add(tmpNode);
					modifiedNodes.add(tmpNode);
					nodeHasBeenModified = false;
				}
				
			}
							
			for (int i = 0; i < removableNode.postsynapticNodes.size(); i++) {
				
				tmpNode = removableNode.postsynapticNodes.get(i);
				
				nodeHasBeenModified = tmpNode.presynapticNodes.remove(removableNode);
				tmpNode.terminal.presynapticTerminals.remove(removableNode.terminal);
				
				if (nodeHasBeenModified) {
					tmpNode.terminal.numOfDendrites += removableNode.terminal.numOfNeurons;
					//unsyncNodes.add(tmpNode);
					modifiedNodes.add(tmpNode);
					nodeHasBeenModified = false;
				}
				
			}								
			
			weightsTable.remove(removableNode.virtualID);
			
			// Shutdown the object stream and the socket. 			
			removableNode.close(); // TODO: This method is useless and confusing. 
			
			//removableNode.terminalFrame = null;
														
			// Sync other nodes that have been eventually modified
			//syncNodes();
			connectNodes(modifiedNodes.toArray(new Node[numOfNodesAffected]));
		
		}
		
	}

	public synchronized static void syncNodes() {		
		
		/*
		 * Sync the GUI with the updated info about the terminals
		 */		
	
		if (!unsyncNodes.isEmpty()) {		
			
			// Iterate over all the nodes that need to be sync
			for (int i = 0; i < unsyncNodes.size(); i++) {			
				
				// TODO: do not call get method repeatedly, instead use reference
																
				// Branch depending on whether the terminal is new or not
				if (unsyncNodes.get(i).terminalFrame.localUpdatedNode == null 
						&& !unsyncNodes.get(i).isShadowNode) {		
					
					VLVisualizer.layeredPaneVL.addNode(unsyncNodes.get(i));
					
					unsyncNodes.get(i).terminalFrame = new TerminalFrame();
					
					// Update the info of the frame associated to the terminal
					unsyncNodes.get(i).terminalFrame.update(unsyncNodes.get(i));
								
					// The terminal is new so its frame must be sent to the screen
					unsyncNodes.get(i).terminalFrame.display();															
						
					// Since the terminal is new the number of sync nodes must be increased
					numberOfSyncNodes++;																			
					
				} else if(!unsyncNodes.get(i).isShadowNode) { 
					
					unsyncNodes.get(i).terminalFrame.update(unsyncNodes.get(i)); 
					
					
				} else if (unsyncNodes.get(i).isShadowNode) {
					
					unsyncNodes.get(i).isShadowNode = false;
					VLVisualizer.layeredPaneVL.addNode(unsyncNodes.get(i));
					unsyncNodes.get(i).terminalFrame.mainPanel.removeAll();
					unsyncNodes.get(i).terminalFrame.update(unsyncNodes.get(i)); 
					unsyncNodes.get(i).terminalFrame.display();					
					numberOfSyncNodes++;																			

				}
				
				/*
				 * Updated info regarding the current terminal are sent back to the physical device
				 */
					
				try {
					
					// A dummy terminal is required to send the updated info to the physical device
					com.example.overmind.Terminal tmpTerminal = new com.example.overmind.Terminal();
					
					// The terminal acting as holder of the new info is updated
					tmpTerminal.update(unsyncNodes.get(i).terminal);			
																		
					// Write the info in the steam					
					unsyncNodes.get(i).writeObjectIntoStream(tmpTerminal);	

					System.out.println("Terminal with ip " + unsyncNodes.get(i).terminal.ip + " has been updated.");
					//System.out.println("Socket is closed: " + unsyncNodes.get(i).client.isClosed());
					//System.out.println("Socket is connected: " + unsyncNodes.get(i).client.isConnected());
					
					// Reset the collection of weights that have not been updated
					unsyncNodes.get(i).terminal.newWeights = new float[0];
					unsyncNodes.get(i).terminal.newWeightsIndexes = new int[0];
																	
				} catch (IOException e) {
		        	e.printStackTrace();
		        	//removeNode(unsyncNodes.get(i));
				}
							
			}				
			
			unsyncNodes.clear();	
												
		}
		
		MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), numberOfSyncNodes, numberOfShadowNodes));
		
	}

}
/* [End of VirtualLayerManager class] */