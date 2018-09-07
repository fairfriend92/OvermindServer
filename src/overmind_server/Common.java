package overmind_server;
import java.util.ArrayList;

import com.example.overmind.Terminal;

public class Common {
	
	/**
	 * Update terminal info, making deep copies whenever necessary.
	 * @param updatedTerminal The terminal holding the new info with which the old terminal must be updated.
	 * @param oldTerminal The old terminal that needs to be updated with the info stored in the updated terminal. 
	 */
	
    public static void updateTerminal(Terminal updatedTerminal, Terminal oldTerminal) {
    	oldTerminal.numOfNeurons = updatedTerminal.numOfNeurons;
    	oldTerminal.numOfDendrites = updatedTerminal.numOfDendrites;
    	oldTerminal.numOfSynapses = updatedTerminal.numOfSynapses;
    	oldTerminal.ip = updatedTerminal.ip;
    	oldTerminal.natPort = updatedTerminal.natPort;
    	oldTerminal.presynapticTerminals = new ArrayList<>(updatedTerminal.presynapticTerminals);
    	oldTerminal.postsynapticTerminals = new ArrayList<>(updatedTerminal.postsynapticTerminals);
    	oldTerminal.newWeights = new byte[updatedTerminal.newWeights.length];
    	oldTerminal.newWeightsIndexes = new int[updatedTerminal.newWeightsIndexes.length];
    	oldTerminal.updateWeightsFlags = new byte[updatedTerminal.updateWeightsFlags.length];
        System.arraycopy(updatedTerminal.newWeights, 0, oldTerminal.newWeights, 0, updatedTerminal.newWeights.length);
        System.arraycopy(updatedTerminal.newWeightsIndexes, 0, oldTerminal.newWeightsIndexes, 0, updatedTerminal.newWeightsIndexes.length);
        System.arraycopy(updatedTerminal.updateWeightsFlags, 0, oldTerminal.updateWeightsFlags, 0, updatedTerminal.updateWeightsFlags.length);
    }

}
