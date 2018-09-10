package com.example.overmind;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        Terminal compare = (Terminal) obj;
        if (!compare.ip.equals(serverIP))
            return (compare.ip.equals(this.ip) & compare.natPort == this.natPort);
        else
            return compare.ip.equals(this.ip);
    }

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
    
    public void updateMaps(int populationId, int terminalId, int FLAG) {
    	if (FLAG == INPUT_TO_POPULATION) {
    		if (!inputsToPopulations.containsKey(terminalId)) { 
    			ArrayList<Integer> arrayList = new ArrayList<>();
    			arrayList.add(populationId);
    			inputsToPopulations.put(terminalId, arrayList);
    		} else {
    			inputsToPopulations.get(terminalId).add(populationId);
    		}
    	} else {
    		if (!populationsToOutputs.containsKey(populationId)) {
    			ArrayList<Integer> arrayList = new ArrayList<>();
    			arrayList.add(terminalId);
    			populationsToOutputs.put(populationId, arrayList);
    		} else {
    			populationsToOutputs.get(populationId).add(terminalId);
    		}
    	}
    }
}
