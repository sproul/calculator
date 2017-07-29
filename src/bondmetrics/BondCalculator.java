package bondmetrics;
import java.util.Date;
import bondmetrics.Util;
public interface BondCalculator {
	public double yield_to_maturity(Util.Bond_frequency_type frequency_type, Util.Interest_basis interest_basis, double clean_price, double coupon_rate, int par, Date settlement, Date maturity);
	public double accrued_interest_at_settlement(Util.Bond_frequency_type frequency_type, Util.Interest_basis interest_basis, double coupon_rate, int par, Date settlement, Date maturity);
}
