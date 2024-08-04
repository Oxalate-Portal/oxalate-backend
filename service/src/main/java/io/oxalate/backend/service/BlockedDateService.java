package io.oxalate.backend.service;

import io.oxalate.backend.api.request.BlockedDateRequest;
import io.oxalate.backend.api.response.BlockedDateResponse;
import io.oxalate.backend.model.BlockedDate;
import io.oxalate.backend.repository.BlockedDateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BlockedDateService {
    private final BlockedDateRepository blockedDateRepository;

    public List<BlockedDateResponse> getAllBlockedDates() {
        var blockedDates = blockedDateRepository.findAll();
        var blockedDateResponses = new ArrayList<BlockedDateResponse>();

        for (var blockedDate : blockedDates) {
            blockedDateResponses.add(blockedDate.toResponse());
        }

        return blockedDateResponses;
    }

    public BlockedDateResponse createBlock(BlockedDateRequest blockedDateRequest, long userId) {
        var blocked = blockedDateRepository.save(BlockedDate.builder()
                                                            .blockedDate(blockedDateRequest.getBlockedDate())
                                                            .createdAt(Instant.now())
                                                            .creator(userId)
                                                            .build());

        return blocked.toResponse();
    }

    public void removeBlock(long blockId) {
        blockedDateRepository.deleteById(blockId);
    }

    public BlockedDateResponse findById(long blockedDateId) {
        var blockedDate = blockedDateRepository.findById(blockedDateId);

        return blockedDate.map(BlockedDate::toResponse).orElse(null);
    }
}
