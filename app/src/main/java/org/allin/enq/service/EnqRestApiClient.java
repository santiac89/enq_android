package org.allin.enq.service;

import org.allin.enq.model.Group;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.POST;

/**
 * Created by Santi on 22/07/2015.
 */
public interface EnqRestApiClient {

    @GET("/groups")
    List<Group> getGroups();

    @POST("/groups/clients")
    void enqueueIn(Group group);

}
