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
	
	private com.example.overmind.LocalNetwork targetDevice;
	private LocalNetworkFrame parentFrame;
	
	RefreshSignalSender(LocalNetworkFrame l) {
		this.parentFrame = l;
	}
	
	public boolean shutdown = true;
	
	@Override public void run() {
		
		targetDevice = parentFrame.localUpdatedNode;
		
        long lastTime = 0, newTime = 0, sendTime = 0;   
        
        targetDevice.numOfDendrites -= 8;
        
        com.example.overmind.LocalNetwork server = new com.example.overmind.LocalNetwork();
        server.postsynapticNodes = new ArrayList<>();
        server.presynapticNodes = new ArrayList<>();
        server.ip = VirtualLayerManager.serverIP;
        //server.ip = "192.168.1.213";
        
        server.postsynapticNodes.add(targetDevice);
        server.numOfNeurons = 8;
        server.numOfSynapses = (short)(1024 - targetDevice.numOfNeurons);
        server.numOfDendrites = 1024;
        server.natPort = VirtualLayerManager.SERVER_PORT_UDP;
        
        targetDevice.presynapticNodes.add(server);
        
        VirtualLayerManager.connectDevices(targetDevice);    
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
			targetDeviceAddr = InetAddress.getByName(targetDevice.ip);
		} catch (UnknownHostException e) {
        	e.printStackTrace();
		}
        
        assert targetDeviceAddr != null;
        
        byte[] dummySignal = new byte[1];
        
        for (int index = 0; index < 8; index++) {
        	dummySignal[0] &= ~(1 << index);
        }
        
        short rateMultiplier = parentFrame.rateMultiplier;
        
        while (!shutdown) {
        	
        	newTime = System.nanoTime();              
	        	
        	while (newTime - lastTime < rateMultiplier * 1000000 - sendTime) {
        		newTime = System.nanoTime();         
        	}          	                 	   
        	        	
            try {
                DatagramPacket outputSpikesPacket = new DatagramPacket(dummySignal, 1, targetDeviceAddr, targetDevice.natPort);	
				outputSocket.send(outputSpikesPacket);			
			} catch (IOException e) {
				System.out.println(e);
			}		
                                   
            lastTime = System.nanoTime();            
        	
        }
        
        outputSocket.close();
       
        targetDevice.presynapticNodes.remove(server);
    	targetDevice.numOfDendrites +=  8;
        
        if (VirtualLayerManager.availableNodes.contains(targetDevice)) {        	
        	VirtualLayerManager.connectDevices(targetDevice);
        	VirtualLayerManager.syncNodes();
        }
        
		
	}

}
