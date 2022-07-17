package com.ntcu.app;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.declarations.AssociableToAST;

public abstract class Solver{

    protected CompilationUnit cu;
    protected Vulnerable vulnerable;
    protected Node occur_node;

    public Solver(){

    }

    public Solver(Vulnerable vulnerable, CompilationUnit cu){
        this.cu = cu;
        this.vulnerable = vulnerable;
        LexicalPreservingPrinter.setup(cu);
    }

    public void setUp(Vulnerable vulnerable, CompilationUnit cu){
        this.vulnerable = vulnerable;
        this.cu = cu;
        LexicalPreservingPrinter.setup(cu);
    }

    public static VariableDeclarator getVariableDeclarator(Node node){
        Optional<VariableDeclarationExpr> opt = ((AssociableToAST)(((NameExpr)node).resolve())).toAst();
        if (!opt.isPresent()) return null;
        for (VariableDeclarator i: opt.get().getVariables()){
            if (i.getName().toString().equals(node.toString())) return i;
        }
        return null;
    }

    public static AssignExpr getAssignExpr(Node node, String keyword){
        Printer.printNode(node);
        if (!(node instanceof NameExpr)) return null;
        AssignExpr ae = null;
        while (true){
            // FIXME : the order problem, AssignExpr appear after main node seem to be legal if in ForExpr
            for (AssignExpr i: node.findAll(AssignExpr.class)){
                if (i.getTarget().toString().contains(keyword)) ae = i;
            }
            if (ae!=null || !node.getParentNode().isPresent()) break;
            node = node.getParentNode().get();
        }
        return ae;
    }

    public String getResult(){
        return LexicalPreservingPrinter.print(cu); 
    }

    public abstract boolean findVulnerableNode();
    public abstract void solve();
}