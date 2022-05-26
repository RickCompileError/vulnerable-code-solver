package com.example;

import java.util.List;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.*;

public class JsonReader {
    public static List<Integer> getSnykErrorLine(String file_path){
        try {
            // System.out.println("Path : " + Path.of(file_path));
            String content = Files.readString(Path.of(file_path), StandardCharsets.UTF_16);
            // System.out.println("Content : \n" + content);
            JSONObject parser = new JSONObject(content);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return new ArrayList<Integer>();
    }
}