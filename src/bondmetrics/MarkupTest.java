package bondmetrics;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import bondmetrics.Markup.Schedule;
import bondmetrics.Markup.Type;

public class MarkupTest {

	private static final double MARGIN_FOR_ERROR = 0.0001;
	private Type type;

	protected void setup_markup_from_input_csv(Markup.Type type, String csv)
	{
		this.type = type;
		Markup.load_markup_schedule(type, csv);
	}
	
	protected void setup_markup_from_input_csv(Markup.Type type)
	{
		this.type = type;
        Markup.load_markup_schedule(type);
	}
	
	protected void assert_exception_from_csv(Markup.Type security_type, String csv, String line_provoking_complaint)
    {
        try {
            Markup.load_markup_schedule(security_type, csv);
        }
		catch(RuntimeException e) {
 			String emsg = e.getMessage();
 			line_provoking_complaint = line_provoking_complaint.replaceAll("[ \t]", "");
            if (!emsg.contains(line_provoking_complaint)) {
                throw new RuntimeException("expected an error complaining about " + line_provoking_complaint + ", but got " + e + " instead");
            }
		}
	}
	
	protected void assert_exception_from(double price, int years_to_maturity) {
    	assert_exception_from(price, years_to_maturity, null);
    }

	protected void assert_exception_from(double price, int years_to_maturity, String rating_string) {
		try {
			assert_markup_is(price, years_to_maturity, rating_string, null);
		}
		catch(RuntimeException e) {
			// expected, so we can ignore it
		}
	}

	protected void assert_markup_is(double price, int years_to_maturity, double expected_marked_up_price) {
    	assert_markup_is(price, years_to_maturity, null, expected_marked_up_price);
    }

	protected void assert_markup_is(double price, int years_to_maturity, String rating_string, Double expected_marked_up_price) {
		Markup m = new Markup(price, this.type, make_date_n_years_from_now(years_to_maturity), rating_string);
        double actual_marked_up_price = m.calculate();
        if (expected_marked_up_price != null) {
        	assertEquals(expected_marked_up_price, actual_marked_up_price, MarkupTest.MARGIN_FOR_ERROR);
        }
	}
	
	protected void static_assert_markup_is(double price, int years_to_maturity, double expected_marked_up_price) {
    	static_assert_markup_is(price, years_to_maturity, null, expected_marked_up_price);
    }

	protected void static_assert_markup_is(double price, int years_to_maturity, String rating_string, Double expected_marked_up_price) {
        double actual_marked_up_price = Markup.calculate(price, this.type, make_date_n_years_from_now(years_to_maturity), rating_string);
        if (expected_marked_up_price != null) {
        	assertEquals(expected_marked_up_price, actual_marked_up_price, MarkupTest.MARGIN_FOR_ERROR);
        }
	}
	
	protected void static_assert_dollar_volume_markup_is(double price, int years_to_maturity, int number_of_securities, Double expected_marked_up_price) {
        double actual_marked_up_price = Markup.calculate(price, this.type, make_date_n_years_from_now(years_to_maturity), number_of_securities);
        if (expected_marked_up_price != null) {
        	assertEquals(expected_marked_up_price, actual_marked_up_price, MarkupTest.MARGIN_FOR_ERROR);
        }
	}
	
    @Test
	public void test_basic() {
        setup_markup_from_input_csv(Markup.Type.MUNI, 
                                    "irrelevant,1,2\n"
                                    + "AAA,1.1,1.2\n"
                                    + "A,2.1,2.2\n");
        assert_exception_from(101, 3, "AAA");
        assert_exception_from(101, 2, "AAAx");
        assert_markup_is(101, 1, "AAA", 101 + 1.1);
        assert_markup_is(101, 1, "A", 101 + 2.1);
        assert_markup_is(101, 2, "AAA", 101 + 1.2);
        assert_markup_is(101, 2, "A", 101 + 2.2);
    }
    
