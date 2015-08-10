package org.allin.enq.service;

import org.allin.enq.model.Group;

import java.util.List;
import java.util.Map;

/**
 * Created by Santi on 21/07/2015.
 */
public interface EnqServiceListener {

    public void OnServiceFound(EnqRestApiInfo enqRestApiInfo);

    public void OnServiceNotFound();

    public void OnGroupsFound(List<Group> groups);

    public void OnClientEnqueued(Map<String, String> result);
}
