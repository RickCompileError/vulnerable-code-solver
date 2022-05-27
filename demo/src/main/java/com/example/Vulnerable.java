package com.example;

public class Vulnerable {

    private final String type;
    private int start_line;
    private int end_line;
    private int start_column;
    private int end_column;

    public Vulnerable(String t){
        this.type = t;
    }

    public Vulnerable(String type, int sl, int el){
        this(type);
        start_line = sl;
        end_line = el;
    }

    public Vulnerable(String type, int sl, int el, int sc, int ec){
        this(type, sl, el);
        start_column = sc;
        end_column = ec;
    }

    public String getType(){
        return type;
    }

    public int getStartLine(){
        return start_line;
    }

    public int getEndLine(){
        return end_line;
    }

    public int getStartColumn(){
        return start_column;
    }

    public int getEndColumn(){
        return end_column;
    }

    public String toString(){
        return "Type: " + getType() + ", Start Line: " + getStartLine()
            + ", End Line: " + getEndLine() + ", Start Column: " + getStartColumn()
            + ", End Column: " + getEndColumn();
    }
}