    private Date make_date_n_years_from_now(int n) {
    	Date now = new Date();
    	@SuppressWarnings("deprecation")
		Date d = new Date(now.getYear() + n, now.getMonth(), now.getDate());
		return d;
	}
    
	@Test
	public void test_disallow_wildcard_plus_another_irrelevant_column() {
        assert_exception_from_csv(Markup.Type.MUNI, 
                                  "irrelevant,1,*\n"
                                  + "A,2.1,2.2\n",
                                  "1,*");
    }
    
	@Test
	public void test_disallow_wildcard_plus_another_irrelevant_column_first() {
        assert_exception_from_csv(Markup.Type.MUNI, 
                                  "irrelevant,*,1\n"
                                  + "A,2.1,2.2\n",
                                  "*,1");
    }
    
	@Test
	public void test_disallow_embedded_single_quote() {
        assert_exception_from_csv(Markup.Type.MUNI,
                                  "irrelevant,2,1\n"
                                  + "A,2.1,'2.2', '3.2'\n",
                                  "A,2.1,'2.2', '3.2'");
    }
    
	@Test
	public void test_disallow_embedded_double_quote() {
        assert_exception_from_csv(Markup.Type.MUNI, 
                                  "irrelevant,2,1\n"
                                  + "A,2.1,\"2.2\", 3.2\n",
                                  "A,2.1,\"2.2\", 3.2");
    }
    
	@Test
	public void test_disallow_multiple_col_ge_thresholds() {
        assert_exception_from_csv(Markup.Type.MUNI, 
                                  "irrelevant,2+,1+\n"
                                  + "A,2.1,\"2.2\", 3.2\n",
                                  "2+,1+");
    }
    
	@Test
	public void test_disallow_multiple_row_ge_thresholds() {
        assert_exception_from_csv(Markup.Type.AGENCY, 
                                  "irrelevant,*\n"
                                  + "3+,2.1\n"
                                  + "4+,2.1\n",
                                  "4+,2.1");
    }
    
	@Test
	public void test_disallow_more_data_than_col_headers() {
        assert_exception_from_csv(Markup.Type.AGENCY, 
                                  "irrelevant,*\n"
                                  + "3,2.1,99\n",
                                  "3,2.1,99");
    }
    
	@Test
	public void test_disallow_less_data_than_col_headers() {
        assert_exception_from_csv(Markup.Type.AGENCY, 
                                  "irrelevant,1,2\n"
                                  + "3,2.1\n",
                                  "3,2.1");
    }
    
	@Test
	public void test_disallow_row_wildcard_plus_another_irrelevant_column() {
        assert_exception_from_csv(Markup.Type.MUNI, 
                                  "irrelevant,1\n"
                                    + "A,2.1\n"
                                    + "*,2.1\n",
                                  "*,2.1");
    }
    
    
	@Test
	public void test_disallow_row_wildcard_plus_another_irrelevant_column_first() {
        assert_exception_from_csv(Markup.Type.MUNI, 
                                  "irrelevant,1,2\n"
                                  + "*,2.1,2.2\n"
                                  + "A,2.1,2.2\n",
                                  "A,2.1,2.2");
    }
    
	@Test
	public void test_expanded_cols() {
        setup_markup_from_input_csv(Markup.Type.MUNI, 
                                    "irrelevant,1,2-4\n"
                                    + "AAA,1.1,1.2\n"
                                    + "A,2.1,2.2\n");
        assert_markup_is(101, 4, "AAA", 101 + 1.2);
        assert_markup_is(101, 1, "AAA", 101 + 1.1);
        assert_markup_is(101, 1, "A", 101 + 2.1);
        assert_markup_is(101, 2, "AAA", 101 + 1.2);
        assert_markup_is(101, 2, "A", 101 + 2.2);
        assert_markup_is(101, 3, "AAA", 101 + 1.2);
        assert_markup_is(101, 3, "A", 101 + 2.2);
        assert_markup_is(101, 4, "A", 101 + 2.2);
    }

