package com.ntcu.app.cmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandOperator{

    public static void diff(String old_dir, String new_dir){   
        System.out.println("Start comparing\n");        
        String cmd = "diff " + old_dir + " " + new_dir;
        try {
            // Run command
            Process process = Runtime.getRuntime().exec(cmd);

            // Get input streams
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Read command standard output
            String s;
            System.out.println("old: " + old_dir + "\n" + "new: " + new_dir + "\n");
            System.out.println("Result:\n");
            while ((s = stdInput.readLine()) != null) System.out.println(s);
            System.out.println("---------------------------------------------------------------------------");
        } 
        catch (Exception e) {
            e.printStackTrace(System.err);
        }  
    } 

}