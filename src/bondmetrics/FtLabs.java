package bondmetrics;

import java.util.Date;

import bondmetrics.Util.Bond_frequency_type;
import bondmetrics.Util.Interest_basis;

import com.ftlabs.fisa.*;
import com.ftlabs.fisa.calc.*;

public class FtLabs implements BondCalculator {

	public static void ftlabs_sample(String[] args) {
		try {
            /*
             * Initialize DiscountSecurity with the appropriate Market.
             */
            DiscountSecurity security = new DiscountSecurity(Market.US.TREASURY);
            
            /*
             * Set all required properties that do not have a Market default.
             * Dated Date and Maturity are required properties for DiscountSecurity.
             */
            security.setDatedDate(new FISADate(2013, 1, 15));
            security.setMaturity(new FISADate(2014, 1, 15));
            
            /*
             * If necessary, override required properties that do have Market default
             * values.  If these values are not set explicitly, then the Market defaults
             * are used.  Each market may have different default values for these properties.
             */
            //security.setEomAdjust(true);
            //security.setDayCountBasis(DayCountBasis.ACT_360);
            
            /*
             * Use specific settlement date.  Could also use BusinessDateFactory to generate
             * the settlement date.
             */
            FISADate settlementDate = new FISADate(2013, 2, 12);
            
            /*
             * Direct DiscountCalculator usage with extended values
             */
            double price = 99.979;
            DiscountCalculator calculator = (DiscountCalculator) security.getCalculator(settlementDate);
            System.out.println("DiscountCalculator Usage:");
            System.out.println("Discount: " + calculator.calculateDiscount(price));
            System.out.println("Yield: " + calculator.calculateYield(price));
            System.out.println("CEY: " + calculator.calculateCEY(price));
            System.out.println("BEY: " + calculator.calculateBEY(price));
            System.out.println("--------------------------");
            
            /*
uncomment imports at top if you want to use this -- will need to bring in Utils sample class or equiv
DiscountQuote quote = new DiscountQuote(.02);
QuoteAnalytics quoteAnalytics =  security.createQuoteAnalytics(quote, settlementDate);

Utils.displayQuoteAnalytics(quoteAnalytics, "Discount Quote: " + quote.getDiscount());
            */
        }
        catch (CalculationException ce) {
            System.out.println("Calculation Exception: " + ce);
        }
    }

	static public double yield_to_maturity_static(Bond_frequency_type frequency_type, Interest_basis interest_basis, double clean_price,
                                           double coupon_rate, int par, Date settlement_date, Date maturity_date) {
        FtLabs ft = new FtLabs();
        return ft.yield_to_maturity(frequency_type, interest_basis, clean_price, coupon_rate, par, settlement_date, maturity_date);
    }
    
	@Override
	public double yield_to_maturity(Bond_frequency_type frequency_type, Interest_basis interest_basis, double clean_price,
                                    double coupon_rate, int par, Date settlement_date, Date maturity_date) {
		double ftLabs_price_multipllier = par / 100;
		double price_as_percentage = clean_price / ftLabs_price_multipllier; 
        FixedInterestRateSecurity security = new FixedInterestRateSecurity( Market.US.GENERICBOND);
        //Date payment_date_preceding_or_coinciding_with_settlement = Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(frequency_type, settlement_date, maturity_date);
        //FISADate fisa_dated_date = date_to_FISADate(payment_date_preceding_or_coinciding_with_settlement);
		//security.setDatedDate(fisa_dated_date);
        FISADate fisa_maturity_date = date_to_FISADate(maturity_date);
		security.setMaturity(fisa_maturity_date);
		security.setParValue(100);		// strange, but seems to be necessary for the calculations to work.  Use ftLabs_price_multipllier to adjust.
        //security.setEomAdjust(true);

        security.setInterestFrequency(remap_to_fisa_bond_frequency(frequency_type));
        security.setDayCountBasis(remap_to_fisa_interest_basis(interest_basis));
        security.setInterestRate(coupon_rate * 100);
        FISADate fisa_settlement_date = date_to_FISADate(settlement_date);
        try 
        {
        	Calculator calculator = security.getCalculator(fisa_settlement_date);
        	return calculator.calculateYield(price_as_percentage) / 100;
        }
        catch (CalculationException e) {
        	throw new RuntimeException("unexpected exception in ftlabs yield code: " + e);
        }
	}

	private InterestFrequency remap_to_fisa_bond_frequency(Bond_frequency_type frequency_type) {
        frequency_type = Util.remap_frequency_type(frequency_type);
		switch (frequency_type) {
		case Annual:
			return InterestFrequency.ANNUAL;
		case SemiAnnual:
			return InterestFrequency.SEMI_ANNUAL;
		case Quarterly:
			return InterestFrequency.QUARTERLY;
		case Monthly:
			return InterestFrequency.MONTHLY;
		default:
			throw new RuntimeException("unknown frequency type " + frequency_type);
		}
	}

