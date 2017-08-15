import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Node {
	
	public int physicalID;
	public int virtualID;
	public boolean isShadowNode;
	public ArrayList<Node> postsynapticNodes;
	public ArrayList<Node> presynapticNodes;
	public TerminalFrame terminalFrame;
	public Socket client;
	public ObjectOutputStream output;
	public com.example.overmind.Terminal terminal;
	private final Object lock = new Object ();
	
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
       
		if (obj == null || obj.getClass() != this.getClass()) { return false; }
		Node compare = (Node) obj;
    	return (compare.physicalID == this.physicalID);
    	
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
