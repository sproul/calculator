package bondmetrics;

import java.util.Date;

public class Util {
	public enum Calculator_mode {
        None,
        Monroe,
        BondOAS,
        FtLabs
    }
	public static String calculator_mode_toString(Calculator_mode mode) {
		return calculator_mode_toString(mode, false);
	}
	static String calculator_mode_toString(Calculator_mode mode, boolean noneOk) {
		switch (mode) {
		case None:
			if (!noneOk) {
				throw new RuntimeException("unexpected None value for calculator mode");
			}
			return "None";
		case Monroe:
			return "Monroe";
		case BondOAS:
			return "BondOAS";
		case FtLabs:
			return "FtLabs";
		default:
			throw new RuntimeException("unrecognized calculator mode " + mode);
		}
	}
	private static Double ytm_increment_starting_value = 0.0000001;
	public static Calculator_mode calculator_mode = Util.init_calculator_mode();
	public static Boolean bondOAS_available = null;
 
	/* based on sample data interest_payment_frequency:
     *
	 * grep interest_payment_frequency $DOWNLOADS/gsm_init_portfolio_c300167_20170417_114931.231.1.csv.xml | sort | uniq
     * <interest_payment_frequency>1</interest_payment_frequency>
     * <interest_payment_frequency>2</interest_payment_frequency>
     * <interest_payment_frequency>3</interest_payment_frequency>
     * <interest_payment_frequency>5</interest_payment_frequency>
     * <original_interest_payment_frequency>1</original_interest_payment_frequency>
     * <original_interest_payment_frequency>3</original_interest_payment_frequency>
     *
	 */
	public enum Bond_frequency_type {
        Unknown(0),
        SemiAnnual(1),
        Monthly(2),
        Annual(3),
        Weekly(4),
        Quarterly(5),
        Every_2_years(6),
        Every_3_years(7),
        Every_4_years(8),
        Every_5_years(9),
        Every_7_years(10),
        Every_8_years(11),
        Biweekly(12),
        Changeable(13),
        Daily(14),
        Term_mode(15),
        Interest_at_maturity(16),
        Bimonthly(17),
        Every_13_weeks(18),
        Irregular(19),
        Every_28_days(20),
        Every_35_days(21),
        Every_26_weeks(22),
        Not_Applicable(23),
        Tied_to_prime(24),
        One_time(25),
        Every_10_years(26),
        Frequency_to_be_determined(27),
        Mandatory_put(28),
        Every_52_weeks(29),
        When_interest_adjusts_commercial_paper(30),
        Zero_coupon(31),
        Certain_years_only(32),
        Under_certain_circumstances(33),
        Every_15_years(34),
        Custom(35),
        Single_Interest_Payment(36);
                                   
        public final int value;
        Bond_frequency_type(int value) {
            this.value = value;
        }
    }

    /* 
     * Discussed at length at https://en.wikipedia.org/wiki/Day_count_convention
     * 
     * Pair of settings: part 1 tells us how many days are in a month accruing interest, part 2 tells how many days are in a year for purposes of deriving the daily interest rate
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
        By_Actual_Actual(1),
        By_Actual_360(2),
        By_30_360(3),
        By_Actual_365(5),
        By_Actual_365_366_Leap_Year_ISDA(7),
        By_30_360_Compounded_Interest(8),
        By_30_365(9),
        By_Future_Data_Not_Available(10),
        By_Historical_Data_Not_Available(11),
        By_30_360_ICMA(12),
        By_Actual_365_366_Leap_Year(13),
        By_Actual_364(14),
        By_Bus_252(15),
        By_365_365(16),
        By_Actual_Actual_ICMA(17),
        By_30_360_US(19),
        By_30_360_US_NASD(20),
        By_30_360_BMA(21),
        By_30_360_ISDA(22),
        By_30_360_IT(23),
        By_30_360_SIA(24),
        By_30E_360(25),
        By_30E_360_ISDA(26),
        By_30E_360b(27),		// not sure of the purpose of this value in the ICE data
        By_NL_365_No_Leap_Year(28);

        public final int value;
        Interest_basis(int value) {
            this.value = value;
        }
    }

	@SuppressWarnings("deprecation")
	static Date date(int year, int month, int day) {
		return new Date(year - 1900, month -1, day);
	}

    private static Calculator_mode init_calculator_mode() {
        return Calculator_mode.FtLabs;
	}

	/**
     * Given a price, bond type, and a date of maturity, return the price including markup.
     * 
     */

