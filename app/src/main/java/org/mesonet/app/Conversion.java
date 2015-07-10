package org.mesonet.app;

import java.util.concurrent.Callable;



public class Conversion
{
	public static final float kInvalidData = -900;
	
	public static double kInchesPerMb = 29.92 / 1013.25;
	public static double kCToF = 9.0 / 5;
	public static double kCtoFOffset = 32;
	public static double kMmPerInch = 1 / 25.4;
	public static double kMpsPerMph = 3600 / 1609.34;
	public static double kDewpointConstantA = 6.1365;
	public static double kDewpointConstantB = 17.502;
	public static double kDewpointConstantC = 240.97;
	public static double kHeatIndexCheckTemp = 80.0;
	public static double kHeatIndexC1 = -42.379;
	public static double kHeatIndexC2 = 2.04901523;
	public static double kHeatIndexC3 = 10.14333127;
	public static double kHeatIndexC4 = -0.22475541;
	public static double kHeatIndexC5 = -6.83783e-03;
	public static double kHeatIndexC6 = -5.481717e-02;
	public static double kHeatIndexC7 = 1.22874e-03;
	public static double kHeatIndexC8 = 8.5282e-04;
	public static double kHeatIndexC9 = -1.99e-06;
	public static double kWindChillCheckTemp = 50.0;
	public static double kWindChillCheckWind = 3.0;
	public static double kWindChillC1 = 35.74;
	public static double kWindChillC2 = 0.6215;
	public static double kWindChillC3 = 35.75;
	public static double kWindChillC4 = 0.4275;
	public static double kWindChillSpeedPower = 0.16;
	
	

	public static boolean Valid(float inData)
	{
		return !Float.isNaN(inData) && inData > kInvalidData;
	}
	
	
	
	public static String FloatToString(float inValue, boolean inUseDecimals)
	{
		if(inUseDecimals)
			return Float.toString(inValue);
		
		return Integer.toString(Math.round(inValue));
	}
	
	
	
	public static double CelsiusToFahrenheit (double inC) 
	{
		return inC * kCToF + kCtoFOffset;
	}
	
	
	
	public static double FahrenheitToCelsius (double inF) 
	{
		return (inF - kCtoFOffset) / kCToF;
	}
	
	
	
	public static double MillimetersToInches (double inMm) 
	{
		return inMm * kMmPerInch;
	}
	
	
	
	public static double MetersPerSecondToMilesPerHour (double inMps) 
	{
		return inMps * kMpsPerMph;
	}
	
	
	
	public static double MilesPerHourToMetersPerSecond (double inMph) 
	{
		return inMph / kMpsPerMph;
	}
	
	
	
	static double MillibarsToInches(double inMb)
	{
		return inMb * kInchesPerMb;
	}
	
	

	static double Dewpoint (double inTempC, double inRelh) 
	{
		if (Double.isNaN(inRelh) || Double.isNaN(inTempC))
			return Double.NaN;
		
	    double es = kDewpointConstantA * Math.exp((kDewpointConstantB * inTempC) / (kDewpointConstantC + inTempC));
	    double e = (inRelh / 100.0) * es;
	    double tdew = kDewpointConstantC * Math.log(e / kDewpointConstantA) / (kDewpointConstantB - Math.log(e / kDewpointConstantA));
	    return (tdew < inTempC) ? tdew : inTempC;
	}
	
	
	
