package bondmetrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.junit.Test;

import bondmetrics.Util.Bond_frequency_type;
import bondmetrics.Util.Calculator_mode;
import bondmetrics.Util.Interest_basis;

/*
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


  I would like to switch to parameterized classes to handle the different calculator flavors, using the following technique:
  
@RunWith(Parameterized.class)
public class FibonacciTest {
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                 { 0, 0 }, { 1, 1 }, { 2, 1 }, { 3, 2 }, { 4, 3 }, { 5, 5 }, { 6, 8 }  
           });
    }

    @Parameter // first data value (0) is default
    public  int fInput;

    @Parameter(1)
    public int fExpected;

    @Test
    public void test() {
        assertEquals(fExpected, Fibonacci.compute(fInput));
    }
}
*/

public class UtilTest
{
    static int variations = 3;
    static boolean recording = true;
    private static PrintWriter recording_output;
    
    static int test_count = 0;
    static int fail_count = 0;
    static boolean running_as_java_app = false;
    static Util.Calculator_mode pushed_mode = Util.Calculator_mode.None;
	private static String last_test_parms = "";
	public static final int DEFAULT_PAR = 100;
	private static final double MARGIN_FOR_ERROR = 0.00005;
	
    static boolean equalish(double a, double b) {
        double diff = a - b;
        return (Math.abs(diff) < MARGIN_FOR_ERROR);
    }
    static void expected_yield(String test_name, Bond_frequency_type frequency_type, Interest_basis interest_basis, double clean_price, double coupon_rate, int par , Date settlement_date, Date maturity_date,  
                               double expected_accrued_interest, double fidelity_expected)
    {
        expected_yield(test_name, frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date, expected_accrued_interest, fidelity_expected, null, null);
    }
    static void expected_yield(String test_name, Bond_frequency_type frequency_type, Interest_basis interest_basis, double clean_price, double coupon_rate, int par , Date settlement_date, Date maturity_date,  
                               double expected_accrued_interest, double fidelity_expected, Double bondOASexpected, Double bond_metrics_expected) 
    {
    	last_test_parms = "" + coupon_rate + " for " + Util.dateRangeToString(settlement_date, maturity_date);
        assertEquals(expected_accrued_interest, Util.accrued_interest_at_settlement(frequency_type, interest_basis, coupon_rate, par, settlement_date, maturity_date), MARGIN_FOR_ERROR);
        expected_result(test_name, Util.yield_to_maturity(frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date),
                        fidelity_expected, bondOASexpected, bond_metrics_expected);
	}
    static void expected_result(String test_name, double actual, double fidelity_expected, Double bondOASexpected, Double bond_metrics_expected) {
        //bond_metrics_expected = null;
    	//ftLabs_expected = null;
        test_count++;
        boolean ok = false;
        String z = test_name + "." + Util.calculator_mode_toString(Util.calculator_mode) + "/" + last_test_parms;
        if (equalish(fidelity_expected, actual)) {
            ok = true;
        }
        else {
            if (bond_metrics_expected != null) { 
                System.out.println(z + "fidelity disagreeement (" + actual + "/" + fidelity_expected + ")");
            }
        }
        switch (Util.calculator_mode) {
        case BondOAS:
        	if (bondOASexpected != null) {
        		if (ok) {
        			System.out.println(z + "OK, but see obsolete override");
        		}
                assert_eq(z, bondOASexpected, actual);
                return;
        	}
        	break;
        case FtLabs:
        	break;
        case Monroe:
        	if (bond_metrics_expected != null) {
        		if (ok) {
        			System.out.println(z + "OK, but see obsolete override");
        		}
                assert_eq(z, bond_metrics_expected, actual);
                return;
        	}
        	break;
        case None:
        	throw new RuntimeException("invalid mode of None");
        default:
        	throw new RuntimeException("bad mode");
        }
        assert_eq(z, fidelity_expected, actual);
    }
    static void assert_eq(String preamble, double expected, double actual) {
        boolean ok = equalish(expected, actual);
        if (!running_as_java_app) {
            assertEquals(expected, actual, MARGIN_FOR_ERROR);
        }
        else {
            String high_or_low = (actual > expected) ? "high" : "low";
            double diff = actual - expected;
            
            long diffL = (long)(diff * 1000000);
            diff = diffL / 1000000.0;
            
            String epilogue = "\t" + preamble + ": %f (%s)\n";
            System.out.printf("%f\t", expected);
            if (ok) {
                System.out.printf("OK\t" + epilogue, diff, high_or_low);
            }
            else {
                fail_count++;
                System.out.printf("FAILED" + epilogue, diff, high_or_low);
            }
        }
    }
    static void expected_result(String test_name, double actual, double fidelity_expected, Double bondOASexpected) {
        expected_result(test_name, actual, fidelity_expected, bondOASexpected, null);
    }

    static void expected_result(String test_name, double actual, double fidelity_expected) {
        expected_result(test_name, actual, fidelity_expected, null, null);
    }

