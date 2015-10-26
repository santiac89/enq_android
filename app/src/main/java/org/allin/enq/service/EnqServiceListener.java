package org.allin.enq.service;

import org.allin.enq.model.Group;

import java.util.List;

import retrofit.RetrofitError;


/**
 * EnqService events listener
 * Created by Santi on 21/07/2015.
 */
public interface EnqServiceListener {

    /**
     * Called when a server is found in the local network
     */
    void OnServerFound();

    /**
     * Called when no server is found or an error ocurred
     * @param e Exception about what happened
     */
    void OnServerNotFound(Exception e);

    /**
     * Called when the groups have been retrieved from the server
     * @param groups A List\<Group\> of groups
     */
    void OnGroupsFound(List<Group> groups);

    /**
     * Called when no groups could be retrieved from the server
     * @param e RetrofitError about why the groups couldn't be retrieved
     */
    void OnGroupsNotFound();

    /**
     * Called when the client was successfully enqueued in a group
     */
    void OnClientEnqueued();

    void OnServiceException(Exception e);

    /**
     * Called when the client couldn't be enqueued in a group
     * @param e Exception about why it couldn't
     */
    void OnClientNotEnqueued(RetrofitError e);

}
