package overmind_server;
/**
 * Called by my MainFrame to manage the connection and disconnection of the devices which make up the 
 * physical layer (PL). The physical devices, described by the Terminal objects, are embedded in the Node objects.
 * The Node object is a higher abstraction that is known only on the server side and contains the necessary 
 * information to communicate with the PL. The nodes are connected together to form the virtual layer (VL).
 * 
 * TERMINOLOGY
 * 
 * a) Device: the physical entity
 * b) Local network: the network hosted by the device
 * c) Terminal: the object describing the device
 * d) Node: the representation of the terminal in the virtual space
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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.example.overmind.Population;
import com.example.overmind.Terminal;

public class VirtualLayerManager extends Thread{			
	
	static boolean shutdown = false;	
	
	/* Server statistics */
	
	static int numberOfSyncNodes = 0;	
	static int numberOfShadowNodes = 0;
	static volatile int activeShadowNodesRatio = 2;	
	
	/* Nodes collections */
	
	public static ConcurrentHashMap<Integer, Node> nodesTable = new ConcurrentHashMap<>(Constants.MAX_CONNECTED_TERMINALS);	
	public static ConcurrentHashMap<Integer, float[]> weightsTable = new ConcurrentHashMap<>(Constants.MAX_CONNECTED_TERMINALS);	
	static ConcurrentHashMap<Integer, ArrayList<Node>> shadowNodesListsTable = new ConcurrentHashMap<>(Constants.MAX_CONNECTED_TERMINALS);	
	
	// When a packet arrives from a terminal we must be able to fetch the node from the information contained in the header of the packet,
	// therefore we create a map from the physical id to the virtual id
	public static ConcurrentHashMap<Integer, Integer> physical2VirtualID = new ConcurrentHashMap<>(Constants.MAX_CONNECTED_TERMINALS);
	
	public static List<Node> unsyncNodes = Collections.synchronizedList(new ArrayList<Node>());
	public static List<Node> availableNodes = Collections.synchronizedList(new ArrayList<Node>());	
	static List<Short> freeNodes = Collections.synchronizedList(new ArrayList<Short>());	
	
	/* Objects related to threading */
	
	static VirtualLayerVisualizer VLVisualizer = new VirtualLayerVisualizer();
	private static ExecutorService syncNodesExecutor = Executors.newCachedThreadPool();
	
	/* Network related objects */
	
	static public String serverIP = null;
	static public String localIP = null;
	private DatagramSocket inputSocket = null;
    private Socket clientSocket = null;		
	ServerSocket serverSocket = null; // Server socket can't be private since it's accessed by MainFrame during shutdown. 	
	private ObjectInputStream input = null;
	private ObjectOutputStream output = null;
	
    static com.example.overmind.Terminal thisServer = new com.example.overmind.Terminal();
			
	@Override
	public void run() {
		super.run();
		
		try {
			localIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		}
		
		System.out.println("Local IP is " + localIP);
		
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
                
        thisServer.ip = serverIP;
        thisServer.natPort = Constants.UDP_PORT;    
        thisServer.id = thisServer.customHashCode();
        
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
						
			try {				
						
				byte[] testPacketBuffer = new byte[1];
				
				DatagramPacket testPacket = new DatagramPacket(testPacketBuffer, 1);				
			
				inputSocket.receive(testPacket);				
				
				terminal.natPort = testPacket.getPort();		
							
				//terminal.ip = testPacket.getAddress().toString().substring(1);
				
				terminal.id = terminal.customHashCode();
			
				System.out.println("Nat port for device with IP " + terminal.ip + " is " + terminal.natPort + " " );				
						
			} catch (IOException e) {
	        	e.printStackTrace();
			} 
			
			terminal.postsynapticTerminals.add(thisServer);
			terminal.updateMaps(0, thisServer.id, Terminal.POPULATION_TO_OUTPUT); // 0 is the default population
			terminal.serverIP = thisServer.ip;
			// TODO: Should the number of synapses of the terminal be decreased by terminal.numOfNeurons to account for the random spike generator?
			
			/*
			 * Create the node containing the terminal object just received
			 */
				
			Node newNode = new Node(clientSocket, terminal, output);
			newNode.id = newNode.hashCode(); // Since the Node objects are local to the server machine the memory address is a valid hash code
			physical2VirtualID.put(terminal.id, newNode.id); 
			
			// If lateral connections have been enabled, to get the number of synapses the node started with the number
			// of neurons must be added to the current number of synapses
			if (newNode.hasLateralConnections())
				newNode.originalNumOfSynapses = (short)(terminal.numOfSynapses + terminal.numOfNeurons);
			else 
				newNode.originalNumOfSynapses = terminal.numOfSynapses;
		
			// Put the new node in the hashmap using the hashcode of the
			// InetAddress of the terminal contained in the node as key
			nodesTable.put(newNode.id, newNode);	
		
			assert terminal != null;				
			
			/*
			 * Connect the new node to the network only if the number of backup Shadow Nodes is sufficient
			 */
			
			if ((numberOfSyncNodes <= activeShadowNodesRatio * numberOfShadowNodes) || !MainFrame.useShadowNodesFlag ) {
			
				System.out.println("Active node added");				
				
				weightsTable.put(newNode.id, new float[0]);
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
				clientSocket.close(); // Closing the client automatically closes the streams too.			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			VLVisualizer.join();
			
			syncNodesExecutor.shutdown();
			boolean syncNodesExecutorIsShutdown = syncNodesExecutor.awaitTermination(1, TimeUnit.SECONDS);
			if (!syncNodesExecutorIsShutdown) {
				System.out.println("syncNodes executor did not shutdown in time");
				syncNodesExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	/* [End of run() method] */	
	
	/**
	 * Modify the connections of a node and its underlying terminal as specified by the user
	 * through the GUI 
	 * @param node: The node that should be modified
	 * @return: Integer flag that describes the result of the operation
	 */
	
	public synchronized static short modifyNode(Node[] node) {			
		
		if (VirtualLayerVisualizer.cutLinkFlag) {
		
			if (node[0].postsynapticNodes.contains(node[1])) {
				node[0].terminal.numOfSynapses += node[1].terminal.numOfNeurons;
				node[1].terminal.numOfDendrites += node[0].terminal.numOfNeurons;
				node[0].terminal.postsynapticTerminals.remove(node[1].terminal);
				node[1].terminal.presynapticTerminals.remove(node[0].terminal);
				node[0].postsynapticNodes.remove(node[1]);
				node[1].presynapticNodes.remove(node[0]);
			} else if (node[1].postsynapticNodes.contains(node[0])) {
				node[1].terminal.numOfSynapses += node[0].terminal.numOfNeurons;
				node[0].terminal.numOfDendrites += node[1].terminal.numOfNeurons;
				node[1].terminal.postsynapticTerminals.remove(node[0].terminal);
				node[0].terminal.presynapticTerminals.remove(node[1].terminal);
				node[1].postsynapticNodes.remove(node[0]);
				node[0].presynapticNodes.remove(node[1]);
			} else
				return 1; // TODO: Use constants to return value with meaningful name
								
		}
		
		if (VirtualLayerVisualizer.createLinkFlag && 
				(node[0].terminal.numOfSynapses - node[1].terminal.numOfNeurons) >= 0 &&
				(node[1].terminal.numOfDendrites - node[0].terminal.numOfNeurons) >= 0) {
			
			if (!node[1].presynapticNodes.contains(node[0]) && 
					(!node[1].postsynapticNodes.contains(node[0]) || VLVisualizer.allowBidirectionalConn)) {
				node[0].terminal.numOfSynapses -= node[1].terminal.numOfNeurons;
				node[1].terminal.numOfDendrites -= node[0].terminal.numOfNeurons;
				node[0].terminal.postsynapticTerminals.add(node[1].terminal);
				node[1].terminal.presynapticTerminals.add(node[0].terminal);
				node[0].postsynapticNodes.add(node[1]);
				node[1].presynapticNodes.add(node[0]);
			} else
				return 2;
			
		} else if (VirtualLayerVisualizer.createLinkFlag) return 3;
		
		connectNodes(node);
		
		return 0;
		
	}
	
	/**
	 * Connect to the virtual layer all the nodes contained in the argument
	 * @param disconnectedNodes Array of nodes that should be connected to the virtual layer
	 */
	
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
	
	/**
	 * Activate a shadow node 
	 * @param shadowNode
	 */
	
	public synchronized static void activateNode(Node shadowNode) {
		
		System.out.println("Activating shadow node with ip " + shadowNode.terminal.ip);
		
		ArrayList<Node> shadowNodesList = shadowNodesListsTable.get((int)shadowNode.terminal.numOfNeurons);
		shadowNodesList.remove(shadowNode);
		weightsTable.put(shadowNode.id, new float[0]);
		numberOfShadowNodes--;
		MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), numberOfSyncNodes, numberOfShadowNodes));
		connectNodes(new Node[]{shadowNode});		
		
	}
	
	/**
	 * Remove a shadow node from the virtual layer
	 * @param shadowNode: The shadow node to be removed
	 */
	
	public synchronized static void removeShadowNode(Node shadowNode) {
		
		System.out.println("Shadow node with ip " + shadowNode.terminal.ip + " is being removed");		
		
		ArrayList<Node> shadowNodesList = shadowNodesListsTable.get((int)shadowNode.terminal.numOfNeurons);
		shadowNodesList.remove(shadowNode);
		nodesTable.remove(shadowNode.id);
		numberOfShadowNodes--;

		shadowNode.terminalFrame.shutdown = true;		
		
		synchronized (shadowNode.terminalFrame.tcpKeepAliveLock) {
			
			shadowNode.terminalFrame.tcpKeepAliveLock.notify();
		
		}
		
		boolean sNodeMonitorIsShutdown = false;			
	
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
	
	/**
	 * Remove an active node from the virtual layers
	 * @param removableNode: Node that needs to be removed 
	 * @param disconnectionIsAbrupt: Flag that signals that the disconnection was unwanted 
	 */	
	
	public synchronized static void removeNode(Node node, boolean unwantedDisconnection) {	
		
		// If the removable node was in line to be updated, drop its reference
		unsyncNodes.remove(node);	
		
		// Remove the reference to the old node
		nodesTable.remove(node.id);
		numberOfSyncNodes--;
		
		physical2VirtualID.remove(node.terminal.id);
		
		/*
		 * Wake up the thread that sends the TCP keep alive packet
		 */
		
		node.terminalFrame.shutdown = true;		
		
		synchronized (node.terminalFrame.tcpKeepAliveLock) {
		
			node.terminalFrame.tcpKeepAliveLock.notify();
		
		}
		
		/*
		 * Shutdown the executor of the the spikes monitor 
		 */		
		
		boolean spikesMonitorIsShutdown = false;
			
		node.terminalFrame.spikesMonitorExecutor.shutdown();	
		
		try {
			spikesMonitorIsShutdown = node.terminalFrame.spikesMonitorExecutor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!spikesMonitorIsShutdown) {
			System.out.println("Failed to shutdown spikes monitor for device with ip " + node.terminal.ip);	
		} 			
		
		/*
		 * Shutdown the executor of the external stimuli
		 */
		
		node.terminalFrame.thisTerminalRSG.shutdown = true;		
		node.terminalFrame.thisTerminalRSS.shutdown = true;	
		
		node.terminalFrame.stimulusExecutor.shutdown();	
		
		boolean stimulusExecIsShutdown = false;
		
		try {
			stimulusExecIsShutdown = node.terminalFrame.stimulusExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!stimulusExecIsShutdown) {
			System.out.println("Failed to close the stimuli executor for device with ip " + node.terminal.ip);	
		}		
		
		/*
		 * Shutdown the executor of the tpcKeepAlive package sender
		 */
		
		boolean tcpKeepAliveExecutorIsShutdown = false;
		
		node.terminalFrame.tcpKeepAliveExecutor.shutdown();
		
		try {
			tcpKeepAliveExecutorIsShutdown = node.terminalFrame.tcpKeepAliveExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (!tcpKeepAliveExecutorIsShutdown) {
			System.out.println("Failed to shutdown tcpKeepAlive package sender with ip " + node.terminal.ip);	
		} 	
		
		/*
		 * Close the frame associated to the terminal and the socket as well
		 */
		
		node.terminalFrame.frame.dispose();		
		
		node.close();
		
		VLVisualizer.layeredPaneVL.removeNode(node);			
		
		// TODO: accept shadow nodes with number of neurons greater than that of the removed node?
		ArrayList<Node> shadowNodesList = shadowNodesListsTable.get((int)node.terminal.numOfNeurons);
		
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
			Node nodeCopy = new Node(null, null, null);	
			nodeCopy.update(node);
			Node shadowNodeCopy = new Node(null, null, null);
			shadowNodeCopy.update(shadowNode);
			
			ApplicationInterface.RemovedNode removedNode = new ApplicationInterface.RemovedNode(nodeCopy, shadowNodeCopy);
			ApplicationInterface.addRemovedNode(removedNode);
			
			/*
			 * Shutdown the partial terminal frame of the shadow node
			 */
			
			shadowNode.terminalFrame.shutdown = true;		
			
			synchronized (shadowNode.terminalFrame.tcpKeepAliveLock) {
				
				shadowNode.terminalFrame.tcpKeepAliveLock.notify();
			
			}
			
			boolean sNodeMonitorIsShutdown = false;				
		
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
			
			System.out.println("Node with ip " + node.terminal.ip + " is being substituted with node with ip " + shadowNode.terminal.ip);		
			
			node.terminalFrame.localUpdatedNode = null;	
						
			// Retrieve the weights of the old node
			float[] weights = weightsTable.get(node.id);
			
			// Update the virtual ID of the old node
			node.id = shadowNode.id;
			
			// Reinsert the old node using the new virtual ID as hash tag
			nodesTable.put(node.id, node);
			
			// Assign the object output stream of the shadow node to the old node		
			node.output = shadowNode.output;
			
			// Close the socket which connected the server to the old terminal, since it's not
			// available anymore
			node.close();
			
			// Assign the socket of the shadow node to the old node
			node.client = shadowNode.client;
			
			// Update the info regarding the physical terminal underlying the old node with that
			// of the terminal which is part of the shadow node
			node.terminal.ip = shadowNode.terminal.ip;
			node.terminal.natPort = shadowNode.terminal.natPort;
			
			// Create the two arrays which represent the sparse array containing only those synaptic 
			// weights which have been modified
			float[] newWeights = new float[node.terminal.numOfNeurons * node.originalNumOfSynapses];
			int[] newWeightsIndexes = new int[node.terminal.numOfNeurons * node.originalNumOfSynapses];
			int index = 0;
			
			// Check which of the retrieved weights are different from the default value (0.0) 
			// and populate the sparse array consequently
			for (int i = 0; i < weights.length; i++) {
				if (weights[i] != 0) {
					newWeights[index] = weights[i];
					newWeightsIndexes[index] = i;
					index++;
				}					
			}
			
			// Resize the sparse array
			node.terminal.newWeights = new byte[index];
			System.arraycopy(weights, 0, node.terminal.newWeights, 0, index);
			node.terminal.newWeightsIndexes = new int[index];
			System.arraycopy(newWeightsIndexes, 0, node.terminal.newWeightsIndexes, 0, index);		
						
			unsyncNodes.addAll(node.presynapticNodes);
			unsyncNodes.addAll(node.postsynapticNodes);
									
			// Update the number of shadow nodes
			numberOfShadowNodes--;
			
			unsyncNodes.add(node);
			
			// Prepare shadow node for garbage collection
			shadowNode = null;			
			
			syncNodes();
			
		} else {
		
			System.out.println("Node with ip " + node.terminal.ip + " is being removed");	
			
			/*
			 * Send the application interface a reference to the removable node so that all 
			 * the currently interfaced applications can handle the removal. No need to make a deep copy
			 * of removable node in this case since its info are left untouched. 
			 */
			
			ApplicationInterface.RemovedNode removedNode = new ApplicationInterface.RemovedNode(node);
			ApplicationInterface.addRemovedNode(removedNode);
	
			availableNodes.remove(node); 							
												
			/*
			 * Remove all references to the current terminal from the other terminals' lists.
			 */
			
			boolean nodeHasBeenModified = false;
			Node tmpNode;
			
			int numOfNodesAffected = node.presynapticNodes.size() + node.postsynapticNodes.size();
			
			ArrayList<Node> modifiedNodes = new ArrayList<>(numOfNodesAffected);
			
			for (int i = 0; i < node.presynapticNodes.size(); i++) {
				
				tmpNode = node.presynapticNodes.get(i);
				
				nodeHasBeenModified = tmpNode.postsynapticNodes.remove(node);
				tmpNode.terminal.postsynapticTerminals.remove(node.terminal);			
				
				if (nodeHasBeenModified) {
					tmpNode.terminal.numOfSynapses += node.terminal.numOfNeurons;
					//unsyncNodes.add(tmpNode);
					modifiedNodes.add(tmpNode);
					
					// Remove all the references to the disconnected terminal from the arraylists containing the indexes 
					// of the terminals stimulated by population
					for (Population population : tmpNode.terminal.populations) {
						tmpNode.terminal.populationsToOutputs.get(population.id).remove(node.terminal.id);
					}
					
					nodeHasBeenModified = false;
				}
				
			}
							
			for (int i = 0; i < node.postsynapticNodes.size(); i++) {
				
				tmpNode = node.postsynapticNodes.get(i);
				
				nodeHasBeenModified = tmpNode.presynapticNodes.remove(node);
				tmpNode.terminal.presynapticTerminals.remove(node.terminal);
				
				// Remove the arraylist containing all the indexes of the populations that were stimulated by the
				// disconnected terminal
				tmpNode.terminal.inputsToPopulations.remove(node.terminal.id);
				
				if (nodeHasBeenModified) {
					short postsynNodeDendrites = (short)(tmpNode.terminal.numOfDendrites + node.terminal.numOfNeurons);
					
					/*
					 * If the number of active synapses is zero, then the reference in the table of weights
					 * must be updated with a zero length array. Otherwise the weights in the array need to 
					 * be shifted since, when removableNode is removed from the presynaptic connections of
					 * postsynNode, the connections in the ArrayList are automatically shifted and therefore
					 * the weights do not coincide anymore with the relative connections. 
					 */					
									
					if (postsynNodeDendrites == tmpNode.originalNumOfSynapses) {
						float[] newWeights = new float[0];
						weightsTable.put(tmpNode.id, newWeights);
					} else {					
						int weightsOffset = 0; // How many synapses come before the ones of the node which is being removed
						short activeSynapses = (short)(tmpNode.originalNumOfSynapses - tmpNode.terminal.numOfDendrites);
						
						/*
						 * Compute the weights offset by iterating over the presynaptic connections of the
						 * node which is connected to the one to be removed and by incrementing the offset
						 * by the number of neurons of said presynaptic connection. 
						 */
						
						Iterator<Terminal> iterator = tmpNode.terminal.presynapticTerminals.iterator();
						while (iterator.hasNext()) {
							Terminal presynTerminal = iterator.next();
							if (presynTerminal.equals(node.terminal)) {
								break;
							} else {
								weightsOffset += presynTerminal.numOfNeurons;
							}
						}
						
						/*
						 * Copy the weights that come after the ones relative to the synapses of the removableNode in place
						 * of the latter. For convenience the weights that now do not refer to any synapse are not zeroed, neither
						 * is the the synaptic weights array resized. 
						 */
						
						float[] postsynNodeWeights = weightsTable.get(tmpNode.id);
						
						// Active synapses that come in the array after the ones relative to removableNode
						int remainingActiveSynapses = activeSynapses - weightsOffset - node.terminal.numOfNeurons;
						
						// Copy the weights of the synapses that come after the ones of removableNode in their positions 
						for (int neuronIndex = 0; neuronIndex < tmpNode.terminal.numOfNeurons; neuronIndex++) {
							System.out.println(" " + (neuronIndex * activeSynapses + weightsOffset + node.terminal.numOfNeurons) + " " 
									+ postsynNodeWeights.length);
							System.arraycopy(postsynNodeWeights, neuronIndex * activeSynapses + weightsOffset + node.terminal.numOfNeurons,
									postsynNodeWeights, neuronIndex * activeSynapses + weightsOffset, remainingActiveSynapses);
						}
						
						weightsTable.put(tmpNode.id, postsynNodeWeights);
					}
					
					tmpNode.terminal.numOfDendrites = postsynNodeDendrites;
					//unsyncNodes.add(tmpNode);
					modifiedNodes.add(tmpNode);
					nodeHasBeenModified = false;
				}
				
			}								
			
			weightsTable.remove(node.id);
			
			// Shutdown the object stream and the socket. 			
			node.close(); // TODO: This method is useless and confusing. 
			
			//removableNode.terminalFrame = null;
														
			// Sync other nodes that have been eventually modified
			//syncNodes();
			connectNodes(modifiedNodes.toArray(new Node[numOfNodesAffected]));
		
		}
		
	}
	
	/**
	 * Method that simply submits a worker thread whose job is that of synchronizing 
	 * all the unsync nodes. 
	 * @return Future object useful to know when the sync operation is done. 
	 */
	
	public static Future<Boolean> syncNodes() {
		
		/*
		 * Threading of the sync operation allows the VLM thread to receive 
		 * new connections even if the SyncNodeWorker is busy. It can also allow 
		 * for multiple synchronization processes to take place at the same time. 
		 */
		
		return syncNodesExecutor.submit(new SyncNodeWorker());			
	}

	private static class SyncNodeWorker implements Callable<Boolean> {	
		
		private final boolean STREAM_INTERRUPTED = false;
		private final boolean SYNC_SUCCESSFUL = true;
		
		@Override
		public Boolean call() {
		
			/*
			 * Sync the GUI with the updated info about the terminals 
			 */	
			
			if (!unsyncNodes.isEmpty()) {		
				
				// TODO: Use iterator instead of for loop. 				
				
				// Iterate over all the nodes that need to be sync
				for (int i = 0; i < unsyncNodes.size(); i++) {		
					
					Node nodeToSync = unsyncNodes.get(i);
																					
					// Branch depending on whether the terminal is new or not
					if (nodeToSync.terminalFrame.localUpdatedNode == null 
							&& !nodeToSync.isShadowNode) {		
						
						VLVisualizer.layeredPaneVL.addNode(nodeToSync);
						
						nodeToSync.terminalFrame = new TerminalFrame();
						
						// Update the info of the frame associated to the terminal
						nodeToSync.terminalFrame.update(nodeToSync);
									
						// The terminal is new so its frame must be sent to the screen
						nodeToSync.terminalFrame.display();															
							
						// Since the terminal is new the number of sync nodes must be increased
						numberOfSyncNodes++;																			
						
					} else if(!nodeToSync.isShadowNode) {  // TODO: Revise terminology used here to make things clearer. 
						
						nodeToSync.terminalFrame.update(nodeToSync); 
						
						
					} else if (nodeToSync.isShadowNode) {
						
						nodeToSync.isShadowNode = false;
						VLVisualizer.layeredPaneVL.addNode(nodeToSync);
						nodeToSync.terminalFrame.mainPanel.removeAll();
						nodeToSync.terminalFrame.update(nodeToSync); 
						nodeToSync.terminalFrame.display();					
						numberOfSyncNodes++;																			
	
					}
					
					/*
					 * Updated info regarding the current terminal are sent back to the physical device
					 */
						
					try {
						
						// A dummy terminal is required to send the updated info to the physical device
						com.example.overmind.Terminal tmpTerminal = new com.example.overmind.Terminal();
						
						// The terminal acting as holder of the new info is updated
						tmpTerminal.updateTerminal(nodeToSync.terminal);
																			
						// Write the info in the steam					
						nodeToSync.writeObjectIntoStream(tmpTerminal);	
	
						System.out.println("Terminal with ip " + nodeToSync.terminal.ip + " has been updated");
						
						// Reset the collection of weights that have not been updated
						nodeToSync.terminal.newWeights = new byte[0];
						nodeToSync.terminal.newWeightsIndexes = new int[0];
						nodeToSync.terminal.updateWeightsFlags = new byte[0];
																		
					} catch (IOException e) {
						System.out.println("Update of terminal with ip " + nodeToSync.terminal.ip + " interrupted abruptly");
						if (!shutdown) // The stream may have been interrupted by a shutdown order. No need to remove the node in that case. 
							removeNode(nodeToSync, true);
						return STREAM_INTERRUPTED;
					}					
								
				}			
																
			}
			
			unsyncNodes.clear();
			
			MainFrame.updateMainFrame(new MainFrameInfo(unsyncNodes.size(), numberOfSyncNodes, numberOfShadowNodes));	
			
			return SYNC_SUCCESSFUL;
		}
		
	}
}