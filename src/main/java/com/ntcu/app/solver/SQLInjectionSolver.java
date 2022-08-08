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
import com.github.javaparser.ast.expr.NameExpr;
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
        // Printer.printNode(occur_node, "\tOccur Vulnerable Code");
        MethodCallExpr occur_method_call = occur_node.findAncestor(MethodCallExpr.class).get();
        Expression statement_expression = occur_method_call.getScope().get();
        // FIXME : argument may not only one
        Expression sql_expression = occur_method_call.getArguments().get(0);
        String statement_type = getVariableDeclarator(statement_expression).getTypeAsString();
        if (statement_type.equals("Statement"))
            solveStatement(occur_method_call, statement_expression, sql_expression);
        else if (statement_type.equals("Connection") || statement_type.equals("CallableStatement"))
            solvePrepareStatement(occur_method_call, sql_expression);
    }

    private void solveStatement(MethodCallExpr occur_method_call, Expression statement, Expression sql){
        if (!(sql instanceof BinaryExpr)) setSqlBeforeStatement(statement, sql);
        modifySql(statement, sql);
        sql = occur_method_call.getArguments().get(0);
        setStatementToPreparedStatement(statement, sql.toString());
        occur_method_call.remove(sql);
    }

    private void solvePrepareStatement(MethodCallExpr occur_method_call, Expression sql){
        AssignExpr occur_assign = occur_method_call.findAncestor(AssignExpr.class).get();
        BinaryExpr be = null;
        if (sql instanceof BinaryExpr) be = sql.toBinaryExpr().get();
        else{
            VariableDeclarator node = getVariableDeclarator(sql);
            be = (isInitializerExist(node)
                    ?node.getInitializer().get()
                    :getAssignExpr(sql, sql.toString()).getValue())
                    .toBinaryExpr().get();
        }
        // retrieve BinaryExpr content
        NodeList<Expression> nl = new NodeList<>();
        List<BinaryExpr.Operator> ls = new ArrayList<>();
        BinaryExprReader.read(be, nl, ls);
        addRequestExpr(occur_assign.getTarget().toString(), occur_method_call, nl, ls);
        modifySqlRequest(be, nl, ls);
    }

    private void setSqlBeforeStatement(Expression statement, Expression sql){
        ExpressionStmt statement_vd = getVariableDeclarator(statement).findAncestor(ExpressionStmt.class).get();
        ExpressionStmt sql_vd = getVariableDeclarator(sql).findAncestor(ExpressionStmt.class).get();
        if (statement_vd.getRange().get().isAfter(sql_vd.getRange().get().begin)) return;
        Comment comment = null;
        if (sql_vd.getComment().isPresent()){
            comment = sql_vd.getComment().get();
            sql_vd.setComment(null);
        }
        Node node = occur_node;
        while (node.findAncestor(BlockStmt.class).isPresent()){
            node = node.findAncestor(BlockStmt.class).get();
            ((BlockStmt)node).remove(sql_vd);
            if (node.containsWithinRange(statement_vd)) break;
        }
        ((BlockStmt)node).getStatements().addBefore(sql_vd, statement_vd);
        sql_vd.setComment(comment);
    }

    private void modifySql(Expression statement, Expression sql){
        // determine the sql type
        BinaryExpr be = null;
        if (sql instanceof BinaryExpr) be = sql.toBinaryExpr().get();
        else{
            VariableDeclarator node = getVariableDeclarator(sql);
            be = (isInitializerExist(node)
                    ?node.getInitializer().get()
                    :getAssignExpr(sql, sql.toString()).getValue())
                    .toBinaryExpr().get();
        }
        // retrieve BinaryExpr content
        NodeList<Expression> nl = new NodeList<>();
        List<BinaryExpr.Operator> ls = new ArrayList<>();
        BinaryExprReader.read(be, nl, ls);
        // get the Node at which statement was initialized
        VariableDeclarator vd = getVariableDeclarator(statement);
        MethodCallExpr mce = (MethodCallExpr)(isInitializerExist(vd)
                            ?vd.getInitializer().get()
                            :getAssignExpr(statement, statement.toString()).getValue());
        // Modify
        addRequestExpr(vd.getNameAsString(), mce, nl, ls);
        modifySqlRequest(be, nl, ls);
    }
    
    // add request expression
    private void addRequestExpr(String statement_name, MethodCallExpr mce, NodeList<Expression> nl, List<BinaryExpr.Operator> ls){
        // get the NodeList from finding BlockStmt and the statement of the occur_node
        NodeList<Statement> nl_s = mce.findAncestor(BlockStmt.class).get().getStatements();
        Statement s = mce.findAncestor(Statement.class).get();
        // the statement will add before occur_node statement and the adding order in NodeList nl is from left to right 
        int count_expression = 1;
        NodeList<ExpressionStmt> nl_es = new NodeList();
        // i is index iterator and j is responsible for record the first non-StringLiteralExpr
        int i = 0, j = 0;
        while (i<nl.size()){
            while (i<nl.size() && nl.get(i) instanceof StringLiteralExpr) i++;
            j = i;
            while (i<nl.size() && !(nl.get(i) instanceof StringLiteralExpr)) i++;
            if (j>=nl.size()) break;

            Expression exp = null;
            if (i-j>=2){
                // n1 op1 n2, op1 index is same as n1 index
                BinaryExpr new_be = new BinaryExpr(nl.get(j++),nl.get(j++),ls.get(j-2));
                while (j<i) new_be = new BinaryExpr(new_be, nl.get(j++), ls.get(j-2));
                exp = new_be;
            }
            else exp = nl.get(j++);
            nl_es.add(
                new ExpressionStmt(
                    new MethodCallExpr(
                        // occur_node.findAncestor(MethodCallExpr.class).get().getScope().get(),
                        new NameExpr(statement_name),
                        "setString",
                        new NodeList<Expression>(new IntegerLiteralExpr(String.valueOf(count_expression++)),exp)
                    )
                )
            );
        }
        nl_s.addAll(nl_s.indexOf(s)+1, nl_es);
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
        else if (sql instanceof MethodCallExpr)
            ((MethodCallExpr)sql).setArgument(0, new StringLiteralExpr(newString));
    }

    private void setStatementToPreparedStatement(Expression statement, String sql){
        VariableDeclarator vd = getVariableDeclarator(statement);
        vd.setType("PreparedStatement");
        MethodCallExpr mce = (MethodCallExpr)(isInitializerExist(vd)
                                            ?vd.getInitializer().get()
                                            :getAssignExpr(statement, statement.toString()).getValue());
        mce.setName("prepareStatement");
        mce.addArgument(sql);
    }



}