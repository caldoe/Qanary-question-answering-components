package eu.wdaqua.qanary.component.qanswer.qb;

import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerResult;
import net.minidev.json.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan("eu.wdaqua.qanary")
@AutoConfigureWebClient
class QAnswerQueryBuilderAndSpaqlResultFetcherTest {
    private static final Logger logger = LoggerFactory.getLogger(QAnswerQueryBuilderAndSpaqlResultFetcherTest.class);
    private final String applicationName = "QAnswerQueryBuilderAndExecutorTest";

    @Autowired
    private Environment env;

    @Autowired
    private RestTemplateWithCaching restTemplate;

    // TODO: replace by CachingRestTemplate when release in qanary.commons
    private RestTemplate restClient;

    private URI realEndpoint;

    private URI resource;
    private URI bool;
    private URI decimal;

    @BeforeEach
    public void init() throws URISyntaxException {
        realEndpoint = new URI(env.getProperty("qanswer.endpoint.url"));

        bool = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        decimal = new URI("http://www.w3.org/2001/XMLSchema#decimal");
        resource = new URI("http://www.w3.org/2001/XMLSchema#anyURI");

        assert (realEndpoint != null) : "qanswer.endpoint.url cannot be empty";

        // RestTemplateBuilder builder = new RestTemplateBuilder();
        this.restClient = new RestTemplate();
        assert this.restClient != null : "restclient cannot be null";
    }

    @Test
    void testTransformationOfNamedEntites() throws URISyntaxException {
        float threshold = 0.5f;
        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, "en", "dbpedia", "open",
                new URI("urn:no:endpoint"), applicationName, restTemplate);
        List<TestData> myTestData = new LinkedList<>();

        List<NamedEntity> entities0 = new LinkedList<>();
        URI parisUri = new URI("http://dbpedia.org/resource/Paris");
        URI londonUri = new URI("http://dbpedia.org/resource/London");
        URI germanyUri = new URI("http://dbpedia.org/resource/Germany");
        String givenQuestion = "Where are Paris, London and Germany?";
        String expectedResult = "Where are " + parisUri + " , " + londonUri + " and " + germanyUri + " ?";
        myTestData.add(new TestData(givenQuestion, expectedResult, entities0));
        entities0.add(new NamedEntity(parisUri, 10, "Paris", threshold));
        entities0.add(new NamedEntity(londonUri, 17, "London", threshold));
        entities0.add(new NamedEntity(germanyUri, 28, "Germany", threshold + 0.001f));

        // TODO: add more test data

