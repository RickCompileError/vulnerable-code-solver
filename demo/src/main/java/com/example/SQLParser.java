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
            VariableDeclarator vd = findVulnerableNode(v, cu);
            printInfo(vd, "vd");
            if (vd==null){
                printError("Can't find VariableDeclarator!");
                System.exit(0);
            }

            /******* Get MethodCallExpr from VariableDeclarator *******/
            /******* Example: st.executeQuery(sql) *******/
            MethodCallExpr mce = findMethodCallExpr(vd);
            printInfo(mce, "Vulnerable's MethodCallExpr");

            /******* Modify MethodCallExpr's Scope part *******/
            /******* Example: st *******/
            Expression scope = mce.getScope().get();
            printInfo(scope, "scope");
            System.out.println(cu.findAll(AssignExpr.class));
            VariableDeclarationExpr scope_declare = getResolvedVariableDeclarationExpr(scope);
            modifyScopeDeclaration(scope_declare);
            printInfo(scope_declare, "After the scope's VariableDeclarationExpr were modified");

            /******* Modify MethodCallExpr's arguments part *******/
            /******* Example: sql *******/
            Expression arg = mce.getArguments().get(0);
            printInfo(arg, "arg");
            VariableDeclarationExpr args_declare = getResolvedVariableDeclarationExpr(arg);
            NodeList<MethodCallExpr> need_extend_expressions = modifyArgsDeclaration(args_declare);
            printInfo(args_declare, "After the args' VariableDeclarationExpr were modified");

            /******* Add the relative Expression of MethodCallExpr's Scope *******/
            /******* Example: st.setString(1, request.getParameter("nametextbox1").toUpperCase()); */
            addRelativeExpression(vd, need_extend_expressions, scope);

            System.out.println(LexicalPreservingPrinter.print(cu));
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

    public static <T extends Node> NodeList<T> getExpressionsInBinaryExpr(BinaryExpr be, Class<T> clazz){
        NodeList<T> nl = new NodeList<>();
        while (true){
            if (clazz.isInstance(be.getRight())) nl.addFirst((T)be.getRight()); 
            if (!(be.getLeft() instanceof BinaryExpr)) break;
            be = (BinaryExpr)be.getLeft();
        }
        if (clazz.isInstance(be.getLeft())) nl.addFirst((T)be.getLeft()); 
        return nl;
    }

    public static NodeList<MethodCallExpr> modifyArgsDeclaration(VariableDeclarationExpr vde){
        VariableDeclarator vde_vd = vde.getVariable(0); // get first variable declaration
        BinaryExpr exp = vde_vd.getInitializer().get().toBinaryExpr().get(); // expression like a+b
        NodeList<MethodCallExpr> nl_mce = getExpressionsInBinaryExpr(exp, MethodCallExpr.class); // store all method call of a expression
        NodeList<StringLiteralExpr> nl_sle = getExpressionsInBinaryExpr(exp, StringLiteralExpr.class); // store all string literal of a expression
        /* build new string of variable declaration */
        String newString = "";
        for (StringLiteralExpr sle : nl_sle) newString += sle.asString();
        newString = newString.replaceAll("\' *\'","?");
        vde_vd.setInitializer(new StringLiteralExpr(newString)); // setting new string 
        return nl_mce;
    }

    public static void addRelativeExpression(VariableDeclarator vd, NodeList<MethodCallExpr> nlmce, Expression scope) {
        BlockStmt bs = vd.findAncestor(BlockStmt.class).get();
        while (!bs.containsWithinRange(getResolvedVariableDeclarationExpr(scope))) bs = bs.findAncestor(BlockStmt.class).get();
        NodeList<Statement> nls = bs.getStatements();
        Statement vd_ancestor = vd.findAncestor(Statement.class).get();
        for (int i=1;i<=nlmce.size();i++){
            MethodCallExpr new_mce = new MethodCallExpr();
            NodeList<Expression> new_nle = new NodeList<>();
            new_nle.add(new IntegerLiteralExpr(String.valueOf(i)));
            new_nle.add(nlmce.get(i-1));
            new_mce.setName("setString")
                    .setScope(scope)
                    .setArguments(new_nle);
            nls.addBefore(new ExpressionStmt(new_mce), vd_ancestor);
        }
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

/* Link
   NodeWithStatements<N extends Node>
   https://www.javadoc.io/static/com.github.javaparser/javaparser-core/3.24.2/com/github/javaparser/ast/nodeTypes/NodeWithStatements.html#addStatement(com.github.javaparser.ast.expr.Expression)
   https://github.com/javaparser/javaparser/issues/945 */
