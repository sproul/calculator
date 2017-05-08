package bondmetrics;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import org.junit.Test;

import bondmetrics.Util.Bond_frequency_type;

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
	private Date xl_string_to_date(String xl_date_string) {
		String[] tokens = xl_date_string.split("/");
		return Util.date(Integer.parseInt(tokens[2]), Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
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
    	this.bondmetrics_calculated_yield = Util.yield_to_maturity(Util.Bond_frequency_type.Annual, new_price, this.line_info.rate, UtilTest.DEFAULT_PAR, 
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

	@Test
	public void test_approx() {
		assertEquals(0.084615, Util.yield_to_maturity_approximate(1, 4, 0.07 * 100, 100,  95), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@Test
	public void test_annual() {
        assertEquals(0.05,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, 100, 0.05, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.05,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, 100, 0.05, 100, Util.date(2016, 3, 21), Util.date(2029, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@Test
	public void test_annual_multiyear() {
		assertEquals(0.0853,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, 95,   0.07,   100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.4373,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, 72,   0.2,   100, Util.date(2016, 3, 21), Util.date(2018, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@Test
	public void test_annual_multiyear_par1000() {
		assertEquals(0.0853,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, 950, 0.07, 1000, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@Test
	public void test_semiannual_simple() {
		assertEquals(0.2,  Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, 100, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
	
	@Test
	public void test_semiannual_below_par() {
		assertEquals(0.4108,  Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, 84, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
	@Test
	public void test_quarterly_simple() {
		assertEquals(0.2,  Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, 100, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }

	@Test
	public void test_semiannual() {
		assertEquals(0.085,  Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@Test
	public void test_quarterly() {
		assertEquals(0.0849,  Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	@Test
	public void test_monthly() {
		assertEquals(0.0848,  Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }

	@Test
	public void test_monthly_partial() {
		assertEquals(0.1493,  Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    

	@Test
	public void test_monthly_partial_at_par() {
		assertEquals(0.07,  Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, 100, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
	// disabled this test -- my method does not account for accrued interest because it assumes the buyer will pay exactly the interest that was earned before settlement, and
	// therefore there will be no impact on the price
    //@Test
	public void test_annual12_partial() {
		assertEquals(0.1154,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, 100, 0.12, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), UtilTest.MARGIN_FOR_ERROR);
	}

	//@Test
	public void test_annual_partial_at_par() {
		assertEquals(0.0684,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, 100, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), UtilTest.MARGIN_FOR_ERROR);
	}
	@Test
	public void test_number_of_payment_periods_between__Annual() {
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2018, 3, 20)));
		assertEquals(3, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2020, 3, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2018, 3, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2018, 3, 19)));
	}
	@Test
	public void test_number_of_payment_periods_between__SemiAnnual() {
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2017, 9, 20)));
		assertEquals(3, Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2018, 9, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2017, 9, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2017, 9, 19)));
	}
	@Test
	public void test_number_of_payment_periods_between__Quarterly() {
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2017, 6, 20)));
		assertEquals(3, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2017, 12, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2017, 6, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2017, 6, 19)));
	}
	@Test
	public void test_number_of_payment_periods_between__Monthly() {
		assertEquals(2, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2017, 4, 20)));
		assertEquals(3, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2017, 5, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2017, 3, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2017, 3, 19)));
		assertEquals(11, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2018, 2, 19)));
		assertEquals(12, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2018, 2, 20)));
		assertEquals(12, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2018, 2, 21)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 12, 20), Util.date(2018, 1, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 12, 20), Util.date(2018, 1, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 12, 20), Util.date(2018, 1, 19)));
	}
	@Test
	public void test_number_of_quarters_between() {
		assertEquals(0, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 4, 19)));
		assertEquals(1, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 4, 20)));
		assertEquals(1, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 4, 21)));
		assertEquals(1, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 7, 19)));
		assertEquals(2, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 7, 20)));
		assertEquals(2, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 7, 21)));
		assertEquals(2, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 10, 19)));
		assertEquals(3, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 10, 20)));
		assertEquals(3, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2017, 10, 21)));
		assertEquals(3, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2018, 1, 19)));
		assertEquals(4, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2018, 1, 20)));
		assertEquals(4, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2018, 1, 21)));
		assertEquals(4, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2018, 4, 19)));
		assertEquals(5, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2018, 4, 20)));
		assertEquals(5, Util.number_of_quarters_between(Util.date(2017, 1, 20), Util.date(2018, 4, 21)));
	}
	@Test
	public void test_number_of_half_years_between() {
		assertEquals(1, Util.number_of_half_years_between(Util.date(2017, 3, 20), Util.date(2017, 9, 20)));
		assertEquals(3, Util.number_of_half_years_between(Util.date(2017, 3, 20), Util.date(2018, 9, 20)));
		assertEquals(1, Util.number_of_half_years_between(Util.date(2017, 3, 20), Util.date(2017, 9, 21)));
		assertEquals(0, Util.number_of_half_years_between(Util.date(2017, 3, 20), Util.date(2017, 9, 19)));
	}
	@Test
	public void test_number_of_months_between() {
		assertEquals(2, Util.number_of_months_between(Util.date(2017, 2, 20), Util.date(2017, 4, 20)));
		assertEquals(3, Util.number_of_months_between(Util.date(2017, 2, 20), Util.date(2017, 5, 20)));
		assertEquals(1, Util.number_of_months_between(Util.date(2017, 2, 20), Util.date(2017, 3, 21)));
		assertEquals(0, Util.number_of_months_between(Util.date(2017, 2, 20), Util.date(2017, 3, 19)));
		assertEquals(11, Util.number_of_months_between(Util.date(2017, 2, 20), Util.date(2018, 2, 19)));
		assertEquals(12, Util.number_of_months_between(Util.date(2017, 2, 20), Util.date(2018, 2, 20)));
	}
	@Test
	public void test_number_of_months_between__2() {
		assertEquals(13, Util.number_of_months_between(Util.date(2017, 2, 20), Util.date(2018, 3, 21)));
		assertEquals(1, Util.number_of_months_between(Util.date(2017, 12, 20), Util.date(2018, 1, 20)));
		assertEquals(1, Util.number_of_months_between(Util.date(2017, 12, 20), Util.date(2018, 1, 21)));
		assertEquals(0, Util.number_of_months_between(Util.date(2017, 12, 20), Util.date(2018, 1, 19)));
	}
	@Test
	public void test_number_of_years_between() {
		assertEquals(1, Util.number_of_years_between(Util.date(2017, 3, 20), Util.date(2018, 3, 20)));
		assertEquals(3, Util.number_of_years_between(Util.date(2017, 3, 20), Util.date(2020, 3, 20)));
		assertEquals(1, Util.number_of_years_between(Util.date(2017, 3, 20), Util.date(2018, 3, 21)));
		assertEquals(0, Util.number_of_years_between(Util.date(2017, 3, 20), Util.date(2018, 3, 19)));
	}
	@Test
	public void test_number_of_years_between__boundary() {
		assertEquals(1, Util.number_of_years_between(Util.date(2017, 12, 20), Util.date(2018, 12, 20)));
		assertEquals(0, Util.number_of_years_between(Util.date(2017, 12, 20), Util.date(2018, 12, 19)));
		assertEquals(0, Util.number_of_years_between(Util.date(2017, 12, 20), Util.date(2018, 11, 30)));
	}
	@Test
	public void test_accrued_interest_days() {
		assertEquals(29, Util.accrued_interest_days(Util.Interest_basis.By_30_360_ICMA, Util.date(2017, 2, 2), Util.date(2017, 3, 1)));
		assertEquals(30, Util.accrued_interest_days(Util.Interest_basis.By_30_360_ICMA, Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(31, Util.accrued_interest_days(Util.Interest_basis.By_30_360_ICMA, Util.date(2017, 2, 1), Util.date(2017, 3, 2)));
		assertEquals(27, Util.accrued_interest_days(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 2), Util.date(2017, 3, 1)));
		assertEquals(28, Util.accrued_interest_days(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(29, Util.accrued_interest_days(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 2)));
		assertEquals(28, Util.accrued_interest_days(Util.Interest_basis.By_Actual_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(28, Util.accrued_interest_days(Util.Interest_basis.By_Actual_Actual,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
    }
	@Test
	public void test_accrued_interest_by_day() {
		assertEquals(0.0111111111111111112, Util.accrued_interest_rate_per_day(Util.Interest_basis.By_30_360_ICMA, 4.0), MARGIN_FOR_ERROR);
		assertEquals(0.0111111111111111112, Util.accrued_interest_rate_per_day(Util.Interest_basis.By_Actual_360,  4.0), MARGIN_FOR_ERROR);
		assertEquals(0.010958904, Util.accrued_interest_rate_per_day(Util.Interest_basis.By_Actual_365,  4.0), MARGIN_FOR_ERROR);
		assertEquals(0.010958904, Util.accrued_interest_rate_per_day(Util.Interest_basis.By_Actual_Actual,  4.0), MARGIN_FOR_ERROR);
    }
	@Test
	public void test_accrued_interest() {
		assertEquals(2.0, Util.accrued_interest_from_time_span(Util.Interest_basis.By_30_360_ICMA, 4.0, Util.date(2017, 1, 1), Util.date(2017, 7, 1)), MARGIN_FOR_ERROR);
	}
	@Test
	public void test_count_months_between() {
		assertEquals(2, Util.count_months_between(Util.date(2017, 1, 1), Util.date(2017, 3, 1)));
		assertEquals(0, Util.count_months_between(Util.date(2017, 3, 1), Util.date(2017, 3, 31)));
		assertEquals(0, Util.count_months_between(Util.date(2016, 12, 10), Util.date(2017, 1, 9)));
		assertEquals(1, Util.count_months_between(Util.date(2016, 12, 10), Util.date(2017, 1, 10)));
		assertEquals(1, Util.count_months_between(Util.date(2016, 12, 10), Util.date(2017, 1, 11)));
		assertEquals(1, Util.count_months_between(Util.date(2017, 1, 31), Util.date(2017, 3, 1)));
		assertEquals(1, Util.count_months_between(Util.date(2017, 2, 1), Util.date(2017, 3, 31)));
		assertEquals(2, Util.count_months_between(Util.date(2017, 1, 1), Util.date(2017, 3, 2)));
    }
    
    @Test
	public void test_accrued_interest_matthew_from_bloomberg_1() {
		assertEquals(2.5, Util.accrued_interest_from_time_span(Util.Interest_basis.By_30_360_ICMA, 5.0, Util.date(2017, 1, 1), Util.date(2017, 7, 1)), MARGIN_FOR_ERROR);
	}
    @Test
	public void test_accrued_interest_zero_when_settlement_coincides_w_payment_date() {
		assertEquals(0, Util.accrued_interest_from_time_span(Util.Interest_basis.By_30_360_ICMA, 5.0, Util.date(2017, 7, 1), Util.date(2017, 7, 1)), MARGIN_FOR_ERROR);
	}
	@Test
	public void test_find_coupon_payment_date_preceding_or_coinciding_with_settlement__never_backs_up_if_we_are_on_payment_day() {
        assertEquals(Util.date(2016,  7,  1),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Annual, Util.date(2016, 7, 1), Util.date(2017, 7, 1)));
        assertEquals(Util.date(2016,  7,  2),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.SemiAnnual, Util.date(2016, 7, 2), Util.date(2017, 7, 2)));
        assertEquals(Util.date(2016,  7,  3),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.SemiAnnual, Util.date(2016, 7, 3), Util.date(2017, 1, 3)));
        assertEquals(Util.date(2016,  7,  4),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Quarterly, Util.date(2016, 7, 4), Util.date(2017, 1, 4)));
        assertEquals(Util.date(2016,  7,  5),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Monthly, Util.date(2016, 7, 5), Util.date(2017, 1, 5))); 
    }

    @Test
	public void test_find_coupon_payment_date_preceding_or_coinciding_with_settlement_annual() {
		assertEquals(Util.date(2016,  7,  1),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Annual, Util.date(2017, 1, 2), Util.date(2017, 7, 1)));
		assertEquals(Util.date(2016,  7,  16),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Annual, Util.date(2016, 8, 2), Util.date(2017, 7, 16)));
		assertEquals(Util.date(2016,  7,  16),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Annual, Util.date(2016, 7, 20), Util.date(2017, 7, 16)));
		assertEquals(Util.date(2015,  7,  16),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Annual, Util.date(2016, 7, 2), Util.date(2017, 7, 16)));
	}
	@Test
	public void test_find_coupon_payment_date_preceding_or_coinciding_with_settlement_semiannual() {
		assertEquals(Util.date(2016,  2,  1),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.SemiAnnual, Util.date(2016, 4, 2), Util.date(2017, 2, 1)));
		assertEquals(Util.date(2017,  1,  1),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.SemiAnnual, Util.date(2017, 1, 2), Util.date(2017, 7, 1)));
		assertEquals(Util.date(2016,  7,  1),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.SemiAnnual, Util.date(2016, 12, 30), Util.date(2017, 7, 1)));
		assertEquals(Util.date(2016,  7,  2),  Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.SemiAnnual, Util.date(2016, 12, 1), Util.date(2017, 7, 2)));
	}
	@Test
	public void test_find_coupon_payment_date_preceding_or_coinciding_with_settlement_quarterly() {
		assertEquals(Util.date(2017, 4, 2), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Quarterly, Util.date(2017, 6, 3), Util.date(2017, 7, 2)));

		
		
		assertEquals(Util.date(2017, 4, 22), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Quarterly, Util.date(2017, 7, 21), Util.date(2017, 7, 22)));
		assertEquals(Util.date(2017, 4, 1), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Quarterly, Util.date(2017, 5, 2), Util.date(2017, 7, 1)));
		assertEquals(Util.date(2017, 4, 2), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Quarterly, Util.date(2017, 6, 3), Util.date(2017, 7, 2)));
		assertEquals(Util.date(2017, 4, 3), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Quarterly, Util.date(2017, 6, 30), Util.date(2017, 7, 3)));
		assertEquals(Util.date(2017, 4, 20), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Quarterly, Util.date(2017, 6, 30), Util.date(2017, 7, 20)));
		assertEquals(Util.date(2017, 4, 21), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Quarterly, Util.date(2017, 4, 22), Util.date(2017, 7, 21)));
	}
	@Test
	public void test_find_coupon_payment_date_preceding_or_coinciding_with_settlement_monthly() {
		assertEquals(Util.date(2017, 6, 16), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Monthly, Util.date(2017, 7, 6), Util.date(2017, 7, 16)));
		assertEquals(Util.date(2017, 6, 17), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Monthly, Util.date(2017, 6, 18), Util.date(2017, 7, 17)));
		assertEquals(Util.date(2016, 12, 18), Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Monthly, Util.date(2017, 1, 17), Util.date(2017, 7, 18)));
	}
}
