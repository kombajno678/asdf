//zadanie 4
import java.util.Random;
import java.io.*;
import java.nio.file.*;
//import java.util.concurrent.TimeUnit;
class Fileop{
    public void Create(String name){
        File f = new File(name);
        try{
            f.createNewFile();
        }catch(Exception e){
            System.out.println(e.getStackTrace());
        }
    }
    public void Readn(){
        Path p1 = Paths.get("nio.txt");
        try (BufferedReader reader = Files.newBufferedReader(p1)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }
    public void Writen(String in){
        Create("nio.txt");
        Path p1 = Paths.get("nio.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(p1)) {
            writer.write(in, 0,in.length());
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }
    public void Read(){
        String line = null;
        try {
            FileReader fileReader = new FileReader("io.txt");
            BufferedReader buffer = new BufferedReader(fileReader);
            while((line = buffer.readLine())!=null){
                System.out.println(line);
            }
            buffer.close();
        }catch(Exception e){
        }
    }
    public void Write(String in){
        Create("io.txt");
        FileOutputStream out = null;
        File f = new File("io.txt");
        try{
            out = new FileOutputStream(f);
            PrintStream file = new PrintStream(out);
            file.print(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            //if(out != null)out.close();
            //if(file!= null)file.close();
        }
    }
}

public class Main {
    public static void main(String[] args) {


        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 1000;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String generatedString = buffer.toString();

        Fileop a = new Fileop();
        System.out.println("io:");
        long startTime = System.nanoTime();
        a.Write(generatedString);
        a.Read();
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        System.out.println("Execution time in nanoseconds  : " + timeElapsed);
        System.out.println("Execution time in milliseconds : " + timeElapsed / 1000000);
        System.out.println("nio:");
        startTime = System.nanoTime();
        a.Writen(generatedString);
        a.Readn();
        endTime = System.nanoTime();
        timeElapsed = endTime - startTime;
        System.out.println("Execution time in nanoseconds  : " + timeElapsed);
        System.out.println("Execution time in milliseconds : " + timeElapsed / 1000000);

    }
}
