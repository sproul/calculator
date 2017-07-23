package bondmetrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.junit.Test;

import bondmetrics.Util.Bond_frequency_type;

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
    static Util.Calculator_mode pushed_mode = Util.Calculator_mode.None;
	public static final int DEFAULT_PAR = 100;
	private static final double MARGIN_FOR_ERROR = 0.00005;
	
    static boolean equalish(double a, double b) {
        double diff = a - b;
        return (Math.abs(diff) < MARGIN_FOR_ERROR);
    }
    static void expected_result(String test_name, double actual, double fidelity_expected, Double bondOASexpected, Double bond_metrics_expected, Double ftLabs_expected) {
        if (!equalish(fidelity_expected, actual)) {
        	System.out.println(test_name + "." + Util.calculator_mode_toString(Util.calculator_mode) +  "/fidelity disagreeement (" + actual + "/" + fidelity_expected + ")");
        }
        switch (Util.calculator_mode) {
        case BondOAS:
        	if (bondOASexpected != null) {
        		assertEquals(bondOASexpected, actual, MARGIN_FOR_ERROR);
        		return;
        	}
        	break;
        case FtLabs:
        	if (ftLabs_expected != null) {
        		assertEquals(ftLabs_expected, actual, MARGIN_FOR_ERROR);
        		return;
        	}
        	break;
        case Monroe:
        	if (bond_metrics_expected != null) {
        		assertEquals(bond_metrics_expected, actual, MARGIN_FOR_ERROR);
        		return;
        	}
        	break;
        case None:
            throw new RuntimeException("invalid mode of None");
        default:
            throw new RuntimeException("bad mode");
        }
        assertEquals(fidelity_expected, actual, MARGIN_FOR_ERROR);
    }

	static void expected_result(String test_name, double actual, double fidelity_expected, Double bondOASexpected, Double bond_metrics_expected) {
        expected_result(test_name, actual, fidelity_expected, bondOASexpected, bond_metrics_expected, null);
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
	public void test_fractional_payment_periods() {
        assertEquals(0.666666666666666666667, 
        		Util.fractional_number_of_payment_periods_between(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, Util.date(2017, 3, 21), Util.date(2017, 11, 21)), 
        		MARGIN_FOR_ERROR);
    }
    
	@Test
	public void test_approx() {
		assertEquals(0.084615, Util.yield_to_maturity_approximate(1, 4, 0.07 * 100, 100,  95), UtilTest.MARGIN_FOR_ERROR);
    }
    
    
	@Test
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
    
    
	@Test
	public void test_annual() {
        assertEquals(0.05,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.05, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
		assertEquals(0.05,  Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.05, 100, Util.date(2016, 3, 21), Util.date(2029, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    
    
	@Test
	public void test_annual_multiyear() {
		expected_result("test_annual_multiyear", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 95,   0.07,  100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), 0.085274, 0.085007);
		expected_result("test_annual_multiyear", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 72,   0.2,   100, Util.date(2016, 3, 21), Util.date(2018, 3, 21)), 0.437332, 0.420547);
    }
    
    
	@Test
	public void test_annual_multiyear_par1000() {
		expected_result("test_annual_multiyear_par1000", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 950, 0.07, 1000, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), 0.085274, -0.466982);
    }
    
    
	@Test
	public void test_semiannual_simple() {
		assertEquals(0.2, Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
        assertEquals(0.07, Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 5, 21), Util.date(2016, 11, 21)), UtilTest.MARGIN_FOR_ERROR); // accrued 0
    }
	
    
	@Test
	public void test_semiannual_below_par() {
        expected_result("test_semiannual_below_par", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 84, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), 0.41083);
    }
    
	@Test
	public void test_quarterly_simple() {
		assertEquals(0.2,  Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, Util.Interest_basis.By_30_360, 100, 0.2, 100, Util.date(2016, 3, 21), Util.date(2017, 3, 21)), UtilTest.MARGIN_FOR_ERROR);
    }

    
	@Test
	public void test_semiannual() {
        assertEquals(0.085,  Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), UtilTest.MARGIN_FOR_ERROR);    // ft 0.08501
    }
    
    
	@Test
	public void test_quarterly() {
        expected_result("test_quarterly", Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), 0.08487, 0.085007);
    }
    
    
	@Test
	public void test_monthly() {
		expected_result("test_monthly", Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2020, 3, 21)), 0.084786, 0.085007);
    }

	@Test
	public void test_quarterly_partial__OAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_quarterly_partial();
        }
        finally {
            pop_mode();
        }
    }
    
    @Test
	public void test_monthly_partial() {
		expected_result("test_monthly_partial", Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 27), Util.date(2016, 11, 21)), 0.1512, 0.15421442165220767, 0.133902);
    }

	@Test
	public void test_quarterly_partial() {
		expected_result("test_quarterly_partial", Util.yield_to_maturity(Util.Bond_frequency_type.Quarterly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 5, 15), Util.date(2016, 11, 21)), 0.1732, 0.17551058902457783, 0.17073);
    }

    @Test
	public void test_semiannual_partial() {
		expected_result("test_semiannual_partial", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 5, 15), Util.date(2016, 11, 21)), 0.1753, 0.17551058902457783, 0.1689);
    }
    
    @Test
	public void test_semiannual_partial_at_par() {
		expected_result("test_semiannual_partial_at_par", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2017, 7, 19), Util.date(2017, 7, 25)), 0.0677, 0.06965, 0.06965);
    }

    @Test
	public void test_semiannual_partial_at_par2() {
		expected_result("test_semiannual_partial_at_par2", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 5, 15), Util.date(2016, 11, 21)), 0.07, 0.068326, 0.0683226);
    }

    @Test
	public void test_semiannual_partial_at_par3() {
        expected_result("test_semiannual_partial_at_par3", Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 10, 6), Util.date(2016, 11, 21)), 0.0682, 0.6634216, 0.066342);
    }

    @Test
	public void test_annual12_partial() {
		expected_result("test_annual12_partial", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.12, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.115384, 0.11302983, 0.11303);
        // fidelity 4.00 accrued
	}

	@Test
	public void test_annual_partial_at_par2() {
		expected_result("test_annual_partial_at_par2", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.0684, 0.06706, 0.06706);
        // fidelity 2.33 accrued
	}
	@Test
	public void test_annual_partial_at_par() {
		expected_result("test_annual_partial_at_par", Util.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.25, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.2452, null, 0.226111823848897);
        // fidelity 33.33 accrued
	}
    @Test
	public void test_semiannual_partial__OAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_semiannual_partial();
        }
        finally {
            pop_mode();
        }
    }
    
    
	@Test
	public void test_monthly3() {
		expected_result("test_monthly3", Util.yield_to_maturity(Util.Bond_frequency_type.Monthly, Util.Interest_basis.By_30_360, 95, 0.07, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0.14926, 0.1521357);
    }

    
	@Test
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
	public void test_accrued_interest_days() {
		assertEquals(29, Util.accrued_interest_days(Util.Interest_basis.By_30_360, Util.date(2017, 2, 2), Util.date(2017, 3, 1)));
		assertEquals(30, Util.accrued_interest_days(Util.Interest_basis.By_30_360, Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(31, Util.accrued_interest_days(Util.Interest_basis.By_30_360, Util.date(2017, 2, 1), Util.date(2017, 3, 2)));
		assertEquals(27, Util.accrued_interest_days(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 2), Util.date(2017, 3, 1)));
		assertEquals(28, Util.accrued_interest_days(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(29, Util.accrued_interest_days(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 2)));
		assertEquals(28, Util.accrued_interest_days(Util.Interest_basis.By_Actual_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
		assertEquals(28, Util.accrued_interest_days(Util.Interest_basis.By_Actual_Actual,  Util.date(2017, 2, 1), Util.date(2017, 3, 1)));
    }
    @Test
	public void test_accrued_interest_days__misc_ice_types() {
		Util.accrued_interest_days(Util.Interest_basis.By_Actual_Actual,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Actual_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Actual_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Actual_365_366_Leap_Year_ISDA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360_Compounded_Interest,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Future_Data_Not_Available,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Historical_Data_Not_Available,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360_ICMA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Actual_365_366_Leap_Year,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Actual_364,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Bus_252,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_365_365,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_Actual_Actual_ICMA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360_US,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360_US_NASD,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360_BMA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360_ISDA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360_IT,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30_360_SIA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30E_360,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30E_360_ISDA,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_30E_360b,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
		Util.accrued_interest_days(Util.Interest_basis.By_NL_365_No_Leap_Year,  Util.date(2017, 2, 1), Util.date(2017, 3, 1));
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
	@Test
	public void test_accrued_interest3() {
		expected_result("test_accrued_interest3", Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_Actual_360, 0.25, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 8.33, null, null, 8.27);
	}
	@Test
	public void test_accrued_interest4() {
		assertEquals(8.33, Util.accrued_interest_from_time_span(Util.Interest_basis.By_30_360, 0.25, 100, Util.date(2015, 11, 21), Util.date(2016, 3, 21)), 0);
	}
	@Test
	public void test_accrued_interest6() {
		assertEquals(8.33, Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 0.25, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21)), 0);
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
	public void test_OASdate_conversion() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
    	BondOASwrapper.init();
        Date jdate = Util.date(2017, 7, 16);
        com.kalotay.akalib.Date oasDate = new com.kalotay.akalib.Date(2017, 7, 16);
        com.kalotay.akalib.Date oasDate2 = BondOASwrapper.date_to_OASdate(jdate);
        assertEquals(oasDate.YearOf(), oasDate2.YearOf());
        assertEquals(oasDate.MonthOf(), oasDate2.MonthOf());
        assertEquals(oasDate.DayOf(), oasDate2.DayOf());
    }
    //@Test
	public void test_yield_to_worst__c1() {
        Date maturity = Util.date(2016, 11, 21);
        Date settlement = Util.date(2016, 5, 15);
        Date c1 = Util.date(2017, 5, 15);
        Date c2 = Util.date(2018, 5, 15);
        double ytm = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, settlement, maturity);
        double ytc1 = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, settlement, c1);
        double ytc2 = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, settlement, c2);
		assertEquals(0.179, ytm, UtilTest.MARGIN_FOR_ERROR);
		assertEquals(-1, ytc1, UtilTest.MARGIN_FOR_ERROR);
		assertEquals(-1, ytc2, UtilTest.MARGIN_FOR_ERROR);
        Date[] call_dates = { c1, c2 };
		double ytw = Util.yield_to_worst(Util.Bond_frequency_type.SemiAnnual,  Util.Interest_basis.By_30_360, 95, 0.07, 100, settlement, maturity, call_dates);
		assertEquals(ytw, ytc1, UtilTest.MARGIN_FOR_ERROR);
		assertTrue("ytw < ytc2", ytw < ytc2);
		assertTrue(ytw < ytm);
    }

    //@Test
	public void test_yield_to_worst__c2_because_c1_precedes_settlement() {
        Date maturity = Util.date(2016, 11, 21);
        Date settlement = Util.date(2016, 5, 15);
        Date c1 = Util.date(2015, 5, 15);
        Date c2 = Util.date(2018, 5, 15);
        double ytm = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, settlement, maturity);
        double ytc1 = 0; // can't legally calculate yield when the date is before settlement
        double ytc2 = Util.yield_to_maturity(Util.Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 95, 0.07, 100, settlement, c2);
		assertEquals(0.179, ytm, UtilTest.MARGIN_FOR_ERROR);
		assertEquals(-1, ytc1, UtilTest.MARGIN_FOR_ERROR);
		assertEquals(-1, ytc2, UtilTest.MARGIN_FOR_ERROR);
        Date[] call_dates = { c1, c2 };
		double ytw = Util.yield_to_worst(Util.Bond_frequency_type.SemiAnnual,  Util.Interest_basis.By_30_360, 95, 0.07, 100, settlement, maturity, call_dates);
		assertEquals(ytw, ytc1, UtilTest.MARGIN_FOR_ERROR);
		assertTrue("ytw < ytc2", ytw < ytc2);
		assertTrue(ytw < ytm);
    }

    //@Test
	public void test_yield_to_worst__c2() {
		fail(); // rewrite for c2
    }
    
    //@Test
	public void test_yield_to_worst__ytm() {
    	Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 0.07, 100, Util.date(2015, 10, 21), Util.date(2016, 11, 21)); // 6.475
		assertTrue(false); // rewrite for ytm
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
    	assertEquals(6.42, Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 0.07, 100, Util.date(2015, 10, 21), Util.date(2016, 11, 21)), UtilTest.MARGIN_FOR_ERROR);
    }
    @Test
	public void test_annual12_partial__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_annual12_partial();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly3_at_par__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_monthly3_at_par();
        }
        finally {
            pop_mode();
        }
    }
    @Test
      public void test_monthly3__bondOAS() {
          if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
          push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_monthly3();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_monthly();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_monthly_partial__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_monthly_partial();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_quarterly__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_quarterly();
        }
        finally {
            pop_mode();
        }
    }
    
    @Test
	public void test_semiannual__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_semiannual();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_quarterly_simple__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_quarterly_simple();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_semiannual_below_par__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_semiannual_below_par();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_semiannual_simple__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_semiannual_simple();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual_multiyear_par1000__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_annual_multiyear_par1000();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual_multiyear__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_annual_multiyear();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_annual();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_misc_ice_freq_types__bondOAS() {
        if (!BondOASwrapper.bondOAS_library_is_available) {
            return;
        }
        push_mode(Util.Calculator_mode.BondOAS);
        try {
            test_misc_ice_freq_types();
        }
        finally {
            pop_mode();
        }
    }
    @Test
	public void test_annual12_partial__ftLabs() {
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

	public static void main(String[] args) {
		if (args.length!=0) {
			System.out.println("main will cf pfr between bond_metrics and OAS");
			if (!BondOASwrapper.bondOAS_library_is_available) {
				System.out.println("main: OAS not available...");
				System.out.println("main: OAS not available...");
				System.out.println("main: OAS not available...");
				System.out.println("main: OAS not available...");
				System.out.println("main: OAS not available...");
				return;
			}
			double start;
			double end;

			int op_cnt = 70000;

			start = System.currentTimeMillis();
			double coupon_rate = 0.2;
			Date settlement = Util.date(2016, 3, 21);
			Date maturity = Util.date(2017, 3, 21);
			for (int j = 0; j < op_cnt; j++) {
				BondOASwrapper.yield_to_maturity(Bond_frequency_type.SemiAnnual, 84.0, coupon_rate, 100, settlement, maturity);
				coupon_rate += j * 0.00001;
			}
			end = System.currentTimeMillis();
			double t = end - start;
			if (t==0) {
				t = 1;
			}
			System.out.println("main: BondOASwrapper.yield_to_maturity " + (1000 * op_cnt / t) + " ops/sec");

			start = System.currentTimeMillis();
			for (int j = 0; j < op_cnt; j++) {
				Util.yield_to_maturity(Bond_frequency_type.SemiAnnual, Util.Interest_basis.By_30_360, 84.0, 0.2, 100, settlement, maturity);
			}
			end = System.currentTimeMillis();
			t = end - start;
			if (t==0) {
				t = 1;
			}
			System.out.println("main: Util.yield_to_maturity " + (1000 * op_cnt / t) + " ops/sec");
		}
		else {
			UtilTest ut = new UtilTest();
			ut.test_annual_multiyear_par1000__FtLabs();
            /*
              double x33 = Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_Actual_360, 0.25, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21));
            System.out.println("main: x33=" + x33);
            double x27 = Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 0.25, 100, Util.date(2016, 3, 21), Util.date(2016, 11, 21));
            System.out.println("main: x27=" + x27);
            double x1 = Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_Actual_360, 0.25, 100, Util.date(2015, 3, 1), Util.date(2016, 2, 1));
            System.out.println("FTlabs 1.92: a/360=" + x1);
            double x2 = Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 0.25, 100, Util.date(2015, 3, 1), Util.date(2016, 2, 1));
            System.out.println("OK: 3/360=" + x2);
            x2 = Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_Actual_Actual, 0.25, 100, Util.date(2015, 3, 1), Util.date(2016, 2, 1));
            System.out.println("OK: a/a=" + x2);
            x2 = Util.accrued_interest_at_settlement(Bond_frequency_type.Annual, Util.Interest_basis.By_Actual_365, 0.25, 100, Util.date(2015, 3, 1), Util.date(2016, 2, 1));
            System.out.println("OK: a/365=" + x2);
            */
        }
    }
}
