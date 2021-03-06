package com.joliciel.talismane.tokeniser;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.LogUtils;

public class TokenEvaluationCorpusWriter implements TokenEvaluationObserver {
	private static final Log LOG = LogFactory.getLog(TokenEvaluationCorpusWriter.class);
	private Writer corpusWriter;
	
	public TokenEvaluationCorpusWriter(Writer corpusWriter) {
		super();
		this.corpusWriter = corpusWriter;
	}

	@Override
	public void onNextTokenSequence(TokenSequence realSequence,
			List<TokenisedAtomicTokenSequence> guessedAtomicSequences) {
		try {
			List<Integer> realSplits = realSequence.getTokenSplits();
			
			TokenisedAtomicTokenSequence tokenisedAtomicTokenSequence = guessedAtomicSequences.get(0);

			Map<Integer,TokeniserOutcome> realOutcomes = new HashMap<Integer, TokeniserOutcome>();
			Map<Integer,TokeniserOutcome> guessedOutcomes = new HashMap<Integer, TokeniserOutcome>();
			Map<Integer,List<String>> guessedAuthorities = new HashMap<Integer, List<String>>();
			List<Integer> indexes = new ArrayList<Integer>();
			
			corpusWriter.write(realSequence.getText() + "\n");
			for (TaggedToken<TokeniserOutcome> guessTag : tokenisedAtomicTokenSequence) {
				TokeniserOutcome guessDecision = guessTag.getTag();
				int startIndex = guessTag.getToken().getStartIndex();
				boolean realSplit = realSplits.contains(startIndex);
				
				TokeniserOutcome realDecision = realSplit ? TokeniserOutcome.SEPARATE : TokeniserOutcome.JOIN;
				
				indexes.add(startIndex);
				realOutcomes.put(startIndex, realDecision);
				guessedOutcomes.put(startIndex, guessDecision);
				guessedAuthorities.put(startIndex, guessTag.getDecision().getAuthorities());
			}
			
			int prevEndIndex = 0;
			int i=0;
			for (Token token : realSequence) {
				corpusWriter.write(token.getOriginalText());
				Set<String> authorities = new TreeSet<String>();
				boolean correct = true;
				for (int index : indexes) {
					if (prevEndIndex <= index && index < token.getEndIndex()) {
						correct = correct && realOutcomes.get(index)==guessedOutcomes.get(index);
						authorities.addAll(guessedAuthorities.get(index));
					}
				}
				if (realSequence.getPosTagSequence()!=null) {
					PosTaggedToken posTaggedToken = realSequence.getPosTagSequence().get(i);
					corpusWriter.write("\t" + posTaggedToken.getTag().getCode());
				}
				corpusWriter.write("\t" + correct);
				for (String authority : authorities) {
					if (!authority.startsWith("_")) {
						corpusWriter.write("\t" + authority);
					}
				}
				corpusWriter.write("\n");
				corpusWriter.flush();
				prevEndIndex = token.getEndIndex();
				i++;
			}
			corpusWriter.write("\n");
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public void onEvaluationComplete() {
		try {
			corpusWriter.close();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

}
