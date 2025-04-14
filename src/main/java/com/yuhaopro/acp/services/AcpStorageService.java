package com.yuhaopro.acp.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.yuhaopro.acp.data.RuntimeEnvironment;
import com.yuhaopro.acp.data.process.AcpStoragePOJO;

// send request to acp storage.
@Service
public class AcpStorageService {

    private final Logger logger = LoggerFactory.getLogger(AcpStorageService.class);
    private final RuntimeEnvironment environment;

    public AcpStorageService(RuntimeEnvironment environment) {
        this.environment = environment;
    }

    public String postToStorage(AcpStoragePOJO data) {
        String uuid = "";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AcpStoragePOJO> requestEntity = new HttpEntity<>(data, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    environment.getAcpStorageUrl() + "api/v1/blob",
                    requestEntity,
                    String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK
                    || responseEntity.getStatusCode() == HttpStatus.CREATED) {
                uuid = responseEntity.getBody();
                uuid = removeSurroundingQuotes(uuid);
                logger.info("Successfully posted data. Received UUID: {}", uuid);
            }

        } catch (HttpClientErrorException e) {
            logger.error("Client error posting data: {}", e.getResponseBodyAsString());
        } catch (RestClientException e) {
            logger.error("Error posting data to storage endpoint: {}", e.getMessage());
        } catch (NullPointerException e) {
            logger.error("UUID is null");
        }

        return uuid;
    }

    public static String removeSurroundingQuotes(String input) {
        if (input != null && input.length() >= 2 &&
                input.startsWith("\"") && input.endsWith("\"")) {
            // Extract the substring between the first and last characters
            return input.substring(1, input.length() - 1);
        }
        return input;
    }

}
