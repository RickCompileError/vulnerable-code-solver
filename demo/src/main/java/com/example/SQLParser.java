package com.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class SQLParser {

    private static final String Base_Path = System.getProperty("user.dir") + "/demo/src/main/webapp/";
    
    public static void main(String[] args) throws Exception {
        Path path = getJSON();
        System.out.println("Get JSON File: " + path);
        SnykVulnerable sv = JsonReader.getSnykVulnerable(path);

        setParser();

        removePrevFile();
        for (Vulnerable v: sv.get("java/Sqli")){
            System.out.println("Start solving\n\t"+v);
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(Base_Path + v.getFilePath()));
            LexicalPreservingPrinter.setup(cu);

            Solver solver = new SQLInjectionSolver(v, cu);
            solver.findVulnerableNode();
            solver.solve();

            int ver = getVersionNumber(v.getFilePath());
            Save("n"+String.valueOf(ver)+"_"+v.getFilePath(), LexicalPreservingPrinter.print(cu));
            System.out.println("Done");
        }
    }
    
    /*
     * Get scanning json file by JFileChooser
     */
    public static Path getJSON(){
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(Base_Path));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON File", "json"));
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue==JFileChooser.APPROVE_OPTION){
            return chooser.getSelectedFile().toPath();
        }
        return null;
    }

    public static void setParser() {
        StaticJavaParser.getConfiguration().setAttributeComments(true);
        JavaSymbolSolver jss = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver()));
        StaticJavaParser.getConfiguration().setSymbolResolver(jss);
    }

    public static void removePrevFile(){
        try{
            for (Path path: Files.newDirectoryStream(Path.of(Base_Path))){
                if (Pattern.matches("n\\d+_.*", path.getFileName().toString())) Files.delete(path);
            }
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public static int getVersionNumber(String file_name){
        int v = 1;
        while (Files.exists(Path.of(Base_Path + "n" + String.valueOf(v) + "_" + file_name))) v++;
        return v;
    }

    public static void Save(String file_name, String output){
        Path path = Path.of(Base_Path+file_name);
        try{
            Files.writeString(path, output);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

}