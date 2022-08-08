package com.ntcu.app.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileOperator{

    private static final String Base_Path = System.getProperty("user.dir");
    private static final String Fix_Path = Base_Path + "/fix/";

    public static String getFileName(String file){
        return Path.of(file).getFileName().toString();
    }

    public static String getFileName(Path path){
        return path.getFileName().toString();
    }
    
    /* Unused */
    public static void removeMatchFile(String file){
        try{
            for (Path path: Files.newDirectoryStream(Path.of(Base_Path))){
                if (Pattern.matches("n\\d+_"+getFileName(file), getFileName(path))) Files.delete(path);
            }
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public static void createFixDir(){
        if (Files.exists(Path.of(Fix_Path))) removeFixDir(Path.of(Fix_Path));
        if (Files.notExists(Path.of(Fix_Path))){
            try{
                Files.createDirectory(Path.of(Fix_Path));
                System.out.println("Create fix directory: " + Fix_Path);
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    public static void removeFixDir(Path path){
        try{
            if (Files.isDirectory(path)) Files.list(path).forEach(i -> removeFixDir(i));
            Files.delete(path);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public static boolean fixDirExist(){
        return Files.exists(Path.of(Fix_Path));
    }

    public static int getFileVersionNumber(String file){
        int v = 1;
        while (Files.exists(Path.of(Fix_Path + "n" + String.valueOf(v) + "_" + getFileName(file)))) v++;
        return v;
    }

    public static boolean checkFileExists(String file){
        return Files.exists(Path.of(file));
    }

    public static String save(String file, String output){
        int ver = getFileVersionNumber(file);
        String file_name = "n" + String.valueOf(ver) + "_" + getFileName(file);
        Path path = Path.of(Fix_Path + file_name);
        try{
            Files.writeString(path, output);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
        return path.toString();
    }

    public static List<Path> getJSON(String[] args){
        List<Path> paths = new ArrayList<>();
        for (String i: args){
            if (FileOperator.checkFileExists(i)) paths.add(Path.of(Base_Path, Path.of(i).getFileName().toString()));
            else System.out.println("File [" + i + "] doesn't exist.");
        }
        return paths;
    }

    /*
     * Get scanning json file by JFileChooser
     */
    public static List<Path> getJSON(){
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(Base_Path));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON File", "json"));
        chooser.setMultiSelectionEnabled(true);
        int returnValue = chooser.showOpenDialog(null);
        List<Path> paths = new ArrayList<>();
        if (returnValue==JFileChooser.APPROVE_OPTION){
            List<File> file = Arrays.asList(chooser.getSelectedFiles());
            // Path is an absolute here 
            for (File i: file) paths.add(i.toPath());
        }
        return paths;
    }

    public static String getBasePath(){
        return Base_Path;
    }
}
