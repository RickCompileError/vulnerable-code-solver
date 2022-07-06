package com.example;

import java.util.List;
import java.util.SortedMap;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.*;

public class JsonReader {
    public static SnykVulnerable getSnykVulnerable(Path file_path){
        SnykVulnerable sv = new SnykVulnerable();
        // String content = tryReadString(Path.of(file_path));
        String content = null;
        try{
            content = Files.readString(file_path, StandardCharsets.UTF_8);
        }catch (IOException ioe){
            System.out.println(ioe.getStackTrace());
        }
        JSONArray parser = new JSONObject(content).
                                getJSONArray("runs").
                                getJSONObject(0).
                                getJSONArray("results");
        Consumer<? super Object> consumer = new Consumer<Object>(){
            public void accept(Object obj){
                JSONObject jsonobj = (JSONObject)obj;
                String ruleid = jsonobj.getString("ruleId");
                String uri = jsonobj.getJSONArray("locations").getJSONObject(0).
                                        getJSONObject("physicalLocation").getJSONObject("artifactLocation").
                                        getString("uri");
                JSONObject region = jsonobj.getJSONArray("locations").getJSONObject(0).
                                        getJSONObject("physicalLocation").getJSONObject("region");
                Vulnerable vul = new Vulnerable(ruleid,
                                                uri,
                                                region.getInt("startLine"),
                                                region.getInt("endLine"),
                                                region.getInt("startColumn"),
                                                region.getInt("endColumn"));
                sv.put(ruleid, vul);
            }
        };
        parser.forEach(consumer);
        return sv;
    }

    public static SnykVulnerable getSnykVulnerable(String file_path){
        return getSnykVulnerable(Path.of(file_path));
    }

    // BUGS : the function may not work
    private static String tryReadString(Path path){
        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        for (String i: charsets.keySet()){
            boolean success = true;
            String content = null;
            try {
                content = Files.readString(path, charsets.get(i));
            } catch (IOException ioe){
                success = false;
            }
            if (success) return content;
        }
        return null;
    }
}