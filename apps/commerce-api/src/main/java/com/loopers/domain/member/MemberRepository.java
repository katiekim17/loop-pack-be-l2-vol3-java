package com.loopers.domain.member;

import java.util.Optional;

public interface MemberRepository {
    MemberModel save(MemberModel memberModel);
    Optional<MemberModel> update(MemberModel memberModel);
    Optional<MemberModel> findByLoginId(String id);
}
