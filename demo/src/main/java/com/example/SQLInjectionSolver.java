package com.example;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
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

public class SQLInjectionSolver extends Solver{

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
        MethodCallExpr occur_method_call = occur_node.findAncestor(MethodCallExpr.class).get();

        Expression method_scope = occur_method_call.getScope().get();
        /* Issue: is argument only one? */
        Expression method_args = occur_method_call.getArguments().get(0);
        modifyScopeContent(method_scope);
        modifyArgsContent(method_args);
    }

    private void modifyScopeContent(Expression e){
        VariableDeclarator vd = getVariableDeclarator(e);
        vd.setType("PreparedStatement");
        MethodCallExpr mce = (MethodCallExpr)(vd.getInitializer().isPresent()
                                            ?vd.getInitializer().get()
                                            :getAssignExpr(e, "createStatement").getValue());
        mce.setName("prepareStatement");
        mce.addArgument("sql");
    }

    private void modifyArgsContent(Expression e){
        BinaryExpr be = (getVariableDeclarator(e).getInitializer().isPresent()
                        ?getVariableDeclarator(e).getInitializer().get()
                        :getAssignExpr(e, e.toString()).getValue())
                        .toBinaryExpr().get();
        // add request expression
        NodeList<MethodCallExpr> nl_mce = getExpressionsInBinaryExpr(be, MethodCallExpr.class); // store all method call of a expression
        BlockStmt bs = occur_node.findAncestor(BlockStmt.class).get();
        NodeList<Statement> nl_s = bs.getStatements();
        Statement s = occur_node.findAncestor(Statement.class).get();
        for (int i=1;i<=nl_mce.size();i++){
            MethodCallExpr new_mce = new MethodCallExpr();
            NodeList<Expression> new_nle = new NodeList<>();
            new_mce.setName("setString")
                    .setScope(occur_node.findAncestor(MethodCallExpr.class).get().getScope().get())
                    .setArguments(new NodeList<Expression>(new IntegerLiteralExpr(String.valueOf(i)),
                                                            nl_mce.get(i-1)));
            nl_s.addBefore(new ExpressionStmt(new_mce), s);
        }
        // replace sql reqeust to '?'
        NodeList<StringLiteralExpr> nl_sle = getExpressionsInBinaryExpr(be, StringLiteralExpr.class); // store all string literal of a expression
        String newString = "";
        for (StringLiteralExpr sle : nl_sle) newString += sle.asString();
        newString = newString.replaceAll("\' *\'","?");
        Node be_e = be.getParentNode().get();
        // setting new string 
        if (be_e instanceof VariableDeclarator)
            ((VariableDeclarator)be_e).setInitializer(new StringLiteralExpr(newString)); 
        else if(be_e instanceof AssignExpr)
            ((AssignExpr)be_e).setValue(new StringLiteralExpr(newString));
    }

    private <T extends Node> NodeList<T> getExpressionsInBinaryExpr(BinaryExpr be, Class<T> clazz){
        NodeList<T> nl = new NodeList<>();
        while (true){
            if (clazz.isInstance(be.getRight())) nl.addFirst((T)be.getRight()); 
            if (!(be.getLeft() instanceof BinaryExpr)) break;
            be = (BinaryExpr)be.getLeft();
        }
        if (clazz.isInstance(be.getLeft())) nl.addFirst((T)be.getLeft()); 
        return nl;
    }

}