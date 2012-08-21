package com.dnanexus;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.util.*;
import org.apache.http.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.*;
import org.apache.commons.io.IOUtils;

public class DXHTTPRequest {
    public String APISERVER_HOST = System.getenv("DX_APISERVER_HOST");
    public String APISERVER_PORT = System.getenv("DX_APISERVER_PORT");
    public String SECURITY_CONTEXT = System.getenv("DX_SECURITY_CONTEXT");
    public String APISERVER_PROTOCOL = System.getenv("DX_APISERVER_PROTOCOL");
    public String JOB_ID = System.getenv("DX_JOB_ID");
    public String WORKSPACE_ID = System.getenv("DX_WORKSPACE_ID");
    public String PROJECT_CONTEXT_ID = System.getenv("DX_PROJECT_CONTEXT_ID");

    private String apiserver;
    private DefaultHttpClient httpclient;
    private ObjectMapper mapper;
    private JsonFactory dxJsonFactory;

    public DXHTTPRequest() throws Exception {
        if (APISERVER_HOST == null) { APISERVER_HOST = "localhost"; }
        if (APISERVER_PORT == null) { APISERVER_PORT = "8124"; }
        if (APISERVER_PROTOCOL == null) { APISERVER_PROTOCOL = "http"; }
        if (SECURITY_CONTEXT == null) { System.err.println("Warning: No security context found"); }

        System.out.println("H:" + APISERVER_PROTOCOL + "://" + APISERVER_HOST + ":" + APISERVER_PORT);

        httpclient = new DefaultHttpClient();
        apiserver = APISERVER_PROTOCOL + "://" + APISERVER_HOST + ":" + APISERVER_PORT;
        dxJsonFactory = new MappingJsonFactory();
    }

    public String request(String resource, String data) throws Exception {
        HttpPost request = new HttpPost(apiserver + resource);
        
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(data));

        ResponseHandler<String> handler = new ResponseHandler<String>() {
            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // TODO: check that this decodes safely
                    return EntityUtils.toString(entity);
                } else {
                    return null;
                }
            }
        };

        String response = httpclient.execute(request, handler);

        //TODO: make handler throw on response.getStatusLine().getStatusCode()
        return response;
    }

    public JsonNode request(String resource, JsonNode data) throws Exception {
        String dataAsString = data.toString();
        String response = this.request(resource, dataAsString);
        System.out.println(response);
        JsonNode root = dxJsonFactory.createJsonParser(response).readValueAsTree();
        return root;
    }
}