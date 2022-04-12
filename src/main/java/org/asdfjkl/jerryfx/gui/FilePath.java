package org.asdfjkl.jerryfx.gui;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class FilePath {
    private File getBaseBookPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")) {
            String jarPath = "";
            String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File tmp = (new File(jarPath));
            if(tmp.getParentFile().exists()) {
                File subBook = new File(tmp.getParentFile(), "book");
                return subBook;
                //bookPath = new File(subBook, "varied.bin").getPath();
                //return bookPath;
            }
        }
        if(os.contains("linux")) {
            String jarPath = "";
            String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File tmp = (new File(jarPath));
            if(tmp.getParentFile().exists()) {
                if(tmp.getParentFile().getParentFile().exists()) {
                    File subBook = new File(tmp.getParentFile().getParentFile(), "book");
                    return subBook;
                    //bookPath = new File(subBook, "varied.bin").getPath();
                    //return bookPath;
                }
            }
        }
        return null;
    }

    String getBookPath() {
        File baseBook = getBaseBookPath();
        if(baseBook != null) {
            String bookPath = new File(baseBook, "varied.bin").getPath();
            return bookPath;
        }
        return null;
    }

    String getExtBookPath() {

        return "C:\\Users\\user\\MyFiles\\workspace\\extbook\\extbook.bin";

        /*
        File baseBook = getBaseBookPath();
        if(baseBook != null) {
            String bookPath = new File(baseBook, "extbook.bin").getPath();
            return bookPath;
        }
        return null;
*/
    }

    String getStockfishPath() {

        String os = System.getProperty("os.name").toLowerCase();
        //System.out.println(os);
        if(os.contains("win")) {

            String stockfishPath = "";
            String jarPath = "";
            String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File tmp = (new File(jarPath));
            if(tmp.getParentFile().exists()) {
                File subEngine = new File(tmp.getParentFile(), "engine");
                stockfishPath = new File(subEngine, "stockfish_14.1.exe").getPath();
                return stockfishPath;
            }
        }
        if(os.contains("linux")) {
            String stockfishPath = "";
            String jarPath = "";
            String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            //System.out.println(path);
            jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            //System.out.println(jarPath);
            File tmp = (new File(jarPath));
            if(tmp.getParentFile().exists()) {
                //System.out.println(tmp.getParentFile().getAbsolutePath());
                if(tmp.getParentFile().getParentFile().exists()) {
                    File subEngine = new File(tmp.getParentFile().getParentFile(), "engine");
                    //System.out.println(subEngine.getPath());
                    stockfishPath = new File(subEngine, "stockfish_x64").getPath();
                    return stockfishPath;
                }
            }

        }
        return null;
    }
}
