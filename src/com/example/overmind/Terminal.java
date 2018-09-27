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
    public HashMap<Integer, Population> populations = new HashMap<>();
    
    // Matrix that organizes populations based on their connections
    public Population[][] popsMatrix = null;

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
        byte secondHalf = Integer.valueOf(natPort).byteValue();
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
     * Method that removes a given population from the map and all its references 
     * from the input and output indexes arrays of the other populations. If, as a result
     * of this, either the inputs or the outputs lists of any given population are empty,
     * the method is called recursively to remove this population too
     * @param pop: The population to be removed
     */

    public void removePopulation(Population pop) {
    	// Remove the population from the table
    	populations.remove(pop.id);
    	
    	// Remove all the references to the population from its inputs
    	for (Integer inputId : pop.inputIndexes) {
    		Population input = populations.get(inputId);
    		
    		// The input might be a terminal, in which case populations.get
    		// returned null
    		if (input != null) {
        		input.outputIndexes.remove(pop.id);
        		input.numOfSynapses += pop.numOfNeurons;
    			
	    		// If the input is not connected to any other postsynaptic
	    		// population, it should be removed too
	    		if (input.outputIndexes.size() == 0)
	    			removePopulation(input);
    		} else {
    			
    			/*
    			 * If the input is a terminal we must remove the population from the input
    			 * to population mapping too
    			 */
    			
    			// Remove the population from the mapping
    			ArrayList<Integer> popsIndexes = inputsToPopulations.get(inputId);
    			popsIndexes.remove(pop.id);
    			
    			// Remove the array of the connections if it is empty
    			if (popsIndexes.size() == 0)
    				inputsToPopulations.remove(inputId);
    		}
    	}
    	
    	// Like before but now for the outputs
    	for (Integer outputId : pop.outputIndexes) {
    		Population output = populations.get(outputId);
    		if (output != null) {
    			output.numOfDendrites += pop.numOfNeurons;
        		output.inputIndexes.remove(pop.id);
	    		if (output.inputIndexes.size() == 0)
	    			removePopulation(output);
    		}

    		/*
    		 * Here the code is different as we merely need to eliminate the list containing
    		 * all the mappings of the population to be removed to the postsynaptic terminals
    		 */
    		
    		populationsToOutputs.remove(pop.id); // It is not guaranteed that there is such a list but this is not a problem
    	}
    }

    /**
     * This method adds a population, updating the lists of its input and outputs populations as well as 
     * the mappings between the terminals and the population itself. The method is very similar
     * to removePopulation
     * @param pop: The population to be added
     */
    
    public void addPopulation(Population pop) {
    	populations.put(pop.id, pop);
    	
    	for (Integer inputId : pop.inputIndexes) {
    		Population input = populations.get(inputId);
    		
    		if (input != null) {
    			input.outputIndexes.add(pop.id);
    		} else {
    			// There's a chance that the input terminal became childless after some populations were removed,
    			// therefore create a new array for the mappings if necessary   			
    			ArrayList<Integer> popsIndexes = inputsToPopulations.get(inputId);
    			if (popsIndexes == null) 
    				popsIndexes = new ArrayList<>();
    			popsIndexes.add(pop.id);
    			inputsToPopulations.put(inputId, popsIndexes);
    		}
    	}
    	
    	for (Integer outputId : pop.outputIndexes) {
    		Population output = populations.get(outputId);
    		if (output != null) {
        		output.inputIndexes.add(pop.id);
    		} else {
    			ArrayList<Integer> popsIndexes = new ArrayList<>();
    			popsIndexes.add(outputId);
    			populationsToOutputs.put(pop.id, popsIndexes);
    		}
    		
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
    	this.populations = new HashMap<>(terminal.populations);
    	this.inputsToPopulations = new HashMap<>(terminal.inputsToPopulations);
    	this.populationsToOutputs = new HashMap<>(terminal.populationsToOutputs);
        this.newWeights = new byte[terminal.newWeights.length];
        this.newWeightsIndexes = new int[terminal.newWeightsIndexes.length];
        this.updateWeightsFlags = new byte[terminal.updateWeightsFlags.length];
        System.arraycopy(terminal.newWeights, 0, this.newWeights, 0, terminal.newWeights.length);
        System.arraycopy(terminal.newWeightsIndexes, 0, this.newWeightsIndexes, 0, terminal.newWeightsIndexes.length);
        System.arraycopy(terminal.updateWeightsFlags, 0, this.updateWeightsFlags, 0, terminal.updateWeightsFlags.length);
        
        if (terminal.popsMatrix != null) {
        	this.popsMatrix = new Population[terminal.popsMatrix.length][];
        	for (int i = 0; i < terminal.popsMatrix.length; i++) {
        		if (popsMatrix[i] != null) {
	        		this.popsMatrix[i] = new Population[terminal.popsMatrix[i].length];
	        		System.arraycopy(terminal.popsMatrix[i], 0, this.popsMatrix[i], 0, terminal.popsMatrix[i].length);
        		}
        	}
        }        
    }
}