    @Test
	public void test_wildcard_col() {
    	setup_markup_from_input_csv(Markup.Type.AGENCY, 
                                    	"irrelevant,10,11,12\n"
                                    		+ "*, 10.1,11.1,12.1\n");
        assert_markup_is(101, 10, 101 + 10.1);
        assert_markup_is(101, 11, 101 + 11.1);
        assert_markup_is(101, 12, 101 + 12.1);
    }
    
	@Test
	public void test_wildcard_col_and_row_range() {
        setup_markup_from_input_csv(Markup.Type.AGENCY, 
                                    "irrelevant,10,11-13\n"
        		+"*,1.4,2.4\n");
        assert_markup_is(101, 10, 101 + 1.4);
        assert_markup_is(101, 11, 101 + 2.4);
        assert_markup_is(101, 12, 101 + 2.4);
        assert_markup_is(101, 13, 101 + 2.4);
    }
    
    @Test
	public void test_wildcard_col_and_row_ge_threshold() {
        setup_markup_from_input_csv(Markup.Type.AGENCY, 
                                    "irrelevant,10,11+\n"
        		+ "*, 1.4,2.4\n");
        assert_markup_is(101, 10, 101 + 1.4);
        assert_markup_is(101, 11, 101 + 2.4);
        assert_markup_is(101, 12, 101 + 2.4);
    }

    @Test
	public void test_flat_markup() {
        setup_markup_from_input_csv(Markup.Type.TREASURY, 
                                    "irrelevant,*\n"
                                    + "*,2.2\n");
    }

    @Test
	public void test_greater_than_equal_with_range() {
        setup_markup_from_input_csv(Markup.Type.MUNI, 
                                    "irrelevant,10-12,13+\n"
        		+"*,98,99\n");
        assert_markup_is(101, 10, null, 101 + 98.0);
        assert_markup_is(101, 11, null, 101 + 98.0);
        assert_markup_is(101, 12, null, 101 + 98.0);
        assert_markup_is(101, 13, null, 101 + 99.0);
        assert_markup_is(101, 14, null, 101 + 99.0);
    }

    @Test
	public void test_matthew_v0_MUNI() {
        setup_markup_from_input_csv(Markup.Type.MUNI, 
                                    "Rating,                        1,         2,     3,     4,     5,  6-10,   11+\n"
                                    + "AAA,                       0.20,0.25,0.40,0.75,1.25,   1.75,   3.00\n"
                                    + "AAplus,                   0.25,0.30,0.50,0.95,1.75,   2.25,   5.00\n"
                                    + "AA,                          0.30,0.35,0.60,1.25,2.25,   2.50,   5.00\n"
                                    + "AAminus,                 0.35,0.40,0.70,1.50,2.75,   3.00,   5.00\n"
                                    + "Aplus,                      0.40,0.45,0.80,2.00,3.25,    3.50,   6.50\n"
                                    + "A,                             0.45,0.50,0.90,2.50,3.75,    4.00,   6.50\n"
                                    + "Aminus,                    0.50,0.55,1.00,2.75,4.25,    4.50,   6.50\n"
                                    + "BBBplus_or_below, 1.00,1.50,2.00,4.00,6.50,    7.00,  10.00\n"
                                    + "NONE,                       2.00,2.25,3.50,5.00,7.50,    7.50,  15.00\n");
        static_assert_markup_is(101, 1, "AAA", 101 + 0.20);
        static_assert_markup_is(101, 1, "Aplus", 101 + 0.40);
        static_assert_markup_is(101, 1, "BBBplus_or_below", 101 + 1.00);
        static_assert_markup_is(101, 1, "NONE", 101 + 2.00);
        static_assert_markup_is(101, 7, "AAA", 101 + 1.75);
        static_assert_markup_is(101, 8, "Aplus", 101 + 3.50);
        static_assert_markup_is(101, 9, "BBBplus_or_below", 101 + 7.00);
        static_assert_markup_is(101, 10, "NONE", 101 + 7.50);
        static_assert_markup_is(101, 11, "BBBplus_or_below", 101 + 10.00);
        static_assert_markup_is(101, 12, "NONE", 101 + 15.00);
    }

