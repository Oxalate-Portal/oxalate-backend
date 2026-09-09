package io.oxalate.backend.service;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.api.UpdateStatusEnum;
import io.oxalate.backend.api.request.DiveGroupRequest;
import io.oxalate.backend.api.request.DiveGroupUpdateRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.DiveGroupMemberResponse;
import io.oxalate.backend.api.response.DiveGroupResponse;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ADD_MEMBER_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ALREADY_IN_GROUP;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ALREADY_OWNER;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_EVENT_ENDED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_EVENT_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_EVENT_STARTED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_INVALID_NAME;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_NOT_MEMBER;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_NOT_PARTICIPANT;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_OWNER_ASSIGNMENT_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_UNAUTHORIZED;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.model.DiveGroup;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.EventsParticipant;
import io.oxalate.backend.repository.DiveGroupRepository;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the dive groups of a dive event.
 * <p>
 * A dive group always belongs to a single dive event. The membership of a group is stored in the
 * {@code event_participants} table, which means that only participants of the dive event can be members of a group.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DiveGroupService {

    private static final long SYSTEM_USER_ID = 1L;
    private static final String NOTIFICATION_TITLE = "Dive group update";

    private final DiveGroupRepository diveGroupRepository;
    private final EventRepository eventRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public List<DiveGroupResponse> getDiveGroupsByEventId(long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new OxalateNotFoundException(DIVE_GROUPS_EVENT_NOT_FOUND + eventId);
        }

        return diveGroupRepository.findAllByEventIdOrderByCreatedAtAsc(eventId)
                                  .stream()
                                  .map(this::toResponse)
                                  .toList();
    }

    @Transactional(readOnly = true)
    public DiveGroupResponse getDiveGroupById(long diveGroupId) {
        return toResponse(getDiveGroup(diveGroupId));
    }

    @Transactional
    public DiveGroupResponse createDiveGroup(DiveGroupRequest diveGroupRequest, long currentUserId, boolean isAdmin, boolean isOrganizer) {
        if (diveGroupRequest == null || diveGroupRequest.getEventId() == null) {
            throw new OxalateValidationException(DIVE_GROUPS_EVENT_NOT_FOUND + "null");
        }

        var name = sanitizeName(diveGroupRequest.getName());
        var eventId = diveGroupRequest.getEventId();
        var event = getEvent(eventId);
        var privileged = mayManageEventGroups(event, currentUserId, isAdmin, isOrganizer);

        assertEventIsModifiable(event, privileged);

        var ownerId = currentUserId;

        if (diveGroupRequest.getOwnerId() != null && diveGroupRequest.getOwnerId() != currentUserId) {
            if (!privileged) {
                throw new OxalateUnauthorizedException(DIVE_GROUPS_OWNER_ASSIGNMENT_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
            }

            ownerId = diveGroupRequest.getOwnerId();
        }

        var ownerParticipant = getParticipant(eventId, ownerId);

        if (diveGroupRepository.findByEventIdAndOwnerId(eventId, ownerId)
                               .isPresent()) {
            throw new OxalateValidationException(DIVE_GROUPS_ALREADY_OWNER + ownerId);
        }

        if (ownerParticipant.getDiveGroupId() != null) {
            throw new OxalateValidationException(DIVE_GROUPS_ALREADY_IN_GROUP + ownerId);
        }

        var diveGroup = diveGroupRepository.save(DiveGroup.builder()
                                                          .eventId(eventId)
                                                          .name(name)
                                                          .ownerId(ownerId)
                                                          .createdAt(Instant.now())
                                                          .build());

        eventParticipantsRepository.assignDiveGroup(eventId, ownerId, diveGroup.getId(), Instant.now());

        if (ownerId != currentUserId) {
            notify(ownerId, currentUserId, "You have been assigned as the owner of the dive group '" + diveGroup.getName() + "'");
        }

        log.debug("Created dive group ID {} for event ID {} with owner ID {}", diveGroup.getId(), eventId, ownerId);
        return toResponse(diveGroup);
    }

    @Transactional
    public DiveGroupResponse updateDiveGroup(long diveGroupId, DiveGroupUpdateRequest diveGroupUpdateRequest, long currentUserId, boolean isAdmin,
            boolean isOrganizer) {
        var diveGroup = getDiveGroup(diveGroupId);
        var event = getEvent(diveGroup.getEventId());
        var privileged = mayManageEventGroups(event, currentUserId, isAdmin, isOrganizer);

        assertEventIsModifiable(event, privileged);

        if (!privileged && diveGroup.getOwnerId() != currentUserId) {
            throw new OxalateUnauthorizedException(DIVE_GROUPS_UNAUTHORIZED + diveGroupId, HttpStatus.UNAUTHORIZED);
        }

        if (diveGroupUpdateRequest == null) {
            throw new OxalateValidationException(DIVE_GROUPS_INVALID_NAME);
        }

        diveGroup.setName(sanitizeName(diveGroupUpdateRequest.getName()));

        var newOwnerId = diveGroupUpdateRequest.getOwnerId();

        if (newOwnerId != null && newOwnerId != diveGroup.getOwnerId()) {
            if (!privileged) {
                throw new OxalateUnauthorizedException(DIVE_GROUPS_OWNER_ASSIGNMENT_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
            }

            var newOwnerParticipant = getParticipant(diveGroup.getEventId(), newOwnerId);

            if (newOwnerParticipant.getDiveGroupId() == null || newOwnerParticipant.getDiveGroupId() != diveGroupId) {
                if (newOwnerParticipant.getDiveGroupId() != null) {
                    throw new OxalateValidationException(DIVE_GROUPS_ALREADY_IN_GROUP + newOwnerId);
                }

                eventParticipantsRepository.assignDiveGroup(diveGroup.getEventId(), newOwnerId, diveGroupId, Instant.now());
            }

            var previousOwnerId = diveGroup.getOwnerId();
            diveGroup.setOwnerId(newOwnerId);
            notify(newOwnerId, currentUserId, "You are now the owner of the dive group '" + diveGroup.getName() + "'");

            if (previousOwnerId != currentUserId) {
                notify(previousOwnerId, currentUserId, "You are no longer the owner of the dive group '" + diveGroup.getName() + "'");
            }
        }

        diveGroup.setUpdatedAt(Instant.now());
        var updatedDiveGroup = diveGroupRepository.save(diveGroup);

        log.debug("Updated dive group ID {}", diveGroupId);
        return toResponse(updatedDiveGroup);
    }

    @Transactional
    public ActionResponse deleteDiveGroup(long diveGroupId, long currentUserId, boolean isAdmin, boolean isOrganizer) {
        var diveGroup = getDiveGroup(diveGroupId);
        var event = getEvent(diveGroup.getEventId());
        var privileged = mayManageEventGroups(event, currentUserId, isAdmin, isOrganizer);

        assertEventIsModifiable(event, privileged);

        if (!privileged && diveGroup.getOwnerId() != currentUserId) {
            throw new OxalateUnauthorizedException(DIVE_GROUPS_UNAUTHORIZED + diveGroupId, HttpStatus.UNAUTHORIZED);
        }

        var members = eventParticipantsRepository.findAllByDiveGroupId(diveGroupId);
        eventParticipantsRepository.clearDiveGroupMembers(diveGroupId);
        diveGroupRepository.delete(diveGroup);

        for (var member : members) {
            if (member.getUserId() != currentUserId) {
                notify(member.getUserId(), currentUserId, "The dive group '" + diveGroup.getName() + "' has been removed");
            }
        }

        log.debug("Deleted dive group ID {}", diveGroupId);
        return ActionResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .message("Dive group removed")
                             .build();
    }

    @Transactional
    public DiveGroupResponse joinDiveGroup(long diveGroupId, long currentUserId) {
        var diveGroup = getDiveGroup(diveGroupId);
        var event = getEvent(diveGroup.getEventId());

        assertEventIsModifiable(event, false);

        var participant = getParticipant(diveGroup.getEventId(), currentUserId);

        if (participant.getDiveGroupId() != null) {
            throw new OxalateValidationException(DIVE_GROUPS_ALREADY_IN_GROUP + currentUserId);
        }

        eventParticipantsRepository.assignDiveGroup(diveGroup.getEventId(), currentUserId, diveGroupId, Instant.now());

        notify(diveGroup.getOwnerId(), currentUserId, resolveName(currentUserId) + " has joined your dive group '" + diveGroup.getName() + "'");

        log.debug("User ID {} joined dive group ID {}", currentUserId, diveGroupId);
        return toResponse(diveGroup);
    }

    @Transactional
    public ActionResponse leaveDiveGroup(long diveGroupId, long currentUserId) {
        var diveGroup = getDiveGroup(diveGroupId);
        var event = getEvent(diveGroup.getEventId());

        assertEventIsModifiable(event, false);

        removeMember(diveGroup, currentUserId, currentUserId,
                resolveName(currentUserId) + " has left your dive group '" + diveGroup.getName() + "'", false);

        log.debug("User ID {} left dive group ID {}", currentUserId, diveGroupId);
        return ActionResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .message("Left the dive group")
                             .build();
    }

    @Transactional
    public DiveGroupResponse addMemberToDiveGroup(long diveGroupId, long userId, long currentUserId, boolean isAdmin, boolean isOrganizer) {
        var diveGroup = getDiveGroup(diveGroupId);
        var event = getEvent(diveGroup.getEventId());
        var privileged = mayManageEventGroups(event, currentUserId, isAdmin, isOrganizer);

        if (!privileged) {
            throw new OxalateUnauthorizedException(DIVE_GROUPS_ADD_MEMBER_UNAUTHORIZED + diveGroupId, HttpStatus.UNAUTHORIZED);
        }

        assertEventIsModifiable(event, true);

        var participant = getParticipant(diveGroup.getEventId(), userId);

        if (participant.getDiveGroupId() != null) {
            throw new OxalateValidationException(DIVE_GROUPS_ALREADY_IN_GROUP + userId);
        }

        eventParticipantsRepository.assignDiveGroup(diveGroup.getEventId(), userId, diveGroupId, Instant.now());

        notify(userId, currentUserId, "You have been added to the dive group '" + diveGroup.getName() + "'");

        if (diveGroup.getOwnerId() != userId && diveGroup.getOwnerId() != currentUserId) {
            notify(diveGroup.getOwnerId(), currentUserId, resolveName(userId) + " has been added to your dive group '" + diveGroup.getName() + "'");
        }

        log.debug("User ID {} added to dive group ID {} by user ID {}", userId, diveGroupId, currentUserId);
        return toResponse(diveGroup);
    }

    @Transactional
    public ActionResponse removeMemberFromDiveGroup(long diveGroupId, long userId, long currentUserId, boolean isAdmin, boolean isOrganizer) {
        var diveGroup = getDiveGroup(diveGroupId);
        var event = getEvent(diveGroup.getEventId());
        var privileged = mayManageEventGroups(event, currentUserId, isAdmin, isOrganizer);

        if (!privileged && diveGroup.getOwnerId() != currentUserId) {
            throw new OxalateUnauthorizedException(DIVE_GROUPS_UNAUTHORIZED + diveGroupId, HttpStatus.UNAUTHORIZED);
        }

        assertEventIsModifiable(event, privileged);

        removeMember(diveGroup, userId, currentUserId,
                resolveName(userId) + " has been removed from your dive group '" + diveGroup.getName() + "'", true);

        log.debug("User ID {} removed from dive group ID {} by user ID {}", userId, diveGroupId, currentUserId);
        return ActionResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .message("Member removed from the dive group")
                             .build();
    }

    /**
     * Removes a member from a dive group. When the removed member is the owner of the group, the ownership is
     * transferred to the next user that joined the group. If no members remain, the group itself is removed.
     */
    private void removeMember(DiveGroup diveGroup, long userId, long actorUserId, String ownerNotification, boolean notifyRemovedUser) {
        var participant = getParticipant(diveGroup.getEventId(), userId);

        if (participant.getDiveGroupId() == null || participant.getDiveGroupId() != diveGroup.getId()) {
            throw new OxalateValidationException(DIVE_GROUPS_NOT_MEMBER + userId);
        }

        eventParticipantsRepository.clearDiveGroupForUser(diveGroup.getEventId(), userId);

        var remainingMembers = eventParticipantsRepository.findAllByDiveGroupId(diveGroup.getId())
                                                          .stream()
                                                          .filter(member -> member.getUserId() != userId)
                                                          .sorted(Comparator.comparing(EventsParticipant::getDiveGroupJoinedAt,
                                                                  Comparator.nullsLast(Comparator.naturalOrder())))
                                                          .toList();

        if (diveGroup.getOwnerId() == userId) {
            if (remainingMembers.isEmpty()) {
                diveGroupRepository.delete(diveGroup);
                log.debug("Dive group ID {} removed because the owner left and no members remain", diveGroup.getId());

                if (notifyRemovedUser && userId != actorUserId) {
                    notify(userId, actorUserId, "You have been removed from the dive group '" + diveGroup.getName() + "'");
                }

                return;
            }

            var newOwnerId = remainingMembers.getFirst()
                                             .getUserId();
            diveGroup.setOwnerId(newOwnerId);
            diveGroup.setUpdatedAt(Instant.now());
            diveGroupRepository.save(diveGroup);
            notify(newOwnerId, actorUserId, "You are now the owner of the dive group '" + diveGroup.getName() + "'");
        } else if (diveGroup.getOwnerId() != actorUserId) {
            notify(diveGroup.getOwnerId(), actorUserId, ownerNotification);
        }

        if (notifyRemovedUser && userId != actorUserId) {
            notify(userId, actorUserId, "You have been removed from the dive group '" + diveGroup.getName() + "'");
        }
    }

    private DiveGroup getDiveGroup(long diveGroupId) {
        return diveGroupRepository.findById(diveGroupId)
                                  .orElseThrow(() -> new OxalateNotFoundException(DIVE_GROUPS_NOT_FOUND + diveGroupId));
    }

    private Event getEvent(long eventId) {
        return eventRepository.findById(eventId)
                              .orElseThrow(() -> new OxalateNotFoundException(DIVE_GROUPS_EVENT_NOT_FOUND + eventId));
    }

    private EventsParticipant getParticipant(long eventId, long userId) {
        var participant = eventParticipantsRepository.findByEventIdAndUserId(eventId, userId);

        if (participant == null) {
            throw new OxalateValidationException(DIVE_GROUPS_NOT_PARTICIPANT + userId);
        }

        return participant;
    }

    /**
     * Verifies that the dive event may still be modified. Nobody may modify the dive groups after the dive event has
     * ended. Non-privileged users may not modify the groups after the dive event has started.
     */
    private void assertEventIsModifiable(Event event, boolean privileged) {
        var now = Instant.now();
        var eventEnd = event.getStartTime()
                            .plus(event.getEventDuration(), ChronoUnit.HOURS);

        if (now.isAfter(eventEnd)) {
            throw new OxalateValidationException(AuditLevelEnum.WARN, DIVE_GROUPS_EVENT_ENDED + event.getId(), HttpStatus.BAD_REQUEST);
        }

        if (!privileged && now.isAfter(event.getStartTime())) {
            throw new OxalateValidationException(AuditLevelEnum.WARN, DIVE_GROUPS_EVENT_STARTED + event.getId(), HttpStatus.BAD_REQUEST);
        }
    }

    private boolean mayManageEventGroups(Event event, long userId, boolean isAdmin, boolean isOrganizer) {
        return isAdmin || (isOrganizer && event.getOrganizerId() == userId);
    }

    private String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new OxalateValidationException(DIVE_GROUPS_INVALID_NAME);
        }

        var trimmedName = name.trim();

        if (trimmedName.length() > 255) {
            throw new OxalateValidationException(DIVE_GROUPS_INVALID_NAME);
        }

        return trimmedName;
    }

    private void notify(long userId, long actorUserId, String message) {
        var creatorId = actorUserId > 0 ? actorUserId : SYSTEM_USER_ID;
        messageService.createSimpleNotification(userId, creatorId, NOTIFICATION_TITLE, NOTIFICATION_TITLE, message);
    }

    private String resolveName(long userId) {
        return userRepository.findById(userId)
                             .map(user -> user.getFirstName() + " " + user.getLastName())
                             .orElse("Unknown user");
    }

    private DiveGroupResponse toResponse(DiveGroup diveGroup) {
        var participants = eventParticipantsRepository.findAllByDiveGroupId(diveGroup.getId());
        var userIds = new ArrayList<Long>();

        for (var participant : participants) {
            userIds.add(participant.getUserId());
        }

        if (!userIds.contains(diveGroup.getOwnerId())) {
            userIds.add(diveGroup.getOwnerId());
        }

        Map<Long, String> names = new HashMap<>();

        for (var user : userRepository.findAllById(userIds)) {
            names.put(user.getId(), user.getFirstName() + " " + user.getLastName());
        }

        var members = new ArrayList<DiveGroupMemberResponse>();

        for (var participant : participants) {
            members.add(DiveGroupMemberResponse.builder()
                                               .userId(participant.getUserId())
                                               .name(names.getOrDefault(participant.getUserId(), "Unknown user"))
                                               .userType(participant.getEventUserType())
                                               .owner(participant.getUserId() == diveGroup.getOwnerId())
                                               .joinedAt(participant.getDiveGroupJoinedAt())
                                               .build());
        }

        return diveGroup.toDiveGroupResponse(Optional.ofNullable(names.get(diveGroup.getOwnerId()))
                                                     .orElse("Unknown user"), members);
    }
}
