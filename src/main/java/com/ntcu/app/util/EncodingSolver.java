package com.ntcu.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class EncodingSolver{

    public static Charset getCharset(Path path){
        try{
            byte b[] = Files.readAllBytes(path);
            String str = null;

            // Check UTF-16
            str = byteToHex(b[0]) + byteToHex(b[1]);
            if (str.equals("FFFE")) return StandardCharsets.UTF_16LE;
            if (str.equals("FEFF")) return StandardCharsets.UTF_16BE;

            // Check UTF-8
            str += byteToHex(b[2]);
            if (str.equals("FEBBBF")) return StandardCharsets.UTF_8;
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        return StandardCharsets.UTF_8;
    }

    private static String byteToHex(byte i){
        return Integer.toHexString(i&0xFF).toUpperCase();
    }

    public static void removeJsonBOM(Path path){
        if (isBOMExist(path)){
            Charset charset = getCharset(path);
            try (InputStream is = Files.newInputStream(path)){
                if (charset==StandardCharsets.UTF_8) is.skip(3);
                if (charset==StandardCharsets.UTF_16BE || charset==StandardCharsets.UTF_16LE) is.skip(2);
                Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    public static boolean isBOMExist(Path path){
        try{
            byte b[] = Files.readAllBytes(path);
            String str = null;

            // Check UTF-16
            str = byteToHex(b[0]) + byteToHex(b[1]);
            if (str.equals("FFFE")) return true;
            if (str.equals("FEFF")) return true;

            // Check UTF-8
            str += byteToHex(b[2]);
            if (str.equals("FEBBBF")) return true;
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        return false;
    }
}