	public static double ApparentTemperature (double inTairF, double inRelhPct, Double inWspdMph)
	{
	    double tapp;

	    if (inTairF <= kWindChillCheckTemp)
	    {
	        if (inWspdMph.isNaN())
	            return Double.NaN;
	        else if (inWspdMph > kWindChillCheckWind)
	            tapp = WindChill(inTairF, inWspdMph);
	        else
	            tapp = inTairF;
	    }
	    else if (inTairF > kHeatIndexCheckTemp)
	    	tapp = HeatIndex(inTairF, inRelhPct);
	    else
	        tapp = inTairF;

	    return tapp;
	}
	
	

	
	public static double HeatIndex (double inTairF, Double inRelhPct)
	{
	    double heat = Double.NaN;

	    if (inTairF >= kHeatIndexCheckTemp)
	    {
	    	if(inRelhPct.isNaN())
	    		return Double.NaN;
	        heat = kHeatIndexC1 + (kHeatIndexC2 * inTairF) + (kHeatIndexC3 * inRelhPct) + (kHeatIndexC4 * inTairF * inRelhPct)
	        		+ (kHeatIndexC5 * Math.pow(inTairF, 2.0)) + (kHeatIndexC6 * Math.pow(inRelhPct, 2.0))
	        		+ (kHeatIndexC7 * Math.pow(inTairF, 2.0) * inRelhPct) + (kHeatIndexC8 * inTairF * Math.pow(inRelhPct, 2.0))
	        		+ (kHeatIndexC9 * Math.pow(inTairF, 2.0) * Math.pow(inRelhPct, 2.0));
	    }

	    return heat;
	}
	
	

	public static double WindChill (double inTairF, double inWspdMph)
	{
	    if ((inTairF <= kWindChillCheckTemp) && (inWspdMph > kWindChillCheckWind))
	        return kWindChillC1 + (kWindChillC2 * inTairF) - (kWindChillC3 * Math.pow(inWspdMph, kWindChillSpeedPower)) + (kWindChillC4 * inTairF * Math.pow(inWspdMph, kWindChillSpeedPower));
	    else
	        return Double.NaN;
	}
	
	
	
	public static String CreateDataString(Converter inConverter, String inImperialUnit, String inMetricUnit, int inRoundDecs, boolean inUseDecimals)
	{
		if(!Valid(inConverter.GetValue()))
			return SavedDataManager.MissingField();
		
		if(MainMenu.GetUnitSystem() == MainMenu.UnitSystem.kImperial)
		{
			float roundDec = (float)Math.pow(10, inRoundDecs);

            float increase = 0;

            try
            {
                increase = inConverter.call() * roundDec;
            }
            catch(Exception exception)
            {
                exception.printStackTrace();
            }

            float rounded = Math.round(increase);
            float decrease = rounded / roundDec;
            return FloatToString(decrease, inUseDecimals) + inImperialUnit;

		}
		
		return FloatToString(inConverter.GetValue(), inUseDecimals) + inMetricUnit;
	}
	
	
	
	public static class CelsToFahrConverter extends Converter
	{
		public CelsToFahrConverter(float inValue)
		{
			super(inValue);
		}
		
		
		
		@Override
		public Float call() throws Exception
		{
			return (float)CelsiusToFahrenheit(mValue);
		}
	}
	
	
	
	public static class MMToInConverter extends Converter
	{
		public MMToInConverter(float inValue)
		{
			super(inValue);
		}
		
		
		
		@Override
		public Float call() throws Exception
		{
			return (float)MillimetersToInches(mValue);
		}
	}
	
	
	
	public static class MpsToMphConverter extends Converter
	{
		public MpsToMphConverter(float inValue)
		{
			super(inValue);
		}
		
		
		
		@Override
		public Float call() throws Exception
		{
			return (float)MetersPerSecondToMilesPerHour(mValue);
		}
	}
	
	
	
	public static class MBToInConverter extends Converter
	{
		public MBToInConverter(float inValue)
		{
			super(inValue);
		}
		
		
		
		@Override
		public Float call() throws Exception
		{
			return (float)MillibarsToInches(mValue);
		}
	}
	
	
	
	public static abstract class Converter implements Callable<Float>
	{
		protected float mValue;
		
		
		
		public Converter(float inValue)
		{
			mValue = inValue;
		}
		
		
		
		public float GetValue()
		{
			return mValue;
		}
	}
}
