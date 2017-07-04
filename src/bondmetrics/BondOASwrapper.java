package bondmetrics;

import com.kalotay.akalib.Bond;
import com.kalotay.akalib.Date;
import com.kalotay.akalib.Duration;
import com.kalotay.akalib.Initialization;
import com.kalotay.akalib.InterestRateModel;
import com.kalotay.akalib.Status;
import com.kalotay.akalib.Value;

public class BondOASwrapper {

	public enum CurveType { FLAT, LINEAR, ASYM };

	// ***********************************
	// COMMAND LINE OPTION VARIABLES
	// ***********************************
	private static String pvdateStr = null;
	private static String mdateStr = null;
	private static boolean quiet = false;
	private static double vol = 0;
	private static boolean bullet = false;
	// set number of pvdates to value 
	private static int days = 100;
	private static boolean fromoas = true;	// go from oas to price
	private static double quote = 0;
	private static CurveType ctype = CurveType.FLAT;
	private static boolean timing = true;

	private static double discount_rate = 7.0; // discount rate 7.0% as 7.0
	private static double coupon = discount_rate;

	/**
	 * Runs some example valuations.
	 *
	 * @param args Command line params are <KEY> <USER>
	 */

	public static void init(int key, String uname) {
		System.loadLibrary("bondoas_java_wrap");
		System.out.println("BondOASwrapper.main() - creating Initialization with key: " + key + " uname: " + uname);
		Initialization akareg = new Initialization(key, uname);
		System.out.println("BondOASwrapper.main() - created Initialization - error: " + akareg.Error());
		if (akareg.Error() > 0) {
			printError("BondOASwrapper.main()", akareg.ErrorString());
			return;
		}

		System.out.println("BondOASwrapper.main() - Version: " +
				Initialization.Version() 
				+ " Expiration Libdate: " + akareg.Expiration().Libdate()
				+ " Expiration YearOf: " + akareg.Expiration().YearOf()
				+ " Expiration DayOf: " + akareg.Expiration().DayOf()
				);

	}
	public static void main(String args[]) {
		BondOASwrapper.init(Integer.parseInt(args[0]), args[1]);
		int error = exampleValuation();
		if (error > 0)
			printError("BondOASwrapper.main()", "exampleValuation() returned an error: " + error);
	}

	/** Example of how to value a bond.	*/

	public static int exampleValuation() {
		Date pvdate = pvdateStr == null ? new Date(2000, 01, 01) : new Date(pvdateStr, Date.ENTRY.MDY);
		Date mdate = mdateStr == null ? new Date(2030, 01, 01) : new Date(mdateStr, Date.ENTRY.MDY);    // maturity date

		System.out.println("pvdateStr: " + pvdateStr + " pvdate: " + pvdate.Libdate() + " mdate: " + mdate.Libdate());
		/* timing variables */
		long start = 0;


		InterestRateModel model = new InterestRateModel();
		if (!model.SetVolatility(vol))
			System.out.format("Warning: invalid volatility '%f', using 0\n", vol);
		if (!model.SetRate(.5, discount_rate)) {
			System.out.format("Warning: invalid input rate '%f', using 2%%\n", discount_rate);
			discount_rate = 2;
			model.SetRate(.5, discount_rate);
		}
		if (ctype == CurveType.LINEAR) {
			model.SetRate(1, discount_rate + .01);
			model.SetRate(30, discount_rate + .3);
		}
		else if (ctype == CurveType.ASYM) {
			double [] terms = { 1, 3, 5, 7, 10, 15, 30 };
			for (int i = 0; i < terms.length; i++)
				model.SetRate(terms[i], discount_rate + 2 * (1 - 1.0 /terms[i]));
		}
		if (quiet == false) {
			System.out.format("Making a par rate curve with %.2g volatility", vol);
			System.out.format(", %.1f yr = %.2f%%", 1.0, model.GetRate(1));
			System.out.format(", %.1f yr = %.2f%%", 30.0, model.GetRate(30));
		}

		if (timing)
			start = System.currentTimeMillis();
		model.Solve();
		if (timing) {
			start = System.currentTimeMillis() - start;
			System.out.format("Seconds to fit the base curve = %.2f\n", INSECS(start));
		}
		if (!msgs(model))
			return model.Error();

		// make the bond 
		if (quiet == false)
			System.out.format("\nMaking a 30 year %.10g%% bond maturing on %d",
					coupon, mdate.Libdate());
		Date idate = new Date(mdate.YearOf() - 30, mdate.MonthOf(), mdate.DayOf());
		Bond bond = new Bond("example", idate, mdate, coupon);
		if (!msgs(bond))
			return bond.Error();

		if (!bullet) {
			Date cdate = new Date(idate.YearOf() + 5, idate.MonthOf(), idate.DayOf());
			if (quiet == false)
				System.out.format(" callable %d at par", cdate.Libdate());
			if (!bond.SetCall(cdate, 100))
				System.out.format("failed to add call at %d\n", cdate.Libdate());
		}
		if (quiet == false)
			System.out.format("\n\n");

		if (quiet == false) {
			String underline = "--------------------";
			String fmt = "%10.10s %8.8s %8.8s %8.8s %8.8s %8.8s";
			System.out.format(fmt, "pvdate  ", fromoas ? "price" : "oas", "accrued", "optval", "duration", "convex.");
			System.out.print("\n");;
			System.out.format(fmt, underline, underline, underline, underline, underline, underline);
			System.out.print("\n");;
		}

		Value value = new Value(bond, model, pvdate);
		if (!msgs(value))
			return value.Error();

		// loop through pvdates 
		start = System.currentTimeMillis();
		int cnt = 0;
		for (cnt = 0; pvdate.IsLT(mdate) && cnt < days; pvdate.PlusEqual(1), cnt++) {
			if (cnt > 0) {
				value.Reset(bond, pvdate);
				if (!msgs(value))
					break;
			}
			double oas = fromoas ? quote : value.OAS(quote);
			double price = fromoas ? value.Price(quote) : quote;
			if (!msgs(value) || Value.IsBadValue(oas) || Value.IsBadValue(price))
				break;

			if (quiet == false) {
				Duration duration = value.EffectiveDuration(oas);
				System.out.format("%02d/%02d/%04d %8.3f %8.3f %8.3f %8.3f %8.3f",
						pvdate.MonthOf(), pvdate.DayOf(), pvdate.YearOf(),
						fromoas ? price : oas,
								value.Accrued(), value.OptionValue(oas),
								duration.getDuration(), duration.getConvexity());
				System.out.print("\n");;
			}
		}
		if (timing)
			System.out.format("\nSeconds to value the bond for %d pvdates = %.2f\n",
					cnt, INSECS(System.currentTimeMillis() - start));
		return 0;
	} // exampleValuation()


