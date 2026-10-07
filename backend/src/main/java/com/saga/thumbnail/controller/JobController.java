package com.saga.thumbnail.controller;

import com.saga.thumbnail.domain.Job;
import com.saga.thumbnail.service.FfmpegService;
import com.saga.thumbnail.service.GeminiService;
import com.saga.thumbnail.service.JobService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final GeminiService geminiService;
    private final FfmpegService ffmpegService;

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @PostMapping("/jobs")
    public ResponseEntity<Job> createJob(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "Untitled");
        Job job = jobService.createJob(name);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/jobs/{id}/video")
    public ResponseEntity<?> uploadVideo(@PathVariable String id, @RequestParam("video") MultipartFile video) {
        Job job = jobService.getJob(id);
        if (job == null) return ResponseEntity.notFound().build();
        
        try {
            File tempVideo = File.createTempFile("upload_job_", ".mp4");
            video.transferTo(tempVideo);
            job.setTempVideoPath(tempVideo.getAbsolutePath());
            log.info("Video uploaded for job {}: {}", id, tempVideo.getAbsolutePath());
            
            jobService.updateJobStatus(id, "uploaded", "영상 업로드 완료");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to upload video", e);
            jobService.updateJobStatus(id, "error", "업로드 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable String id) {
        Job job = jobService.getJob(id);
        if (job != null) {
            if (job.getTempVideoPath() != null) new File(job.getTempVideoPath()).delete();
            if (job.getFinalImagePath() != null) new File(job.getFinalImagePath()).delete();
        }
        jobService.deleteJob(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/series")
    public ResponseEntity<List<Object>> getSeries() {
        return ResponseEntity.ok(List.of());
    }
    
    @PostMapping("/series")
    public ResponseEntity<Map<String, String>> createSeries(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("id", "series-1", "name", body.get("name")));
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of("mediaReady", true, "realAIAvailable", true));
    }
    
    @PostMapping("/ai/plan")
    public ResponseEntity<Map<String, Object>> getAiPlan() {
        return ResponseEntity.ok(Map.of(
            "canExecute", true, 
            "estimatedImageRequestCount", 1,
            "provider", "Gemini",
            "videoCount", 1,
            "conceptCount", 1,
            "aspectRatio", "16:9"
        ));
    }

    @PostMapping("/jobs/{id}/start")
    public ResponseEntity<?> startJob(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Job job = jobService.getJob(id);
            if (job == null) return ResponseEntity.notFound().build();
            
            jobService.updateJobStatus(id, "processing", "AI 분석 및 디자인 중...");
            
            new Thread(() -> {
                File tempAudio = null;
                File tempImage = null;
                try {
                    File videoFile = new File(job.getTempVideoPath());
                    
                    jobService.updateJobStatus(id, "processing", "오디오 추출 중...");
                    tempAudio = ffmpegService.extractAudio(videoFile);
                    
                    jobService.updateJobStatus(id, "processing", "AI 오디오 분석 중...");
                    GeminiService.ThumbnailInfo info = geminiService.analyzeAudioForThumbnail(tempAudio);
                    log.info("Async Job - Audio AI Analysis Result: Timestamp={}, Text={}", info.getTimestamp(), info.getText());
                    
                    jobService.updateJobStatus(id, "processing", "썸네일 합성 중...");
                    String ratio = "16:9"; 
                    tempImage = ffmpegService.extractFrame(videoFile, info.getTimestamp());
                    File finalImage = ffmpegService.addTextToImage(tempImage, info.getText(), ratio, info.getPosition());
                    
                    job.setFinalImagePath(finalImage.getAbsolutePath());
                    
                    Job.Output output = new Job.Output();
                    output.setRatio(ratio);
                    output.setIndex(0);
                    output.setUrl("/api/jobs/" + id + "/image");
                    job.getOutputs().add(output);
                    
                    Job.Plan plan = new Job.Plan();
                    plan.setKeyMessage(info.getText());
                    plan.setSummary("AI가 영상에서 가장 의미있는 순간을 추출하여 디자인했습니다.");
                    plan.setMood("브랜드 톤앤매너 유지");
                    plan.setAudience("구독자");
                    
                    Job.Concept concept = new Job.Concept();
                    concept.setHeadline(info.getText());
                    concept.setMarketingPoint("영상의 핵심 메시지를 썸네일에 반영");
                    plan.getConcepts().add(concept);
                    
                    job.setPlan(plan);
                    jobService.updateJobStatus(id, "ready", "완료");

                } catch (Exception e) {
                    log.error("Async Job failed", e);
                    String errorMsg = "제작 실패: " + e.getMessage();
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        errorMsg = "무료 제공 AI API의 일일 사용량(20회)을 모두 소진했습니다. 내일 다시 이용해주세요.";
                    } else if (e.getMessage() != null && e.getMessage().contains("503")) {
                        errorMsg = "AI 서버에 일시적인 과부하가 발생했습니다. 잠시 후 다시 시도해 주세요.";
                    }
                    jobService.updateJobStatus(id, "error", errorMsg);
                } finally {
                    if (tempAudio != null && tempAudio.exists()) tempAudio.delete();
                    if (tempImage != null && tempImage.exists()) tempImage.delete();
                }
            }).start();
            
            return ResponseEntity.ok(Map.of("status", "accepted"));
        } catch (Exception e) {
            log.error("Internal Error in startJob", e);
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    @PostMapping("/jobs/{id}/real-ai")
    public ResponseEntity<?> startRealAiJob(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            ResponseEntity<?> res = startJob(id, body);
            if (res.getStatusCode().isError()) {
                return res;
            }
            Job job = jobService.getJob(id);
            if (job == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(java.util.Map.of("status", "accepted", "job", job));
        } catch (Exception e) {
            log.error("Internal Error in startRealAiJob", e);
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping("/jobs/{id}/image")
    public ResponseEntity<Resource> getJobImage(@PathVariable String id) {
        Job job = jobService.getJob(id);
        if (job == null || job.getFinalImagePath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        File file = new File(job.getFinalImagePath());
        if (!file.exists()) return ResponseEntity.notFound().build();
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new FileSystemResource(file));
    }
}
