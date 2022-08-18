package com.ntcu.app.cmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandOperator{

    // For Linux 
    public static void diff(String old_dir, String new_dir){   
        String cmd = "diff " + old_dir + " " + new_dir;
        System.out.println("old: " + old_dir + "\n" + "new: " + new_dir + "\n");
        System.out.println("Result:\n");
        exec(cmd);
    } 

    // For Windows
    public static void dir(){
        String cmd = "cmd /c dir";
        exec(cmd);
    }

    // For Windows
    public static void cd(){
        String cmd = "cmd /c cd";
        exec(cmd);
    }

    private static void exec(String cmd){
        try{
            Process process = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Read command standard output
            String s;
            System.out.println("Result:\n");
            while ((s = stdInput.readLine()) != null) System.out.println(s);
        } 
        catch (Exception e) {
            e.printStackTrace(System.err);
        }  
    }

}