	public static void push_mode(Util.Calculator_mode mode) {
        if (pushed_mode != Util.Calculator_mode.None) {
            throw new RuntimeException("no support for multiple pushes, already in " + Util.calculator_mode_toString(Util.calculator_mode) + " mode");
        }
        pushed_mode = Util.calculator_mode;
        Util.calculator_mode = mode;
	}
	public static void pop_mode() {
		if (pushed_mode == Util.Calculator_mode.None) {
            throw new RuntimeException("tried to pop, but no mode had been pushed");
        }
        Util.calculator_mode = pushed_mode;
        pushed_mode = Util.Calculator_mode.None;
	}

	public static String mode_toString() {
        switch (Util.calculator_mode) {
        case None:
            return "None";
        case Monroe:
            return "Monroe";
        case FtLabs:
            return "FtLabs";
        case BondOAS:
            return "BondOAS";
        default:
            throw new RuntimeException("bad mode");
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

    @Test
	public void test_settlement_same_as_maturity() {
    	try {
    		Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 3, 21));
    		fail("should have complained about settlement being same as maturity");
    	}
    	catch (RuntimeException e) {
    		// expected
    	}
    }


    @Test
	public void test_30_360_yield_constant_31rst_to_1rst() {
        double ytm31 = Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 3, 31), Util.date(2016, 11, 21));
        double ytm1  = Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 4,  1), Util.date(2016, 11, 21));
        if (ytm1 != ytm31) {
            fail("for 30/360, yield should not change going from 31rst of a month to the 1rst of the next month");
        }
    }

	@Test
	public void test_number_of_days_between() {
		assertEquals(347, Util.number_of_days_between(Util.date(2016, 11, 7), Util.date(2017, 10, 20)));
		assertEquals(347, Util.number_of_days_between(Util.date(2017, 10, 20), Util.date(2016, 11, 7)));
		assertEquals(1, Util.number_of_days_between(Util.date(2016, 10, 31), Util.date(2016, 11, 1)));
        assertEquals(354, Util.number_of_days_between(Util.date(2016, 10, 31), Util.date(2017, 10, 20)));
        assertEquals(353, Util.number_of_days_between(Util.date(2016, 11, 1), Util.date(2017, 10, 20)));
        assertEquals(352, Util.number_of_days_between(Util.date(2016, 11, 2), Util.date(2017, 10, 20)));
        assertEquals(351, Util.number_of_days_between(Util.date(2016, 11, 3), Util.date(2017, 10, 20)));
        assertEquals(350, Util.number_of_days_between(Util.date(2016, 11, 4), Util.date(2017, 10, 20)));
        assertEquals(349, Util.number_of_days_between(Util.date(2016, 11, 5), Util.date(2017, 10, 20)));
        assertEquals(348, Util.number_of_days_between(Util.date(2016, 11, 6), Util.date(2017, 10, 20)));
        assertEquals(346, Util.number_of_days_between(Util.date(2016, 11, 8), Util.date(2017, 10, 20)));
        assertEquals(345, Util.number_of_days_between(Util.date(2016, 11, 9), Util.date(2017, 10, 20)));
        assertEquals(344, Util.number_of_days_between(Util.date(2016, 11, 10), Util.date(2017, 10, 20)));
        assertEquals(343, Util.number_of_days_between(Util.date(2016, 11, 11), Util.date(2017, 10, 20)));
        assertEquals(365, Util.number_of_days_between(Util.date(2016, 11, 21), Util.date(2017, 11, 21)));
        assertEquals(364, Util.number_of_days_between(Util.date(2016, 11, 21), Util.date(2017, 11, 20)));
        assertEquals(1, Util.number_of_days_between(Util.date(2015, 2, 28), Util.date(2015, 3, 1)));
        assertEquals(2, Util.number_of_days_between(Util.date(2016, 2, 28), Util.date(2016, 3, 1)));
        assertEquals(3, Util.number_of_days_between(Util.date(2016, 12, 30), Util.date(2017, 1, 2)));
        assertEquals(365, Util.number_of_days_between(Util.date(2016, 10, 20), Util.date(2017, 10, 20)));
        assertEquals(333, Util.number_of_days_between(Util.date(2016, 11, 21), Util.date(2017, 10, 20)));
    }

	@Test
	public void test_fractional_payment_periods() {
        assertEquals(0.666666666666666666667,
                     Util.fractional_number_of_payment_periods_between(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, Util.date(2017, 3, 21), Util.date(2017, 11, 21)),
                     MARGIN_FOR_ERROR);
    }

	@Test
	public void test_fractional_payment_periods2() {
        assertEquals(0.9972,
                     Util.fractional_number_of_payment_periods_between(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, Util.date(2016, 11, 22), Util.date(2017, 11, 21)),
                     MARGIN_FOR_ERROR);
    }

	@Test
	public void test_fractional_payment_periods2b() {
        assertEquals(1,
                     Util.fractional_number_of_payment_periods_between(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, Util.date(2016, 11, 21), Util.date(2017, 11, 21)),
                     MARGIN_FOR_ERROR);
    }

	@Test
	public void test_fractional_payment_periods3() {
        assertEquals(1.00278,
                     Util.fractional_number_of_payment_periods_between(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, Util.date(2016, 11, 20), Util.date(2017, 11, 21)),
                     MARGIN_FOR_ERROR);
    }

	@Test
	public void test_approx() {
		assertEquals(0.084615, Util.yield_to_maturity_approximate(1, 4, 0.07 * 100, 100,  95), UtilTest.MARGIN_FOR_ERROR);
    }


	
	public void test_misc_ice_freq_types() {
        Date d1 = Util.date(2016, 3, 21);
        Date d2 = Util.date(2017, 3, 21);
        Util.yield_to_maturity(Util.Bond_frequency_type.Unknown, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Weekly, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_2_years, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_4_years, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_5_years, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_7_years, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_8_years, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Biweekly, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Changeable, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Changeable, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Term_mode, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Interest_at_maturity, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Bimonthly, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_13_weeks, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Irregular, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_28_days, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_35_days, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_26_weeks, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Not_Applicable, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Tied_to_prime, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.One_time, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_10_years, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Frequency_to_be_determined, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Mandatory_put, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_52_weeks, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.When_interest_adjusts_commercial_paper, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Zero_coupon, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Certain_years_only, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Under_certain_circumstances, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Every_15_years, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Custom, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
        Util.yield_to_maturity(Util.Bond_frequency_type.Single_Interest_Payment, Util.Interest_basis.By_30_360, 100, 0.05, 100, d1, d2);
    }


	
	public void test_annual() {
        assertEquals(0.05,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.05, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.05,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.05, 100, Util.date(2016, 3, 21), Util.date(2029, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }


	
	public void test_annual_multiyear() {
		expected_result("test_annual_multiyear", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 95,   0.07,  100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), 0.085274, 0.085007);
		expected_result("test_annual_multiyear", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 72,   0.2,   100, Util.date(2016, 3, 21), Util.date(2018, 3, 21)), 0.437332, 0.420547);
    }


	
	public void test_annual_multiyear_par1000() {
		expected_result("test_annual_multiyear_par1000", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 950, 0.07, 1000, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), 0.085274, -0.466982);
    }


	
	public void test_semiannual_simple() {
		assertEquals(0.2, Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
        assertEquals(0.07, Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 5, 21), Util.date(2016, 11, 21)), UtilTest.MARGIN_FOR_ERROR); // accrued 0
    }


	
	public void test_semiannual_below_par() {
        expected_result("test_semiannual_below_par", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 84, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), 0.41083);
    }

	
	public void test_quarterly_simple() {
		assertEquals(0.2,  Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, Util.Interest_basis.By_30_360, 100, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }


	
	public void test_semiannual() {
        assertEquals(0.085,  Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);    // ft 0.08501
    }


	
	public void test_quarterly() {
		expected_result("test_quarterly", Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), 0.08487, 0.085007);
    }


	
	public void test_monthly() {
		expected_result("test_monthly", Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), 0.084786, 0.085007);
    }

    
	public void test_monthly_partial() {
		expected_result("test_monthly_partial", Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 27), Util.date(2016, 11, 21)), 0.1512, 0.15421442165220767, 0.1513);
    }

	
	public void test_quarterly_partial() {
		expected_result("test_quarterly_partial", Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 5, 15), Util.date(2016, 11, 21)), 0.1732, 0.17551058902457783);
    }

    
	public void test_semiannual_partial() {
		expected_result("test_semiannual_partial", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 5, 15), Util.date(2016, 11, 21)), 0.1753, 0.17551058902457783);
    }

    
	public void test_semiannual_partial_at_par() {
		expected_result("test_semiannual_partial_at_par", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2017, 7, 19), Util.date(2017, 7, 25)), 0.0677, 0.06965);
    }

    
	public void test_zero_coupon() {
        expected_result("test_zero_coupon", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.0, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.0, 0.0);
    }
    
	public void test_negative_coupon() {
        expected_result("test_negative_coupon", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, -0.01, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), -0.010033, 0.55);
    }
    
    
	public void test_semiannual_partial_at_par2() {
		expected_result("test_semiannual_partial_at_par2", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 5, 15), Util.date(2016, 11, 21)), 0.07, 0.068326);
    }

    
	public void test_semiannual_partial_at_par3() {
        expected_result("test_semiannual_partial_at_par3", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 10, 6), Util.date(2016, 11, 21)), 0.0682, 0.6634216);
    }

    
	public void test_annual_act360_v_act365() {
        expected_result("test_annual_act360_v_act365", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.2, 100, Util.date(2016, 10, 6), Util.date(2016, 11, 21)), 0.1702);
        expected_result("test_annual_act360_v_act365A", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_Actual_360, 100, 0.2, 100, Util.date(2016, 10, 6), Util.date(2016, 11, 21)), 0.1477); // FTlabs 0.1702
        expected_result("test_annual_act360_v_act365B", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_Actual_365, 100, 0.2, 100, Util.date(2016, 10, 6), Util.date(2016, 11, 21)), 0.1665);
    }

    
	public void test_annual12_partial() {
		expected_result("test_annual12_partial", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.12, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.115384, 0.11302983);
        // fidelity 4.00 accrued
	}
	
	public void test_annual_partial_at_par2() {
		expected_result("test_annual_partial_at_par2", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.0684, 0.06706);
        // fidelity
	}
	@Test
	public void test_addDay() {
		assertEquals(Util.date(2015, 3, 1), Util.addDay(Util.date(2015, 2, 28)));
		assertEquals(Util.date(2015, 2, 28), Util.subtractDay(Util.date(2015, 3, 1)));
		assertEquals(Util.date(2016, 2, 29), Util.subtractDay(Util.date(2016, 3, 1)));
	}
    @Test
	public void test_dateRangeToString() {
        assertEquals("6/1-7/1/16", Util.dateRangeToString(Util.date(2016, 6, 1), Util.date(2016, 7, 1)));
    }

	
	public void test_monthly3() {
		expected_result("test_monthly3", Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.14926, 0.1521357);
    }


	
	public void test_monthly3_at_par() {
		expected_result("test_monthly3_at_par", Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.06999, 0.070207);
    }


    @SuppressWarnings("deprecation")
	@Test
	public void test_date_increment_behavior() {
        Date d1 = Util.date(2015, 1, 31);
        d1 = Util.day_after(d1);
        assertEquals(115, d1.getYear());
        assertEquals(1, d1.getMonth());
        assertEquals(1, d1.getDate());
    }

	@Test
	public void test_number_of_payment_periods_between__misc_ice_freq_types() {
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Unknown, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Weekly, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_2_years, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_3_years, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_4_years, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_5_years, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_7_years, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_8_years, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Biweekly, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Changeable, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Daily, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Term_mode, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Interest_at_maturity, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Bimonthly, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_13_weeks, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Irregular, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_28_days, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_35_days, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_26_weeks, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Not_Applicable, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Tied_to_prime, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.One_time, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_10_years, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Frequency_to_be_determined, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Mandatory_put, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_52_weeks, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.When_interest_adjusts_commercial_paper, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Zero_coupon, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Certain_years_only, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Under_certain_circumstances, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Every_15_years, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Custom, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
		Util.number_of_payment_periods_between(Util.Bond_frequency_type.Single_Interest_Payment, Util.date(2017, 3, 20), Util.date(2018, 3, 20));
	}
	@Test
	public void test_number_of_payment_periods_between__Annual() {
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2018, 3, 20)));
		assertEquals(2, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2020, 3, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2018, 3, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Annual, Util.date(2017, 3, 20), Util.date(2018, 3, 19)));
	}
	@Test
	public void test_number_of_payment_periods_between__SemiAnnual() {
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2017, 9, 20)));
		assertEquals(2, Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2018, 9, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2017, 9, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.SemiAnnual, Util.date(2017, 3, 20), Util.date(2017, 9, 19)));
	}
	@Test
	public void test_number_of_payment_periods_between__Quarterly() {
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2017, 6, 20)));
		assertEquals(2, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2017, 12, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2017, 6, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Quarterly, Util.date(2017, 3, 20), Util.date(2017, 6, 19)));
	}
	@Test
	public void test_number_of_payment_periods_between__Monthly() {
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2017, 4, 20)));
		assertEquals(2, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2017, 5, 20)));
		assertEquals(1, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2017, 3, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2017, 3, 19)));
		assertEquals(11, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2018, 2, 19)));
		assertEquals(11, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2018, 2, 20)));
		assertEquals(12, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 2, 20), Util.date(2018, 2, 21)));
		assertEquals(0, Util.number_of_payment_periods_between(Util.Bond_frequency_type.Monthly, Util.date(2017, 12, 20), Util.date(2018, 1, 20)));
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
	public void test_interest_days_between() {
		assertEquals(29, Util.interest_days_between(Util.Interest_basis.By_30_360, Util.date(2017, 2, 2), Util.date(2017, 3, 1)));
		assertEquals(30, Util.interest_days_between(Util.Interest_basis.By_30_360, Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(31, Util.interest_days_between(Util.Interest_basis.By_30_360, Util.date(2017, 2, 1), Util.date(2017, 3, 2)));
		assertEquals(27, Util.interest_days_between(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 2), Util.date(2017, 3, 1)));
		assertEquals(28, Util.interest_days_between(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(29, Util.interest_days_between(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 2)));
		assertEquals(28, Util.interest_days_between(Util.Interest_basis.By_Actual_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(28, Util.interest_days_between(Util.Interest_basis.By_Actual_Actual,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
    }
    @Test
	public void test_interest_days_between__misc_ice_types() {
		Util.interest_days_between(Util.Interest_basis.By_Actual_Actual,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Actual_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Actual_365_366_Leap_Year_ISDA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360_Compounded_Interest,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Future_Data_Not_Available,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Historical_Data_Not_Available,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360_ICMA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Actual_365_366_Leap_Year,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Actual_364,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Bus_252,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_365_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_Actual_Actual_ICMA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360_US,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360_US_NASD,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360_BMA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360_ISDA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360_IT,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30_360_SIA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30E_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30E_360_ISDA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_30E_360b,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.interest_days_between(Util.Interest_basis.By_NL_365_No_Leap_Year,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
    }
	@Test
	public void test_accrued_interest_by_day() {
		assertEquals(0.0111111111111111112, Util.daily_interest_rate(Util.Interest_basis.By_30_360, 4.0), MARGIN_FOR_ERROR);
		assertEquals(0.0111111111111111112, Util.daily_interest_rate(Util.Interest_basis.By_Actual_360,  4.0), MARGIN_FOR_ERROR);
		assertEquals(0.010958904, Util.daily_interest_rate(Util.Interest_basis.By_Actual_365,  4.0), MARGIN_FOR_ERROR);
		assertEquals(0.010958904, Util.daily_interest_rate(Util.Interest_basis.By_Actual_Actual,  4.0), MARGIN_FOR_ERROR);
    }
	@Test
	public void test_accrued_interest_by_day__misc_ice_types() {
		Util.daily_interest_rate(Util.Interest_basis.By_Actual_Actual, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Actual_360, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Actual_365, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Actual_365_366_Leap_Year_ISDA, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360_Compounded_Interest, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_365, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Future_Data_Not_Available, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Historical_Data_Not_Available, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360_ICMA, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Actual_365_366_Leap_Year, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Actual_364, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Bus_252, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_365_365, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_Actual_Actual_ICMA, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360_US, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360_US_NASD, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360_BMA, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360_ISDA, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360_IT, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30_360_SIA, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30E_360, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30E_360_ISDA, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_30E_360b, 4.0);
		Util.daily_interest_rate(Util.Interest_basis.By_NL_365_No_Leap_Year, 4.0);
    }
	@Test
	public void test_accrued_interest() {
		assertEquals(2.0, Util.accrued_interest_from_time_span(Util.Interest_basis.By_30_360, 0.04, 100, Util.date(2017, 1, 1), Util.date(2017, 7, 1)), 0);
	}
	
	public void test_accrued_interest3() {
		expected_result("test_accrued_interest3", Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 0.25, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 8.33333333333333);
	}
	@Test
	public void test_accrued_interest4() {
		assertEquals(8.33333333333, Util.accrued_interest_from_time_span(Util.Interest_basis.By_30_360, 0.25, 100, Util.date(2015, 11, 21), Util.date(2016, 3, 21)), MARGIN_FOR_ERROR);
	}
	@Test
	public void test_accrued_interest6() {
		assertEquals(8.33333333333, Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 0.25, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), MARGIN_FOR_ERROR);
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
		assertEquals(2.5, Util.accrued_interest_from_time_span(Util.Interest_basis.By_30_360, 0.05, 100, Util.date(2017, 1, 1), Util.date(2017, 7, 1)), MARGIN_FOR_ERROR);
	}
    @Test
	public void test_accrued_interest_zero_when_settlement_coincides_w_payment_date() {
		assertEquals(0, Util.accrued_interest_from_time_span(Util.Interest_basis.By_30_360, 0.05, 100, Util.date(2017, 7, 1), Util.date(2017, 7, 1)), MARGIN_FOR_ERROR);
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
	@Test
	public void test_ice_enum_vals() {
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Weekly, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_2_years, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_3_years, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_3_years, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_5_years, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_5_years, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_8_years, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Biweekly, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Biweekly, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Daily, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Term_mode, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Interest_at_maturity, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Bimonthly, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Bimonthly, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Irregular, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_28_days, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_28_days, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_26_weeks, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_26_weeks, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Tied_to_prime, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.One_time, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_10_years, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.One_time, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_52_weeks, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.When_interest_adjusts_commercial_paper, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Zero_coupon, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Certain_years_only, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Under_certain_circumstances, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Every_15_years, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Custom, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
		Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(Bond_frequency_type.Single_Interest_Payment, Util.date(2017, 7, 6), Util.date(2017, 7, 16));
	}
    @Test
	public void test_yield_to_worst__is_maturity() {
        Date maturity = Util.date(2019, 5, 15);
        Date settlement = Util.date(2016, 5, 15);
        Date c1 = Util.date(2017, 5, 15);
        Date c2 = Util.date(2018, 5, 15);
        double ytm = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 106, settlement, maturity);
        double ytc1 = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 106, settlement, c1);
        double ytc2 = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 106, settlement, c2);
        Date[] call_dates = { c1, c2 };
		double ytw = Util.yield_to_worst(Util.Bond_frequency_type.SemiAnnual,  Util.Interest_basis.By_30_360, 100, 0.07, 106, settlement, maturity, call_dates);
		assertTrue("ytw < ytc1", ytw < ytc1);
		assertTrue("ytw < ytc2", ytw < ytc2);
		assertEquals(ytm, ytw, UtilTest.MARGIN_FOR_ERROR);
    }

    @Test
	public void test_yield_to_worst__c1() {
        Date maturity = Util.date(2020, 5, 15);
        Date settlement = Util.date(2016, 5, 15);
        Date c1 = Util.date(2017, 5, 15);
        Date c2 = Util.date(2018, 5, 15);
        double ytm = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, maturity);
        double ytc1 = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, c1);
        double ytc2 = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, c2);
		Date[] call_dates = { c1, c2 };
		double ytw = Util.yield_to_worst(Util.Bond_frequency_type.SemiAnnual,  Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, maturity, call_dates);
		assertTrue("ytw < ytc2", ytw < ytc2);
		assertTrue(ytw < ytm);
		assertEquals(ytc1, ytw, UtilTest.MARGIN_FOR_ERROR);
    }

    @Test
	public void test_yield_to_worst__c2_because_c1_precedes_settlement() {
        Date maturity = Util.date(2020, 5, 15);
        Date settlement = Util.date(2017, 5, 16);
        Date c1 = Util.date(2017, 5, 15);
        Date c2 = Util.date(2018, 5, 15);
        double ytm = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, maturity);
        try {
        	Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, c1);
        	fail("should have thrown an exception for settlement following c1");
        }
        catch(RuntimeException e) {
        	// expected
        }
        double ytc2 = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, c2);
		Date[] call_dates = { c1, c2 };
		double ytw = Util.yield_to_worst(Util.Bond_frequency_type.SemiAnnual,  Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, maturity, call_dates);
		assertTrue(ytw < ytm);
		assertEquals(ytc2, ytw, UtilTest.MARGIN_FOR_ERROR);
    }

    @Test
	public void test_yield_to_worst__maturity_because_call_dates_are_past() {
        Date maturity = Util.date(2020, 5, 15);
        Date settlement = Util.date(2020, 3, 15);
        Date c1 = Util.date(2017, 5, 15);
        Date c2 = Util.date(2018, 5, 15);
        try {
        	Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, c1);
        	fail("should have thrown an exception for settlement following c1");
        }
        catch(RuntimeException e) {
        	// expected
        }
        try {
        	Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, c2);
        	fail("should have thrown an exception for settlement following c2");
        }
        catch(RuntimeException e) {
        	// expected
        }
        double ytm = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, maturity);
        Date[] call_dates = { c1, c2 };
		double ytw = Util.yield_to_worst(Util.Bond_frequency_type.SemiAnnual,  Util.Interest_basis.By_30_360, 103, 0.07, 100, settlement, maturity, call_dates);
		assertEquals(ytm, ytw, UtilTest.MARGIN_FOR_ERROR);
    }

    @Test
	public void test_round_to_cent() {
        assertEquals(0, Util.round_to_cent(0), UtilTest.MARGIN_FOR_ERROR);
        assertEquals(0.02, Util.round_to_cent(0.02), UtilTest.MARGIN_FOR_ERROR);
        assertEquals(0.03, Util.round_to_cent(0.034), UtilTest.MARGIN_FOR_ERROR);
        assertEquals(0.04, Util.round_to_cent(0.035), UtilTest.MARGIN_FOR_ERROR);
        assertEquals(-0.02, Util.round_to_cent(-0.02), UtilTest.MARGIN_FOR_ERROR);
        assertEquals(-0.03, Util.round_to_cent(-0.034), UtilTest.MARGIN_FOR_ERROR);
        assertEquals(-0.04, Util.round_to_cent(-0.035), UtilTest.MARGIN_FOR_ERROR);
    }

    @Test
	public void test_accrued_interest2() {
    	// verified at http://apps.finra.org/Calcs/1/AccruedInterest
    	assertEquals(6.41666666666666, Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 0.07, 100, Util.date(2015, 10, 21), Util.date(2016, 11, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    @Test
	public void test_annual12_partial__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_annual12_partial();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly3_at_par__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_monthly3_at_par();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly3__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_monthly3();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_monthly();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly_partial__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_monthly_partial();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_quarterly__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_quarterly();
        }
        finally {
            pop_mode();
        }
    }

    @Test
	public void test_semiannual__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_semiannual();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_quarterly_simple__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_quarterly_simple();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_semiannual_below_par__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_semiannual_below_par();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_semiannual_simple__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_semiannual_simple();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual_multiyear_par1000__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_annual_multiyear_par1000();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual_multiyear__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_annual_multiyear();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_annual();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_misc_ice_freq_types__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_misc_ice_freq_types();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_accrued_interest3__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_accrued_interest3();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_quarterly_partial__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_quarterly_partial();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_semiannual_partial__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_semiannual_partial();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_semiannual_partial_at_par__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_semiannual_partial_at_par();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_semiannual_partial_at_par2__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_semiannual_partial_at_par2();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_annual_act360_v_act365__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_annual_act360_v_act365();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_semiannual_partial_at_par3__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_semiannual_partial_at_par3();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_annual_partial_at_par2__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_annual_partial_at_par2();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual12_partial__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_annual12_partial();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly3_at_par__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_monthly3_at_par();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly3__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_monthly3();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_monthly();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly_partial__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_monthly_partial();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_quarterly__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_quarterly();
        }
        finally {
            pop_mode();
        }
    }

    @Test
	public void test_semiannual__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_semiannual();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_quarterly_simple__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_quarterly_simple();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_semiannual_below_par__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_semiannual_below_par();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_semiannual_simple__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_semiannual_simple();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual_multiyear_par1000__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_annual_multiyear_par1000();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual_multiyear__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_annual_multiyear();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_annual();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_misc_ice_freq_types__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_misc_ice_freq_types();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_accrued_interest3__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_accrued_interest3();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_quarterly_partial__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_quarterly_partial();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_semiannual_partial__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_semiannual_partial();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_semiannual_partial_at_par__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_semiannual_partial_at_par();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_semiannual_partial_at_par2__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_semiannual_partial_at_par2();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_annual_act360_v_act365__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_annual_act360_v_act365();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_semiannual_partial_at_par3__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_semiannual_partial_at_par3();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_annual_partial_at_par2__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_annual_partial_at_par2();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_zero_coupon__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_zero_coupon();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_zero_coupon__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_zero_coupon();
        }
        finally {
            pop_mode();
        }
    }
	@Test	
	// For neg coupon rate, FtLabs throws a CalculationException, advising that ZeroCouponSecurity should be used, but that instrument only supports zero coupon rate (not neg)
	// Currently I intercept the call and resend to Monroe, even if we are in FTlabs mode
	public void test_negative_coupon__FtLabs() {
        push_mode(Util.Calculator_mode.FtLabs);
        try {
            test_negative_coupon();
        }
        finally {
            pop_mode();
        }
    }
	@Test
	public void test_negative_coupon__Monroe() {
        push_mode(Util.Calculator_mode.Monroe);
        try {
            test_negative_coupon();
        }
        finally {
            pop_mode();
        }
    }
	public void test_n() {
        // for the cumulative testing, set default to be Monroe:
        Util.calculator_mode = Calculator_mode.Monroe;
        test_n_interest_basis(Bond_frequency_type.Annual);
        test_n_interest_basis(Bond_frequency_type.SemiAnnual);
        test_n_interest_basis(Bond_frequency_type.Quarterly);
        test_n_interest_basis(Bond_frequency_type.Monthly);
        int success = test_count - fail_count;
        System.out.printf("%d/%d = %2f", success, test_count, (float)success / (float)test_count);
        System.out.println("% success");
    }
	public void test_n_interest_basis(Bond_frequency_type frequency_type) {
		test_n_par(frequency_type, Util.Interest_basis.By_30_360);
        test_n_par(frequency_type, Util.Interest_basis.By_Actual_360);
        test_n_par(frequency_type, Util.Interest_basis.By_Actual_365);
        test_n_par(frequency_type, Util.Interest_basis.By_Actual_Actual);
    }
	private void test_n_par(Bond_frequency_type frequency_type, Interest_basis interest_basis) {
        test_n_coupon_rates(frequency_type, interest_basis, 100);
        test_n_coupon_rates(frequency_type, interest_basis, 1000);
        test_n_coupon_rates(frequency_type, interest_basis, 25);
        test_n_coupon_rates(frequency_type, interest_basis, 50);
	}
	private void test_n_coupon_rates(Bond_frequency_type frequency_type, Interest_basis interest_basis, int par) {
        test_n_clean_price(frequency_type, interest_basis, par, 0.04);
        test_n_clean_price(frequency_type, interest_basis, par, 0.015);
        test_n_clean_price(frequency_type, interest_basis, par, 0.0825);
        test_n_clean_price(frequency_type, interest_basis, par, 0.12);
        test_n_clean_price(frequency_type, interest_basis, par, 0.215);
    }
	private void test_n_clean_price(Bond_frequency_type frequency_type, Interest_basis interest_basis, int par, double coupon_rate) {
        test_n_dates(frequency_type, interest_basis, 71.5, coupon_rate, par);
        test_n_dates(frequency_type, interest_basis, 82, coupon_rate, par);
        test_n_dates(frequency_type, interest_basis, 95, coupon_rate, par);
        test_n_dates(frequency_type, interest_basis, 100, coupon_rate, par);
        test_n_dates(frequency_type, interest_basis, 107, coupon_rate, par);
        test_n_dates(frequency_type, interest_basis, 126.2, coupon_rate, par);
    }
	private void test_n_dates(Bond_frequency_type frequency_type, Interest_basis interest_basis, double clean_price, double coupon_rate, int par) {
        test_n_days(frequency_type, interest_basis, clean_price, coupon_rate, par, Util.date(2016, 2, 27), Util.date(2016, 3, 2), UtilTest.variations);
        test_n_days(frequency_type, interest_basis, clean_price, coupon_rate, par, Util.date(2012, 2, 27), Util.date(2017, 3, 2), UtilTest.variations);
        test_n_days(frequency_type, interest_basis, clean_price, coupon_rate, par, Util.date(2008, 2, 27), Util.date(2020, 3, 2), UtilTest.variations);
        test_n_days(frequency_type, interest_basis, clean_price, coupon_rate, par, Util.date(2025, 2, 27), Util.date(2040, 3, 2), UtilTest.variations);
    }
	public void test_n_days(Bond_frequency_type frequency_type, Util.Interest_basis interest_basis, double clean_price, double coupon_rate, int par, Date settlement_date, Date maturity_date, int days_to_repeat) {
        for (int k = 0; k < days_to_repeat; k++) {
            for (int j = 0; j < days_to_repeat; j++) {
                double expected_accrued_interest = Util.accrued_interest_at_settlement(frequency_type, interest_basis, coupon_rate, par, settlement_date, maturity_date);
                boolean eomAdjust = true;
                double expected_ytm;
                try {
                	expected_ytm = FtLabs.yield_to_maturity_static(frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date, eomAdjust);
                }
                catch (Exception e) {
                	String emsg = e.getMessage();
                	if (emsg.contains("Invalid EOM Adjust setting for this Security. EOMAdjust cannot be enabled for this Security")) {
                		eomAdjust = false;
                    	expected_ytm = FtLabs.yield_to_maturity_static(frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date, eomAdjust);
                    }
                	else {
                		throw new RuntimeException(e);
                	}
                }
                if (UtilTest.recording) {
                    if (UtilTest.recording_output==null) {
                        UtilTest.recording_output = make_recording_output_file();
                    }
                    // frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date, yield, expected_ytm
                    String s = frequency_type.toString() + "," + interest_basis.toString() + "," + clean_price + "," + coupon_rate + "," + par + "," 
                    + Util.dateToString(settlement_date) + "," + Util.dateToString(maturity_date) + "," + eomAdjust + "," + expected_ytm + "\n";
                    UtilTest.recording_output.print(s);
                }
                expected_yield("", frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date, expected_accrued_interest, expected_ytm);
                maturity_date = Util.addDay(maturity_date);
            }
            settlement_date = Util.subtractDay(settlement_date);
        }
	}

	private PrintWriter make_recording_output_file() {
		try {
			FileWriter fw = new FileWriter("c:/Users/nelsons/Dropbox/data/yield_record.csv", true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter out = new PrintWriter(bw);
			return out;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static void performance_comparison() {
        // currently unused
        System.out.println("main will cf pfr between bond_metrics and OAS");
        double start;
        double end;

        int op_cnt = 70000;

        double coupon_rate = 0.2;
        Date settlement = Util.date(2016, 3, 21);
        Date maturity = Util.date(2017, 3, 21);
        start = System.currentTimeMillis();
        for (int j = 0; j < op_cnt; j++) {
            FtLabs.yield_to_maturity_static(Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 84.0, coupon_rate, 100, settlement, maturity);
            coupon_rate += 0.0000001;
        }
        end = System.currentTimeMillis();
        double t = end - start;
        if (t==0) {
            t = 1;
        }
        System.out.println("main: FtLabs.yield_to_maturity " + (1000 * op_cnt / t) + " ops/sec");

        coupon_rate = 0.2;
        start = System.currentTimeMillis();
        for (int j = 0; j < op_cnt; j++) {
            Util.yield_to_maturity(Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 84.0, coupon_rate, 100, settlement, maturity);
            coupon_rate += 0.00000001;
        }
        end = System.currentTimeMillis();
        t = end - start;
        if (t==0) {
            t = 1;
        }
        System.out.println("main: Util.yield_to_maturity " + (1000 * op_cnt / t) + " ops/sec");
    }

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
	        public void run() {
	        	if (UtilTest.recording_output != null) {
	        		UtilTest.recording_output.flush();
	        		UtilTest.recording_output.close();
	        	}
	        }
	    }, "Shutdown-thread"));		
		
        running_as_java_app = true;
        UtilTest ut = new UtilTest();
        
        if (args.length > 0) {
        	UtilTest.variations =  Integer.parseInt(args[0]);
        }
        UtilTest.recording  = true;
        
        ut.test_n();
        
    }
	public void test_examine() {
		Bond_frequency_type frequency_type = Bond_frequency_type.Monthly;
		Util.Interest_basis interest_basis = Util.Interest_basis.By_30_360;
		double clean_price = 95;
		double coupon_rate = 0.07;
		int par = 100;
		Date settlement_date = Util.date(2016,  3, 27);
        Date maturity_date   = Util.date(2016, 11, 21);
        int days_to_repeat = 100;
        for (int j = 0; j < days_to_repeat; j++) {
        	double expected_ytm = FtLabs.yield_to_maturity_static(frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date);
        	double expected_accrued_interest     = Util.accrued_interest_at_settlement(frequency_type, interest_basis, coupon_rate, par, settlement_date, maturity_date);
        	double expected_accrued_interest_ft = FtLabs.accrued_interest_at_settlement_static(frequency_type, interest_basis, coupon_rate, par, settlement_date, maturity_date);
        	if (!equalish(expected_accrued_interest_ft, expected_accrued_interest)) {
                System.out.printf("accrued interest mismatch -- expected %f but saw %f", expected_accrued_interest_ft, expected_accrued_interest);
            }
            //		   	System.out.printf("%f (%f): ", expected_ytm, (expected_ytm - last_expected));
            expected_yield("", frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date, expected_accrued_interest, expected_ytm);
            settlement_date = Util.addDay(settlement_date);
        }
	}
}
