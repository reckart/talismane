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
package com.joliciel.talismane;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.MarkerFilterType;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagRegexBasedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorService;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A class for loading, storing and translating configuration information to be passed to Talismane when processing
 * @author Assaf Urieli
 *
 */
public class TalismaneConfig {
	private static final Log LOG = LogFactory.getLog(TalismaneConfig.class);
	private Command command = Command.analyse;
	
	private Module startModule = Module.SentenceDetector;
	private Module endModule = Module.Parser;
	private Module module = Module.Parser;
	
	private SentenceDetector sentenceDetector;
	private Tokeniser tokeniser;
	private PosTagger posTagger;
	private Parser parser;
	
	private ParserEvaluator parserEvaluator;
	private PosTaggerEvaluator posTaggerEvaluator;

	private TokenRegexBasedCorpusReader tokenCorpusReader = null;
	private PosTagRegexBasedCorpusReader posTagCorpusReader = null;
	private ParserRegexBasedCorpusReader parserCorpusReader = null;

	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;
	
	private MachineLearningModel<PosTag> posTaggerModel = null;
	private MachineLearningModel<Transition> parserModel;

	private boolean processByDefault = true;
	private int maxSentenceCount = 0;
	private int beamWidth = 1;
	private boolean propagateBeam = true;
	private boolean includeDetails = false;	
	private Charset inputCharset = null;
	private Charset outputCharset = null;
	
	private char endBlockCharacter = '\f';
	private String inputRegex;
	private String inputPatternFilePath = null;
	private int maxParseAnalysisTime = 60;
	
	private Reader reader = null;
	private Writer writer = null;
	
	private String inFilePath = null;
	private String outFilePath = null;
	private String outDirPath = null;
	private String parserModelFilePath = null;
	private String posTaggerModelFilePath = null;
	private String tokeniserModelFilePath = null;
	private String sentenceModelFilePath = null;
	private String textFiltersPath = null;
	private String tokenFiltersPath = null;
	private String templatePath = null;

	private String sentenceTemplateName = "sentence_template.ftl";
	private String tokeniserTemplateName = "tokeniser_template.ftl";
	private String posTaggerTemplateName = "posTagger_template.ftl";
	private String parserTemplateName = "parser_conll_template.ftl";
	
	private String fileName = null;
	private boolean logPerformance = false;
	private File outDir = null;
	private String baseName = null;
	
	private List<PosTaggerRule> posTaggerRules = null;
	private String posTaggerRuleFilePath = null;
	private List<TextMarkerFilter> textMarkerFilters = null;
	private List<TokenFilter> tokenFilters = null;
	
	private MarkerFilterType newlineMarker = MarkerFilterType.SENTENCE_BREAK;

	private TalismaneServiceLocator talismaneServiceLocator = null;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private PosTaggerFeatureService posTaggerFeatureService;
	private FilterService filterService;
	private TokenFilterService tokenFilterService;
    private SentenceDetectorService sentenceDetectorService;
    private SentenceDetectorFeatureService sentenceDetectorFeatureService;
	private MachineLearningService machineLearningService;
	private TokeniserPatternService tokeniserPatternService;
	private TokenFeatureService tokenFeatureService;
	private TokeniserService tokeniserService;
	
	private LanguageSpecificImplementation implementation = null;
	
	public TalismaneConfig(LanguageSpecificImplementation implementation, String[] args) throws Exception {
		this.implementation = implementation;
		talismaneServiceLocator = TalismaneServiceLocator.getInstance();
		Map<String,String> argMap = this.convertArgs(args);
		this.loadParameters(argMap);
	}
	
	public TalismaneConfig(LanguageSpecificImplementation implementation, Map<String,String> args) throws Exception {
		this.implementation = implementation;
		talismaneServiceLocator = TalismaneServiceLocator.getInstance();
		this.loadParameters(args);
	}
	
