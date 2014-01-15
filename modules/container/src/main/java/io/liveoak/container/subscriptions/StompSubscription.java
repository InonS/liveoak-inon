/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.subscriptions;

import io.liveoak.common.codec.ResourceCodec;
import io.liveoak.spi.MediaType;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.ResourcePath;
import io.liveoak.spi.ResourceResponse;
import io.liveoak.spi.container.Subscription;
import io.liveoak.spi.container.SubscriptionManager;
import io.liveoak.spi.resource.async.PropertySink;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.stomp.Headers;
import io.liveoak.stomp.StompMessage;
import io.liveoak.stomp.common.DefaultStompMessage;
import io.liveoak.stomp.server.StompConnection;

/**
 * @author Bob McWhirter
 */
public class StompSubscription implements Subscription {

    public StompSubscription(SubscriptionManager subscriptionManager, StompConnection connection, String destination, String subscriptionId, MediaType mediaType, ResourceCodec codec) {
        this.subscriptionManager = subscriptionManager;
        this.connection = connection;
        this.destination = destination;
        this.subscriptionId = subscriptionId;
        this.mediaType = mediaType;
        this.codec = codec;
        this.resourcePath = new ResourcePath(destination);
    }

    @Override
    public Resource parent() {
        return this.subscriptionManager;
    }

    public String id() {
        return this.connection.getConnectionId() + "-" + subscriptionId;
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------

    @Override
    public void readProperties(RequestContext ctx, PropertySink sink) throws Exception {
        sink.accept("type", "stomp");
        sink.accept("path", this.resourcePath.toString());
        sink.accept("subscription-id", this.subscriptionId);
        sink.accept("media-type", this.mediaType.toString());
        sink.close();
    }


    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------

    public ResourcePath resourcePath() {
        return this.resourcePath;
    }

    @Override
    public void resourceCreated(ResourceResponse resourceResponse) throws Exception {
        this.connection.send(createMessage("create", 200, resourceResponse));
    }

    @Override
    public void resourceUpdated(ResourceResponse resourceResponse) throws Exception {
        this.connection.send(createMessage("update", 200, resourceResponse));
    }

    @Override
    public void resourceDeleted(ResourceResponse resourceResponse) throws Exception {
        this.connection.send(createMessage("delete", 200, resourceResponse));
    }

    protected StompMessage createMessage(String action, int status, ResourceResponse resourceResponse) throws Exception {
        Resource resource = resourceResponse.resource();
        StompMessage message = new DefaultStompMessage();
        message.headers().put(Headers.SUBSCRIPTION, this.subscriptionId);
        message.headers().put(Headers.CONTENT_TYPE, this.mediaType.toString());
        message.headers().put("action", action);
        message.headers().put("status", "" + status);
        message.headers().put("location", resource.uri().toString());
        RequestContext requestContext = new RequestContext.Builder().build();
        

        message.content(this.codec.encode(requestContext, resourceResponse.state()));
        return message;
    }

    private SubscriptionManager subscriptionManager;
    private StompConnection connection;
    private String destination;
    private String subscriptionId;
    private MediaType mediaType;
    private ResourceCodec codec;

    private final ResourcePath resourcePath;

}
