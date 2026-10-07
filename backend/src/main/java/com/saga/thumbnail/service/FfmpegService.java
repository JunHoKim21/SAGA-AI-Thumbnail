package com.saga.thumbnail.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FfmpegService {

    public File extractAudio(File videoFile) throws Exception {
        File audioFile = File.createTempFile("audio_", ".mp3");
        
        // ffmpeg -y -i {video} -q:a 0 -map a {audio}
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", videoFile.getAbsolutePath(),
                "-q:a", "0",
                "-map", "a",
                audioFile.getAbsolutePath()
        );

        runProcess(pb, "extractAudio");
        return audioFile;
    }

    public File extractFrame(File videoFile, int timestampSec) throws Exception {
        File outputFile = File.createTempFile("frame_", ".jpg");
        
        // ffmpeg -ss {timestamp} -i {videoFile} -vframes 1 -q:v 2 -y {outputFile}
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-ss", String.valueOf(timestampSec),
                "-i", videoFile.getAbsolutePath(),
                "-vframes", "1",
                "-q:v", "2",
                outputFile.getAbsolutePath()
        );

        runProcess(pb, "extractFrame");
        return outputFile;
    }

    public File addTextToImage(File imageFile, String text, String ratio, String position) throws Exception {
        File outputFile = File.createTempFile("final_thumb_", ".jpg");
        
        File textFile = File.createTempFile("subtitle_", ".txt");
        java.nio.file.Files.writeString(textFile.toPath(), text, java.nio.charset.StandardCharsets.UTF_8);
        
        String fontPath = escapeFilterPath("C:/Windows/Fonts/malgunbd.ttf");
        String textPath = escapeFilterPath(textFile.getAbsolutePath());
        
        int mainFontSize = "9:16".equals(ratio) ? 70 : 120;
        int brandFontSize = "9:16".equals(ratio) ? 35 : 50;
        
        String mainX = "w*0.08"; String mainY = "h*0.65";
        String brandX = "w*0.08"; String brandY = "h*0.12";

        if (position != null) {
            switch (position) {
                case "top-left": mainX = "w*0.08"; mainY = "h*0.25"; break;
                case "top-right": mainX = "w*0.92-text_w"; mainY = "h*0.25"; brandX = "w*0.92-tw"; break;
                case "bottom-left": mainX = "w*0.08"; mainY = "h*0.75"; break;
                case "bottom-right": mainX = "w*0.92-text_w"; mainY = "h*0.75"; brandX = "w*0.92-tw"; break;
                case "center": mainX = "(w-text_w)/2"; mainY = "(h-text_h)/2"; brandX = "w*0.08"; break;
                case "bottom-center": mainX = "(w-text_w)/2"; mainY = "h*0.85"; brandX = "w*0.08"; break;
            }
        }
        
        String brandText = "#SAGA_AI_STUDIO";
        String drawBrandFilter = String.format(
                "drawtext=fontfile='%s':text='%s':fontcolor=white:fontsize=%d:x=%s:y=%s:borderw=3:bordercolor=black",
                fontPath, brandText, brandFontSize, brandX, brandY
        );
        
        String drawMainTextFilter = String.format(
                "drawtext=fontfile='%s':textfile='%s':fontcolor=white:fontsize=%d:x=%s:y=%s:borderw=5:bordercolor=black",
                fontPath, textPath, mainFontSize, mainX, mainY
        );
        
        String drawTextFilter = drawBrandFilter + "," + drawMainTextFilter;
        
        String finalFilter = drawTextFilter;
        if ("9:16".equals(ratio)) {
            // 9:16 크롭 시 화면이 잘리는 문제를 해결하기 위해, 원본을 자르지 않고 위아래에 블랙 레터박스를 추가(Pad)
            finalFilter = "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black," + drawTextFilter;
        } else {
            // 16:9도 업로드 영상 비율이 다를 경우를 대비하여 1920x1080 캔버스에 꽉 차게(레터박스) 맞춤
            finalFilter = "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2:black," + drawTextFilter;
        }

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", imageFile.getAbsolutePath(),
                "-vf", finalFilter,
                outputFile.getAbsolutePath()
        );

        runProcess(pb, "addTextToImage");
        if(textFile.exists()) textFile.delete();
        
        return outputFile;
    }

    public File extractHighlightVideo(File videoFile, int startSec, int endSec, String text, String ratio, String position) throws Exception {
        File outputFile = File.createTempFile("highlight_", ".mp4");
        
        File textFile = File.createTempFile("subtitle_vid_", ".txt");
        java.nio.file.Files.writeString(textFile.toPath(), text, java.nio.charset.StandardCharsets.UTF_8);
        
        String fontPath = escapeFilterPath("C:/Windows/Fonts/malgunbd.ttf");
        String textPath = escapeFilterPath(textFile.getAbsolutePath());
        
        int mainFontSize = "9:16".equals(ratio) ? 70 : 120;
        int brandFontSize = "9:16".equals(ratio) ? 35 : 50;
        
        String mainX = "w*0.08"; String mainY = "h*0.65";
        String brandX = "w*0.08"; String brandY = "h*0.12";

        if (position != null) {
            switch (position) {
                case "top-left": mainX = "w*0.08"; mainY = "h*0.25"; break;
                case "top-right": mainX = "w*0.92-text_w"; mainY = "h*0.25"; brandX = "w*0.92-tw"; break;
                case "bottom-left": mainX = "w*0.08"; mainY = "h*0.75"; break;
                case "bottom-right": mainX = "w*0.92-text_w"; mainY = "h*0.75"; brandX = "w*0.92-tw"; break;
                case "center": mainX = "(w-text_w)/2"; mainY = "(h-text_h)/2"; brandX = "w*0.08"; break;
                case "bottom-center": mainX = "(w-text_w)/2"; mainY = "h*0.85"; brandX = "w*0.08"; break;
            }
        }
        
        String brandText = "#SAGA_AI_STUDIO";
        String drawBrandFilter = String.format(
                "drawtext=fontfile='%s':text='%s':fontcolor=white:fontsize=%d:x=%s:y=%s:borderw=3:bordercolor=black",
                fontPath, brandText, brandFontSize, brandX, brandY
        );
        
        String drawMainTextFilter = String.format(
                "drawtext=fontfile='%s':textfile='%s':fontcolor=white:fontsize=%d:x=%s:y=%s:borderw=5:bordercolor=black",
                fontPath, textPath, mainFontSize, mainX, mainY
        );
        
        String drawTextFilter = drawBrandFilter + "," + drawMainTextFilter;
        
        String finalFilter = drawTextFilter;
        if ("9:16".equals(ratio)) {
            finalFilter = "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black," + drawTextFilter;
        } else {
            finalFilter = "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2:black," + drawTextFilter;
        }

        // -ss {startSec} -to {endSec} 로 구간 자르기. 비디오는 필터적용으로 재인코딩, 오디오는 aac로 재인코딩
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-ss", String.valueOf(startSec),
                "-to", String.valueOf(endSec),
                "-i", videoFile.getAbsolutePath(),
                "-vf", finalFilter,
                "-c:v", "libx264",
                "-c:a", "aac",
                outputFile.getAbsolutePath()
        );

        runProcess(pb, "extractHighlightVideo");
        if(textFile.exists()) textFile.delete();
        
        return outputFile;
    }

    /** FFmpeg 필터 옵션용 경로 변환: 백슬래시 -> 슬래시, 콜론 -> '\:' */
    private String escapeFilterPath(String path) {
        return path.replace("\\", "/").replace(":", "\\:");
    }

    private void runProcess(ProcessBuilder pb, String taskName) throws Exception {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // 결과 출력 무시 (버퍼 가득참 방지)
        process.getInputStream().transferTo(System.out);

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg " + taskName + " timed out.");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("FFmpeg " + taskName + " failed with exit code: " + process.exitValue());
        }
    }
}
