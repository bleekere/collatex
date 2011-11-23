package eu.interedition.collatex2.implementation.vg_alignment;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import eu.interedition.collatex2.implementation.containers.witness.WitnessToken;
import eu.interedition.collatex2.implementation.input.NullToken;
import eu.interedition.collatex2.implementation.matching.EqualityTokenComparator;
import eu.interedition.collatex2.implementation.matching.Matches;
import eu.interedition.collatex2.implementation.vg_analysis.Sequence;
import eu.interedition.collatex2.interfaces.INormalizedToken;
import eu.interedition.collatex2.interfaces.ITokenLinker;
import eu.interedition.collatex2.interfaces.IVariantGraph;
import eu.interedition.collatex2.interfaces.IWitness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.collect.Lists.reverse;

public class TokenLinker implements ITokenLinker {
  private static final Logger LOG = LoggerFactory.getLogger(TokenLinker.class);

  @Override
  public Map<INormalizedToken, INormalizedToken> link(IVariantGraph graph, IWitness b) {
    final IWitness a = new Superbase(graph);

    LOG.trace("Matching tokens of {} and {}", a, b);
    Multimap<INormalizedToken, INormalizedToken> matches = Matches.between(a, b, new EqualityTokenComparator()).getAll();

    // add start and end tokens as matches
    matches.put(WitnessToken.START, a.getTokens().get(0));
    matches.put(WitnessToken.END, a.getTokens().get(a.size() - 1));

    LOG.trace("Matching tokens");
    Matches matchResult1 = Matches.between(a, b, new EqualityTokenComparator());

    LOG.trace("Finding minimal unique token sequences");
    final List<INormalizedToken> bTokens = b.getTokens();
    final int bTokenCount = bTokens.size();

    final List<List<INormalizedToken>> leftExpandingTokenSequences =  Lists.newArrayListWithExpectedSize(matchResult1.getAmbiguous().size());
    final List<List<INormalizedToken>> rightExpandingTokenSequences =  Lists.newArrayListWithExpectedSize(matchResult1.getAmbiguous().size());

    for (int tc = 0; tc < bTokenCount; tc++) {
      // for each ambiguous token
      if (matchResult1.getAmbiguous().contains(bTokens.get(tc))) {
        // find a minimal unique subsequence by walking to the left
        rightExpandingTokenSequences.add(reverse(findMinimalUniquePrefix(reverse(bTokens.subList(0, tc + 1)), matchResult1, WitnessToken.START)));
        // find a minimal unique subsequence by walking to the right
        leftExpandingTokenSequences.add(findMinimalUniquePrefix(bTokens.subList(tc, bTokenCount), matchResult1, WitnessToken.END));
      }
    }

    LOG.trace("Calculate 'aMatches': Ignore non matches from the base");
    List<INormalizedToken> aMatches = findMatches(a, matches.values());

    // try and find matches in the base for each sequence in the witness
    Map<List<INormalizedToken>, List<INormalizedToken>> linkedSequences = Maps.newLinkedHashMap();
    for (List<INormalizedToken> tokenSequence : rightExpandingTokenSequences) {
      List<INormalizedToken> matchedBaseTokens = findMatchingBaseTokensForSequenceToTheRight(tokenSequence, matches, aMatches);
      if (!matchedBaseTokens.isEmpty()) {
        linkedSequences.put(tokenSequence, matchedBaseTokens);
      }
    }
    for (List<INormalizedToken> tokenSequence : leftExpandingTokenSequences) {
      List<INormalizedToken> matchedBaseTokens = findMatchingBaseTokensForSequenceToTheLeft(tokenSequence, matches, aMatches);
      if (!matchedBaseTokens.isEmpty()) {
        linkedSequences.put(tokenSequence, matchedBaseTokens);
      }
    }

    // order the sequences here (first the sequences that expand to the left, then all sequences that expand to the right!)
    // and convert map<tokenseq, phrase> to List<Sequence>
    List<Sequence> sequences = Lists.newArrayList();
    for (List<INormalizedToken> tokenSequence : linkedSequences.keySet()) {
      sequences.add(new Sequence(linkedSequences.get(tokenSequence), tokenSequence));
    }
    // run the old filter method
    sequences = filterAwaySecondChoicesMultipleColumnsOneToken(Collections.unmodifiableList(sequences));
    sequences = filterAwaySecondChoicesMultipleTokensOneColumn(Collections.unmodifiableList(sequences));

    // do the matching
    matches = Matches.between(a, b, new EqualityTokenComparator()).getAll();

    // Calculate MatchResult
    Matches matchResult = Matches.between(a, b, new EqualityTokenComparator());
    // result map
    Map<INormalizedToken, INormalizedToken> alignedTokens = Maps.newLinkedHashMap();
    // put sure matches in the result map
    for (INormalizedToken token: matchResult.getUnique()) {
      alignedTokens.put(token, Iterables.getFirst(matches.get(token), null));
    }
    // add matched sequences to the aligned tokens
    for (Sequence sequence : sequences) {
      List<INormalizedToken> matchedBasePhrase = sequence.getBasePhrase();
      Iterator<INormalizedToken> iterator = matchedBasePhrase.iterator();
      for (INormalizedToken witnessToken : sequence.getWitnessPhrase()) {
        INormalizedToken possibility = iterator.next();
        // skip start and end tokens
        if (!WitnessToken.START.equals(witnessToken) && !WitnessToken.END.equals(witnessToken)) {
          alignedTokens.put(witnessToken, possibility);
        }
      }
    }
    return alignedTokens;
  }

