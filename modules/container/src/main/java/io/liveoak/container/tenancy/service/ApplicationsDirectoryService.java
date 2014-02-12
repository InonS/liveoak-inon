package io.liveoak.container.tenancy.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Bob McWhirter
 */
public class ApplicationsDirectoryService implements Service<File> {

    public ApplicationsDirectoryService(File appsDir) {
        this.appsDir = appsDir;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if ( appsDir == null ) {
            try {
                this.appsDir = Files.createTempDirectory( "liveoak" ).toFile();
            } catch (IOException e) {
                throw new StartException(e);
            }
        }

        this.appsDir.mkdirs();
    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public File getValue() throws IllegalStateException, IllegalArgumentException {
        return this.appsDir;
    }

    private File appsDir;

}
