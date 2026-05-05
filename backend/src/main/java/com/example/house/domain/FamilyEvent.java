

package com.example.house.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "family_event")                                                                                           @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "is_yearly", nullable = false)
    private boolean isYearly;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public FamilyEvent(Long familyId, String type, String title,
                       LocalDate eventDate, boolean isYearly,
                       Long creatorId, String memo) {
        this.familyId = familyId;
        this.type = type;
        this.title = title;
        this.eventDate = eventDate;
        this.isYearly = isYearly;
        this.creatorId = creatorId;
        this.memo = memo;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long memberId) {
        return this.creatorId.equals(memberId);
    }
}
