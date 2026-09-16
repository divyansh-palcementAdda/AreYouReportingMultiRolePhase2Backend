package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.payload.request.TaskProofUploadRequest;
import com.app.AreYouReporting.payload.response.TaskProofDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TaskProofService {

    TaskProofDto uploadProof(UUID taskId, MultipartFile file, TaskProofUploadRequest request);

    Resource downloadProofFile(UUID proofId);

    List<TaskProofDto> getProofsByTaskId(UUID taskId);

    void deleteProof(UUID proofId);
}
