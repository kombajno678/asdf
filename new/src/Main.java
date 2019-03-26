import java.io.*;
import java.util.Scanner;
import java.util.Random;
public class Main {

    public static void main(String[] args)  {
        Random rng = new Random();
        Scanner scan = new Scanner(System.in);
        BufferedReader br = null;
        String fileName = "";
        while (br == null){
            System.out.println("enter file name/path:");
            fileName = scan.nextLine();
            //fileName = "file.txt"; //default file
            try {
                br = new BufferedReader(new FileReader(fileName));
            } catch (Exception e) {
                System.out.println("error: specified file could not be opened");
            }
        }
        //save fileName to new txt file
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("fileName.txt"));
            out.write(fileName);
            out.close();
        }catch(Exception e){
            System.out.println("error: failed writing to file");
        }

        try {
            while (br.ready()) {
                if (scan.hasNext()) {
                    String temp = scan.nextLine();//clear buffer

                    int i = rng.nextInt(5) + 1;
                    //System.out.println(" ... i = " + i);
                    while (br.ready() && i-- > 0) {
                        System.out.print(((char) br.read()));
                    }
                }
            }

            br.close();
        }catch(Exception e){
            System.out.println("error: error while reading file");
        }
        scan.close();

    }
}
