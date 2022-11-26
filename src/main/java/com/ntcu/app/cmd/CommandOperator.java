package com.ntcu.app.cmd;

import java.io.InputStream;

public class CommandOperator{

    // For Linux, compare FILES line by line
    public static InputStream diff(String old_dir, String new_dir){   
        String cmd = "diff -c " + old_dir + " " + new_dir;
        return exec(cmd);
    } 

    // For Linux, use diff and color the diff result
    public static InputStream diffColor(String old_dir, String new_dir){
        String cmd = "diff -c " + old_dir + " " + new_dir;
        CommandPrinter.save(exec(cmd), "diff.txt");
        CommandPrinter.colorPrint();
        //cmd = "chmod +x color_diff_output.sh";
        //exec(cmd);
        //cmd = "./color_diff_output.sh";
        return exec(cmd);
    }

    // For Linux, Snyk scanning vulnerability
    public static InputStream snyk(){
        CommandPrinter.createSnykSH();
        String cmd = "chmod +x snyk.sh";
        exec(cmd);
        cmd = "./snyk.sh";
        return exec(cmd);
    }

    // For Linux, Snyk scanning vulnerability and output a JSON file
    public static InputStream snykCodeTest(){
        CommandPrinter.createSnykCodeTestSH();
        String cmd = "chmod +x snyk_code_test.sh";
        exec(cmd);
        cmd = "./snyk_code_test.sh";
        return exec(cmd);
    }

    // For Windows, show current directory
    public static InputStream dir(){
        String cmd = "cmd /c dir";
        return exec(cmd);
    }

    // For Windows, show current path
    public static InputStream cd(){
        String cmd = "cmd /c cd";
        return exec(cmd);
    }


    // Run the command and Print the results
    private static InputStream exec(String cmd){
        InputStream is = null;
        try{
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
            is = process.getInputStream();
        } 
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return is;
    }
}
