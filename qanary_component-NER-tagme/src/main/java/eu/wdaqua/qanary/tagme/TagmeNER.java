package eu.wdaqua.qanary.tagme;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class TagmeNER extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(TagmeNER.class);

	private final String applicationName;

	public TagmeNER(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * @throws Exception 
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		 QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
	     QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage);
	     String myQuestion = myQanaryQuestion.getTextualRepresentation();
		//String myQuestion = "Who is the wife of Barack Obama ?";
	      ArrayList<Selection> selections = new ArrayList<Selection>();
	      //ArrayList<Link> links = new ArrayList<Link>();
	      logger.info("Question {}", myQuestion);
	      
	      String thePath = "";
	      thePath = URLEncoder.encode(myQuestion, "UTF-8"); 
	      logger.info("Path {}", thePath);
	      
	      HttpClient httpclient = HttpClients.createDefault();
	      HttpGet httpget = new HttpGet("https://tagme.d4science.org/tagme/tag?gcube-token=c0c5a908-eb13-4219-9516-450a9f6d3bc6-843339462&text="+thePath);
	      //httpget.addHeader("User-Agent", USER_AGENT);
	      HttpResponse response = httpclient.execute(httpget);
	      try {
		    	 
	          HttpEntity entity = response.getEntity();
	          if (entity != null) {
 	             InputStream instream = entity.getContent();
    // String result = getStringFromInputStream(instream);
 	             String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
 	             JSONObject response2 = new JSONObject(text); 
 	            logger.info("response2: {}", response2);
 	           if(response2.has("annotations"))
	            {
 	        	   JSONArray jsonArray = (JSONArray) response2.get("annotations");
 	        	  if(jsonArray.length()!=0)
	            	{
	            		for (int i = 0; i < jsonArray.length(); i++) {
	            			JSONObject explrObject = jsonArray.getJSONObject(i);
	            			int begin = (int) explrObject.get("start");
	            			int end = (int) explrObject.get("end");
	            			double link_probability = explrObject.getDouble("link_probability");
	            			logger.info("Question: {}", explrObject);
	            			logger.info("Begin: {}", begin);
	            			logger.info("End: {}", end);
	            			logger.info("link_probability: {}", link_probability);
	            			Selection s = new Selection();
	            			s.begin = begin;
	            			s.end = end;
	            			if(link_probability >= 0.65) {
	            		    logger.info("Adding link_probability >= 0.65 begin:end {} ", begin+":"+end);
	            			selections.add(s);
	            			}
	            		}
	            	}
	            }
	          }
	      }
	      catch (ClientProtocolException e) {
		 		 logger.info("Exception: {}", e);
		         // TODO Auto-generated catch block
		     } catch (IOException e1) {
		     	logger.info("Except: {}", e1);
		         // TODO Auto-generated catch block
		     }
		
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)
		for (Selection s : selections) {
            String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> " //
                    + "prefix oa: <http://www.w3.org/ns/openannotation/core/> " //
                    + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryMessage.getOutGraph() + "> { " //
                    + "  ?a a qa:AnnotationOSpotfInstance . " //
					+ "  ?a oa:hasTarget [ " //
                    + "           a    oa:SpecificResource; " //
					+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
                    + "           oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
                    + "                    oa:start \"" + s.begin + "\"^^xsd:nonNegativeInteger ; " //
                    + "                    oa:end  \"" + s.end + "\"^^xsd:nonNegativeInteger  " //
					+ "           ] " //
                    + "  ] ; " //
					+ "     oa:annotatedBy <urn:qanary:"+this.applicationName+"> ; " //
                    + "	    oa:annotatedAt ?time  " //
					+ "}} " //
					+ "WHERE { " //
					+ "BIND (IRI(str(RAND())) AS ?a) ." //
                    + "BIND (now() as ?time) " //
					+ "}";
           myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
        }
		return myQanaryMessage;
	}
	class Selection {
        public int begin;
        public int end;
    }
}
