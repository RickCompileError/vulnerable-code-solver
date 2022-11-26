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
        try{
            File f = new File("color_diff_output.sh");
            FileWriter mywriter = new FileWriter(f);
            String cmd ="#!/bin/bash\n"+
                        "filename=\"diff.txt\"\n"+
                        "counter=0\n"+
                        "#While loop to read line by line\n"+
                        "while IFS= read -r line; do\n"+
                        "    if [[ $counter -lt 2 ]] ; then\n"+
                        "        printf \"\\e[36m$line\\e[0m\n\"\n"+
                        "        counter=$((counter+1))\n"+
                        "        continue\n"+
                        "    fi\n"+
                        "    if [[ $counter -ge 2  &&  $line == \\*\\*\\*\\** ]] ; then\n"+
                        "        printf \"\\e[30m$line\\e[0m\n\"\n"+
                        "        continue\n"+
                        "    fi\n"+
                        "    if [[ $counter -ge 2  &&  $line == \\*\\*\\*\\ * ]] ; then\n"+
                        "        printf \"\\e[35m$line\\e[0m\n\"\n"+
                        "        color=\"red\"\n"+
                        "        continue\n"+
                        "    fi\n"+
                        "    if [[ $counter -ge 2  &&  $line == ---\\ * ]] ; then\n"+
                        "        printf \"\\e[35m$line\\e[0m\n\"\n"+
                        "        color=\"green\"\n"+
                        "        continue\n"+
                        "    fi\n"+
                        "    if [[ $counter -ge 2  &&  $color == \"red\" ]] ; then\n"+
                        "        if [[ $line == !\\ * ]] ; then\n"+
                        "            printf \"\\e[106m$line\\e[0m\n\"\n"+
                        "        elif [[ $line == -\\ * ]] ; then\n"+
                        "            printf \"\\e[101m$line\\e[0m\n\"\n"+
                        "        else\n"+
                        "            printf \"\\e[91m$line\\e[0m\n\"\n"+
                        "        fi\n"+
                        "    fi\n"+
                        "    if [[ $counter -ge 2  &&  $color == \"green\" ]] ; then\n"+
                        "        if [[ $line == !\\ * ]] ; then\n"+
                        "            printf \"\\e[106m$line\\e[0m\n\"\n"+
                        "        elif [[ $line == +\\ * ]] ; then\n"+
                        "            printf \"\\e[102m$line\\e[0m\n\"\n"+
                        "        else\n"+
                        "            printf \"\\e[32m$line\\e[0m\n\"\n"+
                        "        fi\n"+
                        "    fi\n"+
                        "done < \"$filename\"\n"
            mywriter.write(cmd);
            mywriter.close();
        }
        catch(Exception e){
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
