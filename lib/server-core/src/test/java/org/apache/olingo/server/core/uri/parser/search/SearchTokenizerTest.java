/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.server.core.uri.parser.search;

import static org.apache.olingo.server.core.uri.parser.search.SearchQueryToken.Token.AND;
import static org.apache.olingo.server.core.uri.parser.search.SearchQueryToken.Token.CLOSE;
import static org.apache.olingo.server.core.uri.parser.search.SearchQueryToken.Token.NOT;
import static org.apache.olingo.server.core.uri.parser.search.SearchQueryToken.Token.OPEN;
import static org.apache.olingo.server.core.uri.parser.search.SearchQueryToken.Token.OR;
import static org.apache.olingo.server.core.uri.parser.search.SearchQueryToken.Token.PHRASE;
import static org.apache.olingo.server.core.uri.parser.search.SearchQueryToken.Token.WORD;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class SearchTokenizerTest {

  @Test
  public void parseBasics() throws Exception {
    assertQuery("abd").resultsIn(WORD);
    assertQuery("NOT abc").resultsIn(NOT, WORD);
    assertQuery("(abc)").resultsIn(OPEN, WORD, CLOSE);
    assertQuery("((abc))").resultsIn(OPEN, OPEN, WORD, CLOSE, CLOSE);
  }

  @Test
  public void parseWords() throws Exception {
    assertQuery("somesimpleword").resultsIn(WORD);
    assertQuery("anotherWord\u1234").resultsIn(WORD);
    // special
    assertQuery("NO").resultsIn(word("NO"));
    assertQuery("N").resultsIn(word("N"));
    assertQuery("A").resultsIn(word("A"));
    assertQuery("AN").resultsIn(word("AN"));
    assertQuery("O").resultsIn(word("O"));
    // invalid
    assertQuery("notAw0rd").resultsIn(SearchTokenizerException.MessageKeys.FORBIDDEN_CHARACTER);
  }

  private Validator.Tuple word(String literal) {
    return Validator.tuple(WORD, literal);
  }

  @Test
  public void parsePhrase() throws Exception {
    SearchTokenizer tokenizer = new SearchTokenizer();
    List<SearchQueryToken> result;

    assertQuery("abc AND \"x-y_z\" AND olingo");

    //
    result = tokenizer.tokenize("\"abc\"");
    Assert.assertNotNull(result);
    
    Assert.assertEquals(PHRASE, result.get(0).getToken());

    //
    result = tokenizer.tokenize("\"9988  abs\"");
    Assert.assertNotNull(result);
    
    Assert.assertEquals(PHRASE, result.get(0).getToken());
    Assert.assertEquals("\"9988  abs\"", result.get(0).getLiteral());

    //
    result = tokenizer.tokenize("\"99_88.\"");
    Assert.assertNotNull(result);
    
    Assert.assertEquals(PHRASE, result.get(0).getToken());
    Assert.assertEquals("\"99_88.\"", result.get(0).getLiteral());

    assertQuery("abc or \"xyz\"").resultsIn(WORD, WORD, PHRASE);
  }

  /**
   * https://tools.oasis-open.org/version-control/browse/wsvn/odata/trunk/spec/ABNF/odata-abnf-testcases.xml
   * @throws Exception
   */
  @Test
  @Ignore("Test must be moved to SearchParserTest and SearchParserAndTokenizerTest")
  public void parsePhraseAbnfTestcases() throws Exception {
    //    <TestCase Name="5.1.7 Search - simple phrase" Rule="queryOptions">
    assertQuery("\"blue%20green\"");
    //    <TestCase Name="5.1.7 Search - simple phrase" Rule="queryOptions">
    assertQuery("\"blue%20green%22");
    //    <TestCase Name="5.1.7 Search - phrase with escaped double-quote" Rule="queryOptions">
    //    <Input>$search="blue\"green"</Input>
    assertQuery("\"blue\\\"green\"");

    //    <TestCase Name="5.1.7 Search - phrase with escaped backslash" Rule="queryOptions">
    //    <Input>$search="blue\\green"</Input>
    assertQuery("\"blue\\\\green\"");

    //    <TestCase Name="5.1.7 Search - phrase with unescaped double-quote" Rule="queryOptions" FailAt="14">
    assertQuery("\"blue\"green\"");

    //    <TestCase Name="5.1.7 Search - phrase with unescaped double-quote" Rule="queryOptions" FailAt="16">
    assertQuery("\"blue%22green\"");

//    <TestCase Name="5.1.7 Search - implicit AND" Rule="queryOptions">
//    <Input>$search=blue green</Input>
//    SearchassertQuery("\"blue%20green\"").resultsIn();
    //    <TestCase Name="5.1.7 Search - implicit AND, encoced" Rule="queryOptions">
//    SearchassertQuery("blue%20green").resultsIn();
  }


    @Test
  public void parseNot() throws Exception {
    assertQuery("NOT").resultsIn(NOT);
    assertQuery(" NOT ").resultsIn(NOT);
    assertQuery("NOT abc").resultsIn(NOT, WORD);
    assertQuery("not abc").resultsIn(WORD, WORD);
    assertQuery("NOT    abc").resultsIn(NOT, WORD);
    assertQuery("NOT    \"abc\"").resultsIn(NOT, PHRASE);
    assertQuery("NObody").resultsIn(WORD);
    assertQuery("Nobody").resultsIn(WORD);
    assertQuery("NOT (sdf)").resultsIn(SearchTokenizerException.MessageKeys.FORBIDDEN_CHARACTER);
  }

  @Test
  public void parseOr() throws Exception {
    assertQuery("OR").resultsIn(OR);
    assertQuery(" OR ").resultsIn(OR);
    assertQuery("OR xyz").resultsIn(OR, WORD);
    assertQuery("abc OR xyz").resultsIn(WORD, OR, WORD);
    assertQuery("abc OR xyz OR olingo").resultsIn(WORD, OR, WORD, OR, WORD);
    assertQuery("abc or xyz").addExpected(WORD, WORD, WORD);
  }

  @Test
  public void parseImplicitAnd() throws SearchTokenizerException {
    assertQuery("a b").resultsIn(WORD, WORD);
    assertQuery("a b OR c").resultsIn(WORD, WORD, OR, WORD);
    assertQuery("a bc OR c").resultsIn(WORD, WORD, OR, WORD);
    assertQuery("a bc c").resultsIn(WORD, WORD, WORD);
    assertQuery("(a OR x) bc c").resultsIn(OPEN, WORD, OR, WORD, CLOSE, WORD, WORD);
  }

  @Test
  public void parseAnd() throws Exception {
    assertQuery("AND").resultsIn(AND);
    assertQuery(" AND ").resultsIn(AND);

    assertQuery("abc AND xyz").resultsIn(WORD, AND, WORD);
    // no lower case allowed for AND
    assertQuery("abc and xyz").resultsIn(WORD, WORD, WORD);
    // implicit AND is handled by parser (and not tokenizer)
    assertQuery("abc xyz").resultsIn(WORD, WORD);
    assertQuery("abc AND xyz AND olingo").resultsIn(WORD, AND, WORD, AND, WORD);
    assertQuery("abc AND \"x-y_z\"  AND olingo")
        .resultsIn(WORD, AND, PHRASE, AND, WORD);
  }

  @Test
  public void parseAndOr() throws Exception {
    assertQuery("OR AND ").resultsIn(OR, AND);
    assertQuery("abc AND xyz OR olingo").resultsIn(WORD, AND, WORD, OR, WORD);
    assertQuery("abc AND ANDsomething").addExpected(WORD, AND, WORD);
  }


  @Test
  public void parseCombinations() throws Exception {
    assertQuery("word O NO").resultsIn(word("word"), word("O"), word("NO"));
    assertQuery("O AN NO").resultsIn(word("O"), word("AN"), word("NO"));
    assertQuery("NO AN O").resultsIn(word("NO"), word("AN"), word("O"));
    assertQuery("N A O").resultsIn(word("N"), word("A"), word("O"));
    assertQuery("abc AND NOT xyz OR olingo").resultsIn(WORD, AND, NOT, WORD, OR, WORD);

    assertQuery("foo AND bar OR foo AND baz OR that AND bar OR that AND baz")
        .addExpected(WORD, "foo").addExpected(AND)
        .addExpected(WORD, "bar").addExpected(OR)
        .addExpected(WORD, "foo").addExpected(AND)
        .addExpected(WORD, "baz").addExpected(OR)
        .addExpected(WORD, "that").addExpected(AND)
        .addExpected(WORD, "bar").addExpected(OR)
        .addExpected(WORD, "that").addExpected(AND)
        .addExpected(WORD, "baz")
        .validate();


    assertQuery("(foo OR that) AND (bar OR baz)")
        .addExpected(OPEN)
        .addExpected(WORD, "foo").addExpected(OR).addExpected(WORD, "that")
        .addExpected(CLOSE).addExpected(AND).addExpected(OPEN)
        .addExpected(WORD, "bar").addExpected(OR).addExpected(WORD, "baz")
        .addExpected(CLOSE)
        .validate();
  }


  @Test
  public void parseSpecial() throws Exception {
    assertQuery("NOT abc AND nothing").resultsIn(NOT, WORD, AND, WORD);
    assertQuery("abc AND andsomething").resultsIn(WORD, AND, WORD);
    assertQuery("abc AND ANDsomething").resultsIn(WORD, AND, WORD);
    assertQuery("abc ANDsomething").resultsIn(WORD, WORD);
    assertQuery("abc ORsomething").resultsIn(WORD, WORD);
    assertQuery("abc OR orsomething").resultsIn(WORD, OR, WORD);
    assertQuery("abc OR ORsomething").resultsIn(WORD, OR, WORD);
  }

  @Ignore
  @Test
  public void unicodeInWords() throws Exception {
    // Ll, Lm, Lo, Lt, Lu, Nl
    assertQuery("abc OR Ll\u01E3Lm\u02B5Lo\u1BE4Lt\u01F2Lu\u03D3Nl\u216F")
        .resultsIn(WORD, OR, WORD);
  }

  /**
   * unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
   * other-delims   = "!" /                   "(" / ")" / "*" / "+" / "," / ";"
   * qchar-unescaped       = unreserved / pct-encoded-unescaped / other-delims / ":" / "@" / "/" / "?" / "$" / "'" / "="
   * pct-encoded-unescaped = "%" ( "0" / "1" /   "3" / "4" /   "6" / "7" / "8" / "9" / A-to-F ) HEXDIG
   *   / "%" "2" ( "0" / "1" /   "3" / "4" / "5" / "6" / "7" / "8" / "9" / A-to-F )
   *   / "%" "5" ( DIGIT / "A" / "B" /   "D" / "E" / "F" )
   *
   * qchar-no-AMP-DQUOTE   = qchar-unescaped  / escape ( escape / quotation-mark )
   *
   * escape = "\" / "%5C"     ; reverse solidus U+005C
   * quotation-mark  = DQUOTE / "%22"
   * ALPHA  = %x41-5A / %x61-7A
   * DIGIT  = %x30-39
   * DQUOTE = %x22
   *
   * @throws Exception
   */
  @Test
  public void characterInPhrase() throws Exception {
    assertQuery("\"123\" OR \"ALPHA-._~\"").resultsIn(PHRASE, OR, PHRASE);
  }

  @Test
  public void moreMixedTests() throws SearchTokenizerException {
    validate("abc");
    validate("NOT abc");

    validate("abc AND def");
    validate("abc  OR def");
    validate("abc     def", WORD, WORD);

    validate("abc AND def AND ghi", WORD, AND, WORD, AND, WORD);
    validate("abc AND def  OR ghi");
    validate("abc AND def     ghi");

    validate("abc  OR def AND ghi", WORD, OR, WORD, AND, WORD);
    validate("abc  OR def  OR ghi", WORD, OR, WORD, OR, WORD);
    validate("abc  OR def     ghi", WORD, OR, WORD, WORD);

    validate("abc     def AND ghi");
    validate("abc     def  OR ghi");
    validate("abc     def     ghi");

    // mixed not
    assertQuery("    abc         def AND     ghi").resultsIn(WORD, WORD, AND, WORD);
    validate("NOT abc  NOT    def  OR NOT ghi", NOT, WORD, NOT, WORD, OR, NOT, WORD);
    validate("    abc         def     NOT ghi", WORD, WORD, NOT, WORD);

    // parenthesis
    validate("(abc)", OPEN, WORD, CLOSE);
    validate("(abc AND  def)", OPEN, WORD, AND, WORD, CLOSE);
    validate("(abc AND  def)   OR  ghi", OPEN, WORD, AND, WORD, CLOSE, OR, WORD);
    validate("(abc AND  def)       ghi", OPEN, WORD, AND, WORD, CLOSE, WORD);
    validate("abc AND (def    OR  ghi)", WORD, AND, OPEN, WORD, OR, WORD, CLOSE);
    validate("abc AND (def        ghi)", WORD, AND, OPEN, WORD, WORD, CLOSE);
  }

  @Test
  public void tokenizeInvalid() throws SearchTokenizerException {
    //
    assertQuery("(  abc AND) OR something").resultsIn(SearchTokenizerException.class);

    assertQuery("\"phrase\"word").resultsIn(SearchTokenizerException.class);
    assertQuery("\"p\"w").resultsIn(SearchTokenizerException.class);
    assertQuery("\"\"").resultsIn(SearchTokenizerException.class);
  }

  @Test
  public void tokenizeInvalidQueryForParser() throws SearchTokenizerException {
    assertQuery("AND").resultsIn(AND);
    assertQuery("OR").resultsIn(OR);
    assertQuery("NOT").resultsIn(NOT);
    assertQuery("NOT AND").resultsIn(NOT, AND);
    assertQuery("NOT OR").resultsIn(NOT, OR);
    assertQuery("NOT NOT").resultsIn(NOT, NOT);
    assertQuery("abc AND OR something").resultsIn(WORD, AND, OR, WORD);
    assertQuery("abc AND \"something\" )").resultsIn(WORD, AND, PHRASE, CLOSE);
  }

  public void validate(String query) throws SearchTokenizerException {
    new Validator(query);
  }

  public Validator assertQuery(String query) throws SearchTokenizerException {
    return new Validator(query);
  }

  public void validate(String query, SearchQueryToken.Token ... tokens) throws SearchTokenizerException {
    Validator sv = new Validator(query);
    for (SearchQueryToken.Token token : tokens) {
      sv.addExpected(token);
    }
    sv.validate();
  }

  private static class Validator {
    private List<Tuple> validations = new ArrayList<Tuple>();
    private boolean log;
    private final String searchQuery;

    public void resultsIn(SearchQueryToken.Token... tokens) throws SearchTokenizerException {
      addExpected(tokens);
      validate();
    }
    public void resultsIn(Tuple... tuple) throws SearchTokenizerException {
      for (Tuple t : tuple) {
        addExpected(t.token, t.literal);
      }
      validate();
    }
    public static Tuple tuple(SearchQueryToken.Token token, String literal) {
      return new Tuple(token, literal);
    }
    private static class Tuple {
      final SearchQueryToken.Token token;
      final String literal;
      public Tuple(SearchQueryToken.Token token, String literal) {
        this.token = token;
        this.literal = literal;
      }
      public Tuple(SearchQueryToken.Token token) {
        this(token, null);
      }
    }

    private Validator(String searchQuery) {
      this.searchQuery = searchQuery;
    }

    private static Validator init(String searchQuery) {
      return new Validator(searchQuery);
    }
    
    @SuppressWarnings("unused")
    private Validator enableLogging() {
      log = true;
      return this;
    }
    private Validator addExpected(SearchQueryToken.Token token, String literal) {
      validations.add(new Tuple(token, literal));
      return this;
    }
    private Validator addExpected(SearchQueryToken.Token ... token) {
      for (SearchQueryToken.Token t : token) {
        validations.add(new Tuple(t));
      }
      return this;
    }
    private void resultsIn(Class<? extends Exception> exception) throws SearchTokenizerException {
      try {
        new SearchTokenizer().tokenize(searchQuery);
      } catch (Exception e) {
        Assert.assertEquals(exception, e.getClass());
        return;
      }
      Assert.fail("Expected exception " + exception.getClass().getSimpleName() + " was not thrown.");
    }

    private void resultsIn(SearchTokenizerException.MessageKey key)
        throws SearchTokenizerException {
      try {
        init(searchQuery).validate();
      } catch (SearchTokenizerException e) {
        Assert.assertEquals("SearchTokenizerException with unexpected message was thrown.", key, e.getMessageKey());
        return;
      }
      Assert.fail("No SearchTokenizerException was not thrown.");
    }

    private void validate() throws SearchTokenizerException {
      SearchTokenizer tokenizer = new SearchTokenizer();
      List<SearchQueryToken> result = tokenizer.tokenize(searchQuery);
      Assert.assertNotNull(result);
      if(log) {
        System.out.println(result);
      }
      if(validations.size() != 0) {
        Assert.assertEquals(validations.size(), result.size());

        Iterator<Tuple> validationIt = validations.iterator();
        for (SearchQueryToken iToken : result) {
          Tuple validation = validationIt.next();
          Assert.assertEquals(validation.token, iToken.getToken());
          if(validation.literal != null) {
            Assert.assertEquals(validation.literal, iToken.getLiteral());
          }
        }
      }
    }
  }
}