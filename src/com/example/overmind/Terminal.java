package com.example.overmind;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class that describes the client devices and the populations that live therein. 
 * @author rodolforocco
 *
 */

public class Terminal implements Serializable {
	public static final int INPUT_TO_POPULATION = 0, POPULATION_TO_OUTPUT = 1;
	
    // These are total numbers for all the populations
    public short numOfNeurons, numOfDendrites, numOfSynapses;

    // Populations that live on this terminal
    public ArrayList<Population> populations = new ArrayList<>();

    // Map that connects a presynpatic terminal to the populations it stimulates that live on this terminal
    public HashMap<Integer, ArrayList<Integer>> inputsToPopulations = new HashMap<>();

    // Map that connects the populations on this terminal to the presynaptic terminals
    public HashMap<Integer, ArrayList<Integer>> populationsToOutputs = new HashMap<>();

    public String serverIP;
    public String ip;
    public int natPort;
    public int id;
    public ArrayList<Terminal> presynapticTerminals = new ArrayList<>(); // TODO: Arrays and collections shouldn't be created here.
    public ArrayList<Terminal> postsynapticTerminals = new ArrayList<>();
    public byte[] newWeights = new byte[0];
    public int[] newWeightsIndexes = new int[0];
    public byte[] updateWeightsFlags = new byte[0];

    // TODO: Use customHashCode() for the comparison
    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        Terminal compare = (Terminal) obj;
        if (!compare.ip.equals(serverIP))
            return (compare.ip.equals(this.ip) & compare.natPort == this.natPort);
        else
            return compare.ip.equals(this.ip);
    }


    /**
     * Generate a hash code base on the physical information of the connection
     * associated with the underlying device
     * @return: A custom hash obtained from the FNV-1 algorithm
     */
    
    public int customHashCode() {
        byte[] firstHalf = ip.getBytes();
        byte secondHalf = new Integer(natPort).byteValue();
        byte[] data = new byte[firstHalf.length + 1];
        System.arraycopy(firstHalf, 0, data, 0, firstHalf.length);
        data[firstHalf.length] = secondHalf;

        // Implementation of the FNV-1 algorithm
        int hash = 0x811c9dc5;
        for (int i = 0; i < data.length; i++) {
            hash ^= (int)data[i];
            hash *= 16777619;
        }
        return hash;
    }
    

    /**
     * Update the maps of the internal connections between input connections and populations
     * or populations and output connections
     * @param populationId 
     * @param terminalId
     * @param FLAG: Final value which tells what kind of operation should be executed
     */
    
    public void updateMaps(int populationId, int terminalId, int FLAG) {
    	if (FLAG == INPUT_TO_POPULATION) {
    		if (!inputsToPopulations.containsKey(terminalId)) { 
    			ArrayList<Integer> arrayList = new ArrayList<>();
    			arrayList.add(populationId);
    			inputsToPopulations.put(terminalId, arrayList);
    		} else {
    			inputsToPopulations.get(terminalId).add(populationId);
    		}
    		this.populations.get(this.populations.indexOf(new Integer(populationId))).inputIndexes.add(terminalId);
    	} else {
    		if (!populationsToOutputs.containsKey(populationId)) {
    			ArrayList<Integer> arrayList = new ArrayList<>();
    			arrayList.add(terminalId);
    			populationsToOutputs.put(populationId, arrayList);
    		} else {
    			populationsToOutputs.get(populationId).add(terminalId);
    		}
    		this.populations.get(this.populations.indexOf(new Integer(populationId))).outputIndexes.add(terminalId);
    	}
    }
    
    /**
     * Update all the references of this object and copies the contents of all
     * the arrays
     * @param terminal
     */
    
    public void updateTerminal(Terminal terminal) {
    	this.numOfNeurons = terminal.numOfNeurons;
    	this.numOfDendrites = terminal.numOfDendrites;
    	this.numOfSynapses = terminal.numOfSynapses;
    	this.ip = terminal.ip;
    	this.natPort = terminal.natPort;
    	this.presynapticTerminals = new ArrayList<>(terminal.presynapticTerminals);
    	this.postsynapticTerminals = new ArrayList<>(terminal.postsynapticTerminals);
    	this.newWeights = new byte[terminal.newWeights.length];
    	this.newWeightsIndexes = new int[terminal.newWeightsIndexes.length];
    	this.updateWeightsFlags = new byte[terminal.updateWeightsFlags.length];
        System.arraycopy(terminal.newWeights, 0, this.newWeights, 0, terminal.newWeights.length);
        System.arraycopy(terminal.newWeightsIndexes, 0, this.newWeightsIndexes, 0, terminal.newWeightsIndexes.length);
        System.arraycopy(terminal.updateWeightsFlags, 0, this.updateWeightsFlags, 0, terminal.updateWeightsFlags.length);
    }
}
