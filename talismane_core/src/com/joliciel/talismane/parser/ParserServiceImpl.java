package com.joliciel.talismane.parser;

import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.FeatureWeightVector;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.Ranker;
import com.joliciel.talismane.machineLearning.RankingEventStream;
import com.joliciel.talismane.machineLearning.RankingModel;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.parser.Parser.ParseComparisonStrategyType;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;

public class ParserServiceImpl implements ParserServiceInternal {
	ParserFeatureService parseFeatureService;
	PosTaggerService posTaggerService;
	TokeniserService tokeniserService;
	MachineLearningService machineLearningService;
	TokenFilterService tokenFilterService;
	FeatureService featureService;
	
	@Override
	public DependencyArc getDependencyArc(PosTaggedToken head,
			PosTaggedToken dependent, String label) {
		DependencyArcImpl arc = new DependencyArcImpl(head, dependent, label);
		return arc;
	}

	@Override
	public ParseConfiguration getInitialConfiguration(
			PosTagSequence posTagSequence) {
		ParseConfigurationImpl configuration = new ParseConfigurationImpl(posTagSequence);
		configuration.setParserServiceInternal(this);
		return configuration;
	}

	@Override
	public ParseConfiguration getConfiguration(ParseConfiguration history) {
		ParseConfigurationImpl configuration = new ParseConfigurationImpl(history);
		configuration.setParserServiceInternal(this);
		return configuration;
	}

	@Override
	public NonDeterministicParser getTransitionBasedParser(DecisionMaker<Transition> decisionMaker, TransitionSystem transitionSystem,
			Set<ParseConfigurationFeature<?>> parseFeatures,
			int beamWidth) {
		TransitionBasedParserImpl parser = new TransitionBasedParserImpl(decisionMaker, transitionSystem, parseFeatures, beamWidth);
		parser.setParserServiceInternal(this);
		parser.setFeatureService(this.getFeatureService());
		return parser;
	}

	@Override
	public ClassificationEventStream getParseEventStream(
			ParserAnnotatedCorpusReader corpusReader,
			Set<ParseConfigurationFeature<?>> parseFeatures) {
		ParseEventStream eventStream = new ParseEventStream(corpusReader, parseFeatures);
		eventStream.setParserServiceInternal(this);
		eventStream.setMachineLearningService(this.getMachineLearningService());
		eventStream.setFeatureService(this.getFeatureService());
		return eventStream;
	}
	

	@Override
	public RankingEventStream<PosTagSequence> getGlobalParseEventStream(
			ParserAnnotatedCorpusReader corpusReader,
			Set<ParseConfigurationFeature<?>> parseFeatures) {
		ParseGlobalEventStream eventStream = new ParseGlobalEventStream(corpusReader, parseFeatures);
		eventStream.setParserServiceInternal(this);
		eventStream.setMachineLearningService(this.getMachineLearningService());
		eventStream.setFeatureService(this.getFeatureService());
		return eventStream;
	}
	

	@Override
	public ParserEvaluator getParserEvaluator() {
		ParserEvaluatorImpl evaluator = new ParserEvaluatorImpl();
		evaluator.setParserServiceInternal(this);
		return evaluator;
	}

	@Override
	public TransitionSystem getShiftReduceTransitionSystem() {
		ShiftReduceTransitionSystem transitionSystem = new ShiftReduceTransitionSystem();
		return transitionSystem;
	}

	@Override
	public TransitionSystem getArcEagerTransitionSystem() {
		ArcEagerTransitionSystem transitionSystem = new ArcEagerTransitionSystem();
		return transitionSystem;
	}

	@Override
	public NonDeterministicParser getTransitionBasedParser(
			MachineLearningModel model, int beamWidth, boolean dynamiseFeatures) {
		Collection<ExternalResource<?>> externalResources = model.getExternalResources();
		if (externalResources!=null) {
			for (ExternalResource<?> externalResource : externalResources) {
				this.getParseFeatureService().getExternalResourceFinder().addExternalResource(externalResource);
			}
		}
		
		TransitionSystem transitionSystem = null;
		String transitionSystemClassName = (String) model.getModelAttributes().get("transitionSystem");
		if (transitionSystemClassName.equalsIgnoreCase("ShiftReduceTransitionSystem")) {
			transitionSystem = this.getShiftReduceTransitionSystem();
		} else if (transitionSystemClassName.equalsIgnoreCase("ArcEagerTransitionSystem")) {
			transitionSystem = this.getArcEagerTransitionSystem();
		} else {
			throw new TalismaneException("Unknown transition system: " + transitionSystemClassName);
		}
		
		Set<ParseConfigurationFeature<?>> parseFeatures = this.getParseFeatureService().getFeatures(model.getFeatureDescriptors(), dynamiseFeatures);

		NonDeterministicParser parser = null;
		if (model instanceof ClassificationModel) {
			@SuppressWarnings("unchecked")
			ClassificationModel<Transition> classificationModel = (ClassificationModel<Transition>) model;
			DecisionMaker<Transition> decisionMaker = classificationModel.getDecisionMaker();

			parser = this.getTransitionBasedParser(decisionMaker, transitionSystem, parseFeatures, beamWidth);
		} else if (model instanceof RankingModel) {
			RankingModel rankingModel = (RankingModel) model;
			FeatureWeightVector featureWeightVector = rankingModel.getFeatureWeightVector();
			ParsingConstrainer parsingConstrainer = (ParsingConstrainer) model.getDependencies().get(ParsingConstrainer.class.getSimpleName());
			parser = this.getTransitionBasedGlobalLearningParser(featureWeightVector, parsingConstrainer, parseFeatures, beamWidth);
		} else {
			throw new TalismaneException("Unknown parser model type: " + model.getClass().getSimpleName());
		}
		return parser;
	}

