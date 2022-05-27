package com.example;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.io.*;
import java.io.FileInputStream;
import java.util.*;  
import java.util.ArrayList;
import java.util.List;
//import java.nio.file.*;
public class VoidVisitorComplete {


    private static final String FILE_PATH = "src/main/java/com/example/SQL_Injection_Servlet.java";
    
    public static void main(String[] args) throws Exception {

        //ConfigurationOptions config = new ConfigurationOptions();
        //config.ignoreComments(true);

        CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(FILE_PATH));

        VoidVisitor<Void> methodNameVisitor = new MethodNamePrinter();
        //methodNameVisitor.visit(cu, null);
        //System.out.println(cu.toString());
        
        LexicalPreservingPrinter.setup(cu);
        String code = LexicalPreservingPrinter.print(cu);  //保留原本格式
        Scanner sc = new Scanner(code);
        int counter=1;
        String target="",str="";
        while(sc.hasNextLine()){
            str = sc.nextLine();
            if(counter==108)
                target = str.trim();
            //System.out.println(counter+" :"+str);
            counter++;
        } //抓取指定行數程式碼
        
        System.out.println(target);

        //CompilationUnit cu2 = StaticJavaParser.parse("class A{ "+target+ "}");
        //methodNameVisitor.visit(cu2, null);

        //List<String> methodNames = new ArrayList<>();
        //VoidVisitor<List<String>> methodNameCollector = new CalledMethodCollector();
        //methodNameCollector.visit(cu, methodNames);
        //methodNames.forEach(n -> System.out.println("Method Name Collected: " + n));

    }

    
    private static class MethodNamePrinter extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(MethodCallExpr md, Void arg) {
            super.visit(md, arg);
            System.out.println("Method Name Printed: " + md);
        }
    }
    

    private static class CalledMethodCollector extends VoidVisitorAdapter<List<String>> {

        @Override
        public void visit(ExpressionStmt mce, List<String> collector) {
            super.visit(mce, collector);
            //collector.add(mce.asExpressionStmt());
            //collector.add(mce);
        }
    }
    
}

class ConfigurationOptions {


    public void ignoreComments(boolean flag) {

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setAttributeComments(flag);

        StaticJavaParser.setConfiguration(parserConfiguration);

    }

}