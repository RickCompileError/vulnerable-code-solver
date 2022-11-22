package com.ntcu.app;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "first-demo", defaultPhase = LifecyclePhase.COMPILE)
public class MyMojo extends AbstractMojo
{
    public static <T> List<T> castList(Class <? extends T> clazz, Collection<?> c){
        List<T> l = new ArrayList<T>(c.size());
        for (Object o: c)
            l.add(clazz.cast(o));
        return l;
    }

    public void execute() throws MojoExecutionException{
        CodeGenerator cg = new CodeGenerator();

        // List<Path> paths = null;

        // try{
        //     if (args.length > 0) paths = FileOperator.getJSONPath(args);
        //     else paths = FileOperator.getJSONPath();
        // }catch (Exception e){
        //     e.printStackTrace();
        // }

        // if (paths!=null){
        //     Iterator<Path> iter = paths.iterator();
        //     while (iter.hasNext()){
        //         cg.process(iter.next());
        //     }
        // }
    }
}