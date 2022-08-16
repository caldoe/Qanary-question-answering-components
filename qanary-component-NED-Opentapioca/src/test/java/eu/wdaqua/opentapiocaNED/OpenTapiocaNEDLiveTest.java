package eu.wdaqua.opentapiocaNED;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.client.ClientProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class OpenTapiocaNEDLiveTest {

    private String opentapiocaEndpoint;
    private OpenTapiocaServiceFetcher serviceFetcher;

    private boolean resourcesContainID(JsonArray resources, String id) {
        for (int i = 0; i < resources.size(); i++) {
            JsonObject object = resources.get(i).getAsJsonObject();
            JsonArray tags = object.get("tags").getAsJsonArray();
            for (int j = 0; j < tags.size(); j++) {
                JsonObject entity = tags.get(j).getAsJsonObject();
                String qid = entity.get("id").getAsString();
                if (qid.equals(id))
                    return true;
            }
        }
        return false;
    }

    @BeforeEach
    void init() {
        opentapiocaEndpoint = "https://opentapioca.org/api/annotate";
        serviceFetcher = new OpenTapiocaServiceFetcher();
    }

    @Test
    void testGetJsonFromServiceForPerson() throws ClientProtocolException, IOException {
        String question = "Where and when was Ada Lovelace born?";
        JsonArray resources = serviceFetcher.getJsonFromService(
                question, opentapiocaEndpoint
        );
        assertNotNull(resources);
        assertTrue(resources.size() > 0);
        assertTrue(resourcesContainID(resources, "Q7259"));
    }

    @Disabled
    @Test
        // ignored because the currently used opentapioca implementation does not recognize super heroes
    void testGetJsonFromServiceForSuperHero() throws ClientProtocolException, IOException {
        String question = "Aquaman";
        JsonArray resources = serviceFetcher.getJsonFromService(
                question, opentapiocaEndpoint
        );
        assertNotNull(resources);
        assertTrue(resources.size() > 0);
        assertTrue(resourcesContainID(resources, "Q623059"));
    }

    @Test
    void testGetJsonFromServiceWithEmptyQuestion() throws ClientProtocolException, IOException {
        String question = "";
        JsonArray resources = serviceFetcher.getJsonFromService(
                question, opentapiocaEndpoint
        );
        assertNotNull(resources);
        assertTrue(resources.size() == 0);
    }

    @Test
    void testInvalidQuestionException() throws ClientProtocolException, IOException {
        String question = null;
        try {
            JsonArray resources = serviceFetcher.getJsonFromService(
                    question, opentapiocaEndpoint
            );
            fail("null value question String should not be processed");
        } catch (IOException | IllegalArgumentException e) {
            // pass
        } catch (Exception e) {
            fail("Unexpected Exception");
        }
    }
}
