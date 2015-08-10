package org.allin.enq.service;

import org.allin.enq.model.Group;

import java.util.List;
import java.util.Map;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by Santi on 22/07/2015.
 */
public interface EnqRestApiClient {

    @GET("/groups")
    List<Group> getGroups();

    @POST("/groups/{groupId}/clients")
    Map<String,String> enqueueIn(@Path("groupId") String groupId, @Body Map<String, String> body);

}
