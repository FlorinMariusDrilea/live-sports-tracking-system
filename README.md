# Live Sports Tracking System

A Java-based microservice (built with Spring Boot) for tracking live sports events in real time. It polls an external REST API at fixed intervals (default: every 10 seconds), transforms the data, and sends it to a Kafka topic for downstream consumption.

## Technologies Used

- **Spring Boot** – Core framework for microservices
- **Apache Kafka** – Real-time messaging and event streaming
- **WebClient** – Asynchronous, non-blocking HTTP client
- **Maven** – Build and dependency management
- **Docker & Docker Compose** – Local development environment

## Getting Started

### Prerequisites

- Java 17
- Maven
- Docker
- Kafka (can be run via Docker Compose)

## Design Summary

- **Microservice architecture** – For independent scaling and deployment
- **Scheduled polling** – Polls external REST API every 10 seconds
- **Kafka** – Durable and real-time event delivery
- **WebClient** – Non-blocking API communication
- **ConcurrentHashMap** – Thread-safe in-memory task tracking

## Local Setup

### 1. Clone the repository
```bash
git clone https://github.com/florin-marius-drilea/live-sports-tracking-system.git
cd live-sports-tracking-system
```

### 2. Configure `application.properties`

Update `src/main/resources/application.properties`:
```properties
spring.application.name=live-sports-tracking-system
server.port=8080

app.polling.interval.seconds=10
app.kafka.topic.sports-events=event-updates-topic

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

logging.level.root=INFO
logging.level.com.sports.livesportstrackingsystem=DEBUG

external.api.url=http://localhost:8080/mock-event-api/{eventId}
```

### 3. Start Kafka (with ZooKeeper)
```bash
docker-compose up -d
```

Stop containers:
```bash
docker-compose down
```

### 4. Build the project
```bash
mvn clean install
```

## ▶Running the Application

Start the Spring Boot app:
```bash
mvn spring-boot:run
```
Or directly from the main class `LiveSportsTrackingSystemApplication`.

## API Usage

### 1. Start Tracking an Event
```bash
curl -X POST http://localhost:8080/events/status \
-H "Content-Type: application/json" \
-d '{
  "eventId": "football-match-123",
  "status": "LIVE"
}'
```

To **stop** tracking:
- Send the same request with `"status": "NOT_LIVE"`

### 2. View Kafka Messages
```bash
docker exec -it kafka /bin/bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic event-updates-topic --from-beginning
```

### 3. Application Logging

Monitor logs for:
- Scheduled polling events
- External API calls and responses
- Kafka message production
- Any errors

## Running Tests

```bash
mvn test
```

Includes:
- Unit tests for service, and Kafka layers
- Integration tests for event tracking and message publishing

### AI Tools used
During this project I've used ChatGPT and Github Copilot - mainly for testing and code reviews.

Also, I've used it for the creation of the External Api which is used mocked to send data on a Live Event
For ExternalApiService, it recommended me to use WebClient, because it's a non-blocking and asynchronous I/O, which is a perfect fit for 
the external API needed for this project ("WebClient is the recommended HTTP client for new Spring applications").
Other suggested possiblities:
- RestTemplate (legacy / Blocking Client)
- RestClient  (Blocking Client)
- WebClient (Non-Blocking / Efficient Resource Utilization / Good Handling)

Mono is used because it is the idiomatic way in 
Project Reactor to represent an asynchronous operation that will eventually produce zero or one result

Used chatGPT for guidance in using doOnSuccess and doOnError here instead of using try catch
and also explaining the use of ScheduledFuture interface
```bash
public void fetchAndSendEventData(String eventId) {
        log.debug("Schedule event: {}", eventId);
        externalApiServiceClient.getEventScore(eventId)
            .doOnSuccess(eventExternalApi -> {
                log.debug("Received API response for event {}: {}", eventId, eventExternalApi);
                kafkaProducerService.sendMessage(eventExternalApi);
            })
            .doOnError(error -> log.error("Error polling event {}: {}", eventId, error.getMessage()))
            .subscribe();
    }
    
private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
```

Used GitHub Copilot to generate unit tests and integration especially for Service and Kafka classes to test functionality developed quicker.
Used to cover Status Updates, Scheduling Calls and error conditions in EventServiceIntegrationTests and EventServiceTest
Sending Messages to kafka as well tested in KafkaProducerServiceTest (success and fail)

ChatGPT was also used to review / improve the documentation before submitting it and 
also to review and improve the docker-compose.yaml file which is used to start a kafka instance.