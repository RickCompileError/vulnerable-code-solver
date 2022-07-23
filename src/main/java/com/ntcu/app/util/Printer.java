package com.ntcu.app.util;

import com.github.javaparser.ast.Node;

public class Printer{

    public static <T extends Node> void printNode(T node, String msg){
        if (msg!=null) System.out.print(msg + " --- ");
        System.out.println(node.getMetaModel() + " --- " + node.toString());
    }

    public static <T extends Node> void printNode(T node){
        printNode(node, null);
    }

    public static void printError(String msg){
        System.out.println(msg);
    }
}