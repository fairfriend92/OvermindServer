package overmind_server;

public class MainFrameInfo {
		
	public int numOfUnsyncNodes;
	public int numOfSyncNodes;
	public int numOfShadowNodes;
	
	public MainFrameInfo (int i, int i1, int i2) {
		this.numOfUnsyncNodes = i;
		this.numOfSyncNodes = i1;
		this.numOfShadowNodes = i2;
	}

}
