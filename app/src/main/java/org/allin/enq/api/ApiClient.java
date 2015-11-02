package org.allin.enq.api;


import org.allin.enq.model.Group;
import org.allin.enq.service.ClientInfo;

import java.util.List;
import java.util.Map;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;


/**
 * Created by Santi on 22/07/2015.
 */
public interface ApiClient {

    @GET("/m/groups")
    List<Group> getGroups();

    @POST("/m/groups/{groupId}/clients")
    ClientInfo enqueueIn(@Path("groupId") String groupId, @Body Map<String, String> body);

    @DELETE("/m/clients/{clientId}")
    String cancel(@Path("clientId") String clientId);
}
