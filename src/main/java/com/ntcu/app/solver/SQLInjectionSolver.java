package com.ntcu.app.solver;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.ntcu.app.util.Printer;
import com.ntcu.app.vuln.Vulnerable;

/*
 * There are two situations may cause sql injection
 * 1. Result rs = st.execute(sql);
 * 2. PrepareStetement pstmt = conn.prepareStatement(sql);
 */
public class SQLInjectionSolver extends Solver{

    private BlockStmt body = null;

    public SQLInjectionSolver(){
        super();
    }

    public SQLInjectionSolver(Vulnerable vulnerable, CompilationUnit cu){
        super(vulnerable, cu);
    }

    public void findVulnerableNode(){
        Range target_range = Range.range(vulnerable.getStartLine(),
                                        vulnerable.getStartColumn(),
                                        vulnerable.getEndLine(),
                                        vulnerable.getEndColumn());
        // find "executeQuery"
        for (SimpleName i: cu.findAll(SimpleName.class)){
            if (i.getRange().isPresent() && i.getRange().get().overlapsWith(target_range)) occur_node = i;
        }
    }

    public void solve(){
        if (occur_node==null){
            Printer.printError("Can't find Vulnerable Node");
            return;
        }

        body = occur_node.findAncestor(MethodDeclaration.class).get().getBody().get();
        MethodCallExpr occur_method_call = occur_node.findAncestor(MethodCallExpr.class).get();
        Expression scope_expression = occur_method_call.getScope().get();
        String scope_type = getVariableDeclarator(scope_expression).getTypeAsString();
        Expression sql_expression = occur_method_call.getArguments().get(0);

        // handle the situation that sql not declare
        if (sql_expression instanceof BinaryExpr){
            BlockStmt tmp = occur_method_call.findAncestor(BlockStmt.class).get();
            tmp.addStatement(tmp.getStatements().indexOf(occur_method_call.findAncestor(ExpressionStmt.class).get()),getSql((BinaryExpr)sql_expression));
            occur_method_call.remove(sql_expression);
            occur_method_call.addArgument("psql");
            sql_expression = occur_method_call.getArguments().get(0);
        }

        if (scope_type.equals("Statement")) handleStatement(occur_method_call, scope_expression, sql_expression); // st.executeQuery(sql)
        if (scope_type.equals("Connection")) handlePrepareStatement(occur_method_call, scope_expression); // conn.prepareStatement(sql)
    }

    private void handlePrepareStatement(MethodCallExpr occur_method_call, Expression sql_expression){
        Expression occur_expression = null;
        VariableDeclarator sql_declare = null;
        AssignExpr sql_assign = null;
        BinaryExpr sql = null;
        NodeList<Expression> binary_expression = new NodeList<>();
        List<BinaryExpr.Operator> binary_operator = new ArrayList<>();
        
        sql_declare = getVariableDeclarator(sql_expression); // String sql ...;
        if (!isInitializerExist(sql_declare) || sql_declare.getInitializer().get().toString().equals("")) // String sql = null; || String sql = "";
            sql_assign = getAssignExpr(sql_expression, sql_expression.toString()); // sql = ...;
        if (sql_assign!=null)
            sql = sql_assign.getValue().toBinaryExpr().get();
        else
            sql = sql_declare.getInitializer().get().toBinaryExpr().get();
        BinaryExprReader.read(sql, binary_expression, binary_operator);

        adjustSql(sql, binary_expression, binary_operator, sql_expression, );
    }
    private void handleStatement(MethodCallExpr occur_method_call, Expression statement_expression, Expression sql_expression){
        // Declare necessary Node
        Expression connection_expression = null;
        VariableDeclarator connection_declare = null;
        AssignExpr connection_assign = null;
        VariableDeclarator statement_declare = null;
        AssignExpr statement_assign = null;
        VariableDeclarator sql_declare = null;
        AssignExpr sql_assign = null;
        BinaryExpr sql = null;
        NodeList<Expression> binary_expression = new NodeList<>();
        List<BinaryExpr.Operator> binary_operator = new ArrayList<>();

        // Get the value of the Nodes
        statement_declare = getVariableDeclarator(statement_expression); // Statement st ...;
        if (isInitializerExist(statement_declare))
            connection_expression = statement_declare.getInitializer().get().toMethodCallExpr().get().getScope().get(); // Statement st = connection ...;
        else
            statement_assign = getAssignExpr(statement_expression, statement_expression.toString()); // st = ...;
        if (statement_assign!=null && statement_assign.getValue() instanceof MethodCallExpr)
            connection_expression = statement_assign.getValue().toMethodCallExpr().get().getScope().get(); // st = connection...;
        Printer.printNode(connection_expression, "Fail to find");
        connection_declare = getVariableDeclarator(connection_expression); // Connection connection ...;
        if (!isInitializerExist(connection_declare))
            connection_assign = getAssignExpr(connection_expression, connection_expression.toString()); // connection = ...;
        sql_declare = getVariableDeclarator(sql_expression); // String sql ...;
        if (!isInitializerExist(sql_declare) || sql_declare.getInitializer().get().toString().equals("")) // String sql = null; || String sql = "";
            sql_assign = getAssignExpr(sql_expression, sql_expression.toString()); // sql = ...;
        if (sql_assign!=null)
            sql = sql_assign.getValue().toBinaryExpr().get();
        else
            sql = sql_declare.getInitializer().get().toBinaryExpr().get();
        BinaryExprReader.read(sql, binary_expression, binary_operator);

        // create PrepareStatement and move Connection before the sql
        connection_declare.findAncestor(ExpressionStmt.class).get().remove();
        if (connection_assign!=null) connection_assign.findAncestor(ExpressionStmt.class).get().remove();
        NodeList<Statement> sql_belong_block = sql_declare.findAncestor(BlockStmt.class).get().getStatements();
        NodeList<Statement> add_list = new NodeList<>();
        add_list.add(connection_declare.findAncestor(ExpressionStmt.class).get());
        if (connection_assign!=null) add_list.add(connection_assign.findAncestor(ExpressionStmt.class).get());
        add_list.add(new ExpressionStmt(getPrepareStatement()));
        sql_belong_block.addAll(sql_belong_block.indexOf(sql_declare.findAncestor(ExpressionStmt.class).get()), add_list);

        // adjust "sql = String + Operator + String..."
        adjustSql(sql, binary_expression, binary_operator, sql_expression, connection_expression);

        // add "pstmt = conn.prepareStatement(sql)"
        sql.findAncestor(BlockStmt.class).get().getStatements().
            addAfter(new ExpressionStmt(getAssignPrepareStatement(sql_expression, connection_expression)), sql.findAncestor(Statement.class).get());

        // adjust "st.executeQuery(sql)"
        occur_method_call.setScope(new NameExpr("pstmt"));
        occur_method_call.remove(sql_expression);
    }

