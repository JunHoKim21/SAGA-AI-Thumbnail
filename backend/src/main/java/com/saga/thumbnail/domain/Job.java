package com.saga.thumbnail.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Job {
    private String id;
    private String name;
    private String status; // "uploaded", "queued", "processing", "ready", "error", "cancelled"
    private String message;
    private String error;
    private String mode; // "local", "ai"
    private Integer progress;
    private Double duration;
    private List<Output> outputs = new ArrayList<>();
    
    // 백엔드 처리용 임시 파일 경로
    private String tempVideoPath;
    private String finalImagePath;

    // 추가 메타데이터
    private Boolean hasOriginal = true;
    private Plan plan;

    @Data
    public static class Output {
        private String url;
        private String ratio; // "16:9", "9:16"
        private Integer index;
        private String source;
    }

    @Data
    public static class Plan {
        private String keyMessage;
        private String summary;
        private String mood;
        private String audience;
        private List<String> marketingPoints = new ArrayList<>();
        private List<Concept> concepts = new ArrayList<>();
    }

    @Data
    public static class Concept {
        private String headline;
        private String emphasis;
        private String marketingPoint;
    }
}
