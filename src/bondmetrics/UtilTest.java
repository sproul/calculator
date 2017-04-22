package bondmetrics;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

class XLerator_DLL_spectrum_output_line_info {
    int basis_type;
    int frequency_type;
    Date maturity_date;
    Date settlement_date;
    int src_line_number;
    String src_line;
    Double rate;
    Double reference_price;
    Double reference_yield;
    double discrepancy;
    int payments_per_year;
	HashMap<Double, Double> price_to_xl_calculated_yield;
    
	public XLerator_DLL_spectrum_output_line_info(String spectrum_output_test_line)
    {
        String[] tokens = spectrum_output_test_line.split("\t");

        this.src_line = spectrum_output_test_line;
        this.src_line_number = Integer.parseInt(tokens[0]);
        parse_price_to_yield_pairs(tokens[1]);
        
        
        
        
        
        @SuppressWarnings("unused")
		int xl_frequency_code = Integer.parseInt(tokens[1]);
        int xl_period_code = Integer.parseInt(tokens[2]);
        if (xl_period_code < 1000000) throw new RuntimeException("set this.payments_per_year");
        
        
        
        
        this.settlement_date = xl_string_to_date(tokens[4]);
        this.maturity_date = xl_string_to_date(tokens[5]);
        this.rate = Double.parseDouble(tokens[6]);
    }
	@SuppressWarnings("deprecation")
	private Date xl_string_to_date(String xl_date_string) {
		String[] tokens = xl_date_string.split("/");
		return new Date(Integer.parseInt(tokens[2]), Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
	}
	private void parse_price_to_yield_pairs(String ppy_strings) {
		this.price_to_xl_calculated_yield = new HashMap<Double, Double>();
		for (String ppy_string : ppy_strings.split(",")) {
			String[] ppy_array = ppy_string.split("/");
			Double price = Double.parseDouble(ppy_array[0]);
			Double yield = Double.parseDouble(ppy_array[1]);
            if (this.reference_price == null) {
                this.reference_price = price;
                this.reference_yield = yield;
            }
            else {
                this.price_to_xl_calculated_yield.put(price, yield);
            }
		}
	}
	@SuppressWarnings("deprecation")
	private String date_to_xl_string(Date d) {
		StringBuffer sb = new StringBuffer();
		sb.append(d.getMonth())
		.append("/")
		.append(d.getDate())
		.append("/")
		.append(d.getYear());
		return sb.toString();
	}
	public String toString() {
        return "" + this.frequency_type + "\t" + this.basis_type + "\t" + date_to_xl_string(this.settlement_date) + "\t" 
            + date_to_xl_string(this.maturity_date) + "\t" + this.reference_price + "/" + UtilTest.yield_toString(this.reference_yield) + "\t" + "@" + this.rate;
    }
}
    
class XLerator_DLL_spectrum_output_line extends XLerator_DLL_spectrum_output_line_info {
	public XLerator_DLL_spectrum_output_line(String spectrum_output_test_line) {
        super(spectrum_output_test_line);
    }
	public void calculate_yields() {
        for (double price: this.price_to_xl_calculated_yield.keySet()) {
            double xl_calculated_yield = this.price_to_xl_calculated_yield.get(price);
            if (xl_calculated_yield > 0) {
            	XLerator_DLL_spectrum_output_test test = new XLerator_DLL_spectrum_output_test(this, this.reference_price, this.reference_yield, price, xl_calculated_yield);
            	test.execute();
            	XLerator_DLL_spectrum_output_test.test_record.put(test, test.get_discrepancy());
            }
		}
	}
}
    
class XLerator_DLL_spectrum_output_test implements Comparable<XLerator_DLL_spectrum_output_test> {
    XLerator_DLL_spectrum_output_line_info line_info;
    double new_price;
    double xl_calculated_yield;
    double bondmetrics_calculated_yield;
	
    static HashMap<XLerator_DLL_spectrum_output_test, Double> test_record = new HashMap<XLerator_DLL_spectrum_output_test, Double>();
                                                                                                         
