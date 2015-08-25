package org.allin.enq.service;

import org.allin.enq.model.Group;

import java.util.List;
import java.util.Map;

/**
 * Created by Santi on 21/07/2015.
 */
public interface EnqServiceListener {

    void OnServerFound(EnqRestApiInfo enqRestApiInfo);

    void OnServerNotFound(Exception e);

    void OnGroupsFound(List<Group> groups);

    void OnGroupsNotFound(Exception e);

    void OnClientEnqueued(Map<String, String> result);

    void OnServiceException(Exception e);
}
