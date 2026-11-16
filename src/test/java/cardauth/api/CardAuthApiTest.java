package cardauth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CardAuthApiTest {

    @Autowired
    private MockMvc mvc;

    private String newCard(long limitCents) throws Exception {
        String cardId = "card-" + UUID.randomUUID();
        mvc.perform(post("/api/cards").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cardId":"%s","expiration":"2035-12","creditLimitCents":%d,"currency":"USD"}
                        """.formatted(cardId, limitCents)))
                .andExpect(status().isCreated());
        return cardId;
    }

    private ResultActions authorize(String requestId, String cardId, long amountCents, String currency) throws Exception {
        return mvc.perform(post("/api/authorizations").contentType(MediaType.APPLICATION_JSON).content("""
                {"requestId":"%s","cardId":"%s","amountCents":%d,"currency":"%s","merchantName":"Shop"}
                """.formatted(requestId, cardId, amountCents, currency)));
    }

    private ResultActions card(String cardId) throws Exception {
        return mvc.perform(get("/api/cards/" + cardId));
    }

    @Test
    void createsCardWithFullCreditAvailable() throws Exception {
        String cardId = newCard(100_00);

        card(cardId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.availableCreditCents").value(100_00));
    }

    @Test
    void duplicateCardIsRejected() throws Exception {
        String cardId = newCard(100_00);

        mvc.perform(post("/api/cards").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cardId":"%s","expiration":"2035-12","creditLimitCents":100,"currency":"USD"}
                        """.formatted(cardId)))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownCardReturnsNotFound() throws Exception {
        card("does-not-exist").andExpect(status().isNotFound());
    }

    @Test
    void approvesAndReducesAvailableCredit() throws Exception {
        String cardId = newCard(100_00);

        authorize("r-" + UUID.randomUUID(), cardId, 40_00, "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.authorizationId").isNotEmpty())
                .andExpect(jsonPath("$.declineReason").doesNotExist());

        card(cardId).andExpect(jsonPath("$.availableCreditCents").value(60_00));
    }

    @Test
    void declinesWhenCreditIsInsufficient() throws Exception {
        String cardId = newCard(100_00);

        authorize("r-" + UUID.randomUUID(), cardId, 100_01, "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("INSUFFICIENT_CREDIT"));

        card(cardId).andExpect(jsonPath("$.availableCreditCents").value(100_00));
    }

    @Test
    void declinesUnknownCardAndNonPositiveAmount() throws Exception {
        authorize("r-" + UUID.randomUUID(), "ghost", 1_00, "USD")
                .andExpect(jsonPath("$.declineReason").value("CARD_NOT_FOUND"));

        String cardId = newCard(100_00);
        authorize("r-" + UUID.randomUUID(), cardId, 0, "USD")
                .andExpect(jsonPath("$.declineReason").value("INVALID_AMOUNT"));
    }

    @Test
    void declinesCurrencyMismatch() throws Exception {
        String cardId = newCard(100_00);

        authorize("r-" + UUID.randomUUID(), cardId, 1_00, "EUR")
                .andExpect(jsonPath("$.declineReason").value("CURRENCY_MISMATCH"));
    }

    @Test
    void replayedRequestIdHoldsFundsOnce() throws Exception {
        String cardId = newCard(100_00);
        String requestId = "r-" + UUID.randomUUID();

        authorize(requestId, cardId, 30_00, "USD").andExpect(jsonPath("$.decision").value("APPROVED"));
        authorize(requestId, cardId, 30_00, "USD").andExpect(jsonPath("$.decision").value("APPROVED"));

        card(cardId).andExpect(jsonPath("$.availableCreditCents").value(70_00));
    }

    @Test
    void reversalRestoresCredit() throws Exception {
        String cardId = newCard(100_00);
        String body = authorize("r-" + UUID.randomUUID(), cardId, 40_00, "USD")
                .andReturn().getResponse().getContentAsString();
        String authorizationId = JsonPath.read(body, "$.authorizationId");

        mvc.perform(post("/api/authorizations/" + authorizationId + "/reversal"))
                .andExpect(status().isNoContent());

        card(cardId).andExpect(jsonPath("$.availableCreditCents").value(100_00));
    }

    @Test
    void reversingUnknownAuthorizationReturnsNotFound() throws Exception {
        mvc.perform(post("/api/authorizations/nope/reversal")).andExpect(status().isNotFound());
    }

    @Test
    void invalidPayloadsReturnBadRequest() throws Exception {
        mvc.perform(post("/api/authorizations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        authorize("r-" + UUID.randomUUID(), "any", 1_00, "NOT-A-CURRENCY")
                .andExpect(status().isBadRequest());
    }
}
