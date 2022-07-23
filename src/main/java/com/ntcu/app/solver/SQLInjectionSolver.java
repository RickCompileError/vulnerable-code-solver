package com.ntcu.app.solver;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.ntcu.app.util.Printer;
import com.ntcu.app.vuln.Vulnerable;

public class SQLInjectionSolver extends Solver{

    public SQLInjectionSolver(){
        super();
    }

    public SQLInjectionSolver(Vulnerable vulnerable, CompilationUnit cu){
        super(vulnerable, cu);
    }

    public boolean findVulnerableNode(){
        Range target_range = 
            Range.range(vulnerable.getStartLine(), vulnerable.getStartColumn(), vulnerable.getEndLine(), vulnerable.getEndColumn());
        
        // find "executeQuery"
        for (SimpleName i: cu.findAll(SimpleName.class)){
            if (i.getRange().isPresent() && i.getRange().get().overlapsWith(target_range)){
                occur_node = i;
                return true;
            }
        }
        return false;
    }

    /*
     * SQL Injection code is look like:
     * Statement st = conn.createStatement();
     * String sql = "select * from chi.emp where ename = '" + request.getParameter("nametextbox2").toUpperCase() + "'";
     * ResultSet result = st.executeQuery(sql);
     */
    public void solve(){
        if (!findVulnerableNode()){
            Printer.printError("Can't find Vulnerable Node");
            return;
        }
        Printer.printNode(occur_node, "\tOccur Vulnerable Code");
        // TODO : add the solution of the preparestatement type sql injection
        MethodCallExpr occur_method_call = occur_node.findAncestor(MethodCallExpr.class).get();

        Expression method_scope = occur_method_call.getScope().get();
        // FIXME : argument may not only one
        Expression method_args = occur_method_call.getArguments().get(0);
        setStringBeforeStatement(method_scope, method_args);
        modifyScopeContent(method_scope, method_args.toString());
        modifyArgsContent(method_args);
        occur_method_call.remove(method_args);
    }

    private void modifyScopeContent(Expression e, String args){
        VariableDeclarator vd = getVariableDeclarator(e);
        vd.setType("PreparedStatement");
        MethodCallExpr mce = (MethodCallExpr)(isInitializerExist(vd)
                                            ?vd.getInitializer().get()
                                            :getAssignExpr(e, e.toString()).getValue());
        mce.setName("prepareStatement");
        mce.addArgument(args);
    }

    private void modifyArgsContent(Expression e){
        BinaryExpr be = null;
        if (e instanceof BinaryExpr) be = e.toBinaryExpr().get();
        else{
            VariableDeclarator node = getVariableDeclarator(e);
            be = (isInitializerExist(node)
                    ?node.getInitializer().get()
                    :getAssignExpr(e, e.toString()).getValue())
                    .toBinaryExpr().get();
        }

        NodeList<Expression> nl = new NodeList<>();
        List<BinaryExpr.Operator> ls = new ArrayList<>();
        BinaryExprReader.read(be, nl, ls);
        addRequestExpr(nl, ls);
        modifySqlRequest(be, nl, ls);
    }
    
    // add request expression
    private void addRequestExpr(NodeList<Expression> nl, List<BinaryExpr.Operator> ls){
        // get the NodeList from finding BlockStmt and the statement of the occur_node
        NodeList<Statement> nl_s = occur_node.findAncestor(BlockStmt.class).get().getStatements();
        Statement s = occur_node.findAncestor(Statement.class).get();
        // the statement will add before occur_node statement and the adding order in NodeList nl is from left to right 
        int count_expression = 1;
        // i is index iterator and j is responsible for record the first non-StringLiteralExpr
        for (int i=0, j=0, sz=nl.size();i<sz;i++){
            if (nl.get(i) instanceof StringLiteralExpr){
                if (i==j) continue;
                Expression exp = null;
                if (i-j>1){
                    BinaryExpr be = new BinaryExpr(nl.get(j++),nl.get(j++),ls.get(j-2)); // n1 op1 n2, op1 index is same as n1 index
                    while (j<i) be = new BinaryExpr(be, nl.get(j), ls.get((j++)-1));
                    exp = be;
                }
                else exp = nl.get(j++);
                nl_s.addBefore(
                    new ExpressionStmt(
                        new MethodCallExpr(
                            occur_node.findAncestor(MethodCallExpr.class).get().getScope().get(),
                            "setString",
                            new NodeList<Expression>(new IntegerLiteralExpr(String.valueOf(count_expression)),exp)
                        )
                    ), s
                );
            }
            else if (nl.get(j) instanceof StringLiteralExpr) j = i;
        }
    }

    private void modifySqlRequest(BinaryExpr be, NodeList<Expression> nl, List<BinaryExpr.Operator> ls){
        // replace sql reqeust to '?'
        // the processing have a precondition that
        // sql instruction is made up of ['String'+'Node'(1:)](1:)+'String'
        String newString = "";
        for (int i=0, sz=nl.size();i<sz;i++){
            Expression exp = nl.get(i);
            if (exp instanceof StringLiteralExpr) newString += exp.asStringLiteralExpr().asString();
            else if (i-1>=0 && nl.get(i-1) instanceof StringLiteralExpr) newString += "?";
        }
        // remove '' "". In Java, \d \s ... must be written as \\d \\s ...
        newString = newString.replaceAll("[\'\"]\\s*\\?\\s*[\'\"]","?");
        // setting new string 
        Node sql = be.getParentNode().get();
        if (sql instanceof VariableDeclarator)
            ((VariableDeclarator)sql).setInitializer(new StringLiteralExpr(newString)); 
        else if(sql instanceof AssignExpr)
            ((AssignExpr)sql).setValue(new StringLiteralExpr(newString));
    }

    private void setStringBeforeStatement(Expression scope, Expression args){
        ExpressionStmt scope_vd = getVariableDeclarator(scope).findAncestor(ExpressionStmt.class).get();
        ExpressionStmt args_vd = getVariableDeclarator(args).findAncestor(ExpressionStmt.class).get();
        if (scope_vd.getRange().get().isAfter(args_vd.getRange().get().begin)) return;
        Comment comment = null;
        if (args_vd.getComment().isPresent()){
            comment = args_vd.getComment().get();
            args_vd.setComment(null);
        }
        Node node = occur_node;
        while (node.findAncestor(BlockStmt.class).isPresent()){
            node = node.findAncestor(BlockStmt.class).get();
            ((BlockStmt)node).remove(args_vd);
            if (node.containsWithinRange(scope_vd)) break;
        }
        ((BlockStmt)node).getStatements().addBefore(args_vd, scope_vd);
        args_vd.setComment(comment);
    }

}