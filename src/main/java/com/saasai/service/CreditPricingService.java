package com.saasai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CreditPricingService {

    @Value("${ai.credit.base-hold:10.0}")
    private double baseHoldDefault;

    @Value("${ai.credit.output-weight:5.0}")
    private double outputWeightDefault;

    @Value("${ai.min-credit-per-call:1.0}")
    private double minCreditPerCall;

    private double roundOneDecimal(double v) { return Math.round(v * 10.0) / 10.0; }

    public double calculateBaseCredit(long inputTokens, long outputTokens, Double outputWeight) {
        double outW = (outputWeight != null && outputWeight > 0) ? outputWeight : outputWeightDefault;
        double base = (inputTokens / 1000.0) + ((outputTokens / 1000.0) * outW);
        return roundOneDecimal(Math.max(minCreditPerCall, base));
    }

    public double calculateActualCredit(double baseCredit, Double creditRate) {
        double rate = (creditRate != null) ? creditRate : 1.0;
        return roundOneDecimal(baseCredit * rate);
    }

    public double calculateHoldFromBaseHold(Double baseHold, Double creditRate) {
        double base = (baseHold != null && baseHold > 0) ? baseHold : baseHoldDefault;
        double rate = (creditRate != null) ? creditRate : 1.0;
        return roundOneDecimal(base * rate);
    }

    public double getBaseHoldDefault() { return baseHoldDefault; }
    public double getOutputWeightDefault() { return outputWeightDefault; }
    public double getMinCreditPerCall() { return minCreditPerCall; }
}