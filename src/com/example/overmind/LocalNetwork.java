package com.example.overmind;

import java.io.Serializable;
import java.util.ArrayList;

public class LocalNetwork implements Serializable {
    public short numOfNeurons, numOfDendrites, numOfSynapses;
    public String ip;
    public ArrayList<LocalNetwork> presynapticNodes;
    public ArrayList<LocalNetwork> postsynapticNodes;   
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) { return true; }
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        LocalNetwork compare = (LocalNetwork) obj;
        return compare.ip.equals(ip);
    }
}