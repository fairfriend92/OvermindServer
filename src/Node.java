import java.net.Socket;

import com.example.overmind.LocalNetwork;

public class Node {
	
	public String localNetworkIP;
	public Socket thisClient;
	
	public Node (String s, Socket s1) {
		this.localNetworkIP = s;
		this.thisClient = s1;
	}

	@Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        Node compare = (Node) obj;
        return compare.localNetworkIP.equals(localNetworkIP);
    }
}
