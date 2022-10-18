package com.ntcu.app.cmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;

public class CommandOperator{

    // For Linux 
    public static void diff(String old_dir, String new_dir){   
        String cmd = "diff -c " + old_dir + " " + new_dir;
        exec_and_writeFile(cmd);
        String sh1 = "chmod +x color_diff_output.sh";
        only_exec(sh1);
        String sh2 = "./color_diff_output.sh";
        exec(sh2);
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

    private static void exec_and_writeFile(String cmd){
        try{
            Process process = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Read command standard output
            String s;
            File f = new File("diff.txt");
            FileWriter mywriter = new FileWriter(f);
            while ((s = stdInput.readLine()) != null) {
                mywriter.write(s+"\n");
            }
            mywriter.close();
        } 
        catch (Exception e) {
            e.printStackTrace(System.err);
        }  
    }

    private static void only_exec(String cmd){
        try{
            Process process = Runtime.getRuntime().exec(cmd);
            int status = process.waitFor();
        } 
        catch (Exception e) {
            e.printStackTrace(System.err);
        }  
    }
}
