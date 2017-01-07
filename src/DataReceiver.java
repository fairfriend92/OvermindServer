import java.io.DataInputStream;
import java.io.IOException;

public class DataReceiver implements Runnable {

    	private DataInputStream input;
    	private char dataBytes = (Constants.NUMBER_OF_NEURONS % 8) == 0 ? (char)(Constants.NUMBER_OF_NEURONS / 8) : (char)(Constants.NUMBER_OF_NEURONS / 8) + 1;
        private byte[] outputSpikes = new byte[dataBytes];
        private long startTime = 0, endTime = 0;        
        public DataReceiver (DataInputStream d) {
            this.input = d;
        }
        
        final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
        public static String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for ( int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }

        @Override
        public void run () {
            while (!OvermindServer.shutdown) {
            	startTime = System.nanoTime();
                try {
                    input.readFully(outputSpikes, 0, dataBytes);                
                } catch (IOException e) {
                	System.out.println(e);
                }
                endTime = System.nanoTime(); 
                
                while (endTime - startTime < 200000) {
                	endTime = System.nanoTime();
                }
                
                System.out.println("Elapsed time in nanoseconds " + (System.nanoTime() - startTime) + " Spikes in hex " + bytesToHex(outputSpikes));       
            }
        }
    }