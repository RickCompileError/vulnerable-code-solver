package com.ntcu.app;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CodeGenerator {

    private static final String Base_Path = System.getProperty("user.dir") + "/test/resources";
    
    public static void main(String[] args) {
        CodeGenerator cg = new CodeGenerator();
        try{
            if (args.length>0){
                for (String i: args){
                    cg.process(Path.of(i));
                }
            }else{
                cg.process();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void process(Path path) throws Exception{
        SnykVulnerable sv = JsonReader.getSnykVulnerable(path);

        setParser();

        for (Vulnerable v: sv.get("java/Sqli")){
            removePrevFile(v.getFilePath());
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

    public void process() throws Exception{
        List<Path> path = getJSON();
        System.out.println("Get JSON File: " + path);
        for (Path i: path) process(i);
    }
    
    /*
     * Get scanning json file by JFileChooser
     */
    public List<Path> getJSON(){
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(Base_Path));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON File", "json"));
        chooser.setMultiSelectionEnabled(true);
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue==JFileChooser.APPROVE_OPTION){
            List<File> file = Arrays.asList(chooser.getSelectedFiles());
            List<Path> path = new ArrayList<>();
            for (File i: file) path.add(i.toPath());
            return path;
        }
        return null;
    }

    public void setParser() {
        StaticJavaParser.getConfiguration().setAttributeComments(true);
        JavaSymbolSolver jss = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver()));
        StaticJavaParser.getConfiguration().setSymbolResolver(jss);
    }

    public void removePrevFile(String file){
        try{
            for (Path path: Files.newDirectoryStream(Path.of(Base_Path))){
                if (Pattern.matches("n\\d+_"+file, path.getFileName().toString())) Files.delete(path);
            }
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public int getVersionNumber(String file_name){
        int v = 1;
        while (Files.exists(Path.of(Base_Path + "n" + String.valueOf(v) + "_" + file_name))) v++;
        return v;
    }

    public void Save(String file_name, String output){
        Path path = Path.of(Base_Path+file_name);
        try{
            Files.writeString(path, output);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

}