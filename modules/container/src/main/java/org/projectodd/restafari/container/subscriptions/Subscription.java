package org.projectodd.restafari.container.subscriptions;

import org.projectodd.restafari.container.ResourcePath;
import org.projectodd.restafari.spi.Resource;

/**
 * @author Bob McWhirter
 */
public interface Subscription {

    ResourcePath resourcePath();

    void resourceCreated(Resource resource);
    void resourceUpdated(Resource resource);
    void resourceDeleted(Resource resource);

}