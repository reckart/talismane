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
 * Mimics an in-then-else structure - if condition is true return thenFeature result, else return elseFeature result.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class IfThenElseBooleanFeature<T> extends AbstractCachableFeature<T,Boolean> implements
		BooleanFeature<T> {
	private BooleanFeature<T> condition;
	private BooleanFeature<T> thenFeature;
	private BooleanFeature<T> elseFeature;
	
	public IfThenElseBooleanFeature(BooleanFeature<T> condition, BooleanFeature<T> thenFeature, BooleanFeature<T> elseFeature) {
		super();
		this.condition = condition;
		this.thenFeature = thenFeature;
		this.elseFeature = elseFeature;
		this.setName("IfThenElse(" + condition.getName() + "," + thenFeature.getName() + "," + elseFeature.getName() + ")");
	}

	@Override
	protected FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<Boolean> conditionResult = condition.check(context, env);
		if (conditionResult!=null) {
			boolean conditionOutcome = conditionResult.getOutcome();
			if (conditionOutcome) {
				FeatureResult<Boolean> thenFeatureResult = thenFeature.check(context, env);
				if (thenFeatureResult!=null) {
					boolean result = thenFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			} else {
				FeatureResult<Boolean> elseFeatureResult = elseFeature.check(context, env);
				if (elseFeatureResult!=null) {
					boolean result = elseFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			}
		}
		
		
		return featureResult;
		
	}

	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder, String variableName) {
		String condition1 = builder.addFeatureVariable(condition, "condition");
		
		builder.append("if (" + condition1 + "!=null) {");
		builder.indent();
		builder.append(		"if (" + condition1 +") {");
		builder.indent();
		String thenResult = 	builder.addFeatureVariable(thenFeature, "then");
		builder.append(			"if (" + thenResult + "!=null) " + variableName + " = " + thenResult + ";");
		builder.outdent();
		builder.append(		"} else {");
		builder.indent();
		String elseResult = 	builder.addFeatureVariable(elseFeature, "else");
		builder.append(			"if (" + elseResult + "!=null) " + variableName + " = " + elseResult + ";");
		builder.outdent();
		builder.append(		"}");
		builder.outdent();
		builder.append("}");
		
		return true;
	}
	
	public BooleanFeature<T> getCondition() {
		return condition;
	}

	public BooleanFeature<T> getThenFeature() {
		return thenFeature;
	}

	public BooleanFeature<T> getElseFeature() {
		return elseFeature;
	}

	public void setCondition(BooleanFeature<T> condition) {
		this.condition = condition;
	}

	public void setThenFeature(BooleanFeature<T> thenFeature) {
		this.thenFeature = thenFeature;
	}

	public void setElseFeature(BooleanFeature<T> elseFeature) {
		this.elseFeature = elseFeature;
	}

}
