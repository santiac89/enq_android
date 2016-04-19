package org.allin.enq.api;


import org.allin.enq.model.Group;
import org.allin.enq.service.ClientInfo;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;


/**
 * Created by Santi on 22/07/2015.
 */
public interface ApiClient {

    @GET("/m/groups")
    Call<List<Group>> getGroups();

    @POST("/m/groups/{groupId}/clients")
    Call<ClientInfo> enqueueIn(@Path("groupId") String groupId, @Body Map<String, String> body);

    @DELETE("/m/clients/{clientId}")
    Call<String> cancel(@Path("clientId") String clientId);
}
