package com.example.meetings.integration.external;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.SeatGeekProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
@Tag("light-integration-tests")
public class SeatGeekProviderTest {

    @Autowired SeatGeekProvider seatGeekProvider;

    @Value("${app.discover.seatgeek.client-id:}") String clientId;

    String baseUrl = "https://api.seatgeek.com/2";

    String nextOccurence = LocalDate.now().plusDays(1).toString();

    @Test
    public void testSearch_ShouldReturnEmptyList_WhenApiIsUnavailable() {
        stubFor(get(urlEqualTo(baseUrl + "/events.json"))
                .withQueryParam("q", equalTo("title"))
                .withQueryParam("per_page", equalTo("5"))
                .withQueryParam("client_id", equalTo(clientId))
                .willReturn(aResponse().withStatus(503)));
        List<DiscoveredEvent> events = seatGeekProvider.search("title");
        assertThat(events).isEmpty();
    }

    @Test
    public void testSearch_ShouldReturnEvent_WhenAPICallIsSuccessful() {
        stubFor(get(urlEqualTo("/events"))
                .withQueryParam("search", equalTo("title"))
                .withQueryParam("per_page", equalTo("5"))
                .willReturn((aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                "events": [
                                    "id": 100,
                                    "title": "title",
                                    "short_title": "short_title",
                                    "datetime_utc": %s,
                                    "url": "https://url",
                                    "description": "description",
                                    "venue": {
                                        "name": "venue"
                                    }
                                ]
                                """, nextOccurence))
                ))
        );
        List<DiscoveredEvent> events = seatGeekProvider.search("title");
        assertEquals(1, events.size());
        assertThat(events.get(0).title()).isEqualTo("title");
    }
}