package com.ntcu.app.vuln;

import java.util.function.Consumer;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.*;

import com.ntcu.app.util.EncodingSolver;

public class JsonReader {
    public static SnykVulnerable getSnykVulnerable(Path file_path){
        System.out.println("Start Reading Json File: " + file_path);
        SnykVulnerable sv = new SnykVulnerable();
        String content = null;
        try{
            // snyk code test have a bug leads to JSON encoding in utf-16 and a prefix BOM (JSON can't have BOM)
            Charset charset = EncodingSolver.getCharset(file_path);
            EncodingSolver.removeJsonBOM(file_path);
            content = Files.readString(file_path, charset);
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
                JSONArray locations = jsonobj.getJSONArray("codeFlows").getJSONObject(0)
                                        .getJSONArray("threadFlows").getJSONObject(0)
                                        .getJSONArray("locations");
                JSONObject physicalLocation = locations.getJSONObject(locations.length()-1)
                                                .getJSONObject("location").getJSONObject("physicalLocation");
                Path uri = Path.of(file_path.getParent().toString(),
                                physicalLocation.getJSONObject("artifactLocation").getString("uri"));
                JSONObject region = physicalLocation.getJSONObject("region");
                Vulnerable vul = new Vulnerable(ruleid,
                                                uri.toString(),
                                                region.getInt("startLine"),
                                                region.getInt("endLine"),
                                                region.getInt("startColumn"),
                                                region.getInt("endColumn"));
                sv.put(ruleid, vul);
            }
        };
        parser.forEach(consumer);
        System.out.println("End Reading Json File: " + file_path);
        return sv;
    }

    public static SnykVulnerable getSnykVulnerable(String file_path){
        return getSnykVulnerable(Path.of(file_path));
    }

}