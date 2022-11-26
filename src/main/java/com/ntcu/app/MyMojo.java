package com.ntcu.app;

import java.util.List;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.ntcu.app.cmd.CommandOperator;
import com.ntcu.app.util.FileOperator;
import com.ntcu.app.cmd.CommandPrinter;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE)
public class MyMojo extends AbstractMojo
{
    public void execute() throws MojoExecutionException{
        CodeGenerator cg = new CodeGenerator();
        CommandPrinter.print(CommandOperator.snyk());
        CommandOperator.snykCodeTest();
        List<Path> paths = null;

        paths = FileOperator.getJSONPath(new String[]{"vuln.json"});

        if (paths!=null){
            Iterator<Path> iter = paths.iterator();
            while (iter.hasNext()){
                cg.process(iter.next());
            }
        }
    }
}
