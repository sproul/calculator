package bondmetrics;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Markup {
	public enum Rating {
            AAA,
            AAplus,
            AA,
            AAminus,
            Aplus,
            A,
            Aminus,
            BBBplus_or_below,
            NONE
    }
    /*
     * deprecated -- use Util.Type instead
     */
	public enum Type {
        MUNI,
        AGENCY,
        TREASURY,
        DTC_PRIMARY,
        DTC_SECONDARY
    }
	
	private static String markupPath;
	
	enum Schedule_dimension_data_type  { STRING, NUMERICAL, TIME };
	
	static class Schedule {
		class Schedule_dimension {
			Schedule_dimension_data_type data_type;
			Double greater_than_or_equal_threshold;
			ArrayList<String> headers = null;
	        /*
	         * By default, times are set in terms of years to maturity.  But if the field 'time_month_mode' is true, then
	         * times are set in terms of months to maturity.  This field is set to be true if Schedule sees time settings
	         * "mo" or "yr" (e.g., 7mo for "7 months") when parsing the markup schedule.
	         */
	        private boolean time_month_mode;
	        
			Schedule_dimension() {
				this.data_type = Schedule_dimension_data_type.NUMERICAL;   // default
				this.time_month_mode = false;
			}

			/*
			 * Adjust headers make their format consistent and simplified.
			 * 
			 * Time units are standardized as months, so e.g., 2yr becomes 24mo internally.
			 * 
			 * 25k becomes 25000.
			 */
	        String standardize_column_header(String header) {
	        	header = translate_time_units(header);
	            if (header.matches("^.*\\d+k\\+?$")) {
	            	header = header.replaceFirst("k", "000");
	            }
	        	return header;
			}
			/*
			 * For headers which use the "mo" or "yr" suffixes to indicate months and years, we translate
	         * all numerical quantities to months, in order to keep the internals simple.
	         *
	         * Here are some examples of what the headers look like to the user, versus how they are represented internally:
	         * 
	         * 11		11
	         * 11mo		11
	         * 1mo-2mo	1-2
	         * 
	         * All "year" quantities are translated to month ranges that extend to the next year boundary:
	         * 
	         * 2yr		24-35
	         * 1yr-2yr		12-35
	         * 
			 */
	        String translate_time_units(String header) {
	            if (header.matches(".*\\d+mo-\\d+yr$")) {
					this.data_type = Schedule_dimension_data_type.TIME;
	                int years = Integer.parseInt(header.replaceAll(".*-", "").replaceAll("yr$", ""));
	                String years_str = "" + years + "yr";
	                header = header.replaceAll(years_str, "" + ((12 * years) + 11));
	                header = header.replaceAll("mo", "");
	                return header;
	            }
	            if (header.matches(".*\\d+yr-\\d+mo$")) {
	                int years = Integer.parseInt(header.replaceAll("yr.*", ""));
	                String years_str = "" + years + "yr";
	                header = header.replaceAll(years_str, "" + ((12 * years)));
	                header = header.replaceAll("mo", "");
	                return header;
	            }
	            if (header.matches(".*\\d+mo\\+?$") || header.matches("\\d+mo-.*")) {
	                this.time_month_mode = true;
	        		header = header.replaceAll("mo", "");
	                return header;
	        	}
	        	if (header.matches("\\d+yr\\+?$") || header.matches("\\d+-\\d+yr\\+?$")) {
	                this.time_month_mode = true;
	        		header = header.replaceAll("yr$", "");
	        		header = header.replaceAll("yr\\+$", "+");
	                // simplify by treating all headers as ranges, e.g., treat "1yr" as if it were "1-1yr"
	                boolean and_greater = header.matches(".*\\+$");
	                if (and_greater) {
	                    header = header.replaceAll("\\+$", "");
	                    int months = Integer.parseInt(header) * 12;
	                    StringBuffer sb = new StringBuffer();
	                    sb.append(months)
	                    .append("+");
	                    return sb.toString();
	                }
	                if (!header.matches(".*-.*")) {
	                    header = header + "-" + header;
	                }
	                String[] yr0_to_yr1_string = header.split("-");
	                int yr0 = Integer.parseInt(yr0_to_yr1_string[0]);
	                int yr1 = Integer.parseInt(yr0_to_yr1_string[1]);
	                int mo0 =  yr0 * 12;
	                int mo1 = (yr1 * 12) + 11;
	                StringBuffer sb = new StringBuffer();
	                sb.append(mo0)
	                    .append("-")
	                    .append(mo1);
	                if (and_greater) {
	                    sb.append("+");
	                }
	                return sb.toString();
	        	}
	        	return header;
			}

			String make_key_component(String dimension_value) {
				if (this.greater_than_or_equal_threshold != null) {
					if (!dimension_value.equals("*")) {
						double dim_value = Double.parseDouble(dimension_value);
						if (dim_value > this.greater_than_or_equal_threshold) {
							String threshold_string = this.greater_than_or_equal_threshold.toString();
							threshold_string = threshold_string.replaceAll("\\.0$",  "");
							return threshold_string;
						}
					}
				}
				return dimension_value;
			}
			public String toString() {
				StringBuffer sb = new StringBuffer();
				sb.append("Schedule_dimension(");
                if (greater_than_or_equal_threshold != null) {
					sb.append(greater_than_or_equal_threshold)
					.append("+");
				}
				else {
					sb.append("-");
				}
				sb.append(")");
				return sb.toString();
			}

			public String find_closest_lesser_header(double n) {
				if (this.data_type == Schedule_dimension_data_type.STRING) {
                    return "" + n;
				}
				String last_header_that_was_lt_n = null;
				for (int j = 0; j < this.headers.size(); j++) {
					String header = this.headers.get(j);
					if (header.equals(WILD_CARD)) { 
						return WILD_CARD;
					}
					double header_val = Double.parseDouble(header);
					if (header_val > n) {
						if (last_header_that_was_lt_n == null) {
                            throw new RuntimeException("could not find header val < " + n + " in headers " + this.headers);
						}
                        break;
					}
                    last_header_that_was_lt_n = header;
				}
                return last_header_that_was_lt_n;
			}
		}

		// for now max is 2; 0th is columns, 1rst dim is rows
		Schedule_dimension columns = new Schedule_dimension();
		Schedule_dimension rows = new Schedule_dimension();
		HashMap<String, Double> markups = new HashMap<String, Double>();
		
		Schedule(InputStream csvInputStream) {
			Reader reader = new InputStreamReader(csvInputStream);
			BufferedReader csvInput = new BufferedReader(reader);
			this.rows.headers = new ArrayList<String>();
            try {
                Double row_greater_or_equal_to = null;
				while (csvInput.ready()) {
					String line = csvInput.readLine().replaceAll("[ \t]", "");
					if (line.matches(".*['\"].*")) {
						throw new RuntimeException("no quotes allowed in csv input: " + line);
					}
					if (this.columns.headers == null) {
                        String line_no_row_header = line.replaceAll("^[^,]+,", "");      // col header for the row headers is irrelevant
						this.columns.headers = stringArrayToArrayList(line_no_row_header.split(","));
                        Double col_greater_or_equal_to = null;
						for (int x = 0; x < this.columns.headers.size(); x++) {
							String col_header = columns.standardize_column_header(this.columns.headers.get(x));
                            if (col_header.matches("\\d+\\+$")) {
								col_header = col_header.replaceAll("\\+$", "");
								this.columns.headers.set(x, col_header);
                                if (col_greater_or_equal_to != null) {
									throw new RuntimeException("can only be one greater_or_equal_to value for dimension 1 (cols):" + line);
								}
								col_greater_or_equal_to = Double.parseDouble(col_header);
								columns.greater_than_or_equal_threshold = col_greater_or_equal_to;
							}
                            col_header = col_header.replaceAll("-.*", "");
							this.columns.headers.set(x, col_header);
						}
                    }
                    else {
                        String row_header = rows.standardize_column_header(line.replaceAll(",.*", ""));
                        if (row_header.matches("\\d+\\+$")) {
                            row_header = row_header.replaceAll("\\+$", "");
							if (row_greater_or_equal_to != null) {
                                throw new RuntimeException("can only be one greater_or_equal_to value for dimension 1 (rows):" + line);
                            }
                            row_greater_or_equal_to = Double.parseDouble(row_header);
                            rows.greater_than_or_equal_threshold = row_greater_or_equal_to;
                        }
                        row_header = row_header.replaceAll("-.*", "");
                        this.rows.headers.add(row_header);
                        String line_no_row_header = line.replaceAll("^[^,]+,", "");        // already processed row header in the line above
                        
                        String[] row_data = line_no_row_header.split(",");
                        if (row_data.length != columns.headers.size()) {
                            throw new RuntimeException("since there are " + columns.headers.size() + " column headers, there should be the same number of data columns; but I see " + row_data.length + " columns in " + line);
                        }
                        for(int colIndex = 0; colIndex < row_data.length; colIndex++) {
                        	String col_header = this.columns.headers.get(colIndex);
                        	Double datum = Double.parseDouble(row_data[colIndex]);
                        	this.set_markup(col_header, row_header, datum);
                        }
                    }
				}
			} catch (IOException e) {
				throw new RuntimeException("trouble reading csvInput: ", e);
			}
		}
		private ArrayList<String> stringArrayToArrayList(String[] strings) {
	        ArrayList<String> a = new ArrayList<String>();
	        for (String string : strings) {
				a.add(string);
			}
			return a;
		}
		String make_key(String column_val, String row_val) {
			return column_val + row_val;
		}

		void set_markup(String column_val, String row_val, double val) {
			String key = this.make_key(column_val, row_val);
			this.markups.put(key, val);
		}
    
        Double get_markup(String column_val, String row_val, double price) {
            String key = this.make_key(column_val, row_val);
            Double val = this.markups.get(key);
            if (val == null) {
            	return null;
            }
			return val + price;
		}

    	public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Schedule(")
                .append("columns=")
                .append(columns)
                .append("rows=")
                .append(rows)
                .append(",columns.headers=")
                .append(columns.headers)
                .append(",rows.headers=")
                .append(rows.headers)
                .append(",markups=")
                .append(markups);
            return sb.toString();
        }
	}
	
	private static final String WILD_CARD = "*";
	
	protected Date maturity;
	protected Rating rating;
	protected double price;
	protected Type type;
	protected Integer number_of_securities;
	
    static HashMap<Markup.Type, Schedule> type_to_schedule = new HashMap<Markup.Type, Schedule>();

    /**
     * Given a price, bond type, and a date of maturity, return the price including markup.
     */
	public static double calculate(double price, Type type, Date maturity) {
        Markup m = new Markup(price, type, maturity);
        return m.calculate();
	}

    /**
     * Given a price, bond type, date of maturity, and a rating, return the price including markup.
     */
	public static double calculate(double price, Type type, Date maturity, Rating rating) {
        Markup m = new Markup(price, type, maturity, rating);
        return m.calculate();
    }

    /**
     * Given a price, bond type, date of maturity, and a rating, return the price including markup.
     */
	public static double calculate(double price, Type type, Date maturity, String rating_string) {
    	Markup m = new Markup(price, type, maturity, rating_string);
    	return m.calculate();
    }
	
    /**
     * Given a price, bond type, date of maturity, and a count of the securities in the transaction, return the price including markup.
     */
	public static double calculate(double price, Type type, Date maturity, int number_of_securities) {
    	Markup m = new Markup(price, type, maturity, number_of_securities);
    	return m.calculate();
	}

    /**
     * Given a price, bond type, and a date of maturity, construct a Markup object.
     */
	Markup(double price, Type type, Date maturity) {
		this(price, type, maturity, Rating.NONE);
	}

    /**
     * Given a price, bond type, date of maturity, and a rating, construct a Markup object.
     */
	Markup(double price, Type type, Date maturity, Rating rating) {
        this.price = price;
        this.type = type;
        this.maturity = maturity;
        this.rating = rating;
    }

    /**
     * Given a price, bond type, date of maturity, and a rating, construct a Markup object.
     */
	Markup(double price, Type type, Date maturity, String rating_string) {
    	this(price, type, maturity, Markup.string_toRating(rating_string));
    }
    
    public Markup(double price, Type type, Date maturity, int number_of_securities) {
        this.price = price;
        this.type = type;
        this.maturity = maturity;
        this.number_of_securities = number_of_securities;
	}

	public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("Markup(")
    	.append("price=")
    	.append(price)
    	.append(",type=")
    	.append(type.toString())
    	.append(",maturity=")
    	.append(maturity);
    	if (rating != null) {
    		sb.append(", rating=")
    		.append(rating.toString()	);
    	}
    	sb.append(")");
    	return sb.toString();
    }
    
    String[] make_key(String x, String y) {
    	String [] z = { x, y };
    	return z;
    }

	/**
     * Calculate an appropriate markup for this bond.
     * 
     * Adding the value returned by this routine to this.price will give us the price visible to the user.
     *
     * Initially looks for an exact match on the key, then tries wildcards if needed.
     */
	public double calculate() {
        Schedule schedule = Markup.type_to_schedule.get(this.type);
        long time_to_maturity;
        if (schedule.columns.time_month_mode) {
        	time_to_maturity= calculate_months_to_maturity();
        }
        else {
        	time_to_maturity= calculate_years_to_maturity();
        }
        String col_key = schedule.columns.find_closest_lesser_header(time_to_maturity);
        String row_key;
        if (this.number_of_securities != null) {
        	double dollar_size = this.number_of_securities * this.price;
        	row_key = schedule.rows.find_closest_lesser_header(dollar_size);
        }
        else if (rating == null) {
        	row_key = WILD_CARD;
        }
        else {
        	row_key = rating.toString();
        }
        Double markup = schedule.get_markup(col_key, row_key, this.price);
        if (markup != null) {
                return markup;
        }
        markup = schedule.get_markup(col_key, WILD_CARD, this.price);
        if (markup != null) {
                return markup;
        }
        markup = schedule.get_markup(WILD_CARD, row_key, this.price);
        if (markup != null) {
                return markup;
        }
        markup = schedule.get_markup(WILD_CARD, WILD_CARD, this.price);
        if (markup != null) {
                return markup;
        }
        throw new RuntimeException("no markup set for " + this);
    }

	protected long calculate_months_to_maturity() {
		Date now = new Date();
        @SuppressWarnings("deprecation")
		long months = ((maturity.getYear() - now.getYear()) * 12) + maturity.getMonth() - now.getMonth();
		return months;
	}
	
	protected long calculate_years_to_maturity() {
		return calculate_months_to_maturity() / 12;
	}
	
    /**
     * Load (or reload) the markup schedule for bonds of the given type.
     *
     * <p>This routine expects to find a csv file on the class path whose name consists of
     *
     * <ul>
     * <li>markup_</li>
     * <li>name of the type</li>
     * <li>.csv</li>
     * </ul>
     *
     * <p>So, for example, if we call load_markup_schedule(Markup.Type.MUNI), then the code will
     * expect to find a file markup_MUNI.csv on the class path.
     *
     * <p>The csv is expected to be a grid giving the markup for each combination of rating and
     * years-to-maturity.
     *
     * <p><b>csv contents example #1:</b>
     * <pre>
     * ratings,1,2+
     * AAA,0.45,0.60
     * AA,0.55,0.75
     * </pre>
     *
     * <p>In this example:
     * AAA bonds with 1 year to maturity would be marked up $0.45.
     * AAA bonds with 2 or more years to maturity would be marked up $0.60.
     * AA bonds with 1 year to maturity would be marked up $0.55.
     * AA bonds with 2 or more years to maturity would be marked up $0.75.
     *
     * <p><b>csv contents example #2:</b>
     * <pre>
     * ratings,1,2+
     * *,0.45,0.60
     * </pre>
     *
     * <p>In this example, ratings are ignored and the only thing that counts is the years to maturity:
     * Bonds with 1 year to maturity would be marked up $0.45.
     * Bonds with 2 or more years to maturity would be marked up $0.60.
     *
     * <p><b>csv contents example #3:</b>
     * <pre>
     * ratings,*
     * *,0.45
     * </pre>
     *
     * In this example, a flat markup is applied:
     * All bonds are marked up $0.45.
     *
     */
	static public void load_markup_schedule(Type type) {
		String csvFn = "markup_" + type + ".csv";
		File csvFile = new File (markupPath + "/" + csvFn);
        FileInputStream in;
        
        try
        {
            in = new FileInputStream (csvFile);
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException ("File " + markupPath + "/" + csvFn + " not found.");
        }
		
        load_markup_schedule(type, in);
    }
    
    /**
     * Load (or reload) the markup schedule for securities of the given type.
     *
     * <p>This routine expects its string argument to contain lines organized csv-style, indicating a schedule
     * of markups.
     *
     * <p>The csv is expected to be a grid giving the markup for each combination of rating and
     * years-to-maturity.
     *
     * <p><b>csv contents example #1:</b>
     * <pre>
     * ratings,1,2+
     * AAA,0.45,0.60
     * AA,0.55,0.75
     * </pre>
     *
     * <p>In this example:
     * AAA bonds with 1 year to maturity would be marked up $0.45.
     * AAA bonds with 2 or more years to maturity would be marked up $0.60.
     * AA bonds with 1 year to maturity would be marked up $0.55.
     * AA bonds with 2 or more years to maturity would be marked up $0.75.
     *
     * <p><b>csv contents example #2:</b>
     * <pre>
     * ratings,1,2+
     * *,0.45,0.60
     * </pre>
     *
     * <p>In this example, ratings are ignored and the only thing that counts is the years to maturity:
     * Bonds with 1 year to maturity would be marked up $0.45.
     * Bonds with 2 or more years to maturity would be marked up $0.60.
     *
     * <p><b>csv contents example #3:</b>
     * <pre>
     * ratings,*
     * *,0.45
     * </pre>
     *
     * In this example, a flat markup is applied:
     * All bonds are marked up $0.45.
     *
     */
    static public Schedule load_markup_schedule(Type type, String s) {
    	InputStream is = new ByteArrayInputStream(s.getBytes());
    	return load_markup_schedule(type, is);
	}

    /*
     * Load a markup schedule from an input stream.
     */
    static public Schedule load_markup_schedule(Type type, InputStream in) {
        Schedule sch = new Schedule(in);
        type_to_schedule.put(type, sch);
        return sch;
	}

	/*
     * Load (or reload) all markup schedules.
     */
	static public void load_markup_all_schedules(String markuppath) {
	    markupPath = markuppath;
        for (Type type : Type.values()) {
            load_markup_schedule(type);
        }
    }
    static Rating string_toRating(String ratingString) {
        if (ratingString==null) {
            return null;
        }
        try {
            return Rating.valueOf(ratingString);
        }
        catch (IllegalArgumentException e) {
            return Rating.BBBplus_or_below;  // a little hacky, but this is the catch-all
        }
    }
	
	static public void main(String[] argv) {
		if (argv.length == 0) {
			System.err.println("Args required.");
			return;
		}
		Markup.Type type = Markup.Type.valueOf(argv[0]);
		double price = Double.parseDouble(argv[1]);
		int maturity_month = Integer.parseInt(argv[2]);
		int maturity_day = Integer.parseInt(argv[3]);
		int maturity_year = Integer.parseInt(argv[4]);
		@SuppressWarnings("deprecation")
		Date maturity = new Date(maturity_year, maturity_month, maturity_day);
		double markedUpPrice;
		if (type == Markup.Type.MUNI) {
			Markup.Rating rating = Markup.Rating.valueOf(argv[5]);
			markedUpPrice = Markup.calculate(price, type, maturity, rating);
		}
		else {
			markedUpPrice = Markup.calculate(price, type, maturity);
		}
		System.out.println("" + markedUpPrice);
	}
}