  @Deprecated
  public static List<List<INormalizedToken>> findUniqueTokenSequences(IWitness witness, Matches matches) {
    final List<INormalizedToken> tokens = witness.getTokens();
    final int tokenCount = tokens.size();

    final List<List<INormalizedToken>> tokenSequences =  Lists.newArrayListWithExpectedSize(matches.getAmbiguous().size() * 2);

    for (int tc = 0; tc < tokenCount; tc++) {
      // for each ambiguous token
      if (matches.getAmbiguous().contains(tokens.get(tc))) {
        // find a minimal unique subsequence by walking to the left
        tokenSequences.add(reverse(findMinimalUniquePrefix(reverse(tokens.subList(0, tc + 1)), matches, WitnessToken.START)));
        // find a minimal unique subsequence by walking to the right
        tokenSequences.add(findMinimalUniquePrefix(tokens.subList(tc, tokenCount), matches, WitnessToken.END));
      }
    }

    return tokenSequences;
  }

  public static List<INormalizedToken> findMinimalUniquePrefix(Iterable<INormalizedToken> sequence, Matches matches, INormalizedToken stopMarker) {
    final List<INormalizedToken> result = Lists.newArrayList();

    for (INormalizedToken token : sequence) {
      if (!matches.getUnmatched().contains(token)) {
        result.add(token);
        if (!matches.getAmbiguous().contains(token)) {
          return result;
        }
      }
    }

    result.add(stopMarker);
    return result;
  }

  // check whether this match has an alternative that is equal in weight
  // if so, then skip the alternative!
  // NOTE: multiple witness tokens match with the same table column!
  //EXPECT GRAPH -> Witness TOKEN HERE
  private List<Sequence> filterAwaySecondChoicesMultipleTokensOneColumn(List<Sequence> sequences) {
    List<Sequence> filteredMatches = Lists.newArrayList();
    final Map<INormalizedToken, INormalizedToken> tableToToken = Maps.newLinkedHashMap();
    for (final Sequence sequence : sequences) {
      // step 1. Gather data
      List<TokenPair> pairs = Lists.newArrayList();
      final List<INormalizedToken> tablePhrase = sequence.getBasePhrase();
      final List<INormalizedToken> witnessPhrase = sequence.getWitnessPhrase();
      final Iterator<INormalizedToken> tokens = witnessPhrase.iterator();
      for (final INormalizedToken tableToken : tablePhrase) {
        final INormalizedToken token = tokens.next();
        // skip Start and End Token in variant graph... string equals is not very nice!
        if (!(tableToken.getNormalized().equals("#"))) {
          pairs.add(new TokenPair(tableToken, token));
        }
      }
      // step 2. Look for alternative
      boolean foundAlternative = false;
      for (TokenPair pair : pairs) {
        // check for alternative here!
        final INormalizedToken tableToken = pair.tableToken;
        final INormalizedToken witnessToken = pair.witnessToken;
        if (tableToToken.containsKey(tableToken)) {
          INormalizedToken existingWitnessToken = tableToToken.get(tableToken);
          if (existingWitnessToken != witnessToken) {
            foundAlternative = true;
          }
        } else {
          tableToToken.put(tableToken, witnessToken);
        }
      }
      // step 3. Decide what to do
      if (!foundAlternative) {
        filteredMatches.add(sequence);
      } else {
//        LOG.debug("!Phrase '" + witnessPhrase + "' is an alternative! skipping...");
      }
    }
    return filteredMatches;
  }


