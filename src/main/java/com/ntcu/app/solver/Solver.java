package com.ntcu.app.solver;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.ntcu.app.vuln.Vulnerable;

public abstract class Solver{

    protected CompilationUnit cu = null;
    protected Vulnerable vulnerable = null;
    protected Node occur_node = null;

    public Solver(){}

    public Solver(Vulnerable vulnerable, CompilationUnit cu){
        this.cu = cu;
        this.vulnerable = vulnerable;
        LexicalPreservingPrinter.setup(cu);
        findVulnerableNode();
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

    /*
     * This function is used to check if VariableDeclarator has initialize Expression or not
     * (Note: NullLiteralExpr isn't a initialize Expression)
     */
    public static boolean isInitializerExist(Node node){
        return (node instanceof VariableDeclarator) &&
                ((VariableDeclarator)node).getInitializer().isPresent() &&
                !((VariableDeclarator)node).getInitializer().get().isNullLiteralExpr();
    }

    // Can only accept that the instance of the Node is NameExpr 
    public static AssignExpr getAssignExpr(Node node, String keyword){
        if (!(node instanceof NameExpr)) return null;
        AssignExpr ae = null;
        while (true){
            // FIXME : the order problem, AssignExpr appear after main node seem to be legal if in ForExpr
            for (AssignExpr i: node.findAll(AssignExpr.class)){
                if (i.getTarget().toString().equals(keyword)) ae = i;
            }
            if (ae!=null || !node.getParentNode().isPresent()) break;
            node = node.getParentNode().get();
        }
        return ae;
    }

    public String getResult(){
        return LexicalPreservingPrinter.print(cu); 
    }

    public abstract void findVulnerableNode();
    public abstract void solve();
}