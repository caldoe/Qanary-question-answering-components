package eu.wdaqua.qanary.component.babelfy.ned;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;

@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties
 * (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class BabelfyNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(BabelfyNED.class);

	private final String applicationName;

	@Inject
	private BabelfyServiceFetcher babelfyServiceFetcher;

	@Inject
	private BabelfyConfiguration babelfyConfiguration;

	private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

	public BabelfyNED(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;

		// check if files exists and are not empty
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * 
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();

		logger.info("Question: {}", myQuestion);
		
		// fetch data from external API
		ArrayList<BabelfyServiceFetcher.Link> links = babelfyServiceFetcher.getLinksForQuestion(
				babelfyConfiguration.getEndpoint(), myQuestion, babelfyConfiguration.getParameters()
		);

		logger.debug("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		logger.debug("apply vocabulary alignment on outgraph");
		for (BabelfyServiceFetcher.Link l : links) {
			QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
			bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
			bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
			bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(l.begin), XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(l.end), XSDDatatype.XSDnonNegativeInteger));
			bindingsForInsert.add("answer", ResourceFactory.createStringLiteral(l.link));
			bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

			// get the template of the INSERT query
			String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
			logger.info("SPARQL query: {}", sparql);
			myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

		}
		return myQanaryMessage;
	}

	private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
		return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
	}
}
