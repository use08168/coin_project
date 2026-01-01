package com.team_biance.the_coin_killer.service.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team_biance.the_coin_killer.dao.OpenInterestDao;
import com.team_biance.the_coin_killer.util.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Component
public class OpenInterestPoller {

    @Value("${binance.futures.rest-base}")
    private String restBase;

    @Value("${binance.symbol}")
    private String symbol;

    private final OpenInterestDao oiDao;
    private final ObjectMapper om;
    private final HttpClient client = HttpClient.newHttpClient();

    public OpenInterestPoller(OpenInterestDao oiDao, ObjectMapper om) {
        this.oiDao = oiDao;
        this.om = om;
    }

    @Scheduled(cron = "10 * * * * *") // 매 분 10초에
    public void poll() {
        try {
            String url = restBase + "/fapi/v1/openInterest?symbol=" + symbol;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200)
                return;

            JsonNode root = om.readTree(resp.body());
            double oi = root.get("openInterest").asDouble();

            Instant ts = TimeUtil.floorToMinute(Instant.now());
            oiDao.upsert1m(symbol, ts, oi);
        } catch (Exception ignore) {
        }
    }
}
