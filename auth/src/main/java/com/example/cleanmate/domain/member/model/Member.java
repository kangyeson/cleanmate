package com.example.cleanmate.domain.member.model;

/**
 * 룸메이트 도메인 모델.
 * 순수 Java — JPA 없음.
 */
public class Member {

    private final Long id;
    private final Long roomId;
    private final String name;
    private final MemberRole role;
    private final String salt;
    private final String passwordHash;
    private final boolean mustChangePw;
    private final int turnOrder;  // 1-indexed, 순환 배정 순서
    private final boolean active;

    public Member(Long id, Long roomId, String name, MemberRole role,
                  String salt, String passwordHash,
                  boolean mustChangePw, int turnOrder, boolean active) {
        this.id = id;
        this.roomId = roomId;
        this.name = name;
        this.role = role;
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.mustChangePw = mustChangePw;
        this.turnOrder = turnOrder;
        this.active = active;
    }

    public Long getId()           { return id; }
    public Long getRoomId()       { return roomId; }
    public String getName()       { return name; }
    public MemberRole getRole()   { return role; }
    public String getSalt()       { return salt; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isMustChangePw() { return mustChangePw; }
    public int getTurnOrder()     { return turnOrder; }
    public boolean isActive()     { return active; }
}
