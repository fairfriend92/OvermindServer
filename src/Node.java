import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.example.overmind.LocalNetwork;

public class Node {
	
	public String localNetworkIP;
	public Socket thisClient;
	public ObjectOutputStream output;
		
	public Node (String s, Socket s1) {
		this.localNetworkIP = s;
		this.thisClient = s1;
	}
	
	public void initialize() {
		try {
			output = new ObjectOutputStream(thisClient.getOutputStream());
            thisClient.setTrafficClass(0x04);
			thisClient.setTcpNoDelay(true);
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public void close() {
		try {
			this.thisClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        Node compare = (Node) obj;
        return compare.localNetworkIP.equals(this.localNetworkIP);
    }
}
