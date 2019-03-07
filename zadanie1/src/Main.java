public class Main {

    public static void main(String[] args) {

        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        int c = Integer.parseInt(args[2]);
        double d = b*b - 4 * a * c;
        //System.out.println(a + "x^2 + " + b + "x + " + c);
        if(a == 0){//linear
            if(b != 0){
                double x0 = (double)-c / (double)b;
                System.out.println("Pierwiastek:" + x0);
            }else{//flat
                if(c != 0) {
                    System.out.println("Brak pierwiastkow");
                }else{
                    System.out.println("pierwiastek: zbiór liczb rzeczywistych");
                }
            }
        }else //square
        if(d < 0){
            System.out.println("Brak pierwiastkow");
        }else if(d > 0){
            if(a != 0) {
                double x1 = (-b - Math.sqrt(d)) / (2.0 * a);
                double x2 = (-b + Math.sqrt(d)) / (2.0 * a);
                System.out.println("Pierwiastki:" + x1 + ", " + x2);
            }else{
                System.out.println("Brak pierwiastkow");
            }
        }else{
            double x0 = -b/(2.0*a);
            System.out.println("Pierwiastek:" + x0);
        }
    }
}