    @Test
	public void test_matthew_v1_MUNI() {
        setup_markup_from_input_csv(Markup.Type.MUNI, 
                                    "Rating,    0mo-1mo,  2-3mo,  4-6mo,  7-9mo,  10-23mo,2yr,    3yr,    4yr,    5yr,    6-10yr, 11-20yr,21yr+\n"
                                    + "AAA,     0.002,	0.005,	0.008,	0.01,	0.020,	0.025,	0.035,	0.050,	0.075,	0.125,	0.250,	0.500\n"
                                    + "AAplus, 0.005,	0.005,	0.008,	0.01,	0.025,	0.030,	0.050,	0.065,	0.095,	0.150,	0.300,	0.600\n"
                                    + "AA,        0.005,	0.008,	0.01,	0.01,	0.030,	0.035,	0.060,	0.075,	0.115,	0.175,	0.350,	0.700\n"
                                    + "AAminus, 0.005,	0.008,	0.01,	0.015,	0.035,	0.040,	0.070,	0.090,	0.135,	0.200,	0.400,	0.800\n"
                                    + "Aplus,	   0.008,	0.008,	0.02,	0.025,	0.040,	0.045,	0.080,	0.105,	0.165,	0.225,	0.450,	0.900\n"
                                    + "A,          0.008,	0.01,	0.02,	0.025,	0.045,	0.050,	0.090,	0.125,	1.950,	0.250,	0.500,	1.000\n"
                                    + "Aminus,	0.008,	0.01,	0.025,	0.025,	0.050,	0.055,	0.100,	0.150,	0.225,	0.275,	0.650,	1.300\n"
                                    + "NONE,	    0.01,	0.025,	0.05,	0.075,	0.100,	0.125,	0.175,	0.300,	0.500,	0.500,	1.000,	2.000\n");
        static_assert_markup_is(101, 1, "AAA", 101 + 0.020);
        static_assert_markup_is(101, 1, "Aplus", 101 + 0.040);
        static_assert_markup_is(101, 1, "AAplus", 101 + 0.025);
        static_assert_markup_is(101, 1, "NONE", 101 + 0.100);
        static_assert_markup_is(101, 7, "AAA", 101 + 0.125);
        static_assert_markup_is(101, 8, "Aplus", 101 + 0.225);
        static_assert_markup_is(101, 9, "AA",    101 + 0.175);
        static_assert_markup_is(101, 10, "NONE", 101 + 0.50);
        static_assert_markup_is(101, 11, "AAminus", 101 + 0.4);
        static_assert_markup_is(101, 12, "NONE", 101 + 1.00);
    }
    @Test
	public void test_matthew_v0_AGENCY() {
        setup_markup_from_input_csv(Markup.Type.AGENCY,
                                    "whatever,1,2,3,4,5,6-10,11+\r\n" + 
                                    "*,0.30,0.35,0.60,1.25,2.25,2.50,5.00\r\n");
        static_assert_markup_is(101, 1, 101 + 0.30);
        static_assert_markup_is(101, 7, 101 + 2.50);
        static_assert_markup_is(101, 8, 101 + 2.50);
        static_assert_markup_is(101, 9, 101 + 2.50);
        static_assert_markup_is(101, 10, 101 + 2.50);
        static_assert_markup_is(101, 11, 101 + 5.00);
        static_assert_markup_is(101, 12, 101 + 5.00);
    }

    
    @Test
	public void test_matthew_v0_TREASURY() {
        setup_markup_from_input_csv(Markup.Type.TREASURY, 
                                    "all,*\r\n" + 
                                    "*,0.05\r\n"); 
        static_assert_markup_is(101, 1, 101 + 0.05);
        static_assert_markup_is(101, 22, 101 + 0.05);
    }
    
