package org.jboss.windup.reporting.freemarker;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.Variables;
import org.jboss.windup.config.operation.GraphOperation;
import org.jboss.windup.reporting.service.ReportService;
import org.jboss.windup.util.exception.WindupException;
import org.ocpsoft.rewrite.context.EvaluationContext;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * This class is used to produce a freemarker report (and the associated ReportModel) from outside of an Iteration context.
 * 
 * @author jsightler <jesse.sightler@gmail.com)
 * 
 */
public class FreeMarkerOperation extends GraphOperation
{
    private static final Logger LOG = Logger.getLogger(FreeMarkerOperation.class.getName());

    private Furnace furnace;
    private String templatePath;
    private String outputFilename;
    private List<String> variableNames = new ArrayList<>();

    protected FreeMarkerOperation(Furnace furnace, String templatePath, String outputFilename, String... varNames)
    {
        this.furnace = furnace;
        this.templatePath = templatePath;
        this.outputFilename = outputFilename;
        this.variableNames = Arrays.asList(varNames);
    }

    /**
     * Create a FreeMarkerOperation with the provided furnace instance template path, and varNames.
     * 
     * The variables in varNames will be provided to the template, and a new ReportModel will be created with these variables attached.
     * 
     * 
     */
    public static FreeMarkerOperation create(Furnace furnace, String templatePath, String outputFilename,
                String... varNames)
    {
        return new FreeMarkerOperation(furnace, templatePath, outputFilename, varNames);
    }

    @Override
    public void perform(GraphRewrite event, EvaluationContext context)
    {
        try
        {
            ReportService reportService = new ReportService(event.getGraphContext());
            String outputDir = reportService.getReportDirectory();
            Path outputPath = Paths.get(outputDir, outputFilename);

            LOG.info("Reporting: Writing template \"" + templatePath + "\" to output file \""
                        + outputPath.toAbsolutePath().toString() + "\"");

            freemarker.template.Configuration cfg = new freemarker.template.Configuration();
            cfg.setTemplateLoader(new FurnaceFreeMarkerTemplateLoader());
            cfg.setTemplateUpdateDelay(500);
            Template template = cfg.getTemplate(templatePath);

            Variables varStack = Variables.instance(event);

            // just the variables
            Map<String, Object> vars = FreeMarkerUtil.findFreeMarkerContextVariables(varStack,
                        variableNames.toArray(new String[variableNames
                                    .size()]));

            // also, extension functions
            Map<String, Object> freeMarkerExtensions = FreeMarkerUtil.findFreeMarkerExtensions(furnace, event);

            Map<String, Object> objects = new HashMap<>(vars);
            objects.putAll(freeMarkerExtensions);

            try (FileWriter fw = new FileWriter(outputPath.toFile()))
            {
                template.process(objects, fw);
            }
        }
        catch (IOException e)
        {
            throw new WindupException("Failed to write template results due to: " + e.getMessage(), e);
        }
        catch (TemplateException e)
        {
            throw new WindupException("FreeMarkerOperation TemplateException: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString()
    {
        return "FreeMarkerOperation[template=" + templatePath + ", output=" + outputFilename + "]";
    }
}