	public ParserFeatureService getParseFeatureService() {
		return parseFeatureService;
	}

	public void setParseFeatureService(ParserFeatureService parseFeatureService) {
		this.parseFeatureService = parseFeatureService;
	}

	@Override
	public DependencyNode getDependencyNode(PosTaggedToken token, String label,
			ParseConfiguration parseConfiguration) {
		DependencyNodeImpl dependencyNode = new DependencyNodeImpl(token, label, parseConfiguration);
		dependencyNode.setParserServiceInternal(this);
		return dependencyNode;
	}

	@Override
	public ParserRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader) {
		ParserRegexBasedCorpusReaderImpl corpusReader = new ParserRegexBasedCorpusReaderImpl(reader);
		corpusReader.setParserService(this);
		corpusReader.setPosTaggerService(this.getPosTaggerService());
		corpusReader.setTokeniserService(this.getTokeniserService());
		corpusReader.setTokenFilterService(this.getTokenFilterService());
		return corpusReader;
	}
	

	@Override
	public ParserRegexBasedCorpusReader getRegexBasedCorpusReader(File file,
			Charset charset) {
		ParserRegexBasedCorpusReaderImpl corpusReader = new ParserRegexBasedCorpusReaderImpl(file, charset);
		corpusReader.setParserService(this);
		corpusReader.setPosTaggerService(this.getPosTaggerService());
		corpusReader.setTokeniserService(this.getTokeniserService());
		corpusReader.setTokenFilterService(this.getTokenFilterService());
		return corpusReader;
	}


	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService macheLearningService) {
		this.machineLearningService = macheLearningService;
	}
	
	

	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	@Override
	public ParseComparator getParseComparator() {
		ParseComparatorImpl parseComparator = new ParseComparatorImpl();
		parseComparator.setParserServiceInternal(this);
		return parseComparator;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	@Override
	public ParsingConstrainer getParsingConstrainer() {
		ParsingConstrainerImpl constrainer = new ParsingConstrainerImpl();
		constrainer.setParseServiceInternal(this);
		return constrainer;
	}

	@Override
	public ParsingConstrainer getParsingConstrainer(File file) {
		ParsingConstrainer constrainer = ParsingConstrainerImpl.loadFromFile(file);
		return constrainer;
	}

	@Override
	public Ranker<PosTagSequence> getRanker(
			ParsingConstrainer parsingConstrainer,
			Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth) {
		TransitionBasedGlobalLearningParser parser = this.getTransitionBasedGlobalLearningParser(null, parsingConstrainer, parseFeatures, beamWidth);
		// if training, don't set a maximum time per sentence
		parser.setMaxAnalysisTimePerSentence(0);
		return parser;
	}

	@Override
	public TransitionBasedGlobalLearningParser getTransitionBasedGlobalLearningParser(
			FeatureWeightVector featureWeightVector,
			ParsingConstrainer parsingConstrainer,
			Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth) {
		TransitionBasedGlobalLearningParser parser = new TransitionBasedGlobalLearningParser(featureWeightVector, parsingConstrainer, parseFeatures, beamWidth);
		parser.setParserServiceInternal(this);
		parser.setFeatureService(this.getFeatureService());
		parser.setMachineLearningService(this.getMachineLearningService());
		return parser;
	}

	@Override
	public ParseComparisonStrategy getParseComparisonStrategy(
			ParseComparisonStrategyType type) {
		switch (type) {
		case transitionCount:
			return new TransitionCountComparisonStrategy();
		case bufferSize:
			return new BufferSizeComparisonStrategy();
		case stackAndBufferSize:
			return new StackAndBufferSizeComparsionStrategy();
		case dependencyCount:
			return new DependencyCountComparisonStrategy();
		default:
			throw new TalismaneException("Unknown parse comparison strategy: " + type);
		}
	}

	@Override
	public ParseConfigurationProcessor getParseFeatureTester(
			Set<ParseConfigurationFeature<?>> parserFeatures, File file) {
		ParseFeatureTester tester = new ParseFeatureTester(parserFeatures, file);
		tester.setFeatureService(this.getFeatureService());
		tester.setParserServiceInternal(this);
		return tester;
	}

	
}
