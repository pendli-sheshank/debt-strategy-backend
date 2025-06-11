package com.example.debtstrategist;

public class LoanAnalysisRequest {
    private double principal;
    private double apr;
    private int tenureInYears;
    private int gracePeriodInMonths;
    private double gracePeriodPayment; // <-- NEW FIELD

    // Getters and Setters
    public double getPrincipal() { return principal; }
    public void setPrincipal(double principal) { this.principal = principal; }
    public double getApr() { return apr; }
    public void setApr(double apr) { this.apr = apr; }
    public int getTenureInYears() { return tenureInYears; }
    public void setTenureInYears(int tenureInYears) { this.tenureInYears = tenureInYears; }
    public int getGracePeriodInMonths() { return gracePeriodInMonths; }
    public void setGracePeriodInMonths(int gracePeriodInMonths) { this.gracePeriodInMonths = gracePeriodInMonths; }

    // New Getter and Setter for the grace period payment
    public double getGracePeriodPayment() { return gracePeriodPayment; }
    public void setGracePeriodPayment(double gracePeriodPayment) { this.gracePeriodPayment = gracePeriodPayment; }
}