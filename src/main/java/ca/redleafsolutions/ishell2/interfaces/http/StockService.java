package ca.redleafsolutions.ishell2.interfaces.http;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Random;
 
// A sample class that generates fake stock information.
public class StockService {
 
    // Random stock value generators. One for each sample stock
    private static Random rnd = new Random();
//    private static Iterator<Double> goog = rnd.nextDouble()doubles(525, 527).iterator();
//    private static Iterator<Double> aapl = rnd.doubles(110, 112).iterator();
//    private static Iterator<Double> fb = rnd.doubles(75, 77).iterator();
//    private static Iterator<Double> orcl = rnd.doubles(40, 42).iterator();
 
    private static final String FORMAT =
            "[{0}]\t\tGOOG = {1, number, #0.00}\tAAPL = {2, number, #0.00}" +
                    "\tFB = {3, number, #0.00}\tORCL = {4, number, #0.00}";
 
    public static String getStockInfo() {
        return MessageFormat.format(FORMAT, new Date().toString(),                
        		rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());    }
}