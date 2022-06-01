package com.example;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparser.Navigator;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import org.apache.taglibs.standard.lang.jstl.StringLiteral;

import javassist.compiler.ast.Variable;
import javassist.expr.MethodCall;

import java.io.*;
import java.io.FileInputStream;
import java.sql.PreparedStatement;
import java.util.*;  
import java.util.ArrayList;
import java.util.List;
//import java.nio.file.*;
public class SQLParser {

    private static final String JSON_PATH = System.getProperty("user.dir") + "/demo/snyk.json";
    private static final String FILE_PATH = System.getProperty("user.dir") + "/demo/src/main/webapp/SQL_Injection_Servlet.java";
    
    public static void main(String[] args) throws Exception {
        SnykVulnerable sv = JsonReader.getSnykVulnerable(JSON_PATH);
        if (sv.containsKey("java/Sqli"))
            sv.get("java/Sqli").forEach(System.out::println);

        setParser();

        for (Vulnerable v: sv.get("java/Sqli")){
            /******* Find the Node at Vulnerable line *******/
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(FILE_PATH));
            VariableDeclarator vd = findVulnerableNode(v, cu);
            printInfo(vd, "vd");
            if (vd==null){
                printError("Can't find VariableDeclarator!");
                System.exit(0);
            }

            /******* Get MethodCallExpr from VariableDeclarator *******/
            MethodCallExpr mce = findMethodCallExpr(vd);
            printInfo(mce, "Vulnerable's MethodCallExpr");

            /******* Modify MethodCallExpr's Scope part *******/
            Expression scope = mce.getScope().get();
            printInfo(scope, "scope");
            VariableDeclarationExpr scope_declare_modify = getResolvedVariableDeclarationExpr(scope);
            scope_declare_modify = modifyScopeDeclaration(scope_declare_modify);
            printInfo(scope_declare_modify, "After the scope's VariableDeclarationExpr were modified");

            /******* Add the relative Expression of MethodCallExpr's Scope *******/

            /******* Modify MethodCallExpr's arguments part *******/
            Expression arg = mce.getArguments().get(0);
            printInfo(arg, "arg");
            VariableDeclarationExpr args_declare_modify = getResolvedVariableDeclarationExpr(arg);
            args_declare_modify = modifyArgsDeclaration(args_declare_modify);
            // VariableDeclarator arg_vde = findVariableDeclarator(arg, arg.toString()); 
            printInfo(args_declare_modify, "After the args' VariableDeclarationExpr were modified");

        }
    }
    
    public static void setParser() {
        StaticJavaParser.getConfiguration().setAttributeComments(false);
        JavaSymbolSolver jss = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver()));
        StaticJavaParser.getConfiguration().setSymbolResolver(jss);
    }

    public static VariableDeclarator findVulnerableNode(Vulnerable v, CompilationUnit cu){
        List<VariableDeclarator> stm = cu.findAll(VariableDeclarator.class);
        Range target_range = 
            Range.range(v.getStartLine(), v.getStartColumn(), v.getEndLine(), v.getEndColumn());
        for (VariableDeclarator i: stm){
            if (i.getRange().isPresent() && i.getRange().get().overlapsWith(target_range)){
                return i;
            }
        }
        return null;
    }

    public static MethodCallExpr findMethodCallExpr(VariableDeclarator vd){
        MethodCallExpr mce = null;
        try{
            mce = vd.getInitializer().get().toMethodCallExpr().get();
        } catch (NullPointerException npe){
            printError("It has a null pointer in getting MethodCallExpr process");
            System.exit(0);
        }
        return mce;
    }

    public static VariableDeclarator findVariableDeclarator(Node current_node, String target_name){
        while (!Navigator.demandVariableDeclaration(current_node, target_name).isPresent())
            current_node = current_node.getParentNode().get();
        return Navigator.demandVariableDeclaration(current_node, target_name).get();
    }

    public static VariableDeclarationExpr getResolvedVariableDeclarationExpr(Expression exp){
        ResolvedValueDeclaration rvd = ((NameExpr)exp).resolve();
        VariableDeclarationExpr vde = (VariableDeclarationExpr)((AssociableToAST)rvd).toAst().get();
        return vde;
    }

    public static VariableDeclarationExpr modifyScopeDeclaration(VariableDeclarationExpr scope_vde){
        VariableDeclarator scope_vd = scope_vde.getVariable(0);
        scope_vd.setType(PreparedStatement.class);
        MethodCallExpr scope_vd_mce = (MethodCallExpr)(scope_vd.getInitializer().get());
        scope_vd_mce.addArgument("sql");
        scope_vd_mce.setName("prepareStatement");
        return scope_vde;
    }

    /******* Under Constructing *******/
    public static VariableDeclarationExpr modifyArgsDeclaration(VariableDeclarationExpr arg_vde){
        VariableDeclarator arg_vd = arg_vde.getVariable(0);
        BinaryExpr exp = arg_vd.getInitializer().get().toBinaryExpr().get();
        NodeList<MethodCallExpr> nl_mce = new NodeList<>();
        NodeList<StringLiteralExpr> nl_sle = new NodeList<>();
        while (true) {
            if (exp.getRight() instanceof MethodCallExpr) nl_mce.addFirst((MethodCallExpr)exp.getRight());
            if (exp.getRight() instanceof StringLiteralExpr) nl_sle.addFirst((StringLiteralExpr)exp.getRight());
            if (!(exp.getLeft() instanceof BinaryExpr)) break;
            exp = (BinaryExpr)exp.getLeft();
        }
        if (exp.getLeft() instanceof MethodCallExpr) nl_mce.addFirst((MethodCallExpr)exp.getLeft());
        if (exp.getLeft() instanceof StringLiteralExpr) nl_sle.addFirst((StringLiteralExpr)exp.getLeft());
        String newString = "";
        for (StringLiteralExpr sle : nl_sle) newString += sle.asString();
        newString = newString.replaceAll("\' *\'","?");
        arg_vd.setInitializer(newString);
        System.out.println(nl_mce);
        System.out.println(nl_sle);
        return arg_vde;
    }

    public static <T extends Node> void printInfo(T node, String name){
        if (name!=null) System.out.print(name + " --- ");
        System.out.println(node.getMetaModel() + " --- " + node.toString());
    }

    public static <T extends Node> void printInfo(T node){
        printInfo(node, null);
    }

    public static void printError(String msg){
        System.out.println(msg);
    }

}
