package com.ntcu.app;

import com.github.javaparser.ast.Node;

public class Printer{

    public static <T extends Node> void printNode(T node, String name){
        if (name!=null) System.out.print(name + " --- ");
        System.out.println(node.getMetaModel() + " --- " + node.toString());
    }

    public static <T extends Node> void printNode(T node){
        printNode(node, null);
    }

    public static void printError(String msg){
        System.out.println(msg);
    }
}