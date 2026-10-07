package com.saga.thumbnail.controller;

import com.saga.thumbnail.service.FfmpegService;
import com.saga.thumbnail.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/thumbnail")
@RequiredArgsConstructor
public class ThumbnailController {

    private final GeminiService geminiService;
    private final FfmpegService ffmpegService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createImageThumbnail(
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "ratio", defaultValue = "16:9") String ratio) {
        File tempVideo = null;
        File tempImage = null;
        File finalImage = null;

        try {
            // 1. 동영상 임시 파일 저장
            tempVideo = File.createTempFile("upload_", ".mp4");
            video.transferTo(tempVideo);
            log.info("Video saved to temp file: {}", tempVideo.getAbsolutePath());

            // 2. Gemini 영상 분석 (타임스탬프, 텍스트)
            GeminiService.ThumbnailInfo info = geminiService.analyzeVideoForThumbnail(tempVideo);
            log.info("Gemini Analysis Result: Timestamp={}, Text={}, Position={}", info.getTimestamp(), info.getText(), info.getPosition());

            // 3. FFmpeg 프레임 추출
            tempImage = ffmpegService.extractFrame(tempVideo, info.getTimestamp());

            // 4. FFmpeg 자막 합성 및 크롭
            finalImage = ffmpegService.addTextToImage(tempImage, info.getText(), ratio, info.getPosition());

            // 5. 결과 반환
            Resource resource = createResourceAndDeleteAfter(finalImage);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"thumbnail.jpg\"")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error creating thumbnail", e);
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                return ResponseEntity.status(429).body("{\"error\": \"무료 제공 AI API의 일일 사용량(20회)을 모두 소진했습니다. 내일 다시 이용해주세요.\"}");
            }
            if (e.getMessage() != null && e.getMessage().contains("503")) {
                return ResponseEntity.status(503).body("{\"error\": \"AI 서버에 일시적인 과부하가 발생했습니다. 잠시 후 다시 시도해 주세요.\"}");
            }
            return ResponseEntity.internalServerError().body("{\"error\": \"서버 내부 오류가 발생했습니다.\"}");
        } finally {
            // 임시 파일 삭제 (최종 반환 파일은 유지하거나 일정 시간 후 삭제 로직 필요 - MVP에서는 단순화)
            if (tempVideo != null && tempVideo.exists()) tempVideo.delete();
            if (tempImage != null && tempImage.exists()) tempImage.delete();
        }
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createHighlightVideo(
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "ratio", defaultValue = "16:9") String ratio) {
        File tempVideo = null;
        File finalVideo = null;

        try {
            // 1. 동영상 임시 파일 저장
            tempVideo = File.createTempFile("upload_vid_", ".mp4");
            video.transferTo(tempVideo);
            log.info("Video saved to temp file: {}", tempVideo.getAbsolutePath());

            // 2. Gemini 하이라이트 구간 분석 (start, end, text)
            GeminiService.HighlightInfo info = geminiService.analyzeVideoForHighlight(tempVideo);
            log.info("Gemini Analysis Result: Start={}, End={}, Text={}, Position={}", info.getStart(), info.getEnd(), info.getText(), info.getPosition());

            // 3. FFmpeg 구간 자르기 및 자막 합성
            finalVideo = ffmpegService.extractHighlightVideo(tempVideo, info.getStart(), info.getEnd(), info.getText(), ratio, info.getPosition());

            // 4. 결과 반환
            Resource resource = createResourceAndDeleteAfter(finalVideo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"highlight.mp4\"")
                    .contentType(MediaType.valueOf("video/mp4"))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error creating highlight video", e);
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                return ResponseEntity.status(429).body("{\"error\": \"무료 제공 AI API의 일일 사용량(20회)을 모두 소진했습니다. 내일 다시 이용해주세요.\"}");
            }
            if (e.getMessage() != null && e.getMessage().contains("503")) {
                return ResponseEntity.status(503).body("{\"error\": \"AI 서버에 일시적인 과부하가 발생했습니다. 잠시 후 다시 시도해 주세요.\"}");
            }
            return ResponseEntity.internalServerError().body("{\"error\": \"서버 내부 오류가 발생했습니다.\"}");
        } finally {
            if (tempVideo != null && tempVideo.exists()) tempVideo.delete();
        }
    }

    /** 다운로드용 스트림이 닫힐 때 원본 파일을 자동 삭제하는 Resource 래퍼 */
    private Resource createResourceAndDeleteAfter(File file) {
        Path path = file.toPath();
        return new FileSystemResource(path) {
            @Override
            public java.io.InputStream getInputStream() throws java.io.IOException {
                return new java.io.FilterInputStream(super.getInputStream()) {
                    @Override
                    public void close() throws java.io.IOException {
                        super.close();
                        Files.deleteIfExists(path);
                    }
                };
            }
        };
    }
}
