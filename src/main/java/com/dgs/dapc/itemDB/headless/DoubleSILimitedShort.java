package com.dgs.dapc.itemDB.headless;

import javafx.util.StringConverter;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DoubleSILimitedShort extends StringConverter<Double> {
    public static final DoubleSILimitedShort INSTANCE =new DoubleSILimitedShort();

    private static final NumberFormat FORMAT =NumberFormat.getInstance();
    static {
        FORMAT.setMaximumFractionDigits(6);
        FORMAT.setRoundingMode(RoundingMode.HALF_UP);
        FORMAT.setMinimumIntegerDigits(1);
    }

    @Override
    public String toString(Double object) {
        return Prefix.formatDouble(object);
    }

    @Override
    public Double fromString(String string) {
        return Prefix.parseValue(string);
    }

    public Double fromStringOrNull(String string) {
        return Prefix.parseValueOrNull(string);
    }

    private static final TreeMap<Double, Prefix> powerMapping =new TreeMap<>();
    private static final HashMap<String, Prefix> symbolMapping =new HashMap<>();
    public enum Prefix {
        yotta("Y",24),
        zetta("Z",21),
        exa("E",18),
        peta("P",15),
        tera("T",12),
        giga("G",9),
        mega("M",6),
        //myria("my",4,true),
        kilo("k",3),
        //hecto("h",2,true),
        //deca("da",1,true),
        none("",0),
        //deci("d",-1,true),
        //centi("c",-2,true),
        milli("m",-3),
        micro("μ",-6),
        nano("n",-9),
        pico("p",-12),
        femto("f",-15),
        atto("a",-18),
        zepto("z",-21),
        yocto("y",-24);

        public final String symbol;
        public final int power;
        public final double value;
        public final boolean notMultipleOf3;

        Prefix(String symbol, int power) {
            this(symbol,power,false);
        }

        Prefix(String symbol, int power, boolean notMultipleOf3) {
            this.symbol = symbol;
            this.power = power;
            this.notMultipleOf3=notMultipleOf3;
            this.value=Math.pow(10,power);
            powerMapping.put(value,this);
            symbolMapping.put(symbol,this);
        }

        public static Prefix getPrefix(Double d){
            if(d==null || d.isInfinite() || d.isNaN()) return none;
            Map.Entry<Double, Prefix> entry=powerMapping.floorEntry(Math.abs(d));
            return entry==null?yocto:entry.getValue();
        }

        public static Prefix getPrefixHigher(Double d){
            if(d==null || d.isInfinite() || d.isNaN()) return none;
            Map.Entry<Double, Prefix> entry=powerMapping.floorEntry(Math.abs(d)*10);
            return entry==null?yocto:entry.getValue();
        }

        public static String formatDouble(Double d){
            String higherResult = formatDouble(d, getPrefixHigher(d));
            String result = formatDouble(d, getPrefix(d));
            return higherResult.length() < result.length() ? higherResult : result;
        }

        public static String formatDouble(Double d,Prefix p){
            if(d==null){
                return "";
            }
            if(d.isInfinite() || d.isNaN()){
                return d.toString();
            }
            if(p==null){
                return FORMAT.format(d);
            }
            return FORMAT.format(d/p.value)+p.symbol;
        }

        public static Double parseValue(String s){
            if(s.contains("u")) {
                s=s.replaceAll("u","μ");
            }
            String numberStr=s.replaceAll("[^-0-9.,]+","");
            String prefixStr=s.replaceAll("[^μa-zA-Z]+","");
            return prefixStr.length() > 0 ?
                    Double.parseDouble(numberStr) * symbolMapping.getOrDefault(prefixStr, none).value :
                    Double.parseDouble(numberStr);
        }
        public static Double parseValueOrNull(String s){
            if(s.contains("u")) {
                s=s.replaceAll("u","μ");
            }
            String numberStr=s.replaceAll("[^-0-9.,]+","");
            String prefixStr=s.replaceAll("[^μa-zA-Z]+","");
            try {
                return prefixStr.length() > 0 ?
                        Double.parseDouble(numberStr) * symbolMapping.getOrDefault(prefixStr, none).value :
                        Double.parseDouble(numberStr);
            }catch (Exception e){
                return null;
            }
        }
    }
}