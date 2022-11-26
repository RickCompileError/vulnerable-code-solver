package com.ntcu.app.cmd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;

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

    public static void createSnykSH(){
        try{
            File f = new File("snyk.sh");
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
