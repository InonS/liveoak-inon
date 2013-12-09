package io.liveoak.mongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import io.liveoak.spi.InitializationException;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.ConfigResource;
import io.liveoak.spi.resource.async.PropertySink;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;
import io.liveoak.spi.state.ResourceState;

/**
 * @author Bob McWhirter
 */
public class MongoConfigResource implements ConfigResource {

    public MongoConfigResource(RootMongoResource parent) {
        this.parent = parent;
    }

    @Override
    public Resource parent() {
        return this.parent;
    }

    @Override
    public void updateProperties(RequestContext ctx, ResourceState state, Responder responder) throws Exception {
        String host = (String) state.getProperty("host");
        if (host == null) {
            host = "localhost";
        }

        Integer port = (Integer) state.getProperty("port");
        if (port == null) {
            port = 27017;
        }

        String dbName = (String) state.getProperty("db");

        if (dbName == null) {
            throw new InitializationException("Configuration value required for 'db'");
        }

        MongoClient mongo = new MongoClient(host, port);
        DB db = mongo.getDB(dbName);
        if (db == null) {
            throw new InitializationException("Unknown database " + dbName);
        }

        this.parent.configure( mongo, db );
        responder.resourceUpdated( this );
    }

    @Override
    public void readProperties(RequestContext ctx, PropertySink sink) throws Exception {
        sink.accept( "host", this.parent.mongoClient().getAddress().getHost() );
        sink.accept( "port", this.parent.mongoClient().getAddress().getPort() );
        sink.accept( "db", this.parent.getDB().getName() );
        sink.close();
    }

    private RootMongoResource parent;
}
