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

public class SQLParser {

    private static final String Base_Path = System.getProperty("user.dir") + "/demo/src/main/webapp/";
    
    public static void main(String[] args) throws Exception {
        SnykVulnerable sv = JsonReader.getSnykVulnerable(Base_Path + "snyk.json");
        if (sv.containsKey("java/Sqli"))
            sv.get("java/Sqli").forEach(System.out::println);

        setParser();

        for (Vulnerable v: sv.get("java/Sqli")){
            /******* Find the Node at Vulnerable line *******/
            /******* Example: result = st.executeQuery(sql) *******/
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(Base_Path + v.getFilePath()));
            LexicalPreservingPrinter.setup(cu);

            Solver solver = new SQLInjectionSolver(v, cu);
            solver.findVulnerableNode();
            solver.solve();

            Save("new_"+v.getFilePath(), LexicalPreservingPrinter.print(cu));
        }
    }
    
    public static void setParser() {
        StaticJavaParser.getConfiguration().setAttributeComments(false);
        JavaSymbolSolver jss = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver()));
        StaticJavaParser.getConfiguration().setSymbolResolver(jss);
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