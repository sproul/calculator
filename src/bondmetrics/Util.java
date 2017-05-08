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

    /* 
     * based on sample data interest bases:
     * grep interest_basis  $DOWNLOADS/gsm_init_portfolio_c300167_20170417_114931.231.1.csv.xml | sort | uniq
     * 
     * <interest_basis type="30/360 (ICMA)">12</interest_basis>
     * Simplify accrued interest calculations by assuming all months have 30 days. Derive the daily accrued interest rate by dividing the annual rate by 360.
     * <interest_basis type="Actual/360">2</interest_basis>
     * For accrued interest calculations, use the actual number of days elapsed, but derive the daily accrued interest rate by dividing the annual rate by 360.
     * <interest_basis type="Actual/365 (Fixed)">5</interest_basis>
     * For accrued interest calculations, use the actual number of days elapsed, but derive the daily accrued interest rate by dividing the annual rate by 365.
     * <interest_basis type="Actual/Actual">1</interest_basis>
     * For accrued interest calculations, use the actual number of days elapsed, and derive the daily accrued interest rate by dividing the annual rate by the number of days in the year.
     */
	public enum Interest_basis {
        By_30_360_ICMA,
        By_Actual_360,
        By_Actual_365,
        By_Actual_Actual
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

	public static double yield_to_maturity(Bond_frequency_type frequency_type, double actual_price, double coupon_rate, int par, Date settlement, Date maturity) {
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
			double deviation = Math.abs(price_that_results_from_proposed_ytm - actual_price);
            //System.out.println("yield_to_maturity: " + ytm_lower_bound + ".." + ytm_upper_bound + " (" + deviation + ")");
            if (deviation < 0.001) {
                //System.out.println("yield_to_maturity: done");
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
	

	public static double yield_to_maturity_including_accrued_interest(Interest_basis interest_basis, Bond_frequency_type frequency_type, double clean_price, double coupon_rate, int par, Date settlement, Date maturity) {
		long payment_periods = number_of_payment_periods_between(frequency_type, settlement, maturity);
		double fractional_payment_periods = fractional_number_of_payment_periods_between(frequency_type, settlement, maturity);
		int payments_per_year =  number_of_payment_periods_per_year(frequency_type);
		double coupon_payment = coupon_rate * par / payments_per_year;
		Date last_coupon_payment_before_settlement = Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(frequency_type, settlement, maturity);
		double accrued_interest = accrued_interest_from_time_span(interest_basis, coupon_rate, last_coupon_payment_before_settlement, settlement) * par;
		double dirty_price = clean_price + accrued_interest;
		double proposed_ytm = yield_to_maturity_approximate(payments_per_year, fractional_payment_periods, coupon_payment, par, dirty_price);
		double price_that_results_from_proposed_ytm;
		Double ytm_lower_bound = null;
		Double ytm_upper_bound = null;
		Double ytm_increment = Util.ytm_increment_starting_value ;
		double UNREASONABLE_YTM = -100000000;
		Double last_proposed_ytm = UNREASONABLE_YTM;
		Boolean too_high_last_time = null;
		Boolean too_high = null;
		do {
			price_that_results_from_proposed_ytm = accrued_interest + price_from_ytm(payments_per_year, payment_periods, coupon_payment, proposed_ytm, par);
			double deviation = Math.abs(price_that_results_from_proposed_ytm - dirty_price);
            System.out.println("yield_to_maturity: " + ytm_lower_bound + ".." + ytm_upper_bound + " (" + deviation + ")");
            if (deviation < 0.001) {
                System.out.println("yield_to_maturity: done");
				break;
			}
			else if (price_that_results_from_proposed_ytm < dirty_price) {
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

    /*
     * Given the interest_basis, calculate the number of days between the dates for accrued interest calculating purposes.
     */
    @SuppressWarnings("deprecation")
	public static int accrued_interest_days(Interest_basis interest_basis, Date date1, Date date2) {
		if (interest_basis == Interest_basis.By_30_360_ICMA) {
			int y1 = date1.getYear();
			int y2 = date2.getYear();
			int m1 = date1.getMonth();
			int m2 = date2.getMonth();
			int d1 = date1.getDate();
			int d2 = date2.getDate();
			int months_between = (12 * (y2 - y1)) + m2 - m1;
			return (30 * months_between) + d2 - d1;
		}
		long millisecs = date2.getTime() - date1.getTime();
		long long_days = millisecs / (1000 * 24 * 60 * 60);
		if (long_days > 1000000) {
			throw new RuntimeException("unreasonable span of days for accrued interest=" + long_days);
		}
		return (int) long_days;
	}

    /*
     * Given the interest_basis and the coupon rate, calculate the daily interest rate to be used for calculating accrued interest.
     */
	public static double accrued_interest_rate_per_day(Interest_basis interest_basis, double coupon_rate) {
		switch(interest_basis) {
        case By_30_360_ICMA:
        case By_Actual_360:
            return coupon_rate / 360;
        case By_Actual_365:
        case By_Actual_Actual:
            return coupon_rate / 365;
        default:
            throw new RuntimeException("unrecognizd interest_basis=" + interest_basis);
        }
	}

    /*
     * Given the interest_basis, bond coupon rate, settlement and maturity dates for a bond, calculate the accrued interest that will build up
     * between 
     * 					1.) the last coupon payment date preceding the settlement date and
     * 					2.) the settlement date.
     */
	public static double accrued_interest_at_settlement(Bond_frequency_type frequency_type, Interest_basis interest_basis, double coupon_rate, Date settlement, Date maturity) {
		Date last_coupon_payment_date = find_coupon_payment_date_preceding_or_coinciding_with_settlement(frequency_type, settlement, maturity);
		return accrued_interest_from_time_span(interest_basis, coupon_rate, last_coupon_payment_date, settlement);
	}

	@SuppressWarnings("deprecation")
	public static int count_months_between(Date d1, Date d2) {
        int n = d2.getMonth() - d1.getMonth();
        if (d1.getDate() > d2.getDate()) {
            n--;
        }
        return n + (12 * (d2.getYear() - d1.getYear()));
    }
    
    @SuppressWarnings("deprecation")
	public static Date find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type frequency_type, Date settlement, Date maturity) {
    	int sy = settlement.getYear();
		int sm = settlement.getMonth();
		int mm = maturity.getMonth();
		int sd = settlement.getDate();
		int md = maturity.getDate();
        
        int cy = sy;
        int cm = mm;
        int cd = md;
        
    	switch (frequency_type) {
        case Annual:
            if (sm < mm || (sm==mm && sd < md)) {
                cy--;
            }
            break;
        case SemiAnnual:
            if (cm > sm || (cm==sm && cd > sd)) {
                cm -= 6;
                if (cm > sm || (cm==sm && cd > sd)) {
                    cm += 6;
                    cy--;
                }
            }
            else {
                if (cm < sm-6 || (cm==sm-6 && cd <= sd)) {
                    cm += 6;
                }
            }
            break;
        case Quarterly:
        	cm = sm;
            int n = count_months_between(settlement, maturity); 
            if (n % 3 != 0) {
            	cm = mm - n - 3 + (n % 3); 
            }
            else {
                if (month_precedes_month(sm, mm)) {
                	cm = sm - 2;
                }
                else if (sd < md) {
                	cm = sm - 3;
                }
                else {
                	cm = sm;
                }
            }
            break;
        case Monthly:
            cm = sm;
            if (sd < md) {
                cm--;
            }
            break;
        default:
            throw new RuntimeException("bad bond frequency_type=" + frequency_type);
    	}
        if (cm < 0) {
            cy -= (cm / 12);
            cm = cm % 12;
            if (cm < 0) {
            	cm += 12;
            	cy--;
            }
        }
        return new Date(cy, cm, cd);
    }

	private static boolean month_precedes_month(int m1, int m2) {
		return m1 == m2-1 || (m1==11 && m2==0);
	}

	/*
     * Given the interest_basis, bond coupon rate, calculate the accrued interest that will build up
     * over the span of time bounded by the Dates 'd1' and 'd2'.
     */
	public static double accrued_interest_from_time_span(Interest_basis interest_basis, double coupon_rate, Date d1, Date d2) {
        double interest_rate_per_day = accrued_interest_rate_per_day(interest_basis, coupon_rate);
        int         interest_days               = accrued_interest_days(             interest_basis, d1, d2);
		return interest_days * interest_rate_per_day;
	}
}
