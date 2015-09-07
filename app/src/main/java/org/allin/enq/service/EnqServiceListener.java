package org.allin.enq.service;

import com.google.gson.internal.LinkedTreeMap;

import org.allin.enq.model.Group;

import java.util.List;
import java.util.Map;

import retrofit.RetrofitError;


/**
 * Created by Santi on 21/07/2015.
 */
public interface EnqServiceListener {

    void OnServerFound(EnqRestApiInfo enqRestApiInfo);

    void OnServerNotFound(Exception e);

    void OnGroupsFound(List<Group> groups);

    void OnGroupsNotFound(RetrofitError e);

    void OnClientEnqueued(Map<String, String> result);

    void OnServiceException(Exception e);

    void OnClientNotEnqueued(RetrofitError e);

    void OnServerCall(LinkedTreeMap map);
}