    @Test
	public void test_matthew_v0_DTC_PRIMARY() {
        setup_markup_from_input_csv(Markup.Type.DTC_PRIMARY, 
                                    "all,*\r\n" + 
                                    "*,0\r\n"); 
        static_assert_markup_is(101,  1, 101);
        static_assert_markup_is(101, 22, 101);
    }
    
    @Test
	public void test_matthew_v0_DTC_SECONDARY() {
        setup_markup_from_input_csv(Markup.Type.DTC_SECONDARY, 
                                    "all,1-2,3+\r\n" + 
                                    "*,0.10,0.12\r\n"); 
        static_assert_markup_is(101, 1,  101 + 0.10);
        static_assert_markup_is(101, 22, 101 + 0.12);
    }
    
    @Test
 	public void test_matthew_v2_MUNI() {
         setup_markup_from_input_csv(Markup.Type.MUNI, 
                                     "Rating/Year,	0,	1,	2,	3,	4,	5,	  6-10,	   11-15,	16-20,	21+\r\n" + 
                                     "AAA,	0.025,	0.025,	0.025,	0.03,	0.035,	0.04,	0.05,	0.075,	0.1,	0.15\r\n" + 
                                     "AAplus,	0.025,	0.025,	0.03,	0.03,	0.04,	0.04,	0.05,	0.075,	0.1,	0.15\r\n" + 
                                     "AA,	0.025,	0.03,	0.035,	0.035,	0.04,	0.05,	0.05,	0.075,	0.1,	0.15\r\n" + 
                                     "AAminus,	0.025,	0.035,	0.04,	0.04,	0.045,	0.05,	0.075,	0.1,	0.125,	0.175\r\n" + 
                                     "Aplus,	0.03,	0.04,	0.045,	0.045,	0.05,	0.055,	0.075,	0.125,	0.15,	0.2\r\n" + 
                                     "A,	0.035,	0.045,	0.05,	0.05,	0.05,	0.055,	0.075,	0.15,	0.175,	0.2\r\n" + 
                                     "Aminus,	0.04,	0.05,	0.05,	0.05,	0.05,	0.075,	0.125,	0.175,	0.2,	0.2\r\n" + 
                                     "*,	0.05,	0.05,	0.075,	0.1,	0.1,	0.125,	0.15,	0.25,	0.5,	0.75\r\n"); 
         static_assert_markup_is(101, 1, "AAA", 101 + 0.025);
         static_assert_markup_is(101, 19, "AAA", 101 + 0.1);
         static_assert_markup_is(101, 21, "AAA", 101 + 0.15);
         static_assert_markup_is(101, 0, "Aplus", 101 + 0.03);
         static_assert_markup_is(101, 2, "BBBplus", 101 + 0.075);
         static_assert_markup_is(101, 0, "NONE", 101 + 0.05);
     }
    
