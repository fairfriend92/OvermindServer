
public class Constants {
	
	/* Network constants */
	
    public final static int ABSOLUTE_REFRACTORY_PERIOD = 2;
    public final static float SAMPLING_RATE = (float) 0.5;
    
    /* Numeric constants */
    
    static final float MIN_WEIGHT = 0.0078f;
    
    /* Connection constants */
    
    public final static int UDP_PORT = 4194;
    public final static int SERVER_PORT_TCP = 4195;
	public final static int SERVER_PORT_UDP = 4196;
	public static boolean USE_LOCAL_CONNECTION = false;
	
	/* Hard limits constants */
	
	// Constants used to size memory objects like collections and queues.
	public final static int MAX_CONNECTED_TERMINALS = 32;
	public final static int MAX_REGISTERED_APPS = 32;	
	public final static short MAX_DATA_BYTES = 8192;

}