	public XLerator_DLL_spectrum_output_test(XLerator_DLL_spectrum_output_line_info line_info, double reference_price, double reference_yield, double new_price, double xl_calculated_yield) {
        this.line_info = line_info;
        this.new_price = new_price;
        this.xl_calculated_yield = xl_calculated_yield;
    }
    public void execute() {
    	this.bondmetrics_calculated_yield = UtilTest.yield_to_maturity(new_price, this.line_info.rate, line_info.payments_per_year, UtilTest.DEFAULT_PAR, 
    			line_info.settlement_date, line_info.maturity_date);
	}
	public Double get_discrepancy() {
        return Math.abs(this.bondmetrics_calculated_yield - this.xl_calculated_yield);
    }
	public static double calculate_avg_discrepancy() {
		double total = 0;
		int j = 0;
		for (XLerator_DLL_spectrum_output_test test : test_record.keySet()) {
			j++;
			double discrepancy = test_record.get(test);
			total += Math.abs(discrepancy);
		}
		return total / j;
	}
	public static void report() {
		int j = 0;
		for (XLerator_DLL_spectrum_output_test test : get_sorted_test_record_keys()) {
			if (++j > 400000) break;
			double discrepancy = test_record.get(test);
			System.out.println(UtilTest.yield_toString(discrepancy) + "\t" + test.toString());
		}
		System.out.println("avg discrepancy is " + UtilTest.yield_toString(calculate_avg_discrepancy()));
	}
	private static ArrayList<XLerator_DLL_spectrum_output_test> get_sorted_test_record_keys() {
		ArrayList<XLerator_DLL_spectrum_output_test> testList = new ArrayList<XLerator_DLL_spectrum_output_test>();
		for (XLerator_DLL_spectrum_output_test test: test_record.keySet()) {
			testList.add(test);
		}
		Collections.sort(testList, Collections.reverseOrder());
		return testList;
	}
	public String toString() {
        return this.line_info.toString() + "\t" + this.new_price + " => " + UtilTest.yield_toString(this.bondmetrics_calculated_yield) + " (XL yield was " + UtilTest.yield_toString(this.xl_calculated_yield) + ")";
    }
	@Override
	public int compareTo(XLerator_DLL_spectrum_output_test o) {
		return this.get_discrepancy().compareTo(o.get_discrepancy());
	}
}
    
public class UtilTest
{
	public static final int DEFAULT_PAR = 100;
	private static final double MARGIN_FOR_ERROR = 0.00001;
	private static Double ytm_increment_starting_value = 0.0000001;
	
	public static void test_spectrum_output(String xlerator_spectrum_output_filename) {
		try {
			FileInputStream fstream = new FileInputStream(xlerator_spectrum_output_filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;
            while ((strLine = br.readLine()) != null)   {
				XLerator_DLL_spectrum_output_line xl_test_run_output_line = new XLerator_DLL_spectrum_output_line(strLine);
				xl_test_run_output_line.calculate_yields();
			}
			br.close();
			
			XLerator_DLL_spectrum_output_test.report();
		}
		catch (Exception e) {
			System.err.println("trouble processing " + xlerator_spectrum_output_filename + " in dir " + System.getProperty("user.dir"));
			throw new RuntimeException(e);
		}
	}

	public static String yield_toString(double value) {
        return "" + UtilTest.round(value, 2);
    }

	public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

	public static void main(String[] args) {
		String fn;
		if (args.length > 0) {
			fn = args[0];
		}
		else {
			fn = "../yield_tool/spectrum.out";
		}
		test_spectrum_output(fn);
	}
	
	private static double yield_to_maturity_approximate(int payments_per_year, long years_to_maturity, double coupon_payment, int par, double price) {
		double c = coupon_payment;
		long n = years_to_maturity;
		int f = par;
		double p = price;
		double approx_ytm = (c + ((f - p) / n)) / ((f + p) / 2);
		return approx_ytm;
	}
	
	private static double price_from_ytm(int payments_per_year, long years_to_maturity, double coupon_payment, double ytm, int par) {
		long n = years_to_maturity;
		double pr = ytm / payments_per_year;
		double c = coupon_payment;
		double f = par;
		double x = f / Math.pow(1 + pr, payments_per_year * n);
 		long payment_periods = n * payments_per_year;
		for (int t = 1; t <= payment_periods; t++) {
            double gain_for_period = c / Math.pow(1 + pr, payments_per_year * t);
            System.out.println("gain_for_period=" + gain_for_period);
			x += gain_for_period;
        }		
		double price = x;
		return price;
	}
	
	public static double yield_to_maturity(double actual_price, double coupon_rate, int payments_per_year, int par, Date settlement, Date maturity) {
		int days =  (int)( (maturity.getTime() - settlement.getTime()) / (1000 * 60 * 60 * 24));
		long years_to_maturity = Math.round(days / 365.0);
		double coupon_payment = coupon_rate * par / payments_per_year;
		double proposed_ytm = yield_to_maturity_approximate(payments_per_year, years_to_maturity, coupon_payment, par, actual_price);
		double price_that_results_from_proposed_ytm;
		Double ytm_lower_bound = null;
		Double ytm_upper_bound = null;
		Double ytm_increment = UtilTest.ytm_increment_starting_value ;
		double UNREASONABLE_YTM = -100000000;
		Double last_proposed_ytm = UNREASONABLE_YTM;
		Boolean too_high_last_time = null;
		Boolean too_high = null;
		do {
			price_that_results_from_proposed_ytm = price_from_ytm(payments_per_year, years_to_maturity, coupon_payment, proposed_ytm, par);
			System.out.println("ytm_from_guess=" + proposed_ytm + ", price_that_results_from_proposed_ytm=" + price_that_results_from_proposed_ytm);
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
				System.out.println("bisecting " + ytm_lower_bound + ".." + ytm_upper_bound + ", now proposing " + proposed_ytm);
			}
			else {
				if ((too_high && too_high_last_time != null && too_high_last_time) || (!too_high && too_high_last_time != null &&  !too_high_last_time)) {
					ytm_increment *= 2;       // if we are way off, provide a means to accelerate in the right direction
				}
				else {
					ytm_increment = UtilTest.ytm_increment_starting_value;
				}
				if (too_high) {
					proposed_ytm -= ytm_increment;
					System.out.println("too high, now proposing " + proposed_ytm);
				}
				else {
					proposed_ytm += ytm_increment;
					System.out.println("too low, now proposing " + proposed_ytm);
				}
				too_high_last_time = too_high;
			}
		} while (Math.abs(last_proposed_ytm - proposed_ytm) > 0.00001);
		double ytm = Math.round(10000 * proposed_ytm) / 10000.0;
        return ytm;
	}
	
