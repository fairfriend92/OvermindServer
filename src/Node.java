import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Node {
	
	public short layer, level;
	public Socket thisClient;
	public ObjectOutputStream output;
	public com.example.overmind.Terminal terminal;
	
	public Node (Socket s1, com.example.overmind.Terminal t) {
		this.thisClient = s1;
		this.terminal = t;
	}
	
	public void initialize() {
		try {
			output = new ObjectOutputStream(thisClient.getOutputStream());
            thisClient.setTrafficClass(0x04);
			thisClient.setTcpNoDelay(true);
		} catch (IOException e) {
			System.out.println(e);
		}
		layer = 0;
		level = 0;
	}
	
	public void close() {
		try {
			this.thisClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int hashCode() {
	    return terminal.ip.hashCode();
	}

	@Override
    public boolean equals(Object obj) {		
       
		if (obj == null || obj.getClass() != this.getClass()) { return false; }
		Node compare = (Node) obj;
    	return compare.terminal.ip.equals(this.terminal.ip);
    	
    }
	
}
