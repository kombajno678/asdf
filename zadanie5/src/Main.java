import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.io.*;
import java.util.Vector;
class WektoryRoznejDlugosciException extends Exception{
    WektoryRoznejDlugosciException(int len1, int len2){
        System.out.println("error: vector length mismatch: " + len1 + " vs " + len2);
    }
}
public class Main {

    public static void main(String[] args) throws WektoryRoznejDlugosciException{
        Scanner in = new Scanner(System.in);

        System.out.println("Hello World!");
        Vector<Integer> vec1 = new Vector<>(0, 1);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        try{
            line = input.readLine();
        }catch(Exception e){
            System.out.println(e.getStackTrace());
        }
        String[] strs = line.trim().split("\\s+");
        try {
            for (int i = 0; i < strs.length; i++) {
                vec1.add(Integer.parseInt(strs[i]));
            }
        }catch(Exception e){
            System.out.println(e.getStackTrace());
        }
        System.out.println(vec1.capacity());

        Vector<Integer> vec2 = new Vector<>(0, 1);
        try{
            line = input.readLine();
        }catch(Exception e){
            System.out.println(e.getStackTrace());
        }
        strs = line.trim().split("\\s+");
        try {
            for (int i = 0; i < strs.length; i++) {
                vec2.add(Integer.parseInt(strs[i]));
            }
        }catch(Exception e){
            System.out.println(e.getStackTrace());
        }
        System.out.println(vec2.capacity());
        try{
            if(vec1.capacity() != vec2.capacity()){
                //throw

                throw new WektoryRoznejDlugosciException(vec1.capacity(), vec2.capacity());
                //ask user to input again
            }else{
                //save sum to file
                //Create("io.txt");

                String path = "io.txt";
                File f = new File(path);
                try{
                    f.createNewFile();
                }catch(Exception e){
                    System.out.println(e.getMessage());
                }

                FileOutputStream out = null;
                try{
                    out = new FileOutputStream(f);
                    PrintStream file = new PrintStream(out);
                    file.print(vec1.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally{
                    //if(out != null)out.close();
                    //if(file!= null)file.close();
                }

            }

        }catch(WektoryRoznejDlugosciException e){
            System.out.println(e.getMessage());
        }

    }
}
