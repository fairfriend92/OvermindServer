package overmind_server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import overmind_server.TerminalFrame;
import overmind_server.VirtualLayerManager;

public class Node {
	
	public int id;
	public short originalNumOfSynapses; // The number of synapses (available) decreases as new connections are added. It is useful to remember the number the node starts with. 
	public boolean isShadowNode;
	public ArrayList<Node> postsynapticNodes; // TODO: Are these 2 arrays really needed now that we have the physical2virtual hash map?
	public ArrayList<Node> presynapticNodes;
	public TerminalFrame terminalFrame;
	public Socket client;
	public ObjectOutputStream output;
	public com.example.overmind.Terminal terminal;
	private final Object lock = new Object (); // TODO: Is this lock necessary?
	public boolean isExternallyStimulated; // Flag that tells if another program outside of the OvermindServer package is stimulating this node.
	
	public Node(Socket s1, com.example.overmind.Terminal t, ObjectOutputStream o) {
		this.id = 0;
		this.isShadowNode = false;
		this.client = s1;
		this.output = o;
		this.terminal = t;
		this.postsynapticNodes = new ArrayList<>();
		this.presynapticNodes = new ArrayList<>();
		this.terminalFrame = new TerminalFrame();		
		if (this.client != null && this.output == null) {
			try {
				this.output = new ObjectOutputStream(client.getOutputStream());
			} catch (IOException e) {
				System.out.println(e);
			} 
		} 
		if (this.terminal == null) { this.terminal = new com.example.overmind.Terminal(); }		
		isExternallyStimulated = false;
	}	
	
	/*
	 * Determine if the terminal is being stimulated.
	 */
	
	public boolean isBeingStimulated() {
		return !terminalFrame.thisTerminalRSG.shutdown | !terminalFrame.thisTerminalRSS.shutdown |
				isExternallyStimulated;
	}
	
	/*
	 * Determine if the underlying terminal has enabled lateral connections or not.
	 */
	
	public boolean hasLateralConnections() {
		for (com.example.overmind.Terminal presynapticTerminal : terminal.presynapticTerminals) 
			if (presynapticTerminal.ip.equals(terminal.ip) & 
					presynapticTerminal.natPort == terminal.natPort) return true; 
		return false;		
	}
	
	/*
	 * Enable or disable lateral connections depending on the current status
	 */
	
	// TODO: How should we handle this when populations are introduced?
	
	public boolean changeLateralConnectionsOption() {		
		boolean operationSuccesful = false;
		
		// If lateral connections are enabled, remove the terminal from its own connections. Otherwise add it to them.
		if (this.hasLateralConnections()) {
			terminal.numOfSynapses += terminal.numOfNeurons;
			terminal.numOfDendrites += terminal.numOfNeurons;
			terminal.presynapticTerminals.remove(terminal); 
			terminal.postsynapticTerminals.remove(terminal);
			operationSuccesful = true;
		} else if (terminal.numOfSynapses >= terminal.numOfNeurons & 
				terminal.numOfDendrites >= terminal.numOfNeurons){
			terminal.numOfSynapses -= terminal.numOfNeurons;
			terminal.numOfDendrites -= terminal.numOfNeurons;
			terminal.presynapticTerminals.add(terminal);
			terminal.postsynapticTerminals.add(terminal);
			operationSuccesful = true;
		}
		
		if (operationSuccesful) {
			if (VirtualLayerManager.nodesTable.containsKey(id)) 
				VirtualLayerManager.connectNodes(new Node[]{this});		
		}
		
		return operationSuccesful;
	}
	
	public void update(Node node) {
		this.id = node.id;
		this.isShadowNode = node.isShadowNode;
		this.postsynapticNodes = new ArrayList<>(node.postsynapticNodes);
		this.presynapticNodes = new ArrayList<>(node.presynapticNodes);
		this.terminalFrame.update(node);
		this.client = node.client;
		this.output = node.output;
		this.terminal.updateTerminal(node.terminal);
	}

	public void close() {
		try {
			this.client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int hashCode() {
	    return this.id;
	}

	@Override
    public boolean equals(Object obj) {		
       
		if (obj == null) { return false; }
		if (obj.getClass().equals(com.example.overmind.Terminal.class)) {
			com.example.overmind.Terminal compare = (com.example.overmind.Terminal) obj;
			return this.terminal.equals(compare);
		} else if (obj.getClass().equals(this.getClass())) {
			Node compare = (Node) obj;
	    	return compare.id == this.id;
		} else
			return false;    	
    }
	
	public void writeObjectIntoStream (Object obj) throws IOException
	{			
		synchronized (lock) {	
			output.reset();
			output.writeObject(obj);		
			output.flush();
			
		}		
	}
	
}
