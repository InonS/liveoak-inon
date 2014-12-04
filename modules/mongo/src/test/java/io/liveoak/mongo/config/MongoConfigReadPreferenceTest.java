package io.liveoak.mongo.config;

import java.util.ArrayList;
import java.util.List;

import io.liveoak.common.codec.DefaultResourceState;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.exceptions.InitializationException;
import io.liveoak.spi.state.ResourceState;
import org.fest.assertions.Fail;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author <a href="mailto:mwringe@redhat.com">Matt Wringe</a>
 * @author Ken Finnigan
 */
public class MongoConfigReadPreferenceTest extends BaseMongoConfigTest {

    @Test
    public void readPreference() throws Exception {
        ResourceState config = new DefaultResourceState();
        config.putProperty("db", "testDefaultDB");
        config.putProperty("servers", new ArrayList());
        setUpSystem(config);

        ResourceState result = client.read(new RequestContext.Builder().build(), ADMIN_PATH);
        assertThat(result.getProperty("db")).isEqualTo("testDefaultDB");
        assertThat(result.getProperty("servers")).isNotNull();

        assertThat(result.getProperty(ReadPreferenceState.ID)).isNotNull();
        ResourceState readPreferenceResourceState = (ResourceState) result.getProperty(ReadPreferenceState.ID);
        // the default values
        assertThat(readPreferenceResourceState.getProperty(ReadPreferenceState.TYPE)).isEqualTo(ReadPreferenceState.PRIMARY);
        assertThat(readPreferenceResourceState.getPropertyNames().size()).isEqualTo(1); //Since its a primary type, there cannot be tags
    }

    @Test
    public void configureType() throws Exception {
        ResourceState config = new DefaultResourceState();
        config.putProperty("db", "testConfigureTypeDB");
        config.putProperty("servers", new ArrayList());

        ResourceState configReadPref = new DefaultResourceState();
        configReadPref.putProperty(ReadPreferenceState.TYPE, "secondary"); // use new string value here, if using ReadPreferenceResource.Types.SECONDARY.toString() then '==' will incorrectly work when we should be using equals()
        config.putProperty(ReadPreferenceState.ID, configReadPref);

        setUpSystem(config);

        ResourceState result = client.read(new RequestContext.Builder().build(), ADMIN_PATH);

        assertThat(result.getProperty("db")).isEqualTo("testConfigureTypeDB");
        assertThat(result.getProperty("servers")).isNotNull();

        assertThat(result.getProperty(ReadPreferenceState.ID)).isNotNull();
        ResourceState readPreferenceResourceState = (ResourceState) result.getProperty(ReadPreferenceState.ID);
        assertThat(readPreferenceResourceState.getPropertyNames().size()).isEqualTo(2); // since not a primary type, will have a list of tags
        assertThat(readPreferenceResourceState.getProperty(ReadPreferenceState.TYPE)).isEqualTo(ReadPreferenceState.SECONDARY);
        assertThat((List) readPreferenceResourceState.getProperty(ReadPreferenceState.TAGS)).isEmpty();
    }

    @Test
    public void updateType() throws Exception {
        ResourceState config = createConfig("testUpdateTypeDB", RUNNING_MONGO_HOST, RUNNING_MONGO_PORT);
        setUpSystem(config);

        ResourceState result = client.read(new RequestContext.Builder().build(), ADMIN_PATH);

        assertThat(result.getProperty("db")).isEqualTo("testUpdateTypeDB");
        assertThat(result.getProperty("servers")).isNotNull();

        assertThat(result.getProperty(ReadPreferenceState.ID)).isNotNull();
        ResourceState readPreferenceResourceState = (ResourceState) result.getProperty(ReadPreferenceState.ID);
        assertThat(readPreferenceResourceState.getProperty(ReadPreferenceState.TYPE)).isEqualTo(ReadPreferenceState.PRIMARY);

        ResourceState updatedResourceState = new DefaultResourceState();
        updatedResourceState.putProperty(ReadPreferenceState.TYPE, ReadPreferenceState.NEAREST);

        config = createConfig("testUpdateTypeDB", RUNNING_MONGO_HOST, RUNNING_MONGO_PORT);

        config.putProperty(ReadPreferenceState.ID, updatedResourceState);

        result = client.update(new RequestContext.Builder().build(), ADMIN_PATH, config);
        ResourceState updatedReadPreferenceResourceState = (ResourceState) result.getProperty(ReadPreferenceState.ID);
        assertThat(updatedReadPreferenceResourceState.getProperty(ReadPreferenceState.TYPE)).isEqualTo(ReadPreferenceState.NEAREST);
    }

