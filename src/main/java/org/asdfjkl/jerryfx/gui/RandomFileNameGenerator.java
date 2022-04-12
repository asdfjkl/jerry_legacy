package org.asdfjkl.jerryfx.gui;

import java.util.Date;
import java.util.Random;

public class RandomFileNameGenerator {

    public static String getRandomFilename() {

        Date date = new Date();
        long timeInMilliseconds = date.getTime();
        Random random = new Random(timeInMilliseconds);

        String filename = "";
        for(int i=0;i<8;i++) {
            char c = (char) (random.nextInt(26) + 97);
            filename += c;
        }

        System.out.println(filename + ".tmp");
        return filename + ".tmp";
    }

}