    @Test
 	public void test_matthew_v2_AGENCY() {
         setup_markup_from_input_csv(Markup.Type.AGENCY, 
              "$size/Maturity, 0mo,    3mo,    6mo,    9mo,    1yr,    2yr,    3yr,    4yr,    5yr,    6yr,    7yr,    8yr,    9yr,    10-14yr,15-19yr, 20yr+\r\n" + 
              "0,              0.0075, 0.01,   0.0125, 0.015,  0.0175, 0.02,   0.0225, 0.025,  0.0275, 0.03,   0.0325, 0.035,  0.15,   0.2,    0.25,   0.75\r\n" + 
              "250k,           0.005,  0.0075, 0.01,   0.0125, 0.015,  0.0175, 0.02,   0.0225, 0.025,  0.0275, 0.03,   0.0325, 0.1,    0.125,  0.2,    0.5\r\n" + 
              "500k,           0.0025, 0.005,  0.0075, 0.01,   0.0125, 0.015,  0.0175, 0.02,   0.0225, 0.025,  0.0275, 0.03,   0.075,  0.1,    0.125,	0.25\r\n" + 
              "1000k+,         0.002,  0.0025, 0.005,  0.0075, 0.01,   0.0125, 0.015,  0.0175, 0.02,   0.0225, 0.025,  0.0275, 0.03,   0.05,   0.075,	0.1\r\n" + 
              ""); 
         static_assert_dollar_volume_markup_is(101, 0, 5, 101 + 0.0075);
         static_assert_dollar_volume_markup_is(101, 0, 2500, 101 + 0.005);
         static_assert_dollar_volume_markup_is(101, 0, 5000, 101 + 0.0025);
         static_assert_dollar_volume_markup_is(101, 0, 10000, 101 + 0.002);
         static_assert_dollar_volume_markup_is(101, 0,  99999, 101 + 0.002);

         static_assert_dollar_volume_markup_is(101, 1, 5, 101 + 0.0175);
         static_assert_dollar_volume_markup_is(101, 1, 2500, 101 + 0.015);
         static_assert_dollar_volume_markup_is(101, 1, 5000, 101 + 0.0125);
         static_assert_dollar_volume_markup_is(101, 1, 10000, 101 + 0.01);
         static_assert_dollar_volume_markup_is(101, 1,  99999, 101 + 0.01);

         static_assert_dollar_volume_markup_is(101, 17, 5, 101 + 0.25);
         static_assert_dollar_volume_markup_is(101, 17, 2500, 101 + 0.2);
         static_assert_dollar_volume_markup_is(101, 17, 5000, 101 + 0.125);
         static_assert_dollar_volume_markup_is(101, 17, 10000, 101 + 0.075);
         static_assert_dollar_volume_markup_is(101, 17,  99999, 101 + 0.075);
     }

    @Test
    public void test_date_rounding() {
    	long t_now = new Date().getTime();
    	Date days3FromNow = new Date(t_now + (3 * 24 * 60 * 60 * 1000L));
		Markup m = new Markup(101, Markup.Type.DTC_PRIMARY, days3FromNow);
        long years_to_maturity = m.calculate_years_to_maturity();
        assertEquals(0, years_to_maturity);

        Date years3FromNow = new Date(t_now + (3 * 24 * 60 * 60 * 1000L * 366));
		m = new Markup(101, Markup.Type.DTC_PRIMARY, years3FromNow);
        years_to_maturity = m.calculate_years_to_maturity();
        assertEquals(3, years_to_maturity);
    }

    @Test
	public void test_translate_time_units() {
        Schedule sch = Markup.load_markup_schedule(Markup.Type.MUNI, "abc,0,1");
        Schedule.Schedule_dimension sd = sch.columns;
        assertEquals("1", sd.translate_time_units("1"));
        assertEquals("2", sd.translate_time_units("2mo"));
        assertEquals("1-2", sd.translate_time_units("1-2"));
        assertEquals("1-2", sd.translate_time_units("1-2mo"));
        assertEquals("2-3", sd.translate_time_units("2mo-3mo"));
        assertEquals("12-23", sd.translate_time_units("1yr"));
    }

    @Test
	public void test_standardize_column_header() {
        Schedule sch = Markup.load_markup_schedule(Markup.Type.MUNI, "abc,0,1");
        Schedule.Schedule_dimension sd = sch.columns;
        assertEquals("5000", sd.standardize_column_header("5k"));
        assertEquals("25000+", sd.standardize_column_header("25k+"));
    }

    @Test
    public void test_translate_time_units2() {
        Schedule sch = Markup.load_markup_schedule(Markup.Type.MUNI, "abc,0,1");
        Schedule.Schedule_dimension sd = sch.columns;
        assertEquals("12-15", sd.translate_time_units("1yr-15mo"));
    }

    @Test
	public void test_translate_time_units3() {
        Schedule sch = Markup.load_markup_schedule(Markup.Type.MUNI, "abc,0,1");
        Schedule.Schedule_dimension sd = sch.columns;
        assertEquals("7-23", sd.translate_time_units("7mo-1yr"));
    }
}
