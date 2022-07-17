package com.ntcu.app;

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

import com.github.javaparser.utils.SourceRoot;

public class FileOperator{

    private static final String Base_Path = System.getProperty("user.dir") + "\\src\\test\\resources\\";

    public static String getFileName(String file){
        return Path.of(file).getFileName().toString();
    }

    public static String getFileName(Path path){
        return path.getFileName().toString();
    }
    
    public static void removeMatchFile(String file){
        try{
            for (Path path: Files.newDirectoryStream(Path.of(Base_Path))){
                if (Pattern.matches("n\\d+_"+getFileName(file), getFileName(path))) Files.delete(path);
            }
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public static int getFileVersionNumber(String file){
        int v = 1;
        while (Files.exists(Path.of(Base_Path + "n" + String.valueOf(v) + "_" + getFileName(file)))) v++;
        return v;
    }

    public static boolean checkFileExists(String file){
        return Files.exists(Path.of(file));
    }

    public static void save(String file, String output){
        int ver = getFileVersionNumber(file);
        String file_name = "n" + String.valueOf(ver) + "_" + getFileName(file);
        Path path = Path.of(Base_Path+file_name);
        try{
            Files.writeString(path, output);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public static List<Path> getJSON(String[] args){
        List<Path> paths = new ArrayList<>();
        for (String i: args){
            if (FileOperator.checkFileExists(i)) paths.add(Path.of(i));
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
}
