package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;

public interface CurrentActorProvider {

    AuthenticatedActor currentActor();

}
