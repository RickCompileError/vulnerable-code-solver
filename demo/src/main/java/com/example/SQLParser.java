package com.example;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
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
public class SQLParser {


    private static final String FILE_PATH = "demo/src/main/java/com/example/SQL_Injection_Servlet.java";
    
    public static void main(String[] args) throws Exception {

        ignoreComments();
        CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(FILE_PATH));

        List<ExpressionStmt> stm = cu.findAll(ExpressionStmt.class);
        Range target_range = Range.range(108,33,108,44);
        for (ExpressionStmt i: stm){
            if (i.getRange().isPresent() && i.getRange().get().overlapsWith(target_range)){
                System.out.println(i.toString());
            }
        }
    }
    
    public static void ignoreComments() {
        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setAttributeComments(false);
        StaticJavaParser.setConfiguration(parserConfiguration);
    }
}

class ConfigurationOptions {


    public void ignoreComments(boolean flag) {

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setAttributeComments(flag);

        StaticJavaParser.setConfiguration(parserConfiguration);

    }

}