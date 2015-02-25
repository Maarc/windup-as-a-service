package org.jboss.windup.tests.application;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.jboss.windup.exec.WindupProcessor;
import org.jboss.windup.exec.WindupProgressMonitor;
import org.jboss.windup.exec.configuration.WindupConfiguration;
import org.jboss.windup.exec.configuration.options.UserRulesDirectoryOption;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.GraphContextFactory;
import org.jboss.windup.rules.apps.java.config.ExcludePackagesOption;
import org.jboss.windup.rules.apps.java.config.ScanPackagesOption;
import org.jboss.windup.rules.apps.java.config.SourceModeOption;
import org.junit.Assert;

/**
 * Base class for Windup end-to-end tests.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public abstract class WindupArchitectureTest
{
    @Inject
    private WindupProcessor processor;

    @Inject
    private GraphContextFactory factory;

    /**
     * Get an instance of the {@link GraphContextFactory}.
     */
    protected GraphContextFactory getFactory()
    {
        return factory;
    }

    Path getDefaultPath()
    {
        return FileUtils.getTempDirectory().toPath().resolve("Windup")
                    .resolve("windupgraph_" + RandomStringUtils.randomAlphanumeric(6));
    }

    GraphContext createGraphContext()
    {
        return factory.create(getDefaultPath());
    }

    void runTest(String inputPath, boolean sourceMode) throws Exception
    {
        List<String> includeList = Collections.emptyList();
        List<String> excludeList = Collections.emptyList();
        runTest(createGraphContext(), inputPath, null, sourceMode, includeList, excludeList);
    }

    void runTest(GraphContext graphContext, String inputPath, boolean sourceMode)
                throws Exception
    {
        List<String> includeList = Collections.emptyList();
        List<String> excludeList = Collections.emptyList();
        runTest(graphContext, inputPath, null, sourceMode, includeList, excludeList);
    }

    void runTest(GraphContext graphContext, String inputPath, boolean sourceMode,
                List<String> includePackages) throws Exception
    {
        List<String> excludeList = Collections.emptyList();
        runTest(graphContext, inputPath, null, sourceMode, includePackages, excludeList);
    }

    void runTest(final GraphContext graphContext,
                final String inputPath,
                final File userRulesDir,
                final boolean sourceMode,
                final List<String> includePackages,
                final List<String> excludePackages) throws Exception
    {

        WindupConfiguration wpc = new WindupConfiguration().setGraphContext(graphContext);
        wpc.setInputPath(Paths.get(inputPath));
        wpc.setOutputDirectory(graphContext.getGraphDirectory());
        if (userRulesDir != null)
        {
            wpc.setOptionValue(UserRulesDirectoryOption.NAME, userRulesDir);
        }
        wpc.setOptionValue(SourceModeOption.NAME, sourceMode);
        wpc.setOptionValue(ScanPackagesOption.NAME, includePackages);
        wpc.setOptionValue(ExcludePackagesOption.NAME, excludePackages);

        RecordingWindupProgressMonitor progressMonitor = new RecordingWindupProgressMonitor();
        wpc.setProgressMonitor(progressMonitor);

        processor.execute(wpc);

        Assert.assertFalse(progressMonitor.isCancelled());
        Assert.assertTrue(progressMonitor.isDone());
        Assert.assertFalse(progressMonitor.getSubTaskNames().isEmpty());
        Assert.assertTrue(progressMonitor.getTotalWork() > 0);
        Assert.assertTrue(progressMonitor.getCompletedWork() > 0);
        Assert.assertEquals(progressMonitor.getTotalWork(), progressMonitor.getCompletedWork());
    }

    /*
     * Supporting types
     */
    private static class RecordingWindupProgressMonitor implements WindupProgressMonitor
    {
        private int totalWork = -1;
        private boolean done;
        private boolean cancelled;
        private List<String> taskNames = new ArrayList<>();
        private List<String> subTaskNames = new ArrayList<>();
        private int workDone;

        @Override
        public void beginTask(String name, int totalWork)
        {
            if (this.totalWork == -1)
                this.totalWork = totalWork;
            else
                throw new IllegalStateException("Total work already set.");
        }

        @Override
        public void done()
        {
            this.done = true;
        }

        @Override
        public boolean isCancelled()
        {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled)
        {
            if (cancelled)
                this.cancelled = true;
        }

        @Override
        public void setTaskName(String name)
        {
            this.taskNames.add(name);
        }

        @Override
        public void subTask(String name)
        {
            this.subTaskNames.add(name);
        }

        @Override
        public void worked(int work)
        {
            this.workDone += work;
        }

        public int getTotalWork()
        {
            return totalWork;
        }

        public boolean isDone()
        {
            return done;
        }

        public List<String> getSubTaskNames()
        {
            return subTaskNames;
        }

        public int getCompletedWork()
        {
            return workDone;
        }

    }
}
