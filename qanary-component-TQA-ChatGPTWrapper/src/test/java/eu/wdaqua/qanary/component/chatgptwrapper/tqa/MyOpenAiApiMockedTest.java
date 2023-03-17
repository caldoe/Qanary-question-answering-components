package eu.wdaqua.qanary.component.chatgptwrapper.tqa;

import com.theokanning.openai.Usage;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionResult;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.model.Permission;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.MyCompletionRequest;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.MyOpenAiApi;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.MissingTokenException;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.OpenApiUnreachableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class MyOpenAiApiMockedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyOpenAiApiMockedTest.class);
    MockRestServiceServer mockServer;

    private CacheOfRestTemplateResponse myCacheOfResponse = new CacheOfRestTemplateResponse();
    private RestTemplateWithCaching restTemplate = new RestTemplateWithCaching(this.myCacheOfResponse);

    @Autowired
    private Environment env;


    /**
     * initialize local controller enabled for tests
     */
    @BeforeEach
    public void setUp() throws IOException {
        assertNotNull(env.getProperty("chatGPT.base.url"), "chatGPT.base.url cannot be empty");
        assertNotNull(env.getProperty("chatGPT.getModels.url"), "chatGPT.getModels.url cannot be empty");
        assertNotNull(env.getProperty("chatGPT.getModelById.url"), "chatGPT.getModelById.url cannot be empty");
        assertNotNull(env.getProperty("chatGPT.createCompletions.url"), "chatGPT.createCompletions.url cannot be empty");
        assertNotNull(this.restTemplate, "restTemplate cannot be null");

        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
    }

    @Test
    void missingTokenTest() {
        assertThrows(MissingTokenException.class, () -> {
            new MyOpenAiApi("", false);
        });

        assertThrows(MissingTokenException.class, () -> {
            new MyOpenAiApi(null, false);
        });
    }

    @Test
    void getModelsTest() throws MissingTokenException, URISyntaxException, IOException, OpenApiUnreachableException {
        // mock the response of the OpenAI API /v1/models
        this.mockServer.expect(requestTo(env.getProperty("chatGPT.base.url") + env.getProperty("chatGPT.getModels.url")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(ChatGPTTestConfiguration.getStringFromFile("json_response/getModels.json"), MediaType.APPLICATION_JSON));

        MyOpenAiApi myOpenAiApi = new MyOpenAiApi("some-token", false);

        List<Model> response = myOpenAiApi.getModels(restTemplate, myCacheOfResponse);

        assertNotNull(response);
        assertEquals(68, response.size());
        assertEquals(response.get(0).getClass(), Model.class);
        assertEquals(response.get(0).getPermission().get(0).getClass(), Permission.class);
    }

    @Test
    void getModelByIdTest() throws MissingTokenException, URISyntaxException, IOException, OpenApiUnreachableException {
        // mock the response of the OpenAI API /v1/models/text-davinci-003
        this.mockServer.expect(requestTo(env.getProperty("chatGPT.base.url") + env.getProperty("chatGPT.getModelById.url") + "text-davinci-003"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(ChatGPTTestConfiguration.getStringFromFile("json_response/getModelsById.json"), MediaType.APPLICATION_JSON));

        MyOpenAiApi myOpenAiApi = new MyOpenAiApi("some-token", false);

        Model response = myOpenAiApi.getModelById(restTemplate, myCacheOfResponse, "text-davinci-003");

        assertNotNull(response);
        assertEquals(response.getClass(), Model.class);
        assertEquals(response.getPermission().get(0).getClass(), Permission.class);
    }

    @Test
    void createCompletionsTest() throws MissingTokenException, URISyntaxException, IOException, OpenApiUnreachableException {
        // mock the response of the OpenAI API /v1/completions
        this.mockServer.expect(requestTo(env.getProperty("chatGPT.base.url") + env.getProperty("chatGPT.createCompletions.url")))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(ChatGPTTestConfiguration.getStringFromFile("json_response/createCompletions.json"), MediaType.APPLICATION_JSON));

        MyOpenAiApi myOpenAiApi = new MyOpenAiApi("some-token", false);
        MyCompletionRequest completionRequest = new MyCompletionRequest();
        completionRequest.setModel("text-davinci-003");
        completionRequest.setPrompt("some question?");

        CompletionResult response = myOpenAiApi.createCompletion(restTemplate, myCacheOfResponse, completionRequest);

        assertNotNull(response);
        assertEquals(response.getClass(), CompletionResult.class);
        assertEquals(response.getChoices().get(0).getClass(), CompletionChoice.class);
        assertEquals(response.getUsage().getClass(), Usage.class);
    }




}