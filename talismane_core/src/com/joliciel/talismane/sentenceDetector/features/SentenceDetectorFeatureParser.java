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
package com.joliciel.talismane.sentenceDetector.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.sentenceDetector.PossibleSentenceBoundary;

class SentenceDetectorFeatureParser extends AbstractFeatureParser<PossibleSentenceBoundary> {
	public SentenceDetectorFeatureParser(FeatureService featureService) {
		super(featureService);
	}	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<SentenceDetectorFeature<?>> parseDescriptor(FunctionDescriptor functionDescriptor) {
		List<Feature<PossibleSentenceBoundary, ?>> tokenFeatures = this.parse(functionDescriptor);
		List<SentenceDetectorFeature<?>> wrappedFeatures = new ArrayList<SentenceDetectorFeature<?>>();
		for (Feature<PossibleSentenceBoundary, ?> tokenFeature : tokenFeatures) {
			SentenceDetectorFeature<?> wrappedFeature = null;
			if (tokenFeature instanceof SentenceDetectorFeature) {
				wrappedFeature = (SentenceDetectorFeature<?>) tokenFeature;
			} else {
				wrappedFeature = new SentenceDetectorFeatureWrapper(tokenFeature);
			}
			wrappedFeatures.add(wrappedFeature);
		}
		return wrappedFeatures;
	}
	
	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("Initials", InitialsFeature.class);
		container.addFeatureClass("InParentheses", InParenthesesFeature.class);
		container.addFeatureClass("IsStrongPunctuation", IsStrongPunctuationFeature.class);
		container.addFeatureClass("NextLetterCapital", NextLetterCapitalFeature.class);
		container.addFeatureClass("NextLetters", NextLettersFeature.class);
		container.addFeatureClass("PreviousLetters", PreviousLettersFeature.class);
		container.addFeatureClass("NextTokens", NextTokensFeature.class);
		container.addFeatureClass("PreviousTokens", PreviousTokensFeature.class);
		container.addFeatureClass("Surroundings", SurroundingsFeature.class);
		container.addFeatureClass("BoundaryString", BoundaryStringFeature.class);
	}

	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor) {
		List<FunctionDescriptor> descriptors = new ArrayList<FunctionDescriptor>();
		
		descriptors.add(functionDescriptor);
		
		return descriptors;
	}

	@Override
	public void injectDependencies(@SuppressWarnings("rawtypes") Feature feature) {
		// no dependencies to inject
	}
	
	private static class SentenceDetectorFeatureWrapper<T> extends AbstractFeature<PossibleSentenceBoundary, T> implements
		SentenceDetectorFeature<T>, FeatureWrapper<PossibleSentenceBoundary, T> {
		private Feature<PossibleSentenceBoundary,T> wrappedFeature = null;
		
		public SentenceDetectorFeatureWrapper(
				Feature<PossibleSentenceBoundary, T> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setCollectionName(wrappedFeature.getCollectionName());
		}
		
		@Override
		public FeatureResult<T> check(PossibleSentenceBoundary context, RuntimeEnvironment env) {
			return wrappedFeature.check(context, env);
		}
		

		@Override
		public Feature<PossibleSentenceBoundary, T> getWrappedFeature() {
			return this.wrappedFeature;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Class<? extends Feature> getFeatureType() {
			return wrappedFeature.getFeatureType();
		}
	}

	@Override
	protected boolean canConvert(Class<?> parameterType,
			Class<?> originalArgumentType) {
		return false;
	}

	@Override
	protected Feature<PossibleSentenceBoundary, ?> convertArgument(
			Class<?> parameterType,
			Feature<PossibleSentenceBoundary, ?> originalArgument) {
		return null;
	}

	@Override
	public Feature<PossibleSentenceBoundary, ?> convertFeatureCustomType(
			Feature<PossibleSentenceBoundary, ?> feature) {
		return null;
	}
}
