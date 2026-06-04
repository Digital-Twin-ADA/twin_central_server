package com.digitaltwin.central;

import com.digitaltwin.central.dto.AlertRequestDto;
import com.digitaltwin.central.model.Artist;
import com.digitaltwin.central.model.LineupEvent;
import com.digitaltwin.central.model.ParticipantLocation;
import com.digitaltwin.central.model.PointOfInterest;
import com.digitaltwin.central.model.SpontaneousEvent;
import com.digitaltwin.central.model.Stage;
import com.digitaltwin.central.repository.AlertRepository;
import com.digitaltwin.central.repository.ArtistRepository;
import com.digitaltwin.central.repository.FestivalInfoRepository;
import com.digitaltwin.central.repository.LineupEventRepository;
import com.digitaltwin.central.repository.NotificationAttemptRepository;
import com.digitaltwin.central.repository.ParticipantLocationRepository;
import com.digitaltwin.central.repository.PointOfInterestRepository;
import com.digitaltwin.central.repository.SpontaneousEventRepository;
import com.digitaltwin.central.repository.StageRepository;
import com.digitaltwin.central.repository.WebhookSubscriberRepository;
import com.digitaltwin.central.service.AlertService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class CentralServerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StageRepository stageRepository;

	@Autowired
	private ArtistRepository artistRepository;

	@Autowired
	private LineupEventRepository lineupEventRepository;

	@Autowired
	private SpontaneousEventRepository spontaneousEventRepository;

	@Autowired
	private PointOfInterestRepository pointOfInterestRepository;

	@Autowired
	private ParticipantLocationRepository participantLocationRepository;

	@Autowired
	private FestivalInfoRepository festivalInfoRepository;

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private WebhookSubscriberRepository webhookSubscriberRepository;

	@Autowired
	private NotificationAttemptRepository notificationAttemptRepository;

	@Autowired
	private AlertService alertService;

	@BeforeEach
	void setUp() {
		notificationAttemptRepository.deleteAll();
		webhookSubscriberRepository.deleteAll();
		alertRepository.deleteAll();
		spontaneousEventRepository.deleteAll();
		lineupEventRepository.deleteAll();
		participantLocationRepository.deleteAll();
		pointOfInterestRepository.deleteAll();
		stageRepository.deleteAll();
		festivalInfoRepository.deleteAll();
		artistRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void stageAndTelemetryEndpointsWork() throws Exception {
		String createdStage = mockMvc.perform(post("/api/stages")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Main Stage",
								  "capacity": 1000,
								  "zoneCode": "A1"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Main Stage"))
				.andExpect(jsonPath("$.capacity").value(1000))
				.andExpect(jsonPath("$.currentCrowd").value(0))
				.andExpect(jsonPath("$.overcrowded").value(false))
				.andExpect(jsonPath("$.zoneCode").value("A1"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String stageId = createdStage.replaceAll(".*\"id\":(\\d+).*", "$1");

		mockMvc.perform(get("/api/stages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(Integer.parseInt(stageId)));

		mockMvc.perform(post("/api/telemetry")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stageId": %s,
								  "currentCrowd": 1200
								}
								""".formatted(stageId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentCrowd").value(1200))
				.andExpect(jsonPath("$.overcrowded").value(true));
	}

	@Test
	void festivalInfoEndpointReturnsFestivalAndStageCoordinates() throws Exception {
		mockMvc.perform(post("/api/stages")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Main Stage",
								  "capacity": 1000,
								  "zoneCode": "A1",
								  "latitude": 44.4396,
								  "longitude": 26.0963
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/festival/info")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "ADA Festival",
								  "latitude": 44.438,
								  "longitude": 26.097,
								  "description": "Digital twin test festival"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("ADA Festival"))
				.andExpect(jsonPath("$.latitude").value(44.438))
				.andExpect(jsonPath("$.longitude").value(26.097));

		mockMvc.perform(get("/api/festival/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("ADA Festival"))
				.andExpect(jsonPath("$.stages", hasSize(1)))
				.andExpect(jsonPath("$.stages[0].name").value("Main Stage"))
				.andExpect(jsonPath("$.stages[0].zoneCode").value("A1"))
				.andExpect(jsonPath("$.stages[0].latitude").value(44.4396))
				.andExpect(jsonPath("$.stages[0].longitude").value(26.0963));
	}

	@Test
	void artistEndpointsReturnListAndDetails() throws Exception {
		Artist artist = artistRepository.save(new Artist(
				"Aria Nova",
				"Pop",
				"High-energy pop artist known for immersive festival performances.",
				"Romania",
				"https://example.com/aria-nova.jpg"
		));

		artistRepository.save(new Artist(
				"DJ Pulsewave",
				"Electronic",
				"Electronic producer blending melodic techno with festival bass.",
				"Netherlands",
				null
		));

		mockMvc.perform(get("/api/artists"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].name").value("Aria Nova"))
				.andExpect(jsonPath("$[0].genre").value("Pop"));

		mockMvc.perform(get("/api/artists/{id}", artist.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(artist.getId()))
				.andExpect(jsonPath("$.name").value("Aria Nova"))
				.andExpect(jsonPath("$.genre").value("Pop"))
				.andExpect(jsonPath("$.bio").value("High-energy pop artist known for immersive festival performances."))
				.andExpect(jsonPath("$.country").value("Romania"))
				.andExpect(jsonPath("$.imageUrl").value("https://example.com/aria-nova.jpg"));
	}

	@Test
	void lineupEndpointReturnsEventsOrderedByStartTime() throws Exception {
		Stage mainStage = stageRepository.save(new Stage("Main Stage", 1000, 0, false, "A1"));
		Stage electronicStage = stageRepository.save(new Stage("Electronic Stage", 2000, 0, false, "E1"));

		Artist artist = artistRepository.save(new Artist(
				"Aria Nova",
				"Pop",
				"High-energy pop artist known for immersive festival performances.",
				"Romania",
				null
		));

		Artist dj = artistRepository.save(new Artist(
				"DJ Pulsewave",
				"Electronic",
				"Electronic producer blending melodic techno with festival bass.",
				"Netherlands",
				null
		));

		lineupEventRepository.save(new LineupEvent(
				dj,
				electronicStage,
				OffsetDateTime.parse("2026-07-10T22:00:00+03:00"),
				OffsetDateTime.parse("2026-07-10T23:30:00+03:00"),
				"Late Electronic Set",
				"SCHEDULED"
		));

		lineupEventRepository.save(new LineupEvent(
				artist,
				mainStage,
				OffsetDateTime.parse("2026-07-10T18:00:00+03:00"),
				OffsetDateTime.parse("2026-07-10T19:15:00+03:00"),
				"Opening Pop Set",
				"SCHEDULED"
		));

		mockMvc.perform(get("/api/lineup"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].artistName").value("Aria Nova"))
				.andExpect(jsonPath("$[0].artistGenre").value("Pop"))
				.andExpect(jsonPath("$[0].stageName").value("Main Stage"))
				.andExpect(jsonPath("$[0].stageZoneCode").value("A1"))
				.andExpect(jsonPath("$[0].startsAt").value("2026-07-10T18:00:00+03:00"))
				.andExpect(jsonPath("$[0].endsAt").value("2026-07-10T19:15:00+03:00"))
				.andExpect(jsonPath("$[0].title").value("Opening Pop Set"))
				.andExpect(jsonPath("$[0].status").value("SCHEDULED"))
				.andExpect(jsonPath("$[1].artistName").value("DJ Pulsewave"));
	}

	@Test
	void spontaneousAndCurrentEventEndpointsWork() throws Exception {
		Stage mainStage = stageRepository.save(new Stage("Main Stage", 1000, 0, false, "A1"));
		Stage acousticStage = stageRepository.save(new Stage("Acoustic Stage", 800, 0, false, "B1"));
		Artist artist = artistRepository.save(new Artist("Aria Nova", "Pop", "Festival pop artist.", "Romania", null));

		OffsetDateTime now = OffsetDateTime.now();
		lineupEventRepository.save(new LineupEvent(
				artist,
				mainStage,
				now.minusMinutes(10),
				now.plusMinutes(50),
				"Main Stage Live Set",
				"LIVE"
		));

		SpontaneousEvent spontaneousEvent = new SpontaneousEvent();
		spontaneousEvent.setTitle("Surprise Guest");
		spontaneousEvent.setDescription("Surprise guest at Acoustic Stage");
		spontaneousEvent.setStage(acousticStage);
		spontaneousEvent.setStartsAt(now.minusMinutes(5));
		spontaneousEvent.setEndsAt(now.plusMinutes(30));
		spontaneousEvent.setCreatedAt(now.minusMinutes(5));
		spontaneousEvent.setStatus("LIVE");
		spontaneousEventRepository.save(spontaneousEvent);

		mockMvc.perform(post("/api/events/spontaneous")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "1 + 1 Lemonade",
								  "description": "Buy one lemonade and get one free at the bar.",
								  "stageId": %s
								}
								""".formatted(mainStage.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("spontaneous"))
				.andExpect(jsonPath("$.title").value("1 + 1 Lemonade"))
				.andExpect(jsonPath("$.stageId").value(mainStage.getId()))
				.andExpect(jsonPath("$.status").value("LIVE"));

		mockMvc.perform(get("/api/events/current"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].stageName").value("Main Stage"))
				.andExpect(jsonPath("$[0].events", hasSize(2)))
				.andExpect(jsonPath("$[0].events[0].type").value("lineup"))
				.andExpect(jsonPath("$[1].stageName").value("Acoustic Stage"))
				.andExpect(jsonPath("$[1].events", hasSize(1)))
				.andExpect(jsonPath("$[1].events[0].type").value("spontaneous"));
	}

	@Test
	void pointsOfInterestEndpointsReturnFilteredLocationsAndDetails() throws Exception {
		PointOfInterest restaurant = pointOfInterestRepository.save(new PointOfInterest(
				"Vegan Garden",
				"RESTAURANT",
				"Plant-based meals near Main Stage.",
				44.4391,
				26.0961,
				"A1",
				"10:00-02:00"
		));

		pointOfInterestRepository.save(new PointOfInterest(
				"Craft Beer Bar",
				"BAR",
				"Local craft beer and soft drinks.",
				44.4392,
				26.0962,
				"A2",
				"12:00-03:00"
		));

		mockMvc.perform(get("/api/points-of-interest").param("type", "restaurant"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Vegan Garden"))
				.andExpect(jsonPath("$[0].type").value("RESTAURANT"))
				.andExpect(jsonPath("$[0].latitude").value(44.4391))
				.andExpect(jsonPath("$[0].longitude").value(26.0961))
				.andExpect(jsonPath("$[0].zoneCode").value("A1"));

		mockMvc.perform(get("/api/points-of-interest/{id}", restaurant.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Vegan Garden"))
				.andExpect(jsonPath("$.openingHours").value("10:00-02:00"));
	}

	@Test
	void participantLocationEndpointsIngestLocationsAndReturnHeatmapPoints() throws Exception {
		Stage mainStage = stageRepository.save(new Stage("Main Stage", 1000, 0, false, "A1"));

		mockMvc.perform(post("/api/participant-locations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "participantId": "user-123",
								  "stageId": %s,
								  "latitude": 45.7489,
								  "longitude": 21.2087,
								  "zoneCode": "A1"
								}
								""".formatted(mainStage.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.participantId").value("user-123"))
				.andExpect(jsonPath("$.stageId").value(mainStage.getId()))
				.andExpect(jsonPath("$.stageName").value("Main Stage"))
				.andExpect(jsonPath("$.latitude").value(45.7489))
				.andExpect(jsonPath("$.longitude").value(21.2087))
				.andExpect(jsonPath("$.zoneCode").value("A1"));

		mockMvc.perform(post("/api/participant-locations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "participantId": "user-123",
								  "stageId": %s,
								  "latitude": 45.7499,
								  "longitude": 21.2097,
								  "zoneCode": "A2"
								}
								""".formatted(mainStage.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.participantId").value("user-123"))
				.andExpect(jsonPath("$.latitude").value(45.7499))
				.andExpect(jsonPath("$.longitude").value(21.2097))
				.andExpect(jsonPath("$.zoneCode").value("A2"));

		ParticipantLocation olderLocation = new ParticipantLocation();
		olderLocation.setParticipantId("old-user");
		olderLocation.setLatitude(45.7000);
		olderLocation.setLongitude(21.2000);
		olderLocation.setRecordedAt(OffsetDateTime.now().minusMinutes(90));
		participantLocationRepository.save(olderLocation);

		mockMvc.perform(get("/api/participant-locations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get("/api/participant-locations/heatmap").param("minutes", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].latitude").value(45.7499))
				.andExpect(jsonPath("$[0].longitude").value(21.2097))
				.andExpect(jsonPath("$[0].stageId").value(mainStage.getId()))
				.andExpect(jsonPath("$[0].stageName").value("Main Stage"))
				.andExpect(jsonPath("$[0].zoneCode").value("A2"))
				.andExpect(jsonPath("$[0].weight").value(1));
	}

	@Test
	void openApiDocsIncludeParticipantLocationEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/participant-locations']").exists())
				.andExpect(jsonPath("$.paths['/api/participant-locations/heatmap']").exists())
				.andExpect(jsonPath("$.tags[?(@.name == 'Participant Locations')].description").value(hasSize(1)))
				.andExpect(jsonPath("$.paths['/api/participant-locations'].post.summary")
						.value("Submit participant location"))
				.andExpect(jsonPath("$.paths['/api/participant-locations/heatmap'].get.description")
						.value(containsString("heatmap-ready latest participant location points")));
	}

	@Test
	void webhooksReceiveSignedAlertPayloads() throws Exception {
		LinkedBlockingQueue<ReceivedWebhook> received = new LinkedBlockingQueue<>();
		HttpServer server = startWebhookServer(received);

		try {
			String webhookUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
			mockMvc.perform(post("/api/webhooks")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "url": "%s",
									  "secret": "shared-secret"
									}
									""".formatted(webhookUrl)))
					.andExpect(status().isCreated());

			AlertRequestDto dto = new AlertRequestDto();
			dto.setType("TEST");
			dto.setSeverity("LOW");
			dto.setMessage("Webhook delivery check");
			alertService.createAlert(dto);

			ReceivedWebhook webhook = received.poll(5, TimeUnit.SECONDS);
			assertThat(webhook)
					.as("recorded attempts: %s", notificationAttemptRepository.findAll().stream()
							.map(attempt -> attempt.getStatus() + " " + attempt.getLastResponse())
							.toList())
					.isNotNull();
			assertThat(webhook.body()).contains("\"type\":\"TEST\"");
			assertThat(webhook.body()).contains("\"createdAt\":");
			assertThat(webhook.signature()).startsWith("sha256=");
			assertThat(notificationAttemptRepository.findAll())
					.singleElement()
					.satisfies(attempt -> assertThat(attempt.getStatus()).isEqualTo("200"));
		} finally {
			server.stop(0);
		}
	}

	private HttpServer startWebhookServer(LinkedBlockingQueue<ReceivedWebhook> received) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/hook", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			String signature = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");
			received.add(new ReceivedWebhook(body, signature));
			exchange.sendResponseHeaders(200, 0);
			exchange.close();
		});
		server.start();
		return server;
	}

	private record ReceivedWebhook(String body, String signature) {
	}
}
