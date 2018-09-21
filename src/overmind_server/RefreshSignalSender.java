package overmind_server;
/**
 * This class sends to the target terminal a single byte containing no information.
 * The packet sent has the only purpose of unlocking the blocking buffer of the target terminal,
 * so that a minimal sampling rate is guaranteed. 
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.example.overmind.Terminal;

public class RefreshSignalSender implements Runnable {
	
	public final static int UDP_CLIENT_PORT = 4194;	
	private final static int IPTOS_THROUGHPUT = 0x08;
	
	private com.example.overmind.Terminal targetTerminal;
	private TerminalFrame parentFrame;
	
	RefreshSignalSender(TerminalFrame l) {
		this.parentFrame = l;
	}
	
	public boolean shutdown = true;
	
	@Override public void run() {	
				
		targetTerminal = parentFrame.localUpdatedNode.terminal;	
		
        long staticRefresh = parentFrame.rateMultiplier * 1000000, 
        		dynamicRefresh = 0, rasterGraphRefresh;
     
        com.example.overmind.Terminal server = null;
        
        for (Terminal postsynTerminal : targetTerminal.postsynapticTerminals)        	
        	if (postsynTerminal.id == VirtualLayerManager.thisServer.id)
        		server = postsynTerminal;        
        
        assert server != null;
        
        server.ip = Constants.USE_LOCAL_CONNECTION ? VirtualLayerManager.localIP : VirtualLayerManager.serverIP;        
        server.postsynapticTerminals.add(targetTerminal);
        server.numOfNeurons = 8 < targetTerminal.numOfDendrites ? 8 : targetTerminal.numOfDendrites;;
        server.numOfSynapses = (short)(32767 - targetTerminal.numOfNeurons); 

        targetTerminal.numOfDendrites -= server.numOfNeurons;
        
        targetTerminal.presynapticTerminals.add(server);
        
        VirtualLayerManager.connectNodes(new Node[]{parentFrame.localUpdatedNode});    
                                
        InetAddress targetDeviceAddr = null;
        		
        try {
			targetDeviceAddr = InetAddress.getByName(targetTerminal.ip);
		} catch (UnknownHostException e) {
        	e.printStackTrace();
		}
        
        assert targetDeviceAddr != null;
        
        byte[] dummySignal = new byte[1];        
                 
        while (!shutdown) {
        	long startTime = System.nanoTime();
        	
        	rasterGraphRefresh = (parentFrame.rastergraphPanel.time) * 1000000;  
    		dynamicRefresh = (staticRefresh < rasterGraphRefresh) && parentFrame.waitForLatestPacket ? rasterGraphRefresh : staticRefresh;        	        	        	    
        	                        	        	        	
            try {
                DatagramPacket outputSpikesPacket = new DatagramPacket(dummySignal, 1, targetDeviceAddr, targetTerminal.natPort);	
				SpikesReceiver.datagramSocket.send(outputSpikesPacket);		
			} catch (IOException e) {
				System.out.println(e);
			}		
                                   
            while ((System.nanoTime() - startTime) < dynamicRefresh) {
            	//rasterGraphRefresh = parentFrame.rastergraphPanel.time * 1000000;   	
        		dynamicRefresh = (staticRefresh < rasterGraphRefresh) && parentFrame.waitForLatestPacket ? rasterGraphRefresh : staticRefresh;
        	}   
        	
        }       
      
        targetTerminal.presynapticTerminals.remove(server);
    	targetTerminal.numOfDendrites +=  server.numOfNeurons;
                
        if (VirtualLayerManager.nodesTable.containsKey(parentFrame.localUpdatedNode.id)) {
			VirtualLayerManager.connectNodes(new Node[]{parentFrame.localUpdatedNode});
			//VirtualLayerManager.syncNodes();
		}        
		
	}

}