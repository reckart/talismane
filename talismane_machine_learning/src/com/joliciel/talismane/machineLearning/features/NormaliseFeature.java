///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.machineLearning.features;

/**
 * Changes a numeric feature to a value from 0 to 1,
 * where any value <= minValue is set to 0, and any value >= maxValue is set to 1,
 * and all other values are set to a proportional value between 0 and 1.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class NormaliseFeature<T> extends AbstractCachableFeature<T, Double> implements DoubleFeature<T> {	
	DoubleFeature<T> featureToNormalise;
	DoubleFeature<T> minValueFeature = new DoubleLiteralFeature<T>(0.0);
	DoubleFeature<T> maxValueFeature = null;
	
	/**
	 * Constructor assuming the min value is 0.
	 * @param featureToNormalise
	 * @param maxValueFeature
	 */
	public NormaliseFeature(DoubleFeature<T> featureToNormalise, DoubleFeature<T> maxValueFeature) {
		this.featureToNormalise = featureToNormalise;
		this.maxValueFeature = maxValueFeature;
		this.setName("Normalise(" + featureToNormalise.getName() + "," + maxValueFeature.getName() + ")");
	}
	
	/**
	 * Constructor providing both min and max values.
	 * @param featureToNormalise
	 * @param minValueFeature
	 * @param maxValueFeature
	 */
	public NormaliseFeature(DoubleFeature<T> featureToNormalise, DoubleFeature<T> minValueFeature, DoubleFeature<T> maxValueFeature) {
		super();
		this.featureToNormalise = featureToNormalise;
		this.minValueFeature = minValueFeature;
		this.maxValueFeature = maxValueFeature;
		this.setName("Normalise(" + featureToNormalise.getName() + "," + minValueFeature.getName() + "," + maxValueFeature.getName() + ")");
	}

	@Override
	public FeatureResult<Double> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Double> featureResult = null;
		
		FeatureResult<Double> resultToNormalise = featureToNormalise.check(context, env);
		FeatureResult<Double> minValueResult = minValueFeature.check(context, env);
		FeatureResult<Double> maxValueResult = maxValueFeature.check(context, env);
		
		if (resultToNormalise!=null && minValueResult!=null && maxValueResult!=null) {
			double minValue = minValueResult.getOutcome();
			double maxValue = maxValueResult.getOutcome();
			double normalisedValue = 0.0;
			double initialValue = resultToNormalise.getOutcome();
			if (initialValue<minValue)
				normalisedValue = 0.0;
			else if (initialValue>maxValue)
				normalisedValue = 1.0;
			else {
				normalisedValue = (initialValue - minValue) / (maxValue - minValue);
			}
			featureResult = this.generateResult(normalisedValue);
		}
		return featureResult;
	}
	
	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder, String variableName) {
		String val = builder.addFeatureVariable(featureToNormalise, "val");
		String min = builder.addFeatureVariable(minValueFeature, "min");
		String max = builder.addFeatureVariable(maxValueFeature, "max");
		
		builder.append("if (" + val + "!=null && " + min + "!=null && " + max + "!=null) {");
		builder.indent();
		builder.append(variableName + " = 0.0;");
		builder.append("if (" + val + "<" + min + ")");
		builder.indent();
		builder.append(		variableName + " = 0.0;");
		builder.outdent();
		builder.append("else if (" + val + ">" + max + ")");
		builder.indent();
		builder.append(		variableName + " = 1.0;");
		builder.outdent();
		builder.append("else");
		builder.indent();
		builder.append(		variableName + " = (" + val + " - " + min + ") / (" + max + " - " + min + ");");
		builder.outdent();
		builder.outdent();
		builder.append("}");
		
		return true;
	}

	public DoubleFeature<T> getFeatureToNormalise() {
		return featureToNormalise;
	}

	public void setFeatureToNormalise(DoubleFeature<T> featureToNormalise) {
		this.featureToNormalise = featureToNormalise;
	}

	public DoubleFeature<T> getMinValueFeature() {
		return minValueFeature;
	}

	public void setMinValueFeature(DoubleFeature<T> minValueFeature) {
		this.minValueFeature = minValueFeature;
	}

	public DoubleFeature<T> getMaxValueFeature() {
		return maxValueFeature;
	}

	public void setMaxValueFeature(DoubleFeature<T> maxValueFeature) {
		this.maxValueFeature = maxValueFeature;
	}
	
	
}
