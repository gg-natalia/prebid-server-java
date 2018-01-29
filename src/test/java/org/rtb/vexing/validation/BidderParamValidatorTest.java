package org.rtb.vexing.validation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.rtb.vexing.VertxTest;
import org.rtb.vexing.auction.BidderCatalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

public class BidderParamValidatorTest extends VertxTest {

    public static final String RUBICON = "rubicon";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BidderCatalog bidderCatalog;

    private BidderParamValidator bidderParamValidator;

    @Before
    public void setUp() {
        given(bidderCatalog.names()).willReturn(singleton(RUBICON));

        bidderParamValidator = BidderParamValidator.create(bidderCatalog, "/static/bidder-params");
    }

    @Test
    public void createShouldFailOnNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> BidderParamValidator.create(null, null));
        assertThatNullPointerException().isThrownBy(() -> BidderParamValidator.create(bidderCatalog, null));
    }

    @Test
    public void createShouldFailOnInvalidSchemaPath() {
        assertThatIllegalArgumentException().isThrownBy(() -> BidderParamValidator.create(bidderCatalog, "/noschema"));
    }

    @Test
    public void createShouldFailOnEmptySchemaFile() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> BidderParamValidator.create(bidderCatalog, "schema/empty"));
    }

    @Test
    public void createShouldFailOnInvalidSchemaFile() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> BidderParamValidator.create(bidderCatalog, "schema/invalid"));
    }

    @Test
    public void validateShouldNotReturnValidationMessagesWhenBidderExtIsOk() {

        //given
        final RubiconExt ext = RubiconExt.builder().accountId(1).siteId(2).zoneId(3).build();
        final JsonNode node = defaultNamingMapper.convertValue(ext, JsonNode.class);

        //when
        final Set<String> messages = bidderParamValidator.validate(RUBICON, node);

        //then
        assertThat(messages).isEmpty();
    }

    @Test
    public void validateShouldReturnValidationMessagesWhenBidderExtNotValid() {

        //given
        final RubiconExt ext = RubiconExt.builder().siteId(2).zoneId(3).build();

        final JsonNode node = defaultNamingMapper.convertValue(ext, JsonNode.class);

        final Set<String> messages = bidderParamValidator.validate(RUBICON, node);

        //then
        assertThat(messages.size()).isEqualTo(1);
    }

    @Test
    public void schemaShouldReturnSchemasString() throws IOException {

        //given
        given(bidderCatalog.names()).willReturn(new HashSet<>(asList("test-rubicon", "test-appnexus")));

        bidderParamValidator = BidderParamValidator.create(bidderCatalog, "schema/valid");

        //when
        final String result = bidderParamValidator.schemas();

        //then
        assertThat(result).isEqualTo(readFromClasspath("schema/valid/test-schemas.json"));
    }

    @Builder(toBuilder = true)
    @FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
    private static class RubiconExt {
        Integer accountId;
        Integer siteId;
        Integer zoneId;
    }

    private static String readFromClasspath(String path) throws IOException {
        String content = null;

        final InputStream resourceAsStream = BidderParamValidatorTest.class.getResourceAsStream(path);
        if (resourceAsStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,
                    StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }
        }
        return content;
    }
}