package com.ntcu.app;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class SnykVulnerable {

    private final Map<String,List<Vulnerable>> map;

    public SnykVulnerable(){
        map = new HashMap<>();
    }

    public void put(String type, Vulnerable vul){
        if (!map.containsKey(type))
            map.put(type,new ArrayList<>());
        get(type).add(vul);
    }

    public List<Vulnerable> get(String type){
        return map.get(type);
    }

    public Boolean containsKey(String type){
        return map.containsKey(type);
    }
}