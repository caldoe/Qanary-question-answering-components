package eu.wdaqua.qanary.meaningcloudned;

import com.google.gson.Gson;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
public class MeaningCloudNed extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(MeaningCloudNed.class);

    private final String applicationName;
    private final String cacheFilePath;

    public MeaningCloudNed(@Value("${spring.application.name}") final String applicationName,
                           @Value("${ned-meaningcloud.cache.file}") final String cacheFilePath) {
        this.applicationName = applicationName;
        this.cacheFilePath = cacheFilePath;
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
		// TODO: implement processing of question

		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage,
				myQanaryUtils.getQanaryTripleStoreConnector());
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		// String myQuestion = "Is Selwyn Lloyd the prime minister of Winston Churchill
		// ?";
		ArrayList<Selection> selections = new ArrayList<Selection>();
		logger.info("Question {}", myQuestion);

		String thePath = "";
		thePath = URLEncoder.encode(myQuestion, "UTF-8");
		logger.info("Path {}", thePath);

            while ((line = br.readLine()) != null && flag == 0) {
                String question = line.substring(0, line.indexOf("Answer:"));
                logger.info("{}", line);
                logger.info("{}", myQuestion);

			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
				JSONObject response2 = new JSONObject(text);
				logger.info("response2: {}", response2);
				if (response2.has("entity_list")) {
					JSONArray ents = (JSONArray) response2.get("entity_list");
					for (int j = 0; j < ents.length(); j++) {
						JSONObject formObject = (JSONObject) ents.getJSONObject(j);
						logger.info("formObject_1: {}", formObject);
						if (formObject.has("variant_list")) {
							JSONArray jsonArray = (JSONArray) formObject.get("variant_list");
							String link = null;
							if (formObject.has("semld_list")) {
								JSONArray jsonArray_semld_list = (JSONArray) formObject.get("semld_list");
								link = jsonArray_semld_list.getString(0);
							}
							logger.info("jsonArray_variant_list : {}", jsonArray);
							for (int i = 0; i < jsonArray.length(); i++) {
								JSONObject explrObject = jsonArray.getJSONObject(i);
								logger.info("form_explrObject_2 : {}", explrObject);
								int begin = explrObject.getInt("inip");
								int end = explrObject.getInt("endp");
								logger.info("Question: {}", explrObject);
								logger.info("Begin: {}", begin);
								logger.info("End: {}", end);
								logger.info("link: {}", link);
								Selection s = new Selection();
								s.begin = begin;
								s.end = end;
								String finalUrl = "";
								if (link != null && link.contains("wikipedia")) {
									finalUrl = "http://dbpedia.org/resource" + link.substring(28);
								}
								logger.info("finalUrl: {}", finalUrl);
								s.link = finalUrl;
								selections.add(s);
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			logger.error("Exception: {}", e);
		} catch (ClientProtocolException e) {
			logger.info("Exception: {}", e);
			// TODO Auto-generated catch block
		}

		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)
		for (Selection s : selections) {
			String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfInstance . " //
					+ "  ?a oa:hasTarget [ " //
					+ "           a    oa:SpecificResource; " //
					+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
					+ "           oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
					+ "                    oa:start \"" + s.begin + "\"^^xsd:nonNegativeInteger ; " //
					+ "                    oa:end  \"" + s.end + "\"^^xsd:nonNegativeInteger  " //
					+ "           ] " //
					+ "  ] . " //
					+ "  ?a oa:hasBody <" + s.link + "> ;" //
					+ "     oa:annotatedBy <urn:qanary:" + this.applicationName + "> ; " //
					+ "	    oa:annotatedAt ?time  " + "}} " //
					+ "WHERE { " //
					+ "  BIND (IRI(str(RAND())) AS ?a) ."//
					+ "  BIND (now() as ?time) " //
					+ "}";
			myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
		}
		return myQanaryMessage;
	}

    class Selection {
        public int begin;
        public int end;
        public String link;
    }
}
