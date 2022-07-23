package com.ntcu.app.solver;

import java.util.List;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;

public class BinaryExprReader{

    public static void read(BinaryExpr be, NodeList<Expression> nl, List<BinaryExpr.Operator> ls){
        if (be.getLeft() instanceof BinaryExpr) BinaryExprReader.read((BinaryExpr)be.getLeft(), nl, ls);
        else nl.add(be.getLeft());
        nl.add(be.getRight());
        ls.add(be.getOperator());
    }

}