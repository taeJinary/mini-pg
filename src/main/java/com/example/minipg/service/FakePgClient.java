package com.example.minipg.service;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FakePgClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FakePgClient(
        RestClient.Builder builder,
        ObjectMapper objectMapper,
        @Value("${pg.fake.base-url}") String baseUrl,
        @Value("${pg.fake.timeout-ms:2000}") long timeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = builder
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
        this.objectMapper = objectMapper;
    }

    public PgApproveResult approve(String mode) {
        String resolvedMode = (mode == null || mode.isBlank()) ? "success" : mode;

        try {
            PgSuccessResponse response = restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/fake-pg/approve").queryParam("mode", resolvedMode).build())
                .retrieve()
                .body(PgSuccessResponse.class);

            if (response == null || response.pgTransactionId() == null || response.pgTransactionId().isBlank()) {
                return PgApproveResult.error("EMPTY_RESPONSE", "Empty PG response");
            }

            return PgApproveResult.success(response.pgTransactionId());
        } catch (HttpClientErrorException ex) {
            PgFailResponse fail = parseFail(ex.getResponseBodyAsString());
            String code = fail != null && fail.code() != null ? fail.code() : "DECLINED";
            String message = fail != null && fail.message() != null ? fail.message() : "PG declined";
            return PgApproveResult.declined(code, message);
        } catch (ResourceAccessException ex) {
            return PgApproveResult.timeout("TIMEOUT", "PG timeout");
        } catch (RestClientException ex) {
            return PgApproveResult.error("ERROR", "PG error");
        }
    }

    public PgInquiryResult inquire(String orderId) {
        try {
            PgQueryResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/fake-pg/payments").queryParam("orderId", orderId).build())
                .retrieve()
                .body(PgQueryResponse.class);

            if (response == null || response.status() == null) {
                return new PgInquiryResult(PgInquiryResult.Status.NOT_FOUND);
            }

            return new PgInquiryResult(PgInquiryResult.Status.valueOf(response.status()));
        } catch (HttpClientErrorException ex) {
            return new PgInquiryResult(PgInquiryResult.Status.NOT_FOUND);
        } catch (ResourceAccessException ex) {
            return new PgInquiryResult(PgInquiryResult.Status.NOT_FOUND);
        } catch (RestClientException ex) {
            return new PgInquiryResult(PgInquiryResult.Status.NOT_FOUND);
        } catch (IllegalArgumentException ex) {
            return new PgInquiryResult(PgInquiryResult.Status.NOT_FOUND);
        }
    }

    private PgFailResponse parseFail(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, PgFailResponse.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private record PgSuccessResponse(String pgTransactionId) {
    }

    private record PgFailResponse(String code, String message) {
    }

    private record PgQueryResponse(String status) {
    }
}
