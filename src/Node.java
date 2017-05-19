import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Node {
	
	public short layer, level, index;
	public ArrayList<Node> postsynapticNodes;
	public ArrayList<Node> presynapticNodes;
	public TerminalFrame terminalFrame;
	public Socket client;
	public ObjectOutputStream output;
	public com.example.overmind.Terminal terminal;
	
	public Node(Socket s1, com.example.overmind.Terminal t) {
		this.client = s1;
		this.terminal = t;
	}
	
	public Node() { }
	
	public void update(Node updatedNode) {
		this.layer = updatedNode.layer;
		this.level = updatedNode.level;		
		this.postsynapticNodes = new ArrayList<>(updatedNode.postsynapticNodes);
		this.presynapticNodes = new ArrayList<>(updatedNode.presynapticNodes);
		if (this.terminalFrame == null) { this.terminalFrame = new TerminalFrame(); }
		this.terminalFrame.update(updatedNode);
		this.client = updatedNode.client;
		this.output = updatedNode.output;
		this.terminal.update(updatedNode.terminal);
	}
	
	public void initialize() {
		this.layer = 0;
		this.level = 0;
		this.postsynapticNodes = new ArrayList<>();
		this.presynapticNodes = new ArrayList<>();
		this.terminalFrame = null;		
		if (this.client != null) {
			try {
				this.output = new ObjectOutputStream(client.getOutputStream());
				this.client.setTrafficClass(0x04);
				this.client.setTcpNoDelay(true);
			} catch (IOException e) {
				System.out.println(e);
			} 
		} 
		if (this.terminal == null) { this.terminal = new com.example.overmind.Terminal(); }
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
	    return this.terminal.ip.hashCode();
	}

	@Override
    public boolean equals(Object obj) {		
       
		if (obj == null || obj.getClass() != this.getClass()) { return false; }
		Node compare = (Node) obj;
    	return compare.terminal.ip.equals(this.terminal.ip);
    	
    }
	
}
