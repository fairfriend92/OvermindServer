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
		
        long lastTime = 0, staticRefresh = parentFrame.rateMultiplier * 1000000, 
        		dynamicRefresh = 0, rasterGraphRefresh;   
        
        targetTerminal.numOfDendrites -= 8;
        
        com.example.overmind.Terminal server = new com.example.overmind.Terminal();
        server.postsynapticTerminals = new ArrayList<>();
        server.presynapticTerminals = new ArrayList<>();
        server.ip = VirtualLayerManager.serverIP;
        //server.ip = "192.168.1.213";
        
        server.postsynapticTerminals.add(targetTerminal);
        server.numOfNeurons = 8;
        server.numOfSynapses = (short)(1024 - targetTerminal.numOfNeurons);
        server.numOfDendrites = 1024;
        server.natPort = VirtualLayerManager.SERVER_PORT_UDP;
        
        targetTerminal.presynapticTerminals.add(server);
        
        VirtualLayerManager.connectNodes(parentFrame.localUpdatedNode);    
        VirtualLayerManager.syncNodes();
        
        DatagramSocket outputSocket = null;

        try {
    	    outputSocket = new DatagramSocket();
    	    outputSocket.setTrafficClass(IPTOS_THROUGHPUT);   
        } catch (SocketException e) {
        	e.printStackTrace();
        }
        
        assert outputSocket != null;
        
        InetAddress targetDeviceAddr = null;
        		
        try {
			targetDeviceAddr = InetAddress.getByName(targetTerminal.ip);
		} catch (UnknownHostException e) {
        	e.printStackTrace();
		}
        
        assert targetDeviceAddr != null;
        
        byte[] dummySignal = new byte[1];
        
        for (int index = 0; index < 8; index++) {
        	dummySignal[0] &= ~(1 << index);
        }
                 
        while (!shutdown) {
        	
        	rasterGraphRefresh = (parentFrame.rastergraphPanel.time) * 1000000;  
    		dynamicRefresh = (staticRefresh < rasterGraphRefresh) && parentFrame.waitForLatestPacket ? rasterGraphRefresh : staticRefresh;
        	        	        	    
        	while ((System.nanoTime() - lastTime) < dynamicRefresh) {
            	//rasterGraphRefresh = parentFrame.rastergraphPanel.time * 1000000;   	
        		dynamicRefresh = (staticRefresh < rasterGraphRefresh) && parentFrame.waitForLatestPacket ? rasterGraphRefresh : staticRefresh;
        	}   
                      	        	        	
            try {
                DatagramPacket outputSpikesPacket = new DatagramPacket(dummySignal, 1, targetDeviceAddr, targetTerminal.natPort);	
				outputSocket.send(outputSpikesPacket);		
			} catch (IOException e) {
				System.out.println(e);
			}		
                                   
            lastTime = System.nanoTime();            
        	
        }
        
        outputSocket.close();
       
        targetTerminal.presynapticTerminals.remove(server);
    	targetTerminal.numOfDendrites +=  8;
                
        if (VirtualLayerManager.availableNodes.contains(parentFrame.localUpdatedNode)) {
			VirtualLayerManager.connectNodes(parentFrame.localUpdatedNode);
			VirtualLayerManager.syncNodes();
		}        
		
	}

}