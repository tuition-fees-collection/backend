package com.tuitionnetwork.tuitionnetwork.identity.domain;

import java.util.UUID;

public record AuthenticatedActor(

       UUID accountId,
       UUID actorId,
       ActorType actorType,
       RoleCode role,
        UUID institutionId,
        String branch
) {

}
