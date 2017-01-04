import java.io.DataInputStream;
import java.io.IOException;

public class DataReceiver implements Runnable {

    	private DataInputStream input;
        private byte[] outputSpikes = new byte[Constants.NUMBER_OF_NEURONS / 8 + 1];
        private long oldTime = 0, newTime = 0;
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
                try {
                    input.readFully(outputSpikes, 0, Constants.NUMBER_OF_NEURONS / 8 + 1);                
                } catch (IOException e) {
                	System.out.println(e);
                }
                System.out.println("Elapsed time in nanoseconds " + (System.nanoTime() - oldTime) + " Spikes in hex " + this.bytesToHex(outputSpikes));       
                oldTime = System.nanoTime();
            }
        }
    }