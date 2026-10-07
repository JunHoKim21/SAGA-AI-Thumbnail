package com.saga.thumbnail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class ThumbnailInfo {
        private int timestamp;
        private String text;
        private String position; // e.g., top-left, bottom-right
    }

    public ThumbnailInfo analyzeVideoForThumbnail(File videoFile) throws Exception {
        // 1. 영상 파일 업로드 (File API)
        String fileUri = uploadVideo(videoFile);
        log.info("Video uploaded to Gemini: {}", fileUri);

        // 2. GenerateContent 호출하여 분석
        String generateUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + apiKey;

        String prompt = "이 영상을 분석해줘. 썸네일 이미지로 쓰기 가장 완벽한 1개의 장면의 타임스탬프(초 단위 정수)와, " +
                "사람들의 시선을 끌 만한 썸네일용 텍스트(한국어, 1줄, 15자 이내)를 추출해줘. " +
                "또한 영상의 구도(사람 얼굴이나 주요 피사체 위치)를 고려하여 텍스트가 피사체를 가리지 않고 가장 어울리는 최적의 위치를 'position' 필드에 영어로 (top-left, top-right, bottom-left, bottom-right, center, bottom-center 중 하나) 반환해줘. " +
                "반드시 JSON 형식으로만 응답해. 예시: {\"timestamp\": 5, \"text\": \"이게 진짜라고?\", \"position\": \"bottom-left\"}";

        // JSON Request Body 만들기
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileUri", fileUri);
        fileData.put("mimeType", "video/mp4");

        Map<String, Object> filePart = new HashMap<>();
        filePart.put("fileData", fileData);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(filePart, textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        
        // [중요 수정] Gemini가 순수 JSON만 응답하도록 강제
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // API 호출 (재시도 로직 추가: 503 및 429 에러 방어)
        ResponseEntity<String> response = null;
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                response = restTemplate.postForEntity(generateUrl, entity, String.class);
                break; // 성공 시 탈출
            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {
                log.warn("Gemini API 503 과부하 발생. 3초 후 재시도합니다... ({} / {})", i + 1, maxRetries);
                if (i == maxRetries - 1) throw e;
                Thread.sleep(3000);
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                log.warn("Gemini API 429 한도초과 발생. 5초 후 재시도합니다... ({} / {})", i + 1, maxRetries);
                if (i == maxRetries - 1) throw e;
                Thread.sleep(5000);
            }
        }
        
        return parseResponse(response.getBody());
    }

    public ThumbnailInfo analyzeAudioForThumbnail(File audioFile) throws Exception {
        String fileUri = uploadMedia(audioFile, "audio/mp3");
        log.info("Audio uploaded to Gemini: {}", fileUri);

        String generateUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + apiKey;
        String prompt = "첨부된 오디오(음성) 내용을 듣고, 사람들이 가장 주목할 만한 핵심 발언이 나오는 시작 시점의 타임스탬프(초 단위 정수)와 그 내용에 어울리는 썸네일용 문구(한국어, 1줄, 15자 이내)를 추출해줘. 반드시 JSON 형식으로만 응답해. 예시: {\"timestamp\": 15, \"text\": \"이게 진짜라고?\"}";

        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileUri", fileUri);
        fileData.put("mimeType", "audio/mp3");

        Map<String, Object> filePart = new HashMap<>();
        filePart.put("fileData", fileData);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(filePart, textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = null;
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                response = restTemplate.postForEntity(generateUrl, entity, String.class);
                break;
            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {
                log.warn("Gemini API 503 과부하 발생 (오디오). 3초 후 재시도... ({} / {})", i + 1, maxRetries);
                if (i == maxRetries - 1) throw e;
                Thread.sleep(3000);
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                log.warn("Gemini API 429 한도초과 발생 (오디오). 5초 후 재시도... ({} / {})", i + 1, maxRetries);
                if (i == maxRetries - 1) throw e;
                Thread.sleep(5000);
            }
        }
        
        return parseResponse(response.getBody());
    }

    private String uploadVideo(File videoFile) throws Exception {
        return uploadMedia(videoFile, "video/mp4");
    }

    private String uploadMedia(File file, String mimeType) throws Exception {
        String uploadUrl = "https://generativelanguage.googleapis.com/upload/v1beta/files?uploadType=media&key=" + apiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(mimeType));
        
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("file").path("uri").asText();
    }

    private ThumbnailInfo parseResponse(String jsonResponse) throws Exception {
        // 응답 텍스트 파싱
        JsonNode root = objectMapper.readTree(jsonResponse);
        String text = root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
                        
        log.info("Gemini Raw Text: {}", text);
        
        // 정규식으로 순수 JSON {} 껍데기만 추출 (불필요한 텍스트 섞임 방지)
        int startIndex = text.indexOf('{');
        int endIndex = text.lastIndexOf('}');
        if (startIndex == -1 || endIndex == -1) {
             throw new RuntimeException("AI가 올바른 JSON 형식으로 응답하지 않았습니다. 원본: " + text);
        }
        
        String cleanJson = text.substring(startIndex, endIndex + 1);
        
        JsonNode resultNode = objectMapper.readTree(cleanJson);
        ThumbnailInfo info = new ThumbnailInfo();
        info.setTimestamp(resultNode.path("timestamp").asInt());
        info.setText(resultNode.path("text").asText());
        info.setPosition(resultNode.path("position").asText("bottom-left"));
        
        return info;
    }

    @Data
    public static class HighlightInfo {
        private int start;
        private int end;
        private String text;
        private String position;
    }

    public HighlightInfo analyzeVideoForHighlight(File videoFile) throws Exception {
        String fileUri = uploadVideo(videoFile);
        log.info("Video uploaded to Gemini: {}", fileUri);
        
        // 프롬프트 내부의 따옴표가 JSON payload를 깨트리지 않도록 홑따옴표(')로 변경
        String prompt = "이 영상을 분석해줘. 사람들이 가장 시선을 뺏길 만한 핵심 하이라이트 영상의 시작 시간(start)과 종료 시간(end)(초 단위 정수, 구간 길이는 15~30초 내외)을 정해주고, 이 하이라이트 영상에 어울리는 자막용 텍스트(한국어, 1줄, 15자 이내)를 추출해줘. 또한 영상의 구도(사람 얼굴이나 주요 피사체 위치)를 고려하여 텍스트가 피사체를 가리지 않고 가장 어울리는 최적의 위치를 'position' 필드에 영어로 (top-left, top-right, bottom-left, bottom-right, center, bottom-center 중 하나) 반환해줘. 반드시 JSON 형식으로만 답해줘. 예시: {'start': 10, 'end': 35, 'text': '시선 집중! 최고의 순간', 'position': 'bottom-left'}";

        String requestBody = String.format("""
            {
              "contents": [{
                "parts": [
                  {"fileData": {"mimeType": "video/mp4", "fileUri": "%s"}},
                  {"text": "%s"}
                ]
              }],
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
            """, fileUri, prompt);

        int retryCount = 0;
        int maxRetries = 3;
        ResponseEntity<String> response = null;

        while (retryCount < maxRetries) {
            try {
                String generateUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + apiKey;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                response = restTemplate.postForEntity(generateUrl, entity, String.class);
                break; // 성공하면 루프 탈출
            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable | org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e; 
                }
                log.warn("Gemini API 과부하 발생. 3초 후 재시도합니다... ({} / {})", retryCount, maxRetries);
                Thread.sleep(3000); 
            }
        }
        
        return parseHighlightResponse(response.getBody());
    }

    private HighlightInfo parseHighlightResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        String text = root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
                        
        log.info("Gemini Raw Highlight Text: {}", text);
        
        int startIndex = text.indexOf('{');
        int endIndex = text.lastIndexOf('}');
        if (startIndex == -1 || endIndex == -1) {
             throw new RuntimeException("AI가 올바른 JSON 형식으로 응답하지 않았습니다. 원본: " + text);
        }
        
        String cleanJson = text.substring(startIndex, endIndex + 1);
        JsonNode resultNode = objectMapper.readTree(cleanJson);
        
        HighlightInfo info = new HighlightInfo();
        info.setStart(resultNode.path("start").asInt());
        info.setEnd(resultNode.path("end").asInt());
        info.setText(resultNode.path("text").asText());
        info.setPosition(resultNode.path("position").asText("bottom-left"));
        
        return info;
    }
}