        checkTestData(myTestData, threshold, myApp);
    }

    @Test
    void testThresholdBehavior() throws URISyntaxException {
        float threshold = 0.4f;
        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, "en", "wikidata", "open",
                new URI("urn:no:endpoint"), applicationName, restTemplate);
        List<TestData> myTestData = new LinkedList<>();

        List<NamedEntity> entities0 = new LinkedList<>();
        URI londonUri = new URI("http://dbpedia.org/resource/London");
        URI germanyUri = new URI("http://dbpedia.org/resource/Germany");
        myTestData.add(new TestData(//
                "Where are London and Germany?", //
                "Where are London and " + germanyUri.toString() + " ?", //
                entities0));
        entities0.add(new NamedEntity(londonUri, 10, "London", threshold - 0.001f));
        entities0.add(new NamedEntity(germanyUri, 21, "Germany", threshold));

        // TODO: add more test data

        checkTestData(myTestData, threshold, myApp);
    }

    private void checkTestData(List<TestData> myTestData, float threshold, QAnswerQueryBuilderAndSparqlResultFetcher myApp) {
        for (TestData t : myTestData) {
            String computedQuestion = myApp.computeQuestionStringWithReplacedResources(t.getQuestion(),
                    t.getNamedEntities(), threshold);
            logger.info("given question:  {}", t.getQuestion());
            logger.info("expected output: {}", t.getExpectedResult());
            logger.info("computed output: {}", computedQuestion);
            assertEquals(t.getExpectedResult(), computedQuestion);
        }
    }

    /**
     * creates a matching Wikidata entity URL from given ID, e.g., Q183 -->
     * http://www.wikidata.org/entity/Q183
     *
     * @param id
     * @return
     * @throws URISyntaxException
     */
    private URI getWikidataURI(String id) throws URISyntaxException {
        return new URI("http://www.wikidata.org/entity/" + id);
    }

    /**
     * test actual results from the QAnswer API with question 'What is the capital
     * of Germany?' --> one resource
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServiceWhatIsTheCapitalOfGermanyResultOneResource() throws URISyntaxException, ParseException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";

        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);
        String question = "What is the capital of Germany?";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb, user);

        URI germanyUri = getWikidataURI("Q183");
        String expectedQuestion = "What is the capital of " + germanyUri.toString() + " ?";

        List<NamedEntity> entities0 = new LinkedList<>();
        entities0.add(new NamedEntity(germanyUri, 23, "Germany", threshold + 0.001f));
        String computedQuestion = myApp.computeQuestionStringWithReplacedResources(question, entities0, threshold);

        // check correct transformation of the given question
        assertEquals(expectedQuestion, computedQuestion, //
                "From '" + question + "' it was expected '" + expectedQuestion //
                        + "' but computed '" + computedQuestion + "'");

        //
        QAnswerResult result1 = testWebService(myApp, computedQuestion, lang, kb, user);

    }

    /**
     * test actual results from the QAnswer API with question 'Cities in France?'
     * --> many resources
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServicePersonBornInFranceResultManyResources() throws URISyntaxException, ParseException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";
        int min = 2;
        int max = 60;

        // test while using a plain textual question
        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);
        String question = "Person born in France.";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb, user);


        // test with question enriched with a Wikidata entity
        URI franceUri = getWikidataURI("Q142");
        String expectedQuestion = "Person born in " + franceUri.toString() + " .";

        List<NamedEntity> entities0 = new LinkedList<>();
        entities0.add(new NamedEntity(franceUri, 15, "France", threshold + 0.001f));
        String computedQuestion = myApp.computeQuestionStringWithReplacedResources(question, entities0, threshold);


        // check if transformation of the given question was correct
        assertEquals(expectedQuestion, computedQuestion, //
                "From '" + question + "' it was expected '" + expectedQuestion //
                        + "' but computed '" + computedQuestion + "'");

        // Note: we do not know the exact number of SPALQL queries provided by QAnswer
        QAnswerResult result1 = testWebService(myApp, computedQuestion, lang, kb, user);

        assertTrue(result1.getValues().size() >= min, "problem: not " + result1.getValues().size() + " >= " + min);
        assertTrue(result1.getValues().size() <= max, "problem: not " + result1.getValues().size() + " <= " + max);

    }

    /**
     * test actual results from the QAnswer API with question 'Is Berlin the capital
     * of Germany' --> many resources
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServiceIsBerlinTheCapitalOfGermanyResultBoolean() throws URISyntaxException, ParseException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";

        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);
        String question = "Is Berlin the capital of Germany";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb, user);

        URI berlinUri = getWikidataURI("Q64");
        String expectedQuestion = "Is " + berlinUri.toString() + " the capital of Germany";

        List<NamedEntity> entities0 = new LinkedList<>();
        entities0.add(new NamedEntity(berlinUri, 3, "Berlin", threshold + 0.001f));
        String computedQuestion = myApp.computeQuestionStringWithReplacedResources(question, entities0, threshold);
        // computedQuestion = "http://dbpedia.org/resource/United_Kingdom
        // http://dbpedia.org/ontology/capital http://dbpedia.org/resource/London";

		/*
		// check correct transformation of the given question
		assertEquals("From '" + question + "' it was expected '" + expectedQuestion //
				+ "' but computed '" + computedQuestion + "'", //
				expectedQuestion, computedQuestion);
		*/
        QAnswerResult result1 = testWebService(myApp, computedQuestion, lang, kb, user);

        // TODO: test for a ASK SPARQL query

    }

    /**
     * test actual results from the QAnswer API with question 'What is the capital
     * of Germany?' --> one resource
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServicePopulationOfFranceResultNumber() throws URISyntaxException, ParseException, MalformedURLException {
        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";

        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);
        String question = "population of france";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb, user);

        URI everestUri = getWikidataURI("Q142");
        String expectedQuestion = "population of " + everestUri.toString();

        List<NamedEntity> entities0 = new LinkedList<>();
        entities0.add(new NamedEntity(everestUri, "population of ".length(), "france", threshold + 0.001f));
        String computedQuestion = myApp.computeQuestionStringWithReplacedResources(question, entities0, threshold);

        // check correct transformation of the given question
        assertEquals(expectedQuestion, computedQuestion, //
                "From '" + question + "' it was expected '" + expectedQuestion //
                        + "' but computed '" + computedQuestion + "'");

        // TODO: receive a ASK query answer
        QAnswerResult result1 = testWebService(myApp, computedQuestion, lang, kb, user);

        assertEquals(result0.getValues().get(0).get("query"), result1.getValues().get(0).get("query"), "Results are not equal");
    }

    /**
     * check if string is computed
     *
     * @throws URISyntaxException
     * @throws ParseException
     * @throws NoLiteralFieldFoundException
     * @throws MalformedURLException
     */
    @Test
    void testWebServiceWhatIsTheNicknameOfRomeResultString() throws URISyntaxException, ParseException, MalformedURLException {

        float threshold = 0.4f;
        String lang = "en";
        String kb = "wikidata";
        String user = "open";

        QAnswerQueryBuilderAndSparqlResultFetcher myApp = new QAnswerQueryBuilderAndSparqlResultFetcher(threshold, lang, kb, user,
                this.realEndpoint, applicationName, restTemplate);
        String question = "what is the nickname of Rome";
        QAnswerResult result0 = testWebService(myApp, question, lang, kb, user);

        assertTrue(result0.getValues().size() > 2);
    }


    private QAnswerResult testWebService(QAnswerQueryBuilderAndSparqlResultFetcher myApp, String question, String lang, String kb, String user)
            throws URISyntaxException, MalformedURLException {
        QAnswerResult result = myApp.requestQAnswerWebService(realEndpoint, question, lang, kb, user);
        logger.debug("testWebService result: {}", result);
        assertTrue(result.getValues().size() > 0);
        return result;
    }

}

class TestData {
    private String question;
    private List<NamedEntity> entities;
    private String expectedResult;

    public TestData(String questionGiven, String expectedResult, List<NamedEntity> entities) {
        this.question = questionGiven;
        this.expectedResult = expectedResult;
        this.entities = entities;
    }

    String getQuestion() {
        return this.question;
    }

    String getExpectedResult() {
        return this.expectedResult;
    }

    List<NamedEntity> getNamedEntities() {
        return this.entities;
    }
}