    @Test
    public void setWithTags() throws Exception {
        ResourceState config = new DefaultResourceState();
        config.putProperty("db", "testConfigureWithTagsDB");
        config.putProperty("servers", new ArrayList());

        ResourceState configReadPref = new DefaultResourceState();
        configReadPref.putProperty(ReadPreferenceState.TYPE, ReadPreferenceState.SECONDARY);

        ResourceState tagsResourceState = new DefaultResourceState();
        tagsResourceState.putProperty("foo", "bar");
        tagsResourceState.putProperty("hello", "world");

        configReadPref.putProperty(ReadPreferenceState.TAGS, tagsResourceState);

        config.putProperty(ReadPreferenceState.ID, configReadPref);

        setUpSystem(config);

        ResourceState result = client.read(new RequestContext.Builder().build(), ADMIN_PATH);

        assertThat(result.getProperty("db")).isEqualTo("testConfigureWithTagsDB");
        assertThat(result.getProperty("servers")).isNotNull();

        assertThat(result.getProperty(ReadPreferenceState.ID)).isNotNull();
        ResourceState readPreferenceResourceState = (ResourceState) result.getProperty(ReadPreferenceState.ID);
        assertThat(readPreferenceResourceState.getPropertyNames().size()).isEqualTo(2);
        assertThat(readPreferenceResourceState.getProperty(ReadPreferenceState.TYPE)).isEqualTo(ReadPreferenceState.SECONDARY);
        List<ResourceState> tagsStates = (List) readPreferenceResourceState.getProperty(ReadPreferenceState.TAGS);
        assertThat(tagsStates.size()).isEqualTo(2);
        assertThat(tagsStates.get(0).getProperty("foo")).isEqualTo("bar");
        assertThat(tagsStates.get(1).getProperty("hello")).isEqualTo("world");
    }

    @Test
    public void invalidType() throws Exception {
        ResourceState config = new DefaultResourceState();
        config.putProperty("db", "testInvalidTypeDB");
        config.putProperty("servers", new ArrayList());

        ResourceState configReadPref = new DefaultResourceState();
        configReadPref.putProperty(ReadPreferenceState.TYPE, "foobar");
        config.putProperty(ReadPreferenceState.ID, configReadPref);

        try {
            setUpSystem(config);
            Fail.fail();
        } catch (InitializationException e) {
            //expected
        }
    }

    @Test
    public void nullType() throws Exception {
        ResourceState config = new DefaultResourceState();
        config.putProperty("db", "testNullTypeDB");
        config.putProperty("servers", new ArrayList());

        ResourceState configReadPref = new DefaultResourceState();
        configReadPref.putProperty(ReadPreferenceState.TYPE, null);
        config.putProperty(ReadPreferenceState.ID, configReadPref);

        setUpSystem(config);

        ResourceState result = client.read(new RequestContext.Builder().build(), ADMIN_PATH);

        assertThat(result.getProperty("db")).isEqualTo("testNullTypeDB");
        assertThat(result.getProperty("servers")).isNotNull();

        assertThat(result.getProperty(ReadPreferenceState.ID)).isNotNull();
        ResourceState readPreferenceResourceState = (ResourceState) result.getProperty(ReadPreferenceState.ID);
        // the default values
        assertThat(readPreferenceResourceState.getProperty(ReadPreferenceState.TYPE)).isEqualTo(ReadPreferenceState.PRIMARY);
        assertThat(readPreferenceResourceState.getPropertyNames().size()).isEqualTo(1); //Since its a primary type, there cannot be tags
    }

    @Test
    public void updateTags() throws Exception {
        ResourceState config = new DefaultResourceState();
        config.putProperty("db", "testUpdateTagsDB");
        config.putProperty("servers", new ArrayList());

        ResourceState configReadPref = new DefaultResourceState();
        configReadPref.putProperty(ReadPreferenceState.TYPE, ReadPreferenceState.SECONDARY);

        ResourceState tagsResourceState = new DefaultResourceState();
        tagsResourceState.putProperty("hello", "world");
        tagsResourceState.putProperty("loc", "east");

        configReadPref.putProperty(ReadPreferenceState.TAGS, tagsResourceState);
        config.putProperty(ReadPreferenceState.ID, configReadPref);

        setUpSystem(config);

        ResourceState updatedConfig = createConfig("testUpdateTagsDB", RUNNING_MONGO_HOST, RUNNING_MONGO_PORT);

        ResourceState updatedConfigReadPref = new DefaultResourceState();
        updatedConfigReadPref.putProperty(ReadPreferenceState.TYPE, ReadPreferenceState.SECONDARY_PREFERRED);
        ResourceState updatedTagConfig = new DefaultResourceState();
        updatedTagConfig.putProperty("loc", "west");
        updatedConfigReadPref.putProperty(ReadPreferenceState.TAGS, updatedTagConfig);

        updatedConfig.putProperty(ReadPreferenceState.ID, updatedConfigReadPref);
        ResourceState result = client.update(new RequestContext.Builder().build(), ADMIN_PATH, updatedConfig);

        assertThat(result.getProperty("db")).isEqualTo("testUpdateTagsDB");
        assertThat(result.getProperty("servers")).isNotNull();

        assertThat(result.getProperty(ReadPreferenceState.ID)).isNotNull();
        ResourceState readPreferenceResourceState = (ResourceState) result.getProperty(ReadPreferenceState.ID);
        assertThat(readPreferenceResourceState.getPropertyNames().size()).isEqualTo(2);
        assertThat(readPreferenceResourceState.getProperty(ReadPreferenceState.TYPE)).isEqualTo(ReadPreferenceState.SECONDARY_PREFERRED);
        List<ResourceState> tagsStates = (List) readPreferenceResourceState.getProperty(ReadPreferenceState.TAGS);
        assertThat(tagsStates.size()).isEqualTo(1);
        assertThat(tagsStates.get(0).getProperty("loc")).isEqualTo("west");
    }
}
