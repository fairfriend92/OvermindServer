import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Node {
	
	public int ipHashCode;
	public int[] VLCoordinates;
	public ArrayList<Node> postsynapticNodes;
	public ArrayList<Node> presynapticNodes;
	public TerminalFrame terminalFrame;
	public Socket client;
	public ObjectOutputStream output;
	public com.example.overmind.Terminal terminal;
	private final Object lock = new Object ();
	
	public Node(Socket s1, com.example.overmind.Terminal t, ObjectOutputStream o) {
		this.client = s1;
		this.output = o;
		this.terminal = t;
		this.VLCoordinates = new int[]{0, 0};
		this.ipHashCode = 0;
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
		this.ipHashCode = updatedNode.ipHashCode;		
		this.postsynapticNodes = new ArrayList<>(updatedNode.postsynapticNodes);
		this.presynapticNodes = new ArrayList<>(updatedNode.presynapticNodes);
		this.terminalFrame.update(updatedNode);
		this.client = updatedNode.client;
		this.output = updatedNode.output;
		this.terminal.update(updatedNode.terminal);
		
		System.arraycopy(updatedNode.VLCoordinates, 0, this.VLCoordinates, 0, 2);
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
    	return (compare.terminal.ip.equals(this.terminal.ip) && compare.terminal.natPort == this.terminal.natPort);
    	
    }
	
	public void writeObjectIntoStream (Object obj) throws IOException
	{			
		synchronized (lock) {			
			output.writeObject(obj);		
			//output.flush();
			
		}		
	}
	
}
