# calculator
calculate yield to maturity, accrued interest, other interesting bond metrics

		double coupon_rate = 3.75;
		Interest_basis basis = Interest_basis.By_30_360_ICMA;
		Bond_frequency_type freq = Bond_frequency_type.Quarterly;
		Date settlement = new Date(117, 5, 30);
		Date maturity= new Date(120, 5, 30);
		double accrued_interest;
		accrued_interest = Util.accrued_interest_at_settlement(freq, basis, coupon_rate, settlement, maturity);
