package com.example.meetings.integration.external;

import com.example.meetings.discover.AgendaLxProvider;
import com.example.meetings.discover.DiscoveredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
@Tag("light-integration-tests")
public class AgendaLxProviderTest {

    @Autowired AgendaLxProvider agendaLxProvider;

    DiscoveredEvent expectedEvent;

    String nextOccurence = LocalDate.now().plusDays(1).toString();

    static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

    @BeforeEach
    public void setup() {

        expectedEvent = new DiscoveredEvent(
                agendaLxProvider.name(),
                String.valueOf(1L),
                "title",
                "<p>description1</p>\n<p>description2</p>\n<p>description3</p>\n",
                LocalDate.parse(nextOccurence).atTime(LocalTime.of(17, 42)).atZone(LISBON).toInstant(),
                null,
                "https://url",
                "venue1"
        );
    }

    @Test
    public void testSearch_ShouldReturnEmptyList_WhenApiIsUnavailable() {
        stubFor(get(urlEqualTo("/events"))
                .withQueryParam("search", equalTo("title"))
                .withQueryParam("per_page", equalTo("5"))
                .willReturn(aResponse().withStatus(503)));
        List<DiscoveredEvent> events = agendaLxProvider.search("title");
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
                                [
                                    "id": 100,
                                    "title": {
                                        "rendered": "title"
                                    },
                                    "description": [
                                        "<p>description1</p>",
                                        "<p>description2</p>",
                                        "<p>description3</p>"
                                    ],
                                    "occurences": [
                                        "2007-12-03",
                                        "2007-12-04",
                                        %s
                                    ],
                                    "string_times": "mon: 17h42",
                                    "link": "https://url",
                                    "venue": {
                                        "venue1": {
                                            "name": "venue1"
                                        }
                                    }
                                ]
                                """, nextOccurence))
                ))
        );
        List<DiscoveredEvent> events = agendaLxProvider.search("title");
        assertEquals(1, events.size());
        assertThat(events.get(0).title()).isEqualTo("title");
    }
}