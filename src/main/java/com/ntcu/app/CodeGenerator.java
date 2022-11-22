package com.ntcu.app;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.ntcu.app.cmd.CommandOperator;
import com.ntcu.app.cmd.CommandPrinter;
import com.ntcu.app.solver.SQLInjectionSolver;
import com.ntcu.app.solver.Solver;
import com.ntcu.app.util.FileOperator;
import com.ntcu.app.vuln.JsonReader;
import com.ntcu.app.vuln.SnykVulnerable;
import com.ntcu.app.vuln.Vulnerable;

import java.io.*;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class CodeGenerator {
//test = $(snyk code test --json > CodeVuln.json)
    public static void main(String[] args) {
        CodeGenerator cg = new CodeGenerator();
        CommandOperator.snykCodeTest();
        List<Path> paths = null;

        paths = FileOperator.getJSONPath(new String[]{"vuln.json"});

        if (paths!=null){
            Iterator<Path> iter = paths.iterator();
            while (iter.hasNext()){
                cg.process(iter.next());
            }
        }
    }

    public CodeGenerator(){
        setParser();
        FileOperator.createFixDir();
    }

    public void process(Path path){
        SnykVulnerable sv = JsonReader.getSnykVulnerable(path);

        setParser();

        List<Vulnerable> vul = null;
        if ((vul=sv.get("java/Sqli")) != null) processVul(vul);
        if ((vul=sv.get("java/Sqli/test")) != null) processVul(vul);
    }

    private void processVul(List<Vulnerable> vul){
        for (Vulnerable v: vul){
            System.out.println("---------------------------------------------------------------------------");
            try{
                // FileOperator.removeMatchFile(v.getFilePath());
                System.out.println("Start solving\n\n" + v + "\n");
                CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(v.getFilePath()));
                LexicalPreservingPrinter.setup(cu);

                Solver solver = new SQLInjectionSolver(v, cu);
                solver.findVulnerableNode();
                solver.solve();

                System.out.println("Start comparing\n");        
                String old_dir = v.getFilePath();
                String new_dir = FileOperator.save(v.getFilePath(), LexicalPreservingPrinter.print(cu));
                CommandPrinter.print(CommandOperator.diffColor(old_dir, new_dir));
                // CommandPrinter.print(CommandOperator.cd());
            } catch (Exception e){
                System.out.println("Cannot handle this vulnerable [" + v + "]");
            }
            System.out.println("---------------------------------------------------------------------------");
        }
    }

    public void setParser() {
        StaticJavaParser.getConfiguration().setAttributeComments(true);
        JavaSymbolSolver jss = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver()));
        StaticJavaParser.getConfiguration().setSymbolResolver(jss);
    }

}