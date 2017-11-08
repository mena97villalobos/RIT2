package Logica;

import java.io.File;
import java.io.FileFilter;

public class TextFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
        return pathname.getName().toLowerCase().endsWith(".htm") || pathname.getName().toLowerCase().endsWith(".html");
    }
}