	Map<String, String> convertArgs(String[] args) {
		Map<String,String> argMap = new HashMap<String, String>();
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			argMap.put(argName, argValue);
		}
		return argMap;
	}
	
	void loadParameters(Map<String,String> args) throws Exception {
		if (args.size()==0) {
			System.out.println("Talismane usage instructions: ");
			System.out.println("* indicates optional, + indicates default value");
			System.out.println("");
			System.out.println("Usage: command=analyse *startModule=[sentence+|tokenise|postag|parse] *endModule=[sentence|tokenise|postag|parse+] *inFile=[inFilePath, stdin if missing] *outFile=[outFilePath, stdout if missing] *template=[outputTemplatePath]");
			System.out.println("");
			System.out.println("Additional optional parameters:");
			System.out.println(" *encoding=[UTF-8, ...] *includeDetails=[true|false+] posTaggerRules*=[posTaggerRuleFilePath] regexFilters*=[regexFilterFilePath] *sentenceModel=[path] *tokeniserModel=[path] *posTaggerModel=[path] *parserModel=[path] *inputPatternFile=[inputPatternFilePath] *posTagSet=[posTagSetPath]");
			return;
		}
		
		String encoding = null;
		String inputEncoding = null;
		String outputEncoding = null;
		String builtInTemplate = null;
		
		String posTagSetPath = null;

		String transitionSystemStr = null;
		
		for (Entry<String,String> arg : args.entrySet()) {
			String argName = arg.getKey();
			String argValue = arg.getValue();
			if (argName.equals("command")) {
				String commandString = argValue;
				if (commandString.equals("analyze"))
					commandString = "analyse";
				
				command = Command.valueOf(commandString);
			} else if (argName.equals("module")) {
				if (argValue.equalsIgnoreCase("sentence"))
					module = Talismane.Module.SentenceDetector;
				else if (argValue.equalsIgnoreCase("tokenise"))
					module = Talismane.Module.Tokeniser;
				else if (argValue.equalsIgnoreCase("postag"))
					module = Talismane.Module.PosTagger;
				else if (argValue.equalsIgnoreCase("parse"))
					module = Talismane.Module.Parser;
				else
					throw new TalismaneException("Unknown module: " + argValue);
			} else if (argName.equals("startModule")) {
				if (argValue.equalsIgnoreCase("sentence"))
					startModule = Talismane.Module.SentenceDetector;
				else if (argValue.equalsIgnoreCase("tokenise"))
					startModule = Talismane.Module.Tokeniser;
				else if (argValue.equalsIgnoreCase("postag"))
					startModule = Talismane.Module.PosTagger;
				else if (argValue.equalsIgnoreCase("parse"))
					startModule = Talismane.Module.Parser;
				else
					throw new TalismaneException("Unknown startModule: " + argValue);
			} else if (argName.equals("endModule")) {
				if (argValue.equalsIgnoreCase("sentence"))
					endModule = Talismane.Module.SentenceDetector;
				else if (argValue.equalsIgnoreCase("tokenise"))
					endModule = Talismane.Module.Tokeniser;
				else if (argValue.equalsIgnoreCase("postag"))
					endModule = Talismane.Module.PosTagger;
				else if (argValue.equalsIgnoreCase("parse"))
					endModule = Talismane.Module.Parser;
				else
					throw new TalismaneException("Unknown endModule: " + argValue);
			} else if (argName.equals("inFile"))
				inFilePath = argValue;
			else if (argName.equals("outFile")) 
				outFilePath = argValue;
			else if (argName.equals("outDir")) 
				outDirPath = argValue;
			else if (argName.equals("template")) 
				templatePath = argValue;
			else if (argName.equals("builtInTemplate")) {
				builtInTemplate = argValue;
			}
			else if (argName.equals("encoding")) {
				if (inputEncoding!=null || outputEncoding !=null)
					throw new TalismaneException("The parameter 'encoding' cannot be used with 'inputEncoding' or 'outputEncoding'");
				encoding = argValue;
			} else if (argName.equals("inputEncoding")) {
				if (encoding !=null)
					throw new TalismaneException("The parameter 'encoding' cannot be used with 'inputEncoding' or 'outputEncoding'");
				inputEncoding = argValue;
			} else if (argName.equals("outputEncoding")) {
				if (encoding !=null)
					throw new TalismaneException("The parameter 'encoding' cannot be used with 'inputEncoding' or 'outputEncoding'");
				outputEncoding = argValue;
			} else if (argName.equals("includeDetails"))
				includeDetails = argValue.equalsIgnoreCase("true");
			else if (argName.equals("propagateBeam"))
				propagateBeam = argValue.equalsIgnoreCase("true");
			else if (argName.equals("beamWidth"))
				beamWidth = Integer.parseInt(argValue);
			else if (argName.equals("sentenceModel"))
				sentenceModelFilePath = argValue;
			else if (argName.equals("tokeniserModel"))
				tokeniserModelFilePath = argValue;
			else if (argName.equals("posTaggerModel"))
				posTaggerModelFilePath = argValue;
			else if (argName.equals("parserModel"))
				parserModelFilePath = argValue;
			else if (argName.equals("inputPatternFile"))
				inputPatternFilePath = argValue;
			else if (argName.equals("inputPattern"))
				inputRegex = argValue;
			else if (argName.equals("posTaggerRules"))
				posTaggerRuleFilePath = argValue;
			else if (argName.equals("posTagSet"))
				posTagSetPath = argValue;
			else if (argName.equals("textFilters"))
				textFiltersPath = argValue;
			else if (argName.equals("tokenFilters"))
				tokenFiltersPath = argValue;
			else if (argName.equals("logPerformance")) {
				logPerformance = argValue.equalsIgnoreCase("true");
			} else if (argName.equals("newline"))
				newlineMarker = MarkerFilterType.valueOf(argValue);
			else if (argName.equals("fileName"))
				fileName = argValue;
			else if (argName.equals("processByDefault"))
				processByDefault = argValue.equalsIgnoreCase("true");
			else if (argName.equals("maxParseAnalysisTime"))
				maxParseAnalysisTime = Integer.parseInt(argValue);
			else if (argName.equals("transitionSystem"))
				transitionSystemStr = argValue;
			else if (argName.equals("sentenceCount"))
				maxSentenceCount = Integer.parseInt(argValue);
			else if (argName.equals("endBlockCharCode")) {
				endBlockCharacter = (char) Integer.parseInt(argValue);
			}
			else {
				System.out.println("Unknown argument: " + argName);
				throw new RuntimeException("Unknown argument: " + argName);
			}
		}
		
		if (command==null)
			throw new TalismaneException("No command provided.");
		
		if (command.equals(Command.evaluate)) {
			if (outDirPath.length()==0)
				throw new RuntimeException("Missing argument: outdir");
		}
		
		if (command.equals(Command.evaluate)||command.equals(Command.process)) {
			startModule = module;
			endModule = module;
		}

		if (builtInTemplate!=null && builtInTemplate.equalsIgnoreCase("with_location")) {
			tokeniserTemplateName = "tokeniser_template_with_location.ftl";
			posTaggerTemplateName = "posTagger_template_with_location.ftl";
			parserTemplateName = "parser_conll_template_with_location.ftl";
		}
		
		inputCharset = Charset.defaultCharset();
		outputCharset = Charset.defaultCharset();
		if (encoding!=null) {
			inputCharset = Charset.forName(encoding);
			outputCharset = Charset.forName(encoding);
		} else {
			if (inputEncoding!=null)
				inputCharset = Charset.forName(inputEncoding);
			if (outputEncoding!=null)
				outputCharset = Charset.forName(outputEncoding);
		}

		if (fileName==null && inFilePath!=null) {
			fileName = inFilePath;
		}
		
		if (posTagSetPath!=null) {
			File posTagSetFile = new File(posTagSetPath);
			Scanner posTagSetScanner = new Scanner(posTagSetFile);
			PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
			TalismaneSession.setPosTagSet(posTagSet);
		}
		
		if (transitionSystemStr!=null) {
			TransitionSystem transitionSystem = null;
			if (transitionSystemStr.equalsIgnoreCase("ShiftReduce")) {
				transitionSystem = this.getParserService().getShiftReduceTransitionSystem();
			} else if (transitionSystemStr.equalsIgnoreCase("ArcEager")) {
				transitionSystem = this.getParserService().getArcEagerTransitionSystem();
			} else {
				throw new TalismaneException("Unknown transition system: " + transitionSystemStr);
			}
			TalismaneSession.setTransitionSystem(transitionSystem);
		}
	}

	/**
	 * The actual command to run by Talismane.
	 * @return
	 */
	public Command getCommand() {
		return command;
	}
	public void setCommand(Command command) {
		this.command = command;
	}

	/**
	 * If the command required a start module (e.g. analyse), the start module for this command.
	 * Default is {@link com.joliciel.talismane.Talismane.Module#SentenceDetector}.
	 * @return
	 */
	public Module getStartModule() {
		return startModule;
	}
	public void setStartModule(Module startModule) {
		this.startModule = startModule;
	}

	/**
	 * If the command requires an end module (e.g. analyse), the end module for this command.
	 * Default is {@link com.joliciel.talismane.Talismane.Module#Parser}.
	 * @return
	 */
	public Module getEndModule() {
		return endModule;
	}
	public void setEndModule(Module endModule) {
		this.endModule = endModule;
	}

	/**
	 * For commands which only affect a single module (e.g. evaluate), the module for this command.
	 * @return
	 */
	public Module getModule() {
		return module;
	}
	public void setModule(Module module) {
		this.module = module;
	}

	/**
	 * When analysing, should the raw text be processed by default, or should we wait until a text
	 * marker filter tells us to start processing. Default is true.
	 * @return
	 */
	public boolean isProcessByDefault() {
		return processByDefault;
	}
	public void setProcessByDefault(boolean processByDefault) {
		this.processByDefault = processByDefault;
	}

	/**
	 * For the "process" command, the maximum number of sentences to process. If <=0, all sentences
	 * will be processed. Default is 0 (all).
	 * @return
	 */
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	/**
	 * The charset that is used to interpret the input stream.
	 * @return
	 */
	public Charset getInputCharset() {
		return inputCharset;
	}
	public void setInputCharset(Charset inputCharset) {
		this.inputCharset = inputCharset;
	}

	/**
	 * The charset that is used to write to the output stream.
	 * @return
	 */
	public Charset getOutputCharset() {
		return outputCharset;
	}
	public void setOutputCharset(Charset outputCharset) {
		this.outputCharset = outputCharset;
	}

	/**
	 * A character (typically non-printing) which will mark a stop in the input stream and set-off analysis.
	 * The default value is the form-feed character (code=12).
	 * @return
	 */
	public char getEndBlockCharacter() {
		return endBlockCharacter;
	}
	public void setEndBlockCharacter(char endBlockCharacter) {
		this.endBlockCharacter = endBlockCharacter;
	}

	/**
	 * The beam width for beam-search analysis. Default is 1.
	 * Increasing this value will increase analysis time in a linear fashion, but will typically improve results.
	 * @return
	 */
	public int getBeamWidth() {
		return beamWidth;
	}
	public void setBeamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
	}

	/**
	 * If true, the full beam of analyses produced as output by a given module will be used as input for the next module.
	 * If false, only the single best analysis will be used as input for the next module.
	 * @return
	 */
	public boolean isPropagateBeam() {
		return propagateBeam;
	}
	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	/**
	 * If true, a generates a very detailed analysis on how Talismane obtained the results it displays.
	 * @return
	 */
	public boolean isIncludeDetails() {
		return includeDetails;
	}
	public void setIncludeDetails(boolean includeDetails) {
		this.includeDetails = includeDetails;
	}

	/**
	 * The reader to be used to read the data for this analysis.
	 * @return
	 */
	public Reader getReader() {
		if (this.reader==null) {
			if (inFilePath!=null) {
				try {
					File inFile = new File(inFilePath);
					this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), this.getInputCharset()));
				} catch (FileNotFoundException fnfe) {
					LogUtils.logError(LOG, fnfe);
					throw new RuntimeException(fnfe);
				}
			} else {
				this.reader = new BufferedReader(new InputStreamReader(System.in, this.getInputCharset()));
			}
		}
		return reader;
	}

	/**
	 * A writer to which Talismane should write its output when analysing.
	 * @return
	 */
	public Writer getWriter() {
		try {
			if (writer==null) {
				if (outFilePath!=null) {
					if (outFilePath.lastIndexOf("/")>=0) {
						String outFileDirPath = outFilePath.substring(0, outFilePath.lastIndexOf("/"));
						File outFileDir = new File(outFileDirPath);
						outFileDir.mkdirs();
					}
					File outFile = new File(outFilePath);
					outFile.delete();
					outFile.createNewFile();
				
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), this.getOutputCharset()));
				} else {
					writer = new BufferedWriter(new OutputStreamWriter(System.out, this.getOutputCharset()));
				}
			}
			return writer;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The filename to be applied to this analysis (if filename is included in the output).
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Whether or not we should log performance for this run.
	 * @return
	 */
	public boolean isLogPerformance() {
		return logPerformance;
	}
	public void setLogPerformance(boolean logPerformance) {
		this.logPerformance = logPerformance;
	}

	/**
	 * The directory to which we write any output files.
	 * @return
	 */
	public File getOutDir() {
		if (outDirPath!=null) {
			outDir = new File(outDirPath);
			outDir.mkdirs();
		} else if (outFilePath!=null) {
			if (outFilePath.lastIndexOf("/")>=0) {
				String outFileDirPath = outFilePath.substring(0, outFilePath.lastIndexOf("/"));
				outDir = new File(outFileDirPath);
				outDir.mkdirs();
			}
		}
		return outDir;
	}

	/**
	 * The rules to apply when running the pos-tagger.
	 * @return
	 */
	public List<PosTaggerRule> getPosTaggerRules() {
		try {
			if (posTaggerRules == null) {
				posTaggerRules = new ArrayList<PosTaggerRule>();
				for (int i=0; i<=1; i++) {
					Scanner rulesScanner = null;
					if (i==0) {
						InputStream defaultRulesStream = implementation.getDefaultPosTaggerRulesFromStream();
						if (defaultRulesStream!=null)
							rulesScanner = new Scanner(defaultRulesStream);
					} else {
						if (posTaggerRuleFilePath!=null && posTaggerRuleFilePath.length()>0) {
							File posTaggerRuleFile = new File(posTaggerRuleFilePath);
							rulesScanner = new Scanner(posTaggerRuleFile);
						}
					}
					
					if (rulesScanner!=null) {
						List<String> ruleDescriptors = new ArrayList<String>();
						while (rulesScanner.hasNextLine()) {
							String ruleDescriptor = rulesScanner.nextLine();
							if (ruleDescriptor.length()>0) {
								ruleDescriptors.add(ruleDescriptor);
								LOG.debug(ruleDescriptor);
							}
						}
						List<PosTaggerRule> rules = this.getPosTaggerFeatureService().getRules(ruleDescriptors);
						posTaggerRules.addAll(rules);
						
					}
				}
			}
			return posTaggerRules;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A regex used to process the input, when pre-annotated.
	 * @return
	 */
	public String getInputRegex() {
		try {
			if (inputRegex==null && inputPatternFilePath!=null && inputPatternFilePath.length()>0) {
				Scanner inputPatternScanner = null;
				File inputPatternFile = new File(inputPatternFilePath);
				inputPatternScanner = new Scanner(inputPatternFile);
				if (inputPatternScanner.hasNextLine()) {
					inputRegex = inputPatternScanner.nextLine();
				}
				inputPatternScanner.close();
				if (inputRegex==null)
					throw new TalismaneException("No input pattern found in " + inputPatternFilePath);
			}
			return inputRegex;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Text marker filters are applied to raw text segments extracted from the stream, 3 segments at a time.
	 * This means that if a particular marker crosses segment borders, it is handled correctly.
	 * @return
	 */
	public List<TextMarkerFilter> getTextMarkerFilters() {
		try {
			if (textMarkerFilters==null) {
				textMarkerFilters = new ArrayList<TextMarkerFilter>();
				
				// insert sentence breaks at end of block
				this.addTextMarkerFilter(this.getFilterService().getRegexMarkerFilter(new MarkerFilterType[] { MarkerFilterType.SENTENCE_BREAK }, "" + endBlockCharacter));
				
				// handle newline as requested
				if (newlineMarker.equals(MarkerFilterType.SENTENCE_BREAK))
					this.addTextMarkerFilter(this.getFilterService().getNewlineEndOfSentenceMarker());
				else if (newlineMarker.equals(MarkerFilterType.SPACE))
					this.addTextMarkerFilter(this.getFilterService().getNewlineSpaceMarker());
				
				// get rid of duplicate white-space always
				this.addTextMarkerFilter(this.getFilterService().getDuplicateWhiteSpaceFilter());
	
				for (int i=0; i<=1; i++) {
					LOG.debug("Text marker filters");
					Scanner textFilterScanner = null;
					if (i==0) {
						if (textFiltersPath!=null && textFiltersPath.length()>0) {
							LOG.debug("From: " + textFiltersPath);
							File textFilterFile = new File(textFiltersPath);
							textFilterScanner = new Scanner(textFilterFile);
						}
					} else {
						InputStream stream = implementation.getDefaultTextMarkerFiltersFromStream();
						if (stream!=null) {
							LOG.debug("From default");
							textFilterScanner = new Scanner(stream);
						}
					}
					if (textFilterScanner!=null) {
						while (textFilterScanner.hasNextLine()) {
							String descriptor = textFilterScanner.nextLine();
							LOG.debug(descriptor);
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TextMarkerFilter textMarkerFilter = this.getFilterService().getTextMarkerFilter(descriptor);
								this.addTextMarkerFilter(textMarkerFilter);
							}
						}
					}
				}
				
			}
			return textMarkerFilters;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public void setTextMarkerFilters(List<TextMarkerFilter> textMarkerFilters) {
		this.textMarkerFilters = textMarkerFilters;
	}

	public void addTextMarkerFilter(TextMarkerFilter textMarkerFilter) {
		this.textMarkerFilters.add(textMarkerFilter);
	}
	
	/**
	 * TokenFilters to be applied during analysis.
	 * @return
	 */
	public List<TokenFilter> getTokenFilters() {
		try {
			if (tokenFilters==null) {
				tokenFilters = new ArrayList<TokenFilter>();
				for (int i=0; i<=1; i++) {
					LOG.debug("Token filters");
					Scanner tokenFilterScanner = null;
					if (i==0) {
						if (tokenFiltersPath!=null && tokenFiltersPath.length()>0) {
							LOG.debug("From: " + tokenFiltersPath);
							File tokenFilterFile = new File(tokenFiltersPath);
							tokenFilterScanner = new Scanner(tokenFilterFile);
						}
					} else {
						InputStream stream = implementation.getDefaultTokenFiltersFromStream();
						if (stream!=null) {
							LOG.debug("From default");
							tokenFilterScanner = new Scanner(stream);
						}
					}
					if (tokenFilterScanner!=null) {
						while (tokenFilterScanner.hasNextLine()) {
							String descriptor = tokenFilterScanner.nextLine();
							LOG.debug(descriptor);
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
								tokenFilters.add(tokenFilter);
							}
						}
					}
				}
			}
			return tokenFilters;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public void setTokenFilters(List<TokenFilter> tokenFilters) {
		this.tokenFilters = tokenFilters;
	}

	/**
	 * The sentence detector to use for analysis.
	 * @return
	 */
	public SentenceDetector getSentenceDetector() {
		try {
			if (sentenceDetector==null) {
				LOG.debug("Getting sentence detector model");
				MachineLearningModel<SentenceDetectorOutcome> sentenceModel = null;
				if (sentenceModelFilePath!=null) {
					sentenceModel = this.getMachineLearningService().getModel(new ZipInputStream(new FileInputStream(sentenceModelFilePath)));
				} else {
					sentenceModel = this.getMachineLearningService().getModel(implementation.getDefaultSentenceModelStream());
				}
				Set<SentenceDetectorFeature<?>> sentenceDetectorFeatures =
					this.getSentenceDetectorFeatureService().getFeatureSet(sentenceModel.getFeatureDescriptors());
				sentenceDetector = this.getSentenceDetectorService().getSentenceDetector(sentenceModel.getDecisionMaker(), sentenceDetectorFeatures);
			}
			return sentenceDetector;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The tokeniser to use for analysis.
	 * @return
	 */
	public Tokeniser getTokeniser() {
		try {
			if (tokeniser==null) {
				LOG.debug("Getting tokeniser model");
				MachineLearningModel<TokeniserOutcome> tokeniserModel = null;
				if (tokeniserModelFilePath!=null) {
					tokeniserModel = this.getMachineLearningService().getModel(new ZipInputStream(new FileInputStream(tokeniserModelFilePath)));
				} else {
					tokeniserModel = this.getMachineLearningService().getModel(implementation.getDefaultTokeniserModelStream());
				}
				
				TokeniserPatternManager tokeniserPatternManager = this.getTokeniserPatternService().getPatternManager(tokeniserModel.getDescriptors().get(TokeniserPatternService.PATTERN_DESCRIPTOR_KEY));
				Set<TokeniserContextFeature<?>> tokeniserContextFeatures = this.getTokenFeatureService().getTokeniserContextFeatureSet(tokeniserModel.getFeatureDescriptors(), tokeniserPatternManager.getParsedTestPatterns());
				tokeniser = this.getTokeniserPatternService().getPatternTokeniser(tokeniserPatternManager, tokeniserContextFeatures, tokeniserModel.getDecisionMaker(), beamWidth);
	
				if (includeDetails) {
					String detailsFilePath = this.getBaseName() + "_tokeniser_details.txt";
					File detailsFile = new File(detailsFilePath);
					detailsFile.delete();
					AnalysisObserver observer = tokeniserModel.getDetailedAnalysisObserver(detailsFile);
					tokeniser.addObserver(observer);
				}
				
				for (TokenFilter tokenFilter : this.getTokenFilters()) {
					tokeniser.addTokenFilter(tokenFilter);
					if (this.needsSentenceDetector()) {
						this.getSentenceDetector().addTokenFilter(tokenFilter);
					}
				}
	
				List<String> tokenFilterDescriptors = tokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
				if (tokenFilterDescriptors!=null) {
					for (String descriptor : tokenFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
							tokeniser.addTokenFilter(tokenFilter);
							if (this.needsSentenceDetector()) {
								this.getSentenceDetector().addTokenFilter(tokenFilter);
							}
						}
					}
				}
				
				for (TokenSequenceFilter tokenFilter : implementation.getTokenSequenceFilters()) {
					tokeniser.addTokenSequenceFilter(tokenFilter);
				}
	
				List<String> tokenSequenceFilterDescriptors = tokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
				if (tokenSequenceFilterDescriptors!=null) {
					for (String descriptor : tokenSequenceFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							tokeniser.addTokenSequenceFilter(tokenSequenceFilter);
						}
					}
				}
			}
			return tokeniser;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	MachineLearningModel<PosTag> getPosTaggerModel() {
		try {
			if (posTaggerModel==null) {
				if (posTaggerModelFilePath!=null) {
					posTaggerModel = this.getMachineLearningService().getModel(new ZipInputStream(new FileInputStream(posTaggerModelFilePath)));
				} else {
					posTaggerModel = this.getMachineLearningService().getModel(implementation.getDefaultPosTaggerModelStream());
				}
			}
			return posTaggerModel;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	MachineLearningModel<Transition> getParserModel() {
		try {
			if (parserModel==null) {
				if (parserModelFilePath!=null) {
					parserModel = this.getMachineLearningService().getModel(new ZipInputStream(new FileInputStream(parserModelFilePath)));
				} else {
					parserModel = this.getMachineLearningService().getModel(implementation.getDefaultParserModelStream());
				}
			}
			return parserModel;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * The pos-tagger to use for analysis.
	 * @return
	 */
	public PosTagger getPosTagger() {
		try {
			if (posTagger==null) {
				LOG.debug("Getting pos-tagger model");
				
				MachineLearningModel<PosTag> posTaggerModel = this.getPosTaggerModel();
				Set<PosTaggerFeature<?>> posTaggerFeatures = this.getPosTaggerFeatureService().getFeatureSet(posTaggerModel.getFeatureDescriptors());
				posTagger = this.getPosTaggerService().getPosTagger(posTaggerFeatures, posTaggerModel.getDecisionMaker(), beamWidth);
				
				List<String> posTaggerPreprocessingFilters = posTaggerModel.getDescriptors().get(PosTagger.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
				if (posTaggerPreprocessingFilters!=null) {
					for (String descriptor : posTaggerPreprocessingFilters) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							posTagger.addPreprocessingFilter(tokenSequenceFilter);
						}
					}
				}
				
				for (TokenSequenceFilter tokenFilter : implementation.getPosTaggerPreprocessingFilters()) {
					posTagger.addPreprocessingFilter(tokenFilter);
				}
				posTagger.setPosTaggerRules(this.getPosTaggerRules());
		
				if (includeDetails) {
					String detailsFilePath = outFilePath.substring(0, outFilePath.lastIndexOf(".")) + "_posTagger_details.txt";
					File detailsFile = new File(detailsFilePath);
					detailsFile.delete();
					AnalysisObserver observer = posTaggerModel.getDetailedAnalysisObserver(detailsFile);
					posTagger.addObserver(observer);
				}
			}
			return posTagger;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The parser to use for analysis.
	 * @return
	 */
	public Parser getParser() {
		try {
			if (parser==null) {
				LOG.debug("Getting parser model");
				MachineLearningModel<Transition> parserModel = this.getParserModel();
				
				parser = this.getParserService().getTransitionBasedParser(parserModel, beamWidth);
				parser.setMaxAnalysisTimePerSentence(maxParseAnalysisTime);
				
				if (includeDetails) {
					String detailsFilePath = outFilePath.substring(0, outFilePath.lastIndexOf(".")) + "_parser_details.txt";
					File detailsFile = new File(detailsFilePath);
					detailsFile.delete();
					AnalysisObserver observer = parserModel.getDetailedAnalysisObserver(detailsFile);
					parser.addObserver(observer);
				}
				TalismaneSession.setTransitionSystem(parser.getTransitionSystem());

			}
			return parser;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The maximum amount of time the parser will spend analysing any single sentence, in seconds.
	 * If it exceeds this time, the parser will return a partial analysis, or a "dependency forest",
	 * where certain nodes are left unattached (no governor).<br/>
	 * A value of 0 indicates that there is no maximum time -
	 * the parser will always continue until sentence analysis is complete.<br/>
	 * The default value is 60.<br/>
	 * @return
	 */
	public int getMaxParseAnalysisTime() {
		return maxParseAnalysisTime;
	}

	public void setMaxParseAnalysisTime(int maxParseAnalysisTime) {
		this.maxParseAnalysisTime = maxParseAnalysisTime;
	}
	
	/**
	 * A sentence processor to process sentences that have been read.
	 * @return
	 */
	public SentenceProcessor getSentenceProcessor() {
		try {
			if (sentenceProcessor==null && endModule.equals(Module.SentenceDetector)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(sentenceTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(this.getWriter(), templateReader);
				sentenceProcessor=templateWriter;
			}
			return sentenceProcessor;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A token sequence processor to process token sequences that have been read.
	 * @return
	 */
	public TokenSequenceProcessor getTokenSequenceProcessor() {
		try {
			if (tokenSequenceProcessor==null && endModule.equals(Module.Tokeniser)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(tokeniserTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(this.getWriter(), templateReader);
				tokenSequenceProcessor = templateWriter;
			}
			return tokenSequenceProcessor;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A pos-tag sequence processor to process pos-tag sequences that have been read.
	 * @return
	 */
	public PosTagSequenceProcessor getPosTagSequenceProcessor() {
		try {
			if (posTagSequenceProcessor==null && endModule.equals(Module.PosTagger)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(posTaggerTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(this.getWriter(), templateReader);
				posTagSequenceProcessor = templateWriter;
			}
			return posTagSequenceProcessor;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A parse configuration processor to process parse configurations that have been read.
	 * @return
	 */
	public ParseConfigurationProcessor getParseConfigurationProcessor() {
		try {
			if (parseConfigurationProcessor==null && endModule.equals(Module.Parser)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(parserTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(this.getWriter(), templateReader);
				parseConfigurationProcessor = templateWriter;
			}
			return parseConfigurationProcessor;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A token corpus reader to read a corpus pre-annotated in tokens.
	 * @return
	 */
	public TokenRegexBasedCorpusReader getTokenCorpusReader() {
		if (tokenCorpusReader==null) {
			tokenCorpusReader = this.getTokeniserService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex()!=null)
				tokenCorpusReader.setRegex(this.getInputRegex());
			
			if (startModule.equals(Module.PosTagger)) {
				MachineLearningModel<PosTag> posTaggerModel = this.getPosTaggerModel();
				
				List<String> tokenFilterDescriptors = posTaggerModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
				if (tokenFilterDescriptors!=null) {
					for (String descriptor : tokenFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
							tokenCorpusReader.addTokenFilter(tokenFilter);
						}
					}
				}
				
				List<String> tokenSequenceFilterDescriptors = posTaggerModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
				if (tokenSequenceFilterDescriptors!=null) {
					for (String descriptor : tokenSequenceFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							tokenCorpusReader.addTokenSequenceFilter(tokenSequenceFilter);
						}
					}
				}
			}
			
			for (TokenSequenceFilter tokenSequenceFilter : implementation.getTokenSequenceFilters()) {
				tokenCorpusReader.addTokenSequenceFilter(tokenSequenceFilter);
			}
		}
		return tokenCorpusReader;
	}

	/**
	 * A pos tag corpus reader to read a corpus pre-annotated in postags.
	 * @return
	 */
	public PosTagRegexBasedCorpusReader getPosTagCorpusReader() {
		if (posTagCorpusReader==null) {
			posTagCorpusReader = this.getPosTaggerService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex()!=null)
				posTagCorpusReader.setRegex(this.getInputRegex());
			
			if (startModule.equals(Module.Parser)) {
				MachineLearningModel<Transition> parserModel = this.getParserModel();
				
				List<String> tokenFilterDescriptors = parserModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
				if (tokenFilterDescriptors!=null) {
					List<TokenFilter> parserTokenFilters = new ArrayList<TokenFilter>();
					for (String descriptor : tokenFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
							parserTokenFilters.add(tokenFilter);
						}
					}
					TokenSequenceFilter tokenFilterWrapper = this.getTokenFilterService().getTokenSequenceFilter(parserTokenFilters);
					posTagCorpusReader.addTokenSequenceFilter(tokenFilterWrapper);
				}
				
				List<String> tokenSequenceFilterDescriptors = parserModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
				if (tokenSequenceFilterDescriptors!=null) {
					for (String descriptor : tokenSequenceFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							posTagCorpusReader.addTokenSequenceFilter(tokenSequenceFilter);
						}
					}
				}
				
				List<String> posTaggerPreprocessingFilters = parserModel.getDescriptors().get(PosTagger.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
				if (posTaggerPreprocessingFilters!=null) {
					for (String descriptor : posTaggerPreprocessingFilters) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							posTagCorpusReader.addTokenSequenceFilter(tokenSequenceFilter);
						}
					}
				}
			}
							
			for (TokenSequenceFilter tokenFilter : implementation.getTokenSequenceFilters()) {
				posTagCorpusReader.addTokenSequenceFilter(tokenFilter);
			}
			for (TokenSequenceFilter tokenFilter : implementation.getPosTaggerPreprocessingFilters()) {
				posTagCorpusReader.addTokenSequenceFilter(tokenFilter);
			}
		}
		return posTagCorpusReader;
	}

	/**
	 * A parser corpus reader to read a corpus pre-annotated in dependencies.
	 * @return
	 */
	public ParserRegexBasedCorpusReader getParserCorpusReader() {
		if (parserCorpusReader==null) {
			parserCorpusReader = this.getParserService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex()!=null)
				parserCorpusReader.setRegex(this.getInputRegex());
			
			for (TokenSequenceFilter tokenFilter : implementation.getTokenSequenceFilters()) {
				parserCorpusReader.addTokenSequenceFilter(tokenFilter);
			}
			for (TokenSequenceFilter tokenFilter : implementation.getPosTaggerPreprocessingFilters()) {
				parserCorpusReader.addTokenSequenceFilter(tokenFilter);
			}

		}
		return parserCorpusReader;
	}

	/**
	 * Get a parser evaluator if command=evaluate and module=parser.
	 * @return
	 */
	public ParserEvaluator getParserEvaluator() {
		try {
			if (parserEvaluator==null) {
				Writer csvFileWriter = null;
				boolean includeSentences = true;
				if (includeSentences) {
					File csvFile = new File(outDir, baseName + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
				}
				
				parserEvaluator = this.getParserService().getParserEvaluator();
				parserEvaluator.setParser(this.getParser());
				parserEvaluator.setCsvFileWriter(csvFileWriter);
			}
			
			return parserEvaluator;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a pos-tagger evaluator if command=evaluate and module=pos-tagger.
	 * @return
	 */
	public PosTaggerEvaluator getPosTaggerEvaluator() {
		try {
			if (posTaggerEvaluator==null) {
				Writer csvFileWriter = null;
				boolean includeSentences = true;
				if (includeSentences) {
					File csvFile = new File(outDir, baseName + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
				}
				
				posTaggerEvaluator = posTaggerService.getPosTaggerEvaluator(this.getPosTagger(), csvFileWriter);
				posTaggerEvaluator.setPropagateBeam(propagateBeam);
			}
			return posTaggerEvaluator;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The base name, out of which to construct output file names.
	 * @return
	 */
	public String getBaseName() {
		if (baseName==null) {
			baseName = "Talismane";
			if (outFilePath!=null) {
				if (outFilePath.indexOf('.')>0)
					baseName = outFilePath.substring(outFilePath.lastIndexOf('/')+1, outFilePath.indexOf('.'));
				else
					baseName = outFilePath.substring(outFilePath.lastIndexOf('/')+1);
			} else if (inFilePath!=null) {
				if (inFilePath.indexOf('.')>0)
					baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1, inFilePath.indexOf('.'));
				else
					baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1);
			} else if (sentenceModelFilePath!=null && module.equals(Talismane.Module.SentenceDetector)||endModule.equals(Talismane.Module.SentenceDetector)) {
				if (sentenceModelFilePath.indexOf('.')>0)
					baseName = sentenceModelFilePath.substring(sentenceModelFilePath.lastIndexOf('/')+1, sentenceModelFilePath.indexOf('.'));
				else
					baseName = sentenceModelFilePath.substring(sentenceModelFilePath.lastIndexOf('/')+1);
			} else if (tokeniserModelFilePath!=null && module.equals(Talismane.Module.Tokeniser)||endModule.equals(Talismane.Module.Tokeniser)) {
				if (tokeniserModelFilePath.indexOf('.')>0)
					baseName = tokeniserModelFilePath.substring(tokeniserModelFilePath.lastIndexOf('/')+1, tokeniserModelFilePath.indexOf('.'));
				else
					baseName = tokeniserModelFilePath.substring(tokeniserModelFilePath.lastIndexOf('/')+1);
			} else if (posTaggerModelFilePath!=null && module.equals(Talismane.Module.PosTagger)||endModule.equals(Talismane.Module.PosTagger)) {
				if (posTaggerModelFilePath.indexOf('.')>0)
					baseName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1, posTaggerModelFilePath.indexOf('.'));
				else
					baseName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1);
			} else if (parserModelFilePath!=null && module.equals(Talismane.Module.Parser)||endModule.equals(Talismane.Module.Parser)) {
				if (parserModelFilePath.indexOf('.')>0)
					baseName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1, parserModelFilePath.indexOf('.'));
				else
					baseName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1);
			}
		}
		return baseName;
	}

	public PosTaggerService getPosTaggerService() {
		if (posTaggerService==null) {
			posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
		}
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		if (parserService==null) {
			parserService = talismaneServiceLocator.getParserServiceLocator().getParserService();
		}
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public PosTaggerFeatureService getPosTaggerFeatureService() {
		if (posTaggerFeatureService==null) {
			posTaggerFeatureService = talismaneServiceLocator.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService();
		}
		return posTaggerFeatureService;
	}

	public void setPosTaggerFeatureService(
			PosTaggerFeatureService posTaggerFeatureService) {
		this.posTaggerFeatureService = posTaggerFeatureService;
	}

	public FilterService getFilterService() {
		if (filterService==null) {
			filterService = talismaneServiceLocator.getFilterServiceLocator().getFilterService();
		}
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	public TokenFilterService getTokenFilterService() {
		if (tokenFilterService==null) {
			tokenFilterService = talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService();
		}
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	public SentenceDetectorService getSentenceDetectorService() {
		if (sentenceDetectorService==null) {
			sentenceDetectorService=talismaneServiceLocator.getSentenceDetectorServiceLocator().getSentenceDetectorService();
		}
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(
			SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		if (sentenceDetectorFeatureService==null) {
			sentenceDetectorFeatureService = talismaneServiceLocator.getSentenceDetectorFeatureServiceLocator().getSentenceDetectorFeatureService();
		}
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(
			SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	public MachineLearningService getMachineLearningService() {
		if (machineLearningService==null) {
			machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();
		}
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		if (tokeniserPatternService==null) {
			tokeniserPatternService = talismaneServiceLocator.getTokenPatternServiceLocator().getTokeniserPatternService();
		}
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(
			TokeniserPatternService tokeniserPatternService) {
		this.tokeniserPatternService = tokeniserPatternService;
	}

	public TokenFeatureService getTokenFeatureService() {
		if (tokenFeatureService==null) {
			tokenFeatureService = talismaneServiceLocator.getTokenFeatureServiceLocator().getTokenFeatureService();
		}
		return tokenFeatureService;
	}

	public void setTokenFeatureService(TokenFeatureService tokenFeatureService) {
		this.tokenFeatureService = tokenFeatureService;
	}
	
	
	public TokeniserService getTokeniserService() {
		if (this.tokeniserService==null)
			this.tokeniserService = talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService();
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	/**
	 * Does this instance of Talismane need a sentence detector to perform the requested processing.
	 */
	public boolean needsSentenceDetector() {
		return startModule.compareTo(Module.SentenceDetector)<=0 && endModule.compareTo(Module.SentenceDetector)>=0;
	}
	
	/**
	 * Does this instance of Talismane need a tokeniser to perform the requested processing.
	 */
	public boolean needsTokeniser() {
		return startModule.compareTo(Module.Tokeniser)<=0 && endModule.compareTo(Module.Tokeniser)>=0;
	}

	/**
	 * Does this instance of Talismane need a pos tagger to perform the requested processing.
	 */
	public boolean needsPosTagger() {
		return startModule.compareTo(Module.PosTagger)<=0 && endModule.compareTo(Module.PosTagger)>=0;
	}
	
	/**
	 * Does this instance of Talismane need a parser to perform the requested processing.
	 */
	public boolean needsParser() {
		return startModule.compareTo(Module.Parser)<=0 && endModule.compareTo(Module.Parser)>=0;
	}
	
	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/output/" + resource;
		InputStream inputStream = Talismane.class.getResourceAsStream(path); 
		
		return inputStream;
	}
}