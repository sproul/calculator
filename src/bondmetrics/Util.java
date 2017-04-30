package bondmetrics;

import java.util.Date;


public class Util {
	private static Double ytm_increment_starting_value = 0.0000001;
 
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
     */

	public static int number_of_payment_periods_between(Util.Bond_frequency_type frequency_type, Date d1, Date d2) {
		switch (frequency_type) {
		case Annual:
			return number_of_years_between(d1, d2);
		case SemiAnnual:
			return number_of_half_years_between(d1, d2);
		case Quarterly:
			return number_of_quarters_between(d1, d2);
		case Monthly:
			return number_of_months_between(d1, d2);
		default:
			throw new RuntimeException("unknown frequency type " + frequency_type);
		}
    }
	private static double fractional_number_of_payment_periods_between(Util.Bond_frequency_type frequency_type, Date d1, Date d2) {
		long days = number_of_days_between(d1, d2);
		switch (frequency_type) {
		case Annual:
			return days / 365.0;
		case SemiAnnual:
			return days / 182.5;
		case Quarterly:
			return days / 91.25;
		case Monthly:
			return days / 30.5;
		default:
			throw new RuntimeException("unknown frequency type " + frequency_type);
		}
    }
	static int number_of_quarters_between(Date d1, Date d2) {
		int months = number_of_months_between(d1, d2);
		return months / 3;
	}

	static int number_of_half_years_between(Date d1, Date d2) {
		int months = number_of_months_between(d1, d2);
		return months / 6;
	}

	@SuppressWarnings("deprecation")
	static int number_of_months_between(Date d1, Date d2) {
		int y1 = d1.getYear();
		int y2 = d2.getYear();
		int m1 = d1.getMonth();
		int m2 = d2.getMonth();
		d1.getDate();
		int day1 = d1.getDate();
		int day2 = d2.getDate();
        int n = ((y2 - y1) * 12) + m2 - m1;
        if (day1 > day2) {
            n--;
        }
        return n;
	}

	private static long number_of_days_between(Date d1, Date d2) {
		long t1 = d1.getTime();
		long t2 = d2.getTime();
        return (t2 - t1) / (24 * 60 * 60 * 1000);
	}

	@SuppressWarnings("deprecation")
	static int number_of_years_between(Date d1, Date d2) {
		int n = d2.getYear() - d1.getYear();
		if (d2.getMonth() < d1.getMonth()) {
			n--;
		}
		else if (d2.getMonth() == d1.getMonth()) {
			if (d2.getDate() < d1.getDate()) {
				n--;
			}
		}
		return n;
	}

	public static double yield_to_maturity_approximate(int payments_per_year, double years_to_maturity, double coupon_payment, int par, double price) {
		double c = coupon_payment;
		double n = years_to_maturity;
		int f = par;
		double p = price;
		double approx_ytm = (c + ((f - p) / n)) / ((f + p) / 2);
		return approx_ytm;
	}
	
	private static double price_from_ytm(int payments_per_year, long payment_periods, double coupon_payment, double ytm, int par) {
		long n = payment_periods;
		double pr = ytm / payments_per_year;
		double c = coupon_payment;
		double f = par;
		double x = f / Math.pow(1 + pr, n);
		
		for (int t = 1; t <= payment_periods; t++) {
            double gain_for_period = c / Math.pow(1 + pr, t);
            x += gain_for_period;
        }		
		double price = x;
		return price;
	}
	
	public static double yield_to_maturity(Util.Bond_frequency_type frequency_type, double actual_price, double coupon_rate, int par, Date settlement, Date maturity) {
		long payment_periods = number_of_payment_periods_between(frequency_type, settlement, maturity);
		double fractional_payment_periods = fractional_number_of_payment_periods_between(frequency_type, settlement, maturity);
		int payments_per_year =  number_of_payment_periods_per_year(frequency_type);
		double coupon_payment = coupon_rate * par / payments_per_year;
		double proposed_ytm = yield_to_maturity_approximate(payments_per_year, fractional_payment_periods, coupon_payment, par, actual_price);
		double price_that_results_from_proposed_ytm;
		Double ytm_lower_bound = null;
		Double ytm_upper_bound = null;
		Double ytm_increment = Util.ytm_increment_starting_value ;
		double UNREASONABLE_YTM = -100000000;
		Double last_proposed_ytm = UNREASONABLE_YTM;
		Boolean too_high_last_time = null;
		Boolean too_high = null;
		do {
			price_that_results_from_proposed_ytm = price_from_ytm(payments_per_year, payment_periods, coupon_payment, proposed_ytm, par);
			if (Math.abs(price_that_results_from_proposed_ytm - actual_price) < 0.001) {
				break;
			}
			else if (price_that_results_from_proposed_ytm < actual_price) {
				if (ytm_upper_bound != null && ytm_upper_bound < proposed_ytm) {
					throw new RuntimeException("unexpected ytm_upper_bound=" + ytm_upper_bound);
				}
				ytm_upper_bound = proposed_ytm;
				too_high = true;
			}
			else {
				if (ytm_lower_bound != null && ytm_lower_bound > proposed_ytm) {
					throw new RuntimeException("unexpected ytm_lower_bound=" + ytm_lower_bound);
				}
				ytm_lower_bound = proposed_ytm;
				too_high = false;
			}
			if (ytm_lower_bound != null && ytm_upper_bound != null) {
				if (ytm_upper_bound < ytm_lower_bound) {
					throw new RuntimeException("unexpected crossing of bounds");
				}
				proposed_ytm = (ytm_upper_bound + ytm_lower_bound) / 2;
			}
			else {
				if ((too_high && too_high_last_time != null && too_high_last_time) || (!too_high && too_high_last_time != null &&  !too_high_last_time)) {
					ytm_increment *= 2;       // if we are way off, provide a means to accelerate in the right direction
				}
				else {
					ytm_increment = Util.ytm_increment_starting_value;
				}
				if (too_high) {
					proposed_ytm -= ytm_increment;
				}
				else {
					proposed_ytm += ytm_increment;
				}
				too_high_last_time = too_high;
			}
		} while (Math.abs(last_proposed_ytm - proposed_ytm) > 0.00001);
		double ytm = Math.round(10000 * proposed_ytm) / 10000.0;
        return ytm;
	}
	
	private static int number_of_payment_periods_per_year(Bond_frequency_type frequency_type) {
		switch (frequency_type) {
		case Annual:
			return 1;
		case SemiAnnual:
			return 2;
		case Quarterly:
			return 4;
		case Monthly:
			return 12;
		default:
			throw new RuntimeException("unknown frequency type " + frequency_type);
		}
	}
}