    private void adjustSql(BinaryExpr sql, NodeList<Expression> binary_expression, List<BinaryExpr.Operator> binary_operator, Expression sql_expression, Expression connection_expression){
        NodeList<Statement> nodelist = sql.findAncestor(BlockStmt.class).get().getStatements();
        Statement sql_statement = sql.findAncestor(Statement.class).get();
        // the statement will add before occur_node statement and the adding order in NodeList nl is from left to right 
        int count_expression = 1;
        NodeList<ExpressionStmt> nl_es = new NodeList();
        // i is index iterator and j is responsible for record the first non-StringLiteralExpr
        int i = 0, j = 0;
        while (i<binary_expression.size()){
            while (i<binary_expression.size() && binary_expression.get(i) instanceof StringLiteralExpr) i++;
            j = i;
            while (i<binary_expression.size() && !(binary_expression.get(i) instanceof StringLiteralExpr)) i++;
            if (j>=binary_expression.size()) break;

            Expression exp = null;
            if (i-j>=2){
                // n1 op1 n2, op1 index is same as n1 index
                BinaryExpr new_be = new BinaryExpr(binary_expression.get(j++),binary_expression.get(j++),binary_operator.get(j-2));
                while (j<i) new_be = new BinaryExpr(new_be, binary_expression.get(j++), binary_operator.get(j-2));
                exp = new_be;
            }
            else exp = binary_expression.get(j++);
            nl_es.add(
                new ExpressionStmt(
                    new MethodCallExpr(
                        // occur_node.findAncestor(MethodCallExpr.class).get().getScope().get(),
                        new NameExpr("pstmt"),
                        "setString",
                        new NodeList<Expression>(new IntegerLiteralExpr(String.valueOf(count_expression++)),exp)
                    )
                )
            );
        }
        nodelist.addAll(nodelist.indexOf(sql_statement)+1, nl_es);

        modifySqlRequest(sql, binary_expression, binary_operator);
    }

    private VariableDeclarationExpr getSql(BinaryExpr be){
        VariableDeclarator vd = new VariableDeclarator(new ClassOrInterfaceType(null, "String"), "psql", be);
        return new VariableDeclarationExpr(vd);
    }

    private VariableDeclarationExpr getPrepareStatement(){
        VariableDeclarator vd = new VariableDeclarator(new ClassOrInterfaceType(null, "PrepareStatement"), "pstmt", new NullLiteralExpr());
        return new VariableDeclarationExpr(vd);
    }

    private AssignExpr getAssignPrepareStatement(Expression sql_expression, Expression connection_expression){
        AssignExpr ae = new AssignExpr();
        ae.setTarget(new NameExpr("pstmt"));
        ae.setOperator(AssignExpr.Operator.ASSIGN);
        ae.setValue(new MethodCallExpr(connection_expression, "prepareStatement", new NodeList<Expression>(sql_expression)));
        return ae;
    }

    /*
     * SQL Injection code is look like:
     * Statement st = conn.createStatement();
     * String sql = "select * from chi.emp where ename = '" + request.getParameter("nametextbox2").toUpperCase() + "'";
     * ResultSet result = st.executeQuery(sql);
     */
    public void solve2(){
        if (occur_node==null){
            Printer.printError("Can't find Vulnerable Node");
            return;
        }
        // Printer.printNode(occur_node, "\tOccur Vulnerable Code");
        MethodCallExpr occur_method_call = occur_node.findAncestor(MethodCallExpr.class).get();
        Expression statement_expression = occur_method_call.getScope().get();
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
            Expression exp = null;
            if (isInitializerExist(node)) exp = node.getInitializer().get();
            if (exp instanceof StringLiteralExpr && exp.toString().equals(""))
                exp = getAssignExpr(sql, sql.toString()).getValue();
            be = exp.toBinaryExpr().get();
        }
        Printer.printNode(be);
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