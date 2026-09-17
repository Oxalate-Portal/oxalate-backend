package io.oxalate.backend.service;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.api.DiveGroupTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.FRONTEND;
import static io.oxalate.backend.api.PortalConfigEnum.FrontendConfigEnum.DIVE_GROUP_DESCRIPTION_MAX_LENGTH;
import io.oxalate.backend.api.UpdateStatusEnum;
import io.oxalate.backend.api.request.DiveGroupDetailsRequest;
import io.oxalate.backend.api.request.DiveGroupOrderRequest;
import io.oxalate.backend.api.request.DiveGroupRequest;
import io.oxalate.backend.api.request.DiveGroupUpdateRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.DiveGroupMemberResponse;
import io.oxalate.backend.api.response.DiveGroupResponse;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ADD_MEMBER_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ALREADY_IN_GROUP;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_ALREADY_OWNER;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_DETAILS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_EVENT_ENDED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_EVENT_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_EVENT_STARTED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_INVALID_DESCRIPTION;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_INVALID_NAME;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_INVALID_ORDER;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_NOT_FOUND;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_NOT_MEMBER;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_NOT_PARTICIPANT;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_OWNER_ASSIGNMENT_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.DIVE_GROUPS_REORDER_UNAUTHORIZED;
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
import io.oxalate.backend.service.filetransfer.DiveFileTransferService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private final DiveGroupRepository diveGroupRepository;
    private final EventRepository eventRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final NotificationLocalizationService notificationLocalizationService;
    private final DiveFileTransferService diveFileTransferService;
    private final PortalConfigurationService portalConfigurationService;

    @Transactional(readOnly = true)
    public List<DiveGroupResponse> getDiveGroupsByEventId(long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new OxalateNotFoundException(DIVE_GROUPS_EVENT_NOT_FOUND + eventId);
        }

        return diveGroupRepository.findAllByEventIdOrderByGroupOrderAscCreatedAtAsc(eventId)
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
        var description = sanitizeDescription(diveGroupRequest.getDescription());
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
                                                          .description(description)
                                                          .ownerId(ownerId)
                                                          .groupType(diveGroupRequest.getGroupType() != null
                                                                  ? diveGroupRequest.getGroupType()
                                                                  : DiveGroupTypeEnum.NORMAL)
                                                          .groupOrder(nextGroupOrder(eventId))
                                                          .createdAt(Instant.now())
                                                          .build());

        eventParticipantsRepository.assignDiveGroup(eventId, ownerId, diveGroup.getId(), Instant.now());

        if (ownerId != currentUserId) {
            notify(ownerId, currentUserId, "notification.dive-group.assigned-owner", diveGroup.getName());
        }

        addInitialMembers(diveGroup, diveGroupRequest.getMemberIds(), ownerId, currentUserId);

        log.debug("Created dive group ID {} for event ID {} with owner ID {}", diveGroup.getId(), eventId, ownerId);
        return toResponse(diveGroup);
    }

    /**
     * Adds the requested initial members to a newly created dive group. The group owner is skipped because the owner
     * is already a member, and duplicate IDs are only processed once. Every member must be a participant of the dive
     * event and may not already belong to another dive group of the event.
     */
    private void addInitialMembers(DiveGroup diveGroup, List<Long> memberIds, long ownerId, long currentUserId) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        var uniqueMemberIds = new LinkedHashSet<Long>();

        for (var memberId : memberIds) {
            if (memberId != null && memberId != ownerId) {
                uniqueMemberIds.add(memberId);
            }
        }

        for (var memberId : uniqueMemberIds) {
            var memberParticipant = getParticipant(diveGroup.getEventId(), memberId);

            if (memberParticipant.getDiveGroupId() != null) {
                throw new OxalateValidationException(DIVE_GROUPS_ALREADY_IN_GROUP + memberId);
            }

            eventParticipantsRepository.assignDiveGroup(diveGroup.getEventId(), memberId, diveGroup.getId(), Instant.now());

            if (memberId != currentUserId) {
                notify(memberId, currentUserId, "notification.dive-group.added", diveGroup.getName());
            }

            log.debug("User ID {} added to dive group ID {} at creation by user ID {}", memberId, diveGroup.getId(), currentUserId);
        }
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
        diveGroup.setDescription(sanitizeDescription(diveGroupUpdateRequest.getDescription()));

        if (diveGroupUpdateRequest.getGroupType() != null) {
            diveGroup.setGroupType(diveGroupUpdateRequest.getGroupType());
        }

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
            notify(newOwnerId, currentUserId, "notification.dive-group.new-owner", diveGroup.getName());

            if (previousOwnerId != currentUserId) {
                notify(previousOwnerId, currentUserId, "notification.dive-group.previous-owner", diveGroup.getName());
            }
        }

        diveGroup.setUpdatedAt(Instant.now());
        var updatedDiveGroup = diveGroupRepository.save(diveGroup);

        log.debug("Updated dive group ID {}", diveGroupId);
        return toResponse(updatedDiveGroup);
    }

    /**
     * Updates the name and description of a dive group on behalf of one of its members. Unlike
     * {@link #updateDiveGroup}, which is reserved for the owner and the event organizer, every member of the group may
     * change these two fields. Ownership and group type are never touched here.
     *
     * @param diveGroupId             ID of the dive group to update
     * @param diveGroupDetailsRequest the new name and description
     * @param currentUserId           ID of the calling user
     * @param isAdmin                 whether the calling user is an administrator
     * @param isOrganizer             whether the calling user has the organizer role
     * @return the updated dive group
     */
    @Transactional
    public DiveGroupResponse updateDiveGroupDetails(long diveGroupId, DiveGroupDetailsRequest diveGroupDetailsRequest, long currentUserId,
            boolean isAdmin, boolean isOrganizer) {
        var diveGroup = getDiveGroup(diveGroupId);
        var event = getEvent(diveGroup.getEventId());
        var privileged = mayManageEventGroups(event, currentUserId, isAdmin, isOrganizer);

        if (!privileged && diveGroup.getOwnerId() != currentUserId && !isMemberOfDiveGroup(diveGroup, currentUserId)) {
            throw new OxalateUnauthorizedException(AuditLevelEnum.WARN, DIVE_GROUPS_DETAILS_UNAUTHORIZED + diveGroupId, HttpStatus.UNAUTHORIZED);
        }

        assertEventIsModifiable(event, privileged);

        if (diveGroupDetailsRequest == null) {
            throw new OxalateValidationException(DIVE_GROUPS_INVALID_NAME);
        }

        diveGroup.setName(sanitizeName(diveGroupDetailsRequest.getName()));
        diveGroup.setDescription(sanitizeDescription(diveGroupDetailsRequest.getDescription()));
        diveGroup.setUpdatedAt(Instant.now());
        var updatedDiveGroup = diveGroupRepository.save(diveGroup);

        log.debug("Updated details of dive group ID {} by user ID {}", diveGroupId, currentUserId);
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
        resequenceGroupOrder(diveGroup.getEventId(), diveGroupId);

        for (var member : members) {
            if (member.getUserId() != currentUserId) {
                notify(member.getUserId(), currentUserId, "notification.dive-group.removed", diveGroup.getName());
            }
        }

        log.debug("Deleted dive group ID {}", diveGroupId);
        return ActionResponse.builder()
                             .status(UpdateStatusEnum.OK)
                             .message("Dive group removed")
                             .build();
    }

    /**
     * Sets the order in which the dive groups of a dive event are presented. Only the organizer of the dive event, or
     * an administrator, may change the order. The request must list every dive group of the dive event exactly once,
     * which also prevents a caller from reordering, or probing for, dive groups of another dive event.
     *
     * @param eventId               ID of the dive event whose dive group order is set
     * @param diveGroupOrderRequest the dive group IDs in the wanted order
     * @param currentUserId         ID of the calling user
     * @param isAdmin               whether the calling user is an administrator
     * @param isOrganizer           whether the calling user has the organizer role
     * @return the dive groups of the dive event in the new order
     */
    @Transactional
    public List<DiveGroupResponse> reorderDiveGroups(long eventId, DiveGroupOrderRequest diveGroupOrderRequest, long currentUserId, boolean isAdmin,
            boolean isOrganizer) {
        var event = getEvent(eventId);

        if (!mayManageEventGroups(event, currentUserId, isAdmin, isOrganizer)) {
            throw new OxalateUnauthorizedException(AuditLevelEnum.WARN, DIVE_GROUPS_REORDER_UNAUTHORIZED + eventId, HttpStatus.UNAUTHORIZED);
        }

        assertEventIsModifiable(event, true);

        if (diveGroupOrderRequest == null || diveGroupOrderRequest.getDiveGroupIds() == null || diveGroupOrderRequest.getDiveGroupIds()
                                                                                                                     .isEmpty()) {
            throw new OxalateValidationException(AuditLevelEnum.WARN, DIVE_GROUPS_INVALID_ORDER + eventId, HttpStatus.BAD_REQUEST);
        }

        var requestedIds = diveGroupOrderRequest.getDiveGroupIds();

        if (requestedIds.contains(null) || new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new OxalateValidationException(AuditLevelEnum.WARN, DIVE_GROUPS_INVALID_ORDER + eventId, HttpStatus.BAD_REQUEST);
        }

        var diveGroups = diveGroupRepository.findAllByEventIdOrderByGroupOrderAscCreatedAtAsc(eventId);
        var diveGroupsById = new HashMap<Long, DiveGroup>();

        for (var diveGroup : diveGroups) {
            diveGroupsById.put(diveGroup.getId(), diveGroup);
        }

        if (requestedIds.size() != diveGroups.size() || !diveGroupsById.keySet()
                                                                       .containsAll(requestedIds)) {
            throw new OxalateValidationException(AuditLevelEnum.WARN, DIVE_GROUPS_INVALID_ORDER + eventId, HttpStatus.BAD_REQUEST);
        }

        var now = Instant.now();
        var orderedGroups = new ArrayList<DiveGroup>();
        var position = 1;

        for (var diveGroupId : requestedIds) {
            var diveGroup = diveGroupsById.get(diveGroupId);

            if (diveGroup.getGroupOrder() != position) {
                diveGroup.setGroupOrder(position);
                diveGroup.setUpdatedAt(now);
            }

            orderedGroups.add(diveGroup);
            position++;
        }

        diveGroupRepository.saveAll(orderedGroups);

        log.debug("Dive group order of event ID {} set by user ID {}", eventId, currentUserId);
        return orderedGroups.stream()
                            .map(this::toResponse)
                            .toList();
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

        notify(diveGroup.getOwnerId(), currentUserId, "notification.dive-group.member-joined", resolveName(currentUserId), diveGroup.getName());

        log.debug("User ID {} joined dive group ID {}", currentUserId, diveGroupId);
        return toResponse(diveGroup);
    }

    @Transactional
    public ActionResponse leaveDiveGroup(long diveGroupId, long currentUserId) {
        var diveGroup = getDiveGroup(diveGroupId);
        var event = getEvent(diveGroup.getEventId());

        assertEventIsModifiable(event, false);

        removeMember(diveGroup, currentUserId, currentUserId, "notification.dive-group.member-left",
                false, resolveName(currentUserId));

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

        notify(userId, currentUserId, "notification.dive-group.added", diveGroup.getName());

        if (diveGroup.getOwnerId() != userId && diveGroup.getOwnerId() != currentUserId) {
            notify(diveGroup.getOwnerId(), currentUserId, "notification.dive-group.member-added",
                    resolveName(userId), diveGroup.getName());
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

        removeMember(diveGroup, userId, currentUserId, "notification.dive-group.member-removed",
                true, resolveName(userId));

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
    private void removeMember(DiveGroup diveGroup, long userId, long actorUserId, String ownerNotificationKey,
            boolean notifyRemovedUser, String memberName) {
        var participant = getParticipant(diveGroup.getEventId(), userId);

        if (participant.getDiveGroupId() == null || participant.getDiveGroupId() != diveGroup.getId()) {
            throw new OxalateValidationException(DIVE_GROUPS_NOT_MEMBER + userId);
        }

        eventParticipantsRepository.clearDiveGroupForUser(diveGroup.getEventId(), userId);

        var remainingMembers = eventParticipantsRepository.findAllByDiveGroupId(diveGroup.getId())
                                                          .stream()
                                                          .filter(member -> member.getUserId() != userId)
                                                          .sorted(Comparator.comparing(EventsParticipant::getCreatedAt,
                                                                  Comparator.nullsLast(Comparator.naturalOrder())))
                                                          .toList();

        if (diveGroup.getOwnerId() == userId) {
            if (remainingMembers.isEmpty()) {
                diveGroupRepository.delete(diveGroup);
                resequenceGroupOrder(diveGroup.getEventId(), diveGroup.getId());
                log.debug("Dive group ID {} removed because the owner left and no members remain", diveGroup.getId());

                if (notifyRemovedUser && userId != actorUserId) {
                    notify(userId, actorUserId, "notification.dive-group.removed", diveGroup.getName());
                }

                return;
            }

            var newOwnerId = remainingMembers.getFirst()
                                             .getUserId();
            diveGroup.setOwnerId(newOwnerId);
            diveGroup.setUpdatedAt(Instant.now());
            diveGroupRepository.save(diveGroup);
            notify(newOwnerId, actorUserId, "notification.dive-group.new-owner", diveGroup.getName());
        } else if (diveGroup.getOwnerId() != actorUserId) {
            notify(diveGroup.getOwnerId(), actorUserId, ownerNotificationKey, memberName, diveGroup.getName());
        }

        if (notifyRemovedUser && userId != actorUserId) {
            notify(userId, actorUserId, "notification.dive-group.removed", diveGroup.getName());
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

    /**
     * Whether the user is currently a member of the dive group, which is recorded on the event participation row.
     */
    private boolean isMemberOfDiveGroup(DiveGroup diveGroup, long userId) {
        var participant = eventParticipantsRepository.findByEventIdAndUserId(diveGroup.getEventId(), userId);
        return participant != null && participant.getDiveGroupId() != null && participant.getDiveGroupId() == diveGroup.getId();
    }

    /**
     * Resolves the order of a newly created dive group, which is the last position of the dive event.
     */
    private int nextGroupOrder(long eventId) {
        return diveGroupRepository.findAllByEventIdOrderByGroupOrderAscCreatedAtAsc(eventId)
                                  .stream()
                                  .mapToInt(DiveGroup::getGroupOrder)
                                  .max()
                                  .orElse(0) + 1;
    }

    /**
     * Renumbers the dive groups of a dive event to a gapless 1..n sequence after a group has been removed. The removed
     * group is filtered out explicitly because it may still be present in the persistence context.
     */
    private void resequenceGroupOrder(long eventId, long removedDiveGroupId) {
        var remainingGroups = diveGroupRepository.findAllByEventIdOrderByGroupOrderAscCreatedAtAsc(eventId)
                                                 .stream()
                                                 .filter(diveGroup -> diveGroup.getId() != removedDiveGroupId)
                                                 .toList();

        var position = 1;
        var changedGroups = new ArrayList<DiveGroup>();

        for (var diveGroup : remainingGroups) {
            if (diveGroup.getGroupOrder() != position) {
                diveGroup.setGroupOrder(position);
                changedGroups.add(diveGroup);
            }

            position++;
        }

        if (!changedGroups.isEmpty()) {
            diveGroupRepository.saveAll(changedGroups);
        }
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

    /**
     * Normalizes a dive group description. A missing or blank description is stored as null. The maximum length is
     * read from the portal configuration on every call so that an administrator can change it at runtime. The
     * description is stored verbatim as plain text; it is never interpreted as HTML by the server, and the client is
     * expected to render it as text.
     */
    private String sanitizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        var trimmedDescription = description.trim();
        var maxLength = portalConfigurationService.getNumericConfiguration(FRONTEND.group, DIVE_GROUP_DESCRIPTION_MAX_LENGTH.key);

        if (trimmedDescription.length() > maxLength) {
            throw new OxalateValidationException(AuditLevelEnum.WARN, DIVE_GROUPS_INVALID_DESCRIPTION, HttpStatus.BAD_REQUEST);
        }

        return trimmedDescription;
    }

    private void notify(long userId, long actorUserId, String messageKey, Object... arguments) {
        var creatorId = actorUserId > 0 ? actorUserId : SYSTEM_USER_ID;
        var user = userRepository.findById(userId)
                                 .orElse(null);
        var title = notificationLocalizationService.getMessage(user, "notification.dive-group.title");
        var message = notificationLocalizationService.getMessage(user, messageKey, arguments);
        messageService.createSimpleNotification(userId, creatorId, title, title, message);
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
                                                     .orElse("Unknown user"), members,
                diveFileTransferService.findDiveFilesByDiveGroupId(diveGroup.getId()));
    }
}
