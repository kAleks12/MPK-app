package com.example.app;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args){
        URL urlCheck = Main.class.getResource("Main.class");

        if (urlCheck != null && urlCheck.toString().contains(".jar")) {
            String currentDir = System.getProperty("user.dir");
            Path libCheck = new File(currentDir).toPath().resolve("resources").resolve("pedata");

            if (!Files.exists(libCheck)) {
                System.out.println("You need to put ARCGis jniLibs and resources folders in the jar folder");
                System.exit(0);
            }
        }
        App.main(args);
    }
}