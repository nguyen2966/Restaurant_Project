package softarch.restaurant.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson (JSON) configuration.
 *
 * Key decisions:
 *  - Dates as ISO-8601 strings ("2025-04-01T10:30:00") — not epoch millis
 *  - Null fields excluded from responses (cleaner API output)
 *  - Unknown properties on deserialization are ignored (forward-compatible)
 *  - Enum values serialised as string names (not ordinal integers)
 *  - Case-insensitive enum deserialization (accept "dine_in" or "DINE_IN")
 *  - JSONB fields (allergens, options) handled by Hibernate — ObjectMapper
 *    only handles HTTP layer serialization
 */
@Configuration
public class JacksonConfig {

    /** Consistent datetime format across all API responses. */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern(DATETIME_FORMAT);

    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // Explicit serializer/deserializer for LocalDateTime with our format
        javaTimeModule.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(DATETIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(DATETIME_FORMATTER));

        return Jackson2ObjectMapperBuilder.json()
            .modules(javaTimeModule)
            // ── Date/Time ──────────────────────────────────────────────────
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // ── Null handling ──────────────────────────────────────────────
            // Exclude null fields from responses — reduces payload size
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            // ── Enum ───────────────────────────────────────────────────────
            // Enums serialized as string names (DINE_IN not 0)
            .featuresToDisable(SerializationFeature.WRITE_ENUMS_USING_INDEX)
            // Accept enum values case-insensitively on input
            .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            // ── Deserialization safety ─────────────────────────────────────
            // Don't fail if client sends extra fields (API evolution)
            .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // Treat "null" JSON literal as empty collection (not null)
            .featuresToDisable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
    }
}