	/**
    Purpose: display usage message
    Returns: nothing
	 */

	public static void usage() {
		System.out.print("Purpose: value a 30 year bond\n");
		System.out.print("Usage: BondOASwrapper [FLAGS] <key> <username> <discount-rate> [<coupon> : defaults to discout-rate]\n");
		System.out.print(
				"\nFlags:\n"
						+ "\t-b -- bond is bullet bond\n");
		System.out.print(
				"\t-c <curve-type> -- make the curve be:\n"
						+ "\t\tflat -- same rate (default)\n"
						+ "\t\tlinear -- grow linearly (slowly)\n"
						+ "\t\tasymptotic -- grow (quickly) but level off\n");
		System.out.print(
				"\t-d <cnt> -- set number of pvdates to value (default 1)\n"
						+ "\t-m <mdate> -- set bond maturity date, default 1/1/2030\n"
						+ "\t-p <pvdate> -- set initial pvdate to value, default bond dated date\n");
		System.out.print(
				"\t-q -- use price of 100 as quote, default use oas of zero\n"
						+ "\t-T <arg> -- set tax rates as: income[,short[,long[,superlong]]]\n"
						+ "\t-t -- display timings\n"
						+ "\t-v <vol> -- set curve volatility, default zero\n"
						+ "\t-z -- silent mode, no output, for timing\n");
	}

	// **********************************************
	// DEBUG/TRACING ROUTINES
	// **********************************************

	static double INSECS(long millis) { return millis / 1000.0; };

	/** 
    Purpose: print warnings and errors, if any
    Returns: true if no errors, else false
	 */
	public static boolean msgs(Status status) {
		for (int i = 0; i < status.WarningCount(); i++)
			System.out.format("Warning: %2d \"%s\"\n", status.Warning(i), status.WarningString(i));
		if (status.Error() > 0)
			System.out.format("%2d \"%s\"\n", status.Error(),
					status.ErrorString());

		return status.Error() == 0;
	}

	static void printError(String method, String error) {
		StringBuffer buff = new StringBuffer();
		buff.append("\n");
		buff.append("\n");
		buff.append("************************************* ERROR *************************************");
		buff.append("\n");
		buff.append("Method: ");
		buff.append(method);
		buff.append("\n");
		buff.append("Error: ");
		buff.append(error);
		buff.append("\n");
		buff.append("************************************* ERROR *************************************");
		buff.append("\n");
		buff.append("\n");
		System.out.println(buff.toString());
	}
}
