# live-sports-tracking-system
A Java-based microservice (using Spring Boot) that tracks live sports events in real time. It polls an external REST API every 10 seconds, transforms the data, and sends it to a Kafka topic for downstream consumption.

## Technologies Used

* **Spring Boot**: The core framework for building the application.
* **Apache Kafka**: Used for building real-time data pipelines and streaming. Event updates are sent to a Kafka topic.
* **WebClient**: For asynchronous and non-blocking communication with the external sports API.
* **Lombok**: Reduces boilerplate code (e.g., getters, setters, constructors).
* **Maven**: Dependency management and build automation.
* **Docker & Docker Compose**: For local development environment setup, particularly for Kafka and ZooKeeper.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

* Java Development Kit (JDK) 17
* Maven 3.6.0
* Docker
* A running Kafka instance (Docker Compose as described below)

### Design Choice Summary
Microservice architecture type -> chosen for scalability, allowing the event status tracking functionality to operate and scale alone
Used scheduling to get data -> allowing interacting with the external REST APIs used
Kafka -> used for real-time data producing messages into topics / provide message durability for downstream systems
Spring WebClient -> used for non blocking, asynchronous HTTP communication
In-memory ConcurrentHashMap -> provides high-performance and thread-safe storage

### Local Development Setup

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/florin-marius-drilea/live-sports-tracking-system.git](https://github.com/florin-marius-drilea/live-sports-tracking-system.git)
    cd live-sports-tracking-system
    ```

2.  **Configure `application.properties`:**
    Ensure your `src/main/resources/application.properties` file is configured correctly. Key properties include:
    ```properties
    spring.application.name=live-sports-tracking-system
    server.port=8080

    app.polling.interval.seconds=10
    app.kafka.topic.sports-events=event-updates-topic # Ensure this matches your KafkaProducerService

    spring.kafka.bootstrap-servers=localhost:9092
    spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
    spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

    logging.level.root=INFO
    logging.level.com.sports.livesportstrackingsystem=DEBUG

    external.api.url=http://localhost:8080/mock-event-api/{eventId}
    ```

3.  **Run Kafka Locally with Docker Compose:**
    Before starting the Spring Boot application, you need a running Kafka broker and ZooKeeper. Use the provided `docker-compose.yml` file:
    ```bash
    docker-compose up -d
    ```
    This will start ZooKeeper and Kafka containers.

    Tear down docker / kafka
    ```bash
    docker-compose down
    ```

4.  **Build the Project:**
    ```bash
    mvn clean install
    ```

### Running the Application

After building, you can run the Spring Boot application:

```bash
mvn spring-boot:run
```
or start it from LiveSportsTrackingSystemApplication

### How to use it

Once the app is up and running - you can interact with sports events and keep track of it.
The app will automatically poll an external API and publish updates to Kafka.

1. Start Tracking a Live Event
- use curl in terminal for example (use NOT_LIVE to stop tracking an event - also stop de scheduling happening every 10 seconds)
```bash
curl -X POST http://localhost:8080/events/status \
-H "Content-Type: application/json" \
-d '{
"eventId": "football-match-123",
"status": "LIVE"
}'
```

2. Verify Kafka Messages 
```
docker exec -it kafka /bin/bash 
kafka-console-consumer --bootstrap-server localhost:9092 --topic {sports-events-topic} --from-beginning
```

3. Observe application logs
   - when scheduling starts/ stops
   - each api call made
   - responses received from api
   - when sending messages to kafka topic
   - observe any errors that occur

Run Tests
```bash
mvn test
``` 


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

Used GitHub Copilot to generate unit tests especially for Controller / Service and Kafka classes to test functionality developed quicker.
Used to cover Status Updates, Scheduling Calls and error conditions in EventServiceIntegrationTests and EventServiceTest
Sending Messages to kafka as well tested in KafkaProducerServiceTest (success and fail)
