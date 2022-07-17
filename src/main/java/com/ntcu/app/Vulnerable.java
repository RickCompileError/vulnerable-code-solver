package com.ntcu.app;

public class Vulnerable {

    private String type;
    private String file_path;
    private int start_line;
    private int end_line;
    private int start_column;
    private int end_column;

    public Vulnerable(String t, String fp){
        type = t;
        file_path = fp;
        start_line = 0;
        end_line = 0;
        start_column = 0;
        end_column = 0;
    }

    public Vulnerable(String t, String fp, int sl, int el){
        this(t, fp);
        start_line = sl;
        end_line = el;
    }

    public Vulnerable(String t, String fp, int sl, int el, int sc, int ec){
        this(t, fp, sl, el);
        start_column = sc;
        end_column = ec;
    }

    public String getFilePath(){
        return file_path;
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
        return "Type: " + getType() + ", File Path: " + getFilePath()
            + ", Start Line: " + getStartLine() + ", End Line: " + getEndLine()
            + ", Start Column: " + getStartColumn() + ", End Column: " + getEndColumn();
    }
}