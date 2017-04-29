package bondmetrics;

import java.util.Date;


public class Util {
	public enum Bond_frequency_type {
        Annual,
        SemiAnnual,
        Quarterly,
        Monthly
    }
	@SuppressWarnings("deprecation")
	static public Date date(int year, int month, int day) {
		return new Date(year - 1900, month -1, day);
	}

    /**
     * Given a price, bond type, and a date of maturity, return the price including markup.
     * 
     * Note that the "years to maturity" is derived by rounding up, e.g., if the bond is maturing 
     *      in 3 days, it is treated as being 1 year to maturity.
     */
}
