package bondmetrics;

import java.util.Date;

import com.ftlabs.fisa.FISADate;
import com.ftlabs.fisa.Market;
import com.ftlabs.fisa.ZeroCouponSecurity;
import com.ftlabs.fisa.calc.CalculationException;
import com.ftlabs.fisa.calc.Calculator;

import bondmetrics.Util.Bond_frequency_type;
import bondmetrics.Util.Interest_basis;

public class FtLabs_zero_coupon_rate_calculator extends FtLabs {

	@Override
	public double yield_to_maturity(Bond_frequency_type frequency_type, Interest_basis interest_basis, double clean_price,
			double coupon_rate, int par, Date settlement_date, Date maturity_date, boolean eomAdjust) {

		if (coupon_rate != 0) {
			throw new RuntimeException("FtLabs_nonpositive_coupon_rate_calculator only appopriate for zero interest rate securities");
		}

		double ftLabs_price_multipllier = par / 100;
		double price_as_percentage = clean_price / ftLabs_price_multipllier; 
		ZeroCouponSecurity security = new ZeroCouponSecurity(Market.US.GENERICBOND);
		//Date payment_date_preceding_or_coinciding_with_settlement = Util.find_coupon_payment_date_preceding_or_coinciding_with_settlement(frequency_type, settlement_date, maturity_date);
		//FISADate fisa_dated_date = date_to_FISADate(payment_date_preceding_or_coinciding_with_settlement);
		//security.setDatedDate(fisa_dated_date);
		FISADate fisa_maturity_date = date_to_FISADate(maturity_date);
		security.setMaturity(fisa_maturity_date);
		security.setParValue(100);		// strange, but seems to be necessary for the calculations to work.  Use ftLabs_price_multipllier to adjust.
		security.setEomAdjust(eomAdjust);

		security.setInterestFrequency(remap_to_fisa_bond_frequency(frequency_type));
		security.setDayCountBasis(remap_to_fisa_interest_basis(interest_basis));
		FISADate fisa_settlement_date = date_to_FISADate(settlement_date);
		try 
		{
			Calculator calculator = security.getCalculator(fisa_settlement_date);
			return calculator.calculateYield(price_as_percentage) / 100;
		}
		catch (CalculationException e) {
			throw new RuntimeException(e);
		}
	}
}
