package com.ntcu.app;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class CodeGenerator {

    public static void main(String[] args) {
        CodeGenerator cg = new CodeGenerator();
        try{
            FileOperator.createFixDir();
            List<Path> paths = null;
            if (args.length > 0) paths = FileOperator.getJSON(args);
            else paths = FileOperator.getJSON();
            for (Path path: paths) cg.process(path);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public CodeGenerator(){
        setParser();
    }

    public void process(Path path) throws Exception{
        SnykVulnerable sv = JsonReader.getSnykVulnerable(path);

        setParser();

        for (Vulnerable v: sv.get("java/Sqli/test")){
            // FileOperator.removeMatchFile(v.getFilePath());
            System.out.println("Start solving\n\t"+v);
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(v.getFilePath()));
            LexicalPreservingPrinter.setup(cu);

            Solver solver = new SQLInjectionSolver(v, cu);
            solver.findVulnerableNode();
            solver.solve();

            FileOperator.save(v.getFilePath(), LexicalPreservingPrinter.print(cu));
            System.out.println("Done");
        }
    }

    public void setParser() {
        StaticJavaParser.getConfiguration().setAttributeComments(true);
        JavaSymbolSolver jss = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver()));
        StaticJavaParser.getConfiguration().setSymbolResolver(jss);
    }

}