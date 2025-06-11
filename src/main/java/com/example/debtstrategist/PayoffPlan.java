package com.example.debtstrategist;

// Notice we have removed the @Data annotation
public class PayoffPlan {
    private String strategyName;
    private int totalMonths;
    private double totalInterestPaid;

    // Manually added Getters and Setters
    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public int getTotalMonths() {
        return totalMonths;
    }

    public void setTotalMonths(int totalMonths) {
        this.totalMonths = totalMonths;
    }

    public double getTotalInterestPaid() {
        return totalInterestPaid;
    }

    public void setTotalInterestPaid(double totalInterestPaid) {
        this.totalInterestPaid = totalInterestPaid;
    }
}