	private DayCountBasis remap_to_fisa_interest_basis(Interest_basis interest_basis) {
		interest_basis = Util.remap_interest_basis(interest_basis);
		DayCountBasis fisa_interest_basis;
        switch(interest_basis) {
        case By_30_360:
        	fisa_interest_basis = DayCountBasis._30_360;
        	break;
        case By_Actual_360:
            fisa_interest_basis = DayCountBasis.ACT_360;
            break;
        case By_Actual_365:
        	fisa_interest_basis = DayCountBasis.ACT_365;
        	break;
        case By_Actual_Actual:
        	fisa_interest_basis = DayCountBasis.ACT_ACT;
        	break;
        default:
            throw new RuntimeException("unrecognized interest_basis=" + interest_basis);
        }
		return fisa_interest_basis;
	}
	
	@SuppressWarnings("deprecation")
	private FISADate date_to_FISADate(Date date) {
		return new FISADate(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
	}

	static double accrued_interest_at_settlement_static(Bond_frequency_type frequency_type, Interest_basis interest_basis, double coupon_rate, int par, Date settlement_date, Date maturity_date) {
        FtLabs ft = new FtLabs();
        double unrounded_accrued_interest = 100 * ft.accrued_interest_at_settlement(frequency_type, interest_basis, coupon_rate, par, settlement_date, maturity_date);
        return unrounded_accrued_interest;
    }
    
	@Override
	public double accrued_interest_at_settlement(Bond_frequency_type frequency_type, Interest_basis interest_basis, double coupon_rate, int par, Date settlement_date, Date maturity_date) {
		double ftLabs_price_multipllier = par / 100;
		
        FixedInterestRateSecurity security = new FixedInterestRateSecurity( Market.US.GENERICBOND);
        Date payment_date_preceding_or_coinciding_with_settlement = Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(frequency_type, settlement_date, maturity_date);
        FISADate fisa_dated_date = date_to_FISADate(payment_date_preceding_or_coinciding_with_settlement);
		security.setDatedDate(fisa_dated_date);
        FISADate fisa_maturity_date = date_to_FISADate(maturity_date);
		security.setMaturity(fisa_maturity_date);
		security.setParValue(100);		// strange, but seems to be necessary for the calculations to work.  Use ftLabs_price_multipllier to adjust.
        //security.setEomAdjust(true);
        security.setInterestRate(coupon_rate);
        security.setInterestFrequency(remap_to_fisa_bond_frequency(frequency_type));
        security.setDayCountBasis(remap_to_fisa_interest_basis(interest_basis));
        security.setInterestRate(coupon_rate);
        FISADate fisa_settlement_date = date_to_FISADate(settlement_date);
        try 
        {
        	Calculator calculator = security.getCalculator(fisa_settlement_date);
        	return ftLabs_price_multipllier * calculator.calculateAccruedInterest();
        }
        catch (CalculationException e) {
        	throw new RuntimeException("unexpected exception in ftlabs yield code: " + e);
        }
	}

//  FISADate fisa_dated_date = new FISADate(2016, 3, 21);
//	security.setDatedDate(fisa_dated_date);

	public static void ft_case_par() {
        FixedInterestRateSecurity security = new FixedInterestRateSecurity(Market.US.GENERICBOND);
        FISADate fisa_maturity_date = new FISADate(2020, 3, 21);
		security.setMaturity(fisa_maturity_date);
		security.setParValue(1000);
        security.setInterestRate(7);
        security.setInterestFrequency(InterestFrequency.ANNUAL);
        security.setDayCountBasis(DayCountBasis._30_360);
        try 
        {
        	Calculator calculator = security.getCalculator(new FISADate(2016, 3, 21));
        	double ytm = calculator.calculateYield(95);
            System.out.println("ft_case: ytm=" + ytm);
        	double accrued_interest = calculator.calculateAccruedInterest();
            System.out.println("ft_case: accrued_interest=" + accrued_interest);
        }
        catch (CalculationException e) {
        	throw new RuntimeException("unexpected exception in ftlabs yield code: " + e);
        }
    }

	public static void ft_case() {
        FixedInterestRateSecurity security = new FixedInterestRateSecurity(Market.US.GENERICBOND);
        FISADate fisa_maturity_date = new FISADate(2015, 2, 21);
		security.setMaturity(fisa_maturity_date);
		security.setParValue(100);
        security.setInterestRate(20);
        security.setInterestFrequency(InterestFrequency.ANNUAL);
        security.setDayCountBasis(DayCountBasis._30_360);
        try 
        {
        	Calculator calculator = security.getCalculator(new FISADate(2013, 8, 21));
        	double ytm = calculator.calculateYield(100);
            System.out.println("ft_case: ytm=" + ytm);
        }
        catch (CalculationException e) {
        	throw new RuntimeException("unexpected exception in ftlabs yield code: " + e);
        }
    }
	public static void main(String[] args) {
		ft_case();
		//ftlabs_sample(args);
		/*
		FtLabs ft = new FtLabs();
		Date settlement = Util.date(2016, 3, 21);
		Date maturity = Util.date(2017, 3, 21);
		double ytm = ft.yield_to_maturity(Util.Bond_frequency_type.Annual, Util.Interest_basis.By_30_360, 100, 0.07, 100, settlement, maturity); 
        System.out.println("main: ytm = " + ytm);
        */
	}
}
