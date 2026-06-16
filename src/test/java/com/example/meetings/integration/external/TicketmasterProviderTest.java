package com.example.meetings.integration.external;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.TicketmasterProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
@Tag("light-integration-tests")
public class TicketmasterProviderTest {

    @Autowired TicketmasterProvider ticketmasterProvider;

    @Value("${app.discover.ticketmaster.api-key:}") String apiKey;

    String baseUrl = "https://app.ticketmaster.com/discovery/v2";

    String nextOccurence = LocalDate.now().plusDays(1).toString();

    @Test
    public void testSearch_ShouldReturnEmptyList_WhenApiIsUnavailable() {
        stubFor(get(urlEqualTo(baseUrl + "/events.json"))
                .withQueryParam("keyword", equalTo("title"))
                .withQueryParam("size", equalTo("5"))
                .withQueryParam("apikey", equalTo(apiKey))
                .willReturn(aResponse().withStatus(503)));
        List<DiscoveredEvent> events = ticketmasterProvider.search("title");
        assertThat(events).isEmpty();
    }

    @Test
    public void testSearch_ShouldReturnEvent_WhenAPICallIsSuccessful() {
        stubFor(get(urlEqualTo(baseUrl + "/events.json"))
                .withQueryParam("keyword", equalTo("title"))
                .withQueryParam("size", equalTo("5"))
                .withQueryParam("apikey", equalTo(apiKey))
                .willReturn((aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                "_embedded": {
                                    "events": [
                                        "id": 100,
                                        "name": "name",
                                        "url": "https://url",
                                        "info": "info",
                                        "dates": {
                                            "start": {
                                                "dateTime": %s
                                            }
                                        },
                                        "_embedded": {
                                            "venues": [
                                                {
                                                    "name": "venue"
                                                }
                                            ]
                                        }
                                    ]
                                }
                                """, nextOccurence))
                ))
        );
        List<DiscoveredEvent> events = ticketmasterProvider.search("title");
        assertEquals(1, events.size());
        assertThat(events.get(0).title()).isEqualTo("title");
    }
}