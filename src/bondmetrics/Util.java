package bondmetrics;

import java.util.Date;

public class Util {
	protected Date maturity;
	protected double transacted_yield;
	protected double transacted_price;

    /**
     * Given a price, bond type, and a date of maturity, return the price including markup.
     * 
     * Note that the "years to maturity" is derived by rounding up, e.g., if the bond is maturing 
     *      in 3 days, it is treated as being 1 year to maturity.
     */
	public static double calculate_yield_to_maturity(double new_price, double transacted_price, double transacted_yield, Date settlement, Date maturity) {
		throw new RuntimeException("impl");
	}
	public static double calculate_yield_current(double new_price, double rate) {
		double annual_interest = rate;
		double new_yield = annual_interest * 100 / new_price;  
        return new_yield;
	}
}
