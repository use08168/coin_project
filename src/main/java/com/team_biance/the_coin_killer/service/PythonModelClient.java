package com.team_biance.the_coin_killer.service;

import com.team_biance.the_coin_killer.model.PythonPredictRequest;
import com.team_biance.the_coin_killer.model.PythonPredictResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PythonModelClient {

    private final RestTemplate restTemplate;

    @Value("${app.python.base-url}")
    private String baseUrl;

    public PythonModelClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PythonPredictResponse predict(PythonPredictRequest req) {
        String url = baseUrl + "/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PythonPredictRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<PythonPredictResponse> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                PythonPredictResponse.class);

        return resp.getBody();
    }
}
