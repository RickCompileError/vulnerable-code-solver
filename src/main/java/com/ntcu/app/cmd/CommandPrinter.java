package com.ntcu.app.cmd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;

public class CommandPrinter{

    public static void print(InputStream is){
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String s;
            System.out.println("Result:\n");
            while ((s = br.readLine()) != null) System.out.println(s);
        }
        catch (Exception e){
            e.printStackTrace(System.err);
        }
    }

    public static void save(InputStream is, String file_name){
        try{
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(is));

            // Read command standard output
            String s;
            File f = new File(file_name);
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

    public static void colorPrint(){
        String file_name = "diff.txt";
        File f = new File(file_name);
        try{
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            String color = null;
            System.out.println("\\e[36m"+reader.readLine()+"\\e[0m");
            System.out.println("\\e[36m"+reader.readLine()+"\\e[0m");
            while (true){
                line = reader.readLine();
                if (line==null) break;
                if (line.matches("\\*\\*\\*\\*.*")){
                    System.out.println("\\e[30m"+line+"\\e[0m");
                    continue;
                }
                if (line.matches("\\*\\*\\* .*")){
                    System.out.println("\\e[35m"+line+"\\e[0m");
                    color = "red";
                    continue;
                }
                if (line.matches("--- .*")){
                    System.out.println("\\e[35m"+line+"\\e[0m");
                    color = "green";
                    continue;
                }
                if (color=="red"){
                    if (line.matches("! .*")){
                        System.out.println("\\e[106m"+line+"\\e[0m");
                    }
                    else if (line.matches("- .*")){
                        System.out.println("\\e[101m"+line+"\\e[0m");
                    }
                    else{
                        System.out.println("\\e[91m"+line+"\\e[0m");
                    }
                }
                if (color=="green"){
                    if (line.matches("! .*")){
                        System.out.println("\\e[106m"+line+"\\e[0m");
                    }
                    else if (line.matches("- .*")){
                        System.out.println("\\e[102m"+line+"\\e[0m");
                    }
                    else{
                        System.out.println("\\e[32m"+line+"\\e[0m");
                    }
                }
            }
            reader.close();
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public static void createSnykSH(){
        try{
            File f = new File("snyk.sh");
            System.out.println(f.getAbsolutePath());
            FileWriter mywriter = new FileWriter(f);
            String cmd = "#!/bin/bash\n\nsnyk code test";
            mywriter.write(cmd);
            mywriter.close();
        }
        catch(Exception e){
            e.printStackTrace(System.err);
        }
    }

    public static void createSnykCodeTestSH(){
        try{
            File f = new File("snyk_code_test.sh");
            FileWriter mywriter = new FileWriter(f);
            String cmd = "#!/bin/bash\n\nsnyk code test --json > \"vuln.json\"";
            mywriter.write(cmd);
            mywriter.close();
        }
        catch(Exception e){
            e.printStackTrace(System.err);
        }
    }
}
