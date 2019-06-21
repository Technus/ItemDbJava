package com.dgs.dapc.itemDB.javafx.spinnerValueFactory;

import javafx.scene.control.SpinnerValueFactory;

import java.math.BigDecimal;

public class DoubleInfinitySpinnerValueFactory extends SpinnerValueFactory.DoubleSpinnerValueFactory {

    public DoubleInfinitySpinnerValueFactory(double min, double max) {
        super(min, max);
    }

    public DoubleInfinitySpinnerValueFactory(double min, double max, double initialValue) {
        super(min, max, initialValue);
    }

    public DoubleInfinitySpinnerValueFactory(double min, double max, double initialValue, double amountToStepBy) {
        super(min, max, initialValue, amountToStepBy);
    }

    /** {@inheritDoc} */
    @Override public void decrement(int steps) {
        try {
            final BigDecimal currentValue = BigDecimal.valueOf(getValue());
            final BigDecimal minBigDecimal = BigDecimal.valueOf(getMin());
            final BigDecimal maxBigDecimal = BigDecimal.valueOf(getMax());
            final BigDecimal amountToStepByBigDecimal = BigDecimal.valueOf(getAmountToStepBy());
            BigDecimal newValue = currentValue.subtract(amountToStepByBigDecimal.multiply(BigDecimal.valueOf(steps)));
            setValue(newValue.compareTo(minBigDecimal) >= 0 ? newValue.doubleValue() :
                    (isWrapAround() ? wrapValue(newValue, minBigDecimal, maxBigDecimal).doubleValue() : getMin()));
        }catch (NumberFormatException e){
            Double newValue = getValue()-getAmountToStepBy()*steps;
            setValue(newValue.compareTo(getMin()) >= 0 ? newValue :
                    (isWrapAround() ? wrapValue(newValue, getMin(), getMax()) : getMin()));
        }
    }

    /** {@inheritDoc} */
    @Override public void increment(int steps) {
        try {
            final BigDecimal minBigDecimal = BigDecimal.valueOf(getMin());
            final BigDecimal maxBigDecimal = BigDecimal.valueOf(getMax());
            final BigDecimal currentValue = BigDecimal.valueOf(getValue());
            final BigDecimal amountToStepByBigDecimal = BigDecimal.valueOf(getAmountToStepBy());
            BigDecimal newValue = currentValue.add(amountToStepByBigDecimal.multiply(BigDecimal.valueOf(steps)));
            setValue(newValue.compareTo(maxBigDecimal) <= 0 ? newValue.doubleValue() :
                    (isWrapAround() ? wrapValue(newValue, minBigDecimal, maxBigDecimal).doubleValue() : getMax()));
        }catch (NumberFormatException e){
            Double newValue = getValue()+getAmountToStepBy()*steps;
            setValue(newValue.compareTo(getMax()) <= 0 ? newValue :
                    (isWrapAround() ? wrapValue(newValue, getMin(), getMax()) : getMax()));
        }
    }

    private static BigDecimal wrapValue(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (max.doubleValue() == 0) {
            throw new RuntimeException();
        }

        // note that this wrap method differs from the others where we take the
        // difference - in this approach we wrap to the min or max - it feels better
        // to go from 1 to 0, rather than 1 to 0.05 (where max is 1 and step is 0.05).
        if (value.compareTo(min) < 0) {
            return max;
        } else if (value.compareTo(max) > 0) {
            return min;
        }
        return value;
    }

    private static Double wrapValue(Double value, Double min, Double max) {
        if (max == 0) {
            throw new RuntimeException();
        }

        // note that this wrap method differs from the others where we take the
        // difference - in this approach we wrap to the min or max - it feels better
        // to go from 1 to 0, rather than 1 to 0.05 (where max is 1 and step is 0.05).
        if (value.compareTo(min) < 0) {
            return max;
        } else if (value.compareTo(max) > 0) {
            return min;
        }
        return value;
    }
}
