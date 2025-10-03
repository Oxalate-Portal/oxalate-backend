package io.oxalate.backend.service;

import io.oxalate.backend.api.request.BlockedDateRequest;
import io.oxalate.backend.api.response.BlockedDateResponse;
import io.oxalate.backend.model.BlockedDate;
import io.oxalate.backend.repository.BlockedDateRepository;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
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
    private final UserService userService;

    public List<BlockedDateResponse> getAllBlockedDates() {
        var blockedDates = blockedDateRepository.findAllByBlockedDateAfterOrderByBlockedDateAsc(Date.valueOf(LocalDate.now()));
        var blockedDateResponses = new ArrayList<BlockedDateResponse>();

        for (var blockedDate : blockedDates) {
            var blockedDateResponse = populateResponse(blockedDate);
            blockedDateResponses.add(blockedDateResponse);
        }

        return blockedDateResponses;
    }

    public BlockedDateResponse createBlock(BlockedDateRequest blockedDateRequest, long userId) {
        var blocked = blockedDateRepository.save(BlockedDate.builder()
                                                            .blockedDate(blockedDateRequest.getBlockedDate())
                                                            .createdAt(Instant.now())
                                                            .creator(userId)
                                                            .reason(blockedDateRequest.getReason())
                                                            .build());

        return populateResponse(blocked);
    }

    public void removeBlock(long blockId) {
        blockedDateRepository.deleteById(blockId);
    }

    public BlockedDateResponse findById(long blockedDateId) {
        var blockedDate = blockedDateRepository.findById(blockedDateId);

        return blockedDate.map(BlockedDate::toResponse).orElse(null);
    }

    private BlockedDateResponse populateResponse(BlockedDate blockedDate) {
        var user = userService.findUserEntityById(blockedDate.getCreator());
        var username = "Unknown";

        if (user != null) {
            username = user.getFirstName() + " " + user.getLastName();
        }

        var blockedDateResponse = blockedDate.toResponse();
        blockedDateResponse.setCreatorName(username);
        return blockedDateResponse;
    }
}