  // check whether this match has an alternative that is equal in weight
  // if so, then skip the alternative!
  // NOTE: multiple columns match with the same token!
  // EXPECT WITNESS TOKEN TO GRAPH TOKEN HERE!
  private List<Sequence> filterAwaySecondChoicesMultipleColumnsOneToken(List<Sequence> sequences) {
    List<Sequence> filteredMatches = Lists.newArrayList();
    final Map<INormalizedToken, INormalizedToken> tokenToTable = Maps.newLinkedHashMap();
    for (final Sequence sequence : sequences) {
      // step 1. Gather data
      List<TokenPair> pairs = Lists.newArrayList();
      final List<INormalizedToken> tablePhrase = sequence.getBasePhrase();
      final List<INormalizedToken> witnessPhrase = sequence.getWitnessPhrase();
      final Iterator<INormalizedToken> tokens = witnessPhrase.iterator();
      for (final INormalizedToken tableToken : tablePhrase) {
        final INormalizedToken token = tokens.next();
        // skip NullColumn and NullToken
        if (!(tableToken instanceof NullToken)) {
          pairs.add(new TokenPair(tableToken, token));
        }
      }
      // step 2. Look for alternative
      boolean foundAlternative = false;
      for (TokenPair pair : pairs) {
        // check for alternative here!
        final INormalizedToken tableToken = pair.tableToken;
        final INormalizedToken token = pair.witnessToken;
        if (tokenToTable.containsKey(token)) {
          INormalizedToken existingTable = tokenToTable.get(token);
          if (existingTable != tableToken) {
            foundAlternative = true;
          }
        } else {
          tokenToTable.put(token, tableToken);
        }
      }
      // step 3. Decide what to do
      if (!foundAlternative) {
        filteredMatches.add(sequence);
      } else {
//        LOG.debug("Phrase '" + witnessPhrase + "' is an alternative! skipping...");
      }
    }
    return filteredMatches;
  }

  // This method should return the matching base tokens for a given sequence
  // Note: this method works for sequences that have the fixed token on the left and expand to the right
  private List<INormalizedToken> findMatchingBaseTokensForSequenceToTheRight(List<INormalizedToken> sequence, Multimap<INormalizedToken, INormalizedToken> matches, List<INormalizedToken> aMatches) {
    final INormalizedToken startTokenInB = sequence.get(0);
    final Collection<INormalizedToken> startMatches = matches.get(startTokenInB);

    Preconditions.checkState(!startMatches.isEmpty(), "No match found in base for fixed witness token");
    Preconditions.checkState(startMatches.size() == 1, "Multiple matches found in base for fixed witness token");

    final INormalizedToken startTokenInA = Iterables.getFirst(startMatches, null);
    return tryTheDifferentPossibilities(matches, aMatches, startTokenInA, sequence.subList(1, sequence.size()), 1);
  }

  // This method should return the matching base tokens for a given sequence
  // Note: this method works for sequences that have the fixed token on the right and expand to the left
  private List<INormalizedToken> findMatchingBaseTokensForSequenceToTheLeft(List<INormalizedToken> sequence, Multimap<INormalizedToken, INormalizedToken> matches, List<INormalizedToken> aMatches) {
    final INormalizedToken startTokenInB = sequence.get(sequence.size() - 1);
    final Collection<INormalizedToken> startMatches = matches.get(startTokenInB);

    Preconditions.checkState(!startMatches.isEmpty(), "No match found in base for fixed witness token");
    Preconditions.checkState(startMatches.size() == 1, "Multiple matches found in base for fixed witness token");

    INormalizedToken startTokenInA = Iterables.getFirst(startMatches, null);
    return reverse(tryTheDifferentPossibilities(matches, aMatches, startTokenInA, reverse(sequence.subList(0, sequence.size() - 1)), -1));
  }

  private List<INormalizedToken> tryTheDifferentPossibilities(Multimap<INormalizedToken, INormalizedToken> matches, List<INormalizedToken> aMatches, INormalizedToken startTokenInA,
      List<INormalizedToken> restTokens, int expectedDirection) {
    boolean validWholeSequence = true;
    List<INormalizedToken> matchedBaseTokens = Lists.newArrayList(startTokenInA);
    INormalizedToken lastToken = startTokenInA;
    for (INormalizedToken token : restTokens) {
      boolean valid = false;
      for (INormalizedToken possibleMatch : matches.get(token)) {
        int direction = aMatches.indexOf(possibleMatch) - aMatches.indexOf(lastToken);
        if (direction == expectedDirection) {
          matchedBaseTokens.add(possibleMatch);
          lastToken = possibleMatch;
          valid = true;
          break;
        }
      }
      validWholeSequence = valid;
      if (!valid) {
        break;
      }
    }
    if (validWholeSequence) {
      return matchedBaseTokens;
    }
    return Collections.emptyList();
  }

  public static List<INormalizedToken> findMatches(IWitness a, Collection<INormalizedToken> matches) {
    return Lists.newArrayList(Iterables.filter(a.getTokens(), Predicates.in(matches)));
  }

  public static String toString(List<INormalizedToken> tokens) {
    final StringBuilder str = new StringBuilder();
    for (INormalizedToken token : tokens) {
      str.append(token.getNormalized()).append(" ");
    }
    return str.toString().trim();

  }
}
