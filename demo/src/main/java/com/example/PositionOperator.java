package com.example;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

public class PositionOperator {

    /* negative: a first, 0: equals, positive: b first */
    public static int compare(Range a, Range b){
        int a_line = a.begin.line;
        int b_line = b.begin.line;
        return a_line-b_line;
    }

}