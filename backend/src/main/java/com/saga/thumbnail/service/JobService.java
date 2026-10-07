package com.saga.thumbnail.service;

import com.saga.thumbnail.domain.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {
    
    // InMemory 저장소 (MVP용)
    private final Map<String, Job> jobStore = new ConcurrentHashMap<>();

    public List<Job> getAllJobs() {
        return new ArrayList<>(jobStore.values());
    }

    public Job getJob(String id) {
        return jobStore.get(id);
    }

    public Job createJob(String name) {
        Job job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setName(name);
        job.setStatus("created");
        job.setMessage("대기 중");
        jobStore.put(job.getId(), job);
        return job;
    }

    public void updateJobStatus(String id, String status, String message) {
        Job job = getJob(id);
        if (job != null) {
            job.setStatus(status);
            if (message != null) job.setMessage(message);
        }
    }

    public void deleteJob(String id) {
        jobStore.remove(id);
    }
}