	@Test
	public void test_approx() {
		assertEquals(0.084615, yield_to_maturity_approximate(1, 4, 0.07 * 100, 100,  95), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@SuppressWarnings("deprecation")
	@Test
	public void test_annual() {
		assertEquals(0.05,  yield_to_maturity(100, 0.05, 1, 100, new Date(2016, 3, 21), new Date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.05,  yield_to_maturity(100, 0.05, 1, 100, new Date(2016, 3, 21), new Date(2029, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@SuppressWarnings("deprecation")
	@Test
	public void test_annual_multiyear() {
		assertEquals(0.0853,  yield_to_maturity(95,   0.07, 1,   100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.4373,  yield_to_maturity(72,   0.2,   1,   100, new Date(2016, 3, 21), new Date(2018, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@SuppressWarnings("deprecation")
	@Test
	public void test_annual_multiyear_par1000() {
		assertEquals(0.0853,  yield_to_maturity(950, 0.07, 1, 1000, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@SuppressWarnings("deprecation")
	@Test
	public void test_semiannual_simple() {
		assertEquals(0.2,  yield_to_maturity(100, 0.2, 2, 100, new Date(2016, 3, 21), new Date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }

	/*

	@SuppressWarnings("deprecation")
	@Test
	public void test_semiannual_below_par() {
		assertEquals(99,  yield_to_maturity(84, 0.2, 2, 100, new Date(2016, 3, 21), new Date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
	@SuppressWarnings("deprecation")
	@Test
	public void test_quarterly_simple() {
		assertEquals(0.2,  yield_to_maturity(100, 0.2, 4, 100, new Date(2016, 3, 21), new Date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }

	@SuppressWarnings("deprecation")
	@Test
	public void test_semiannual() {
		assertEquals(0.085,  yield_to_maturity(95, 0.07, 2, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@SuppressWarnings("deprecation")
	@Test
	public void test_quarterly() {
		assertEquals(0.0849,  yield_to_maturity(95, 0.07, 4, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@SuppressWarnings("deprecation")
	@Test
	public void test_monthly() {
		assertEquals(0.0848	,  yield_to_maturity(95, 0.07, 12, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    */
    /*
	@SuppressWarnings("deprecation")
	@Test
	public void test_ytm@@() {
		assertEquals(0.084615, yield_to_maturity_approximate(1, 4, 0.07 * 100, 100,  95), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.05,  yield_to_maturity(100, 0.05, 1, 100, new Date(2016, 3, 21), new Date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0853,  yield_to_maturity(95,   0.07, 1,   100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0853,  yield_to_maturity(950, 0.07, 1, 1000, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.085,  yield_to_maturity(95, 0.07, 2, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0849,  yield_to_maturity(95, 0.07, 4, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0848	,  yield_to_maturity(95, 0.07, 12, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@SuppressWarnings("deprecation")
	@Test
	public void test_ytm@@() {
		assertEquals(0.084615, yield_to_maturity_approximate(1, 4, 0.07 * 100, 100,  95), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.05,  yield_to_maturity(100, 0.05, 1, 100, new Date(2016, 3, 21), new Date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0853,  yield_to_maturity(95,   0.07, 1,   100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0853,  yield_to_maturity(950, 0.07, 1, 1000, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.085,  yield_to_maturity(95, 0.07, 2, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0849,  yield_to_maturity(95, 0.07, 4, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0848	,  yield_to_maturity(95, 0.07, 12, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@SuppressWarnings("deprecation")
	@Test
	public void test_ytm@@() {
		assertEquals(0.084615, yield_to_maturity_approximate(1, 4, 0.07 * 100, 100,  95), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.05,  yield_to_maturity(100, 0.05, 1, 100, new Date(2016, 3, 21), new Date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0853,  yield_to_maturity(95,   0.07, 1,   100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0853,  yield_to_maturity(950, 0.07, 1, 1000, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.085,  yield_to_maturity(95, 0.07, 2, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0849,  yield_to_maturity(95, 0.07, 4, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.0848	,  yield_to_maturity(95, 0.07, 12, 100, new Date(2016, 3, 21), new Date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    */
 }