	static int number_of_payment_periods_between(Util.Bond_frequency_type frequency_type, Date d1, Date d2) {
        // adding one day to starting date to avoid counting payment on initial day; e.g., if the span is 3/21/16 to 3/21/17 for an annual, we expect payments to be 0
        d1 = day_after(d1);
        frequency_type = remap_frequency_type(frequency_type);
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

	@SuppressWarnings("deprecation")
	static Date day_after(Date d1) {
		return new Date(d1.getYear(), d1.getMonth(), d1.getDate() + 1);
	}
	@SuppressWarnings("deprecation")
	static double fractional_number_of_payment_periods_between(Util.Bond_frequency_type frequency_type, Interest_basis interest_basis, Date d1, Date d2) {
        frequency_type = remap_frequency_type(frequency_type);
        interest_basis = remap_interest_basis(interest_basis);
        // this simplifies the date handling such that our treatment of leap years is not correct for By_NL_365_No_Leap_Year -- not sure if that's important.
        // more info: https://en.wikipedia.org/wiki/Day_count_convention
        float year_length = 365;
        boolean mode_30_day_months;
        switch(interest_basis) {
		case By_30_360:
		case By_30_360_Compounded_Interest:
        case By_30_360_ICMA:
        case By_30_360_US:
        case By_30_360_US_NASD:
        case By_30_360_BMA:
        case By_30_360_ISDA:
        case By_30_360_IT:
        case By_30_360_SIA:
        case By_30E_360:
        case By_30E_360_ISDA:
        case By_30E_360b:
            mode_30_day_months = true;
            year_length = 360;
            break;
		case By_30_365:
            mode_30_day_months = true;
			break;
        default:
            mode_30_day_months = false;
        }
        
        // subtracting one to avoid counting payment on initial day; e.g., if the span is 3/21/16 to 3/21/17 for an annual, we expect payments to be 0
        int year_count = d2.getYear() - d1.getYear();
        if (mode_30_day_months) {
            int months_left_over = d2.getMonth() - d1.getMonth();
            int days_left_over = d2.getDate() - d1.getDate();
            switch (frequency_type) {
            case Annual:
                return year_count + (((months_left_over * 30) + days_left_over) / year_length);
            case SemiAnnual:
                return (2 * year_count) + (((months_left_over * 30) + days_left_over) / (year_length / 2));
            case Quarterly:
                return (4 * year_count) + (((months_left_over * 30) + days_left_over) / (year_length / 4));
            case Monthly:
                return (12 * year_count) + (((months_left_over * 30) + days_left_over) / 30.0);
            default:
                throw new RuntimeException("unknown frequency type " + frequency_type);
            }
		}
        else {
        	long days = number_of_days_between(d1, d2) - 1;
            switch (frequency_type) {
            case Annual:
                return  days / 365.0;
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

	public static long number_of_days_between(Date d1, Date d2) {
		long t1 = d1.getTime();
		long t2 = d2.getTime();
        if (t2 < t1) {
            long z = t1;
            t1 = t2;
            t2 = z;
        }
        long dif = t2 - t1;
        dif += (2 * 60 * 60 * 1000);    // eliminate any daylight savings time nonsense
        long days = dif / (24 * 60 * 60 * 1000);
		return days;
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

	static double yield_to_maturity_approximate(int payments_per_year, double years_to_maturity, double coupon_payment, int par, double price) {
		double c = coupon_payment;
		double n = years_to_maturity;
		int f = par;
		double p = price;
		double approx_ytm = (c + ((f - p) / n)) / ((f + p) / 2);
		return approx_ytm;
	}
	
	private static double price_from_ytm(int payments_per_year, double payment_periods, double coupon_payment, double ytm, int par) {
        if (payment_periods <= 0) {
            throw new RuntimeException("payment_periods must be a positive number (not " + payment_periods + ")");
        }
		double ytm_per_payment = ytm / payments_per_year;
		double c = coupon_payment;
		double x = par / Math.pow(1 + ytm_per_payment, payment_periods);
		double initial_fractional_payment_period = payment_periods - (long) payment_periods;
		
		for (double t = (initial_fractional_payment_period > 0) ? initial_fractional_payment_period : 1; t <= payment_periods; t++) {
            double gain_for_period = c / Math.pow(1 + ytm_per_payment, t);
            x += gain_for_period;
        }		
		double price = x;
		return price;
	}

	public static double yield_to_worst(Bond_frequency_type frequency_type, Interest_basis interest_basis, double clean_price, double coupon_rate, int par, Date settlement, Date maturity, Date[] call_or_sink_dates) {
        // http://www.investinganswers.com/financial-dictionary/bonds/yield-worst-ytw-2761
        double worst = yield_to_maturity(frequency_type, interest_basis, clean_price, coupon_rate, par, settlement, maturity);
        for (int j = 0; j < call_or_sink_dates.length; j++) {
            Date cdj = call_or_sink_dates[j];
            if (cdj.getTime() < settlement.getTime()) {
                continue;
            }
            if (cdj.getTime() > maturity.getTime()) {
                throw new RuntimeException("call date must precede maturity, but " + cdj + " is after " + maturity);
            }
            double t = yield_to_maturity(frequency_type, interest_basis, clean_price, coupon_rate, par, settlement, cdj);
            if (t < worst) {
                worst = t;
            }
        }
        return worst;
	}
	
	@SuppressWarnings("deprecation")
	public static double yield_to_maturity(Bond_frequency_type frequency_type, Interest_basis interest_basis, double clean_price, double coupon_rate, int par, Date settlement, Date maturity) {
		if (maturity.getTime() < settlement.getTime()) {
            throw new RuntimeException("settlement must precede maturity");
		}
		if (maturity.getYear()==settlement.getYear() && maturity.getMonth()==settlement.getMonth() && maturity.getDate()==settlement.getDate()) {
            throw new RuntimeException("settlement cannot be on the same date as maturity");
		}
        Calculator_mode mode = Util.calculator_mode;
		if (coupon_rate >= 1.0) {
            throw new RuntimeException("bad coupon_rate of " + coupon_rate + " (" + (100 * coupon_rate) + "%)");
        }
        else if (coupon_rate < 0) {
        	if (mode == Calculator_mode.FtLabs) {
        		mode = Calculator_mode.Monroe;
        	}
        }
		switch (mode) {
        case BondOAS:
            throw new RuntimeException("BondOAS not supported");
            //return BondOASwrapper.yield_to_maturity(frequency_type, clean_price, coupon_rate, par, settlement, maturity);
        case FtLabs:
            return FtLabs.yield_to_maturity_static(frequency_type, interest_basis, clean_price, coupon_rate, par, settlement, maturity);
        default:
        	break;            	
        }
        frequency_type = remap_frequency_type(frequency_type);
        interest_basis = remap_interest_basis(interest_basis);
        double fractional_payment_periods = fractional_number_of_payment_periods_between(frequency_type, interest_basis, settlement, maturity);
        double accrued_interest = accrued_interest_at_settlement(frequency_type, interest_basis, coupon_rate, par, settlement, maturity, mode);
        double dirty_price = accrued_interest + clean_price;
		int payments_per_year =  number_of_payment_periods_per_year(frequency_type);
        if (fractional_payment_periods < 1) {
        	return yield_to_maturity_short_term(interest_basis, coupon_rate, dirty_price, accrued_interest, payments_per_year, par, settlement, maturity);
        }
		double coupon_payment = coupon_rate * par / payments_per_year;
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
			price_that_results_from_proposed_ytm = price_from_ytm(payments_per_year, fractional_payment_periods, coupon_payment, proposed_ytm, par);
			double deviation = Math.abs(price_that_results_from_proposed_ytm - dirty_price);
            //System.out.println("yield_to_maturity: " + ytm_lower_bound + ".." + ytm_upper_bound + " (" + deviation + ")");
            if (deviation < 0.001) {
                //System.out.println("yield_to_maturity: done======================================================");
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
		} while (Math.abs(last_proposed_ytm - proposed_ytm) > 0.00000000001);
        
        double ytm = proposed_ytm;
        return ytm;
	}

	// 18-30 __using_interest_days
	private static double yield_to_maturity_short_term(Interest_basis interest_basis, double coupon_rate, double dirty_price, double accrued_interest, int payments_per_year, int par, Date settlement, Date maturity) {
        double last_bond_payment = coupon_rate * par / payments_per_year;
        double days = interest_days_between(interest_basis, settlement, maturity);
        double days_in_a_year = interest_days_in_a_year(interest_basis);
        double payout = par + last_bond_payment;
        double ytm_short_term = (payout / dirty_price) - 1;
        double ytm = ytm_short_term * days_in_a_year / days;
//        System.out.println("yield_to_maturity_short_term: days=" + days + ", dirty_price=" + dirty_price + ", ytm_short_term=" + ytm_short_term);
        return ytm;
	}

	private static int number_of_payment_periods_per_year(Bond_frequency_type frequency_type) {
        frequency_type = remap_frequency_type(frequency_type);
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

	public static long interest_days_in_a_year(Interest_basis interest_basis) {
    	interest_basis = remap_interest_basis(interest_basis);
        switch (interest_basis) {
        case By_30_360:
        case By_Actual_360:
            return 360;
        default:
            return 365;
        }
    }
    
    /*
     * Given the interest_basis, calculate the number of days between the dates for accrued interest calculating purposes.
     */
    @SuppressWarnings("deprecation")
	public static long interest_days_between(Interest_basis interest_basis, Date date1, Date date2) {
        if (date2.getTime() < date1.getTime()) {
            throw new RuntimeException("expect date1 to precede date2");
        }
    	interest_basis = remap_interest_basis(interest_basis);
        switch (interest_basis) {
        case By_30_360:
			int y1 = date1.getYear();
			int y2 = date2.getYear();
			int m1 = date1.getMonth();
			int m2 = date2.getMonth();
			int d1 = date1.getDate();
			int d2 = date2.getDate();
			int months_between = (12 * (y2 - y1)) + m2 - m1;
			return (30 * months_between) + d2 - d1;
        case By_Actual_Actual:
        case By_Actual_360:
        case By_Actual_365:
            return number_of_days_between(date1, date2);
        default:
            throw new RuntimeException("unexpected interest basis " + interest_basis);
        }
	}

    /*
     * Given the interest_basis and the coupon rate, calculate the daily interest rate to be used for calculating accrued interest.
     */
	static double daily_interest_rate(Interest_basis interest_basis, double coupon_rate) {
		interest_basis = remap_interest_basis(interest_basis);
		switch(interest_basis) {
		case By_30_360:
		case By_30_360_ICMA:
		case By_Actual_360:
            return coupon_rate / 360;
        case By_Actual_365:
        case By_Actual_Actual:
            return coupon_rate / 365;
        default:
            throw new RuntimeException("unrecognized interest_basis=" + interest_basis);
        }
	}

    public static Interest_basis remap_interest_basis(Interest_basis interest_basis) {
        switch (interest_basis) {
        case By_Actual_Actual:
        case By_Actual_360:
        case By_Actual_365:
        case By_30_360:
            return interest_basis;
        case By_30E_360:
        case By_30E_360_ISDA:
        case By_30E_360b:
        case By_30_360_BMA:
        case By_30_360_Compounded_Interest:
        case By_30_360_ISDA:
        case By_30_360_IT:
        case By_30_360_SIA:
        case By_30_360_US:
        case By_30_360_US_NASD:
            return Interest_basis.By_30_360;
        case By_30_365:
        case By_365_365:
        case By_Actual_364:
        case By_Actual_365_366_Leap_Year:
        case By_Actual_365_366_Leap_Year_ISDA:
        case By_Actual_Actual_ICMA:
        case By_Bus_252:
        case By_Future_Data_Not_Available:
        case By_Historical_Data_Not_Available:
        case By_NL_365_No_Leap_Year:
        default:
            return Interest_basis.By_Actual_Actual;
        }
	}

	/*
     * Given the interest_basis, bond coupon rate, settlement and maturity dates for a bond, calculate the accrued interest that will build up
     * between 
     * 					1.) the last coupon payment date preceding the settlement date and
     * 					2.) the settlement date.
     */
	public static double accrued_interest_at_settlement(Bond_frequency_type frequency_type, Interest_basis interest_basis, double coupon_rate, int par, Date settlement, Date maturity, Calculator_mode mode) {
		switch (mode) {
        case FtLabs:
            return FtLabs.accrued_interest_at_settlement_static(frequency_type, interest_basis, coupon_rate, par, settlement, maturity);
        default:
        	break;            	
        }
        frequency_type = remap_frequency_type(frequency_type);
        interest_basis = remap_interest_basis(interest_basis);
        Date last_coupon_payment_date = find_coupon_payment_date_preceding_or_coinciding_with_settlement(frequency_type, settlement, maturity);
        
        last_coupon_payment_date = adjust_last_coupon_payment_date(interest_basis, last_coupon_payment_date, settlement, maturity);
        settlement               = adjust_settlement_date(         interest_basis, last_coupon_payment_date, settlement, maturity);
        
		return accrued_interest_from_time_span(interest_basis, coupon_rate, par, last_coupon_payment_date, settlement);
	}

	public static double accrued_interest_at_settlement(Bond_frequency_type frequency_type, Interest_basis interest_basis, double coupon_rate, int par, Date settlement, Date maturity) {
		return accrued_interest_at_settlement(frequency_type, interest_basis, coupon_rate, par, settlement, maturity, Util.calculator_mode);
	}

	@SuppressWarnings("deprecation")
	private static Date adjust_settlement_date(Interest_basis interest_basis, 	Date last_coupon_payment_date, Date settlement, Date maturity) {
		switch (interest_basis) {
        case By_30_360:
            // https://en.wikipedia.org/wiki/Day_count_convention#30.2F360_US
            if (settlement.getDate() == (31 - 1) && last_coupon_payment_date.getDate()== (29 - 1)) { // d2==31 && d1==30)
                settlement = Util.subtractDay(settlement);
            }
            break;
        default:
            ;
        }
		return settlement;
	}
	@SuppressWarnings("deprecation")
	public static Date subtractDay(Date d) {
		return new Date(d.getYear(), d.getMonth(), d.getDate() - 1);
	}
	@SuppressWarnings("deprecation")
	public static Date addDay(Date d) {
		return new Date(d.getYear(), d.getMonth(), 1+d.getDate());
	}
	@SuppressWarnings("deprecation")
	private static Date adjust_last_coupon_payment_date(Interest_basis interest_basis, Date last_coupon_payment_date, Date settlement, Date maturity) {
		switch (interest_basis) {
		case By_30_360:
			// https://en.wikipedia.org/wiki/Day_count_convention#30.2F360_US
			if (last_coupon_payment_date.getDate()==(31 - 1)) {	// d1==31
				last_coupon_payment_date = Util.subtractDay(last_coupon_payment_date);
			}
			break;
		default:
			;
		}
		return last_coupon_payment_date;
	}
	@SuppressWarnings("deprecation")
	static int count_months_between(Date d1, Date d2) {
        int n = d2.getMonth() - d1.getMonth();
        if (d1.getDate() > d2.getDate()) {
            n--;
        }
        return n + (12 * (d2.getYear() - d1.getYear()));
    }
    
    @SuppressWarnings("deprecation")
	static Date find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type frequency_type, Date settlement, Date maturity) {
        frequency_type = remap_frequency_type(frequency_type);
    	int sy = settlement.getYear();
		int sm = settlement.getMonth();
		int mm = maturity.getMonth();
		int sd = settlement.getDate();
		int md = maturity.getDate();
        
        int cy = sy;
        int cm = mm;
        int cd = md;
        
        frequency_type = remap_frequency_type(frequency_type);
        
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

	static Bond_frequency_type remap_frequency_type(Bond_frequency_type frequency_type) {
        switch (frequency_type) {
        case SemiAnnual:
        case Monthly:
        case Annual:
        case Quarterly:
            return frequency_type;
        case Daily:
        case Biweekly:
        case Weekly:
        case Bimonthly:
            return Bond_frequency_type.Monthly;
        case Every_2_years:
        case Every_3_years:
        case Every_4_years:
        case Every_5_years:
        case Every_7_years:
        case Every_8_years:
            return Bond_frequency_type.Annual;
        case Interest_at_maturity:
        case Every_52_weeks:
            return Bond_frequency_type.Annual;
        case Every_13_weeks:
            return Bond_frequency_type.Quarterly;
        case Irregular:
        case Every_28_days:
        case Every_35_days:
            return Bond_frequency_type.Monthly;
        case Every_26_weeks:
            return Bond_frequency_type.SemiAnnual;
        case Not_Applicable:
        case Tied_to_prime:
        case One_time:
        case Every_10_years:
        case Frequency_to_be_determined:
        case Mandatory_put:
        case When_interest_adjusts_commercial_paper:
        case Zero_coupon:
        case Certain_years_only:
        case Under_certain_circumstances:
        case Every_15_years:
        case Custom:
        case Single_Interest_Payment:
        default:
            //System.out.println("Util.remap_frequency_type(" + frequency_type + "): unsure, defaulting to SemiAnnual");
            return Bond_frequency_type.SemiAnnual;
        }
	}

	private static boolean month_precedes_month(int m1, int m2) {
		return m1 == m2-1 || (m1==11 && m2==0);
	}

	/*
     * Given the interest_basis, bond coupon rate, calculate the accrued interest that will build up
     * over the span of time bounded by the Dates 'd1' and 'd2'.
     */
	public static double accrued_interest_from_time_span(Interest_basis interest_basis, double coupon_rate, int par, Date d1, Date d2) {
        interest_basis = remap_interest_basis(interest_basis);
        double daily_interest_rate = daily_interest_rate(interest_basis, coupon_rate);
        long interest_days         = interest_days_between(        interest_basis, d1, d2);
        double unrounded_accrued_interest = par * interest_days * daily_interest_rate;
        //System.out.println("accrued_interest_from_time_span: daily_interest_rate="+daily_interest_rate+", interest_days="+interest_days);
		return unrounded_accrued_interest;
	}
    
	public static double round_to_cent(double p) {
        return Math.round (p * 100.0) / 100.0; 
    }
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		double coupon_rate = 3.75;
		Interest_basis interest_basis = Interest_basis.By_30_360_ICMA;
		Bond_frequency_type frequency_type = Bond_frequency_type.Quarterly;
		Date settlement = new Date(117, 5, 30);
		Date maturity= new Date(120, 5, 30);
		double accrued_interest = Util.accrued_interest_at_settlement(frequency_type, interest_basis, coupon_rate, 100, settlement, maturity, Calculator_mode.Monroe);
		System.out.println("accrued interest is " + accrued_interest);
	}

	public static String getenv(String env_name) {
        String val = System.getenv(env_name);
        if (val==null) {
            throw new RuntimeException("could not find environment variable " + env_name);
        }
        return val;
    }

	public static int getenvInt(String env_name) {
        String valString = Util.getenv(env_name);
        try {
        	int val = Integer.parseInt(valString);
        	return val;
        }
        catch (Exception e) {
        	throw new RuntimeException("could not find parse integer from " + valString + " from environment variable " + env_name);
        }
    }
	@SuppressWarnings("deprecation")
	public static String dateToString(Date d) {
        return "" + (1 + d.getMonth()) + "/" + d.getDate() + "/" + (1900 + d.getYear() - 2000);
    }
    @SuppressWarnings("deprecation")
	public static String dateRangeToString(Date d1, Date d2) {
        String z = dateToString(d1) + "-" + dateToString(d2);
        if (d1.getYear() == d2.getYear()) {
            int year = d1.getYear() - 100;
            z = z.replaceFirst("/" + year + "-", "-");
        }
        return z;
    }
}
