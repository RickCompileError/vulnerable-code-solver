package com.example;

import java.util.List;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.*;

public class JsonReader {
    public static SnykVulnerable getSnykVulnerable(String file_path){
        SnykVulnerable sv = new SnykVulnerable();
        try {
            // System.out.println("Path : " + Path.of(file_path));
            String content = Files.readString(Path.of(file_path), StandardCharsets.UTF_16);
            // System.out.println("Content : \n" + content);
            JSONArray parser = new JSONObject(content).
                                    getJSONArray("runs").
                                    getJSONObject(0).
                                    getJSONArray("results");
            Consumer<? super Object> consumer = new Consumer<Object>(){
                public void accept(Object obj){
                    JSONObject jsonobj = (JSONObject)obj;
                    String ruleid = jsonobj.getString("ruleId");
                    JSONObject region = jsonobj.getJSONArray("locations").getJSONObject(0).
                                            getJSONObject("physicalLocation").getJSONObject("region");
                    Vulnerable vul = new Vulnerable(ruleid,
                                                    region.getInt("startLine"),
                                                    region.getInt("endLine"),
                                                    region.getInt("startColumn"),
                                                    region.getInt("endColumn"));
                    sv.put(ruleid, vul);
                }
            };
            parser.forEach(consumer);
            return sv;
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return sv;
    }
}