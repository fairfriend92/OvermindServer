import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Node {
	
	public int physicalID;
	public int virtualID;
	public short originalNumOfSynapses; // The number of synapses (available) decreases as new connections are added. It is useful to remember the number the node start with. 
	public boolean isShadowNode;
	public ArrayList<Node> postsynapticNodes;
	public ArrayList<Node> presynapticNodes;
	public TerminalFrame terminalFrame;
	public Socket client;
	public ObjectOutputStream output;
	public com.example.overmind.Terminal terminal;
	private final Object lock = new Object (); // TODO: Is this lock necessary?
	public boolean isExternallyStimulated; // Flag that tells if another program outside of the OvermindServer package is stimulating this node.
	
	public Node(Socket s1, com.example.overmind.Terminal t, ObjectOutputStream o) {
		this.physicalID = 0;
		this.virtualID = 0;
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
					presynapticTerminal.natPort == presynapticTerminal.natPort) return true; 
		return false;		
	}
	
	/*
	 * Enable or disable lateral connections depending on the current status
	 */
	
	public boolean changeLateralConnectionsOption() {		
		boolean operationSuccesful = false;
		
		// If lateral connections are enabled, remove the terminal from its own connections. Otherwise add it to them.
		if (this.hasLateralConnections()) {
			terminal.numOfSynapses += terminal.numOfNeurons;
			terminal.numOfDendrites += terminal.numOfNeurons;
			terminal.presynapticTerminals.remove(terminal); // TODO: equals method of terminal should also check the nat port
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
			if (VirtualLayerManager.nodesTable.containsKey(physicalID)) 
				VirtualLayerManager.connectNodes(new Node[]{this});		
		}
		
		return operationSuccesful;
	}
	
	public void update(Node updatedNode) {
		this.physicalID = updatedNode.physicalID;		
		this.virtualID = updatedNode.virtualID;
		this.isShadowNode = updatedNode.isShadowNode;
		this.postsynapticNodes = new ArrayList<>(updatedNode.postsynapticNodes);
		this.presynapticNodes = new ArrayList<>(updatedNode.presynapticNodes);
		this.terminalFrame.update(updatedNode);
		this.client = updatedNode.client;
		this.output = updatedNode.output;
		this.terminal.update(updatedNode.terminal);		
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
	    return this.physicalID;
	}

	@Override
    public boolean equals(Object obj) {		
       
		if (obj == null) { return false; }
		if (obj.getClass().equals(com.example.overmind.Terminal.class)) {
			com.example.overmind.Terminal compare = (com.example.overmind.Terminal) obj;
			return this.terminal.equals(compare);
		} else if (obj.getClass().equals(this.getClass())) {
			Node compare = (Node) obj;
	    	return compare.physicalID == this.physicalID;
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
