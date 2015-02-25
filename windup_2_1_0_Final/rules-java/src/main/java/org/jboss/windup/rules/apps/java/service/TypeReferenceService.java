package org.jboss.windup.rules.apps.java.service;

import java.util.HashMap;
import java.util.Map;

import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.ProjectModel;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.graph.service.GraphService;
import org.jboss.windup.reporting.model.InlineHintModel;
import org.jboss.windup.rules.apps.java.scan.ast.JavaTypeReferenceModel;
import org.jboss.windup.rules.apps.java.scan.ast.TypeReferenceLocation;
import org.jboss.windup.rules.files.model.FileLocationModel;
import org.jboss.windup.util.ExecutionStatistics;

import com.thinkaurelius.titan.core.attribute.Text;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;

public class TypeReferenceService extends GraphService<JavaTypeReferenceModel>
{
    public TypeReferenceService(GraphContext context)
    {
        super(context, JavaTypeReferenceModel.class);
    }

    /**
     * Returns the list of most frequently hinted packages (based upon JavaInlineHintModel references) within the given ProjectModel. If recursive is
     * set to true, then also include child projects.
     * 
     * nameDepth controls how many package levels to include (com.* vs com.example.* vs com.example.sub.*)
     */
    public Map<String, Integer> getPackageUseFrequencies(ProjectModel projectModel, int nameDepth, boolean recursive)
    {
        ExecutionStatistics.get().begin("TypeReferenceService.getPackageUseFrequencies(projectModel,nameDepth,recursive)");
        Map<String, Integer> packageUseCount = new HashMap<>();
        getPackageUseFrequencies(packageUseCount, projectModel, nameDepth, recursive);
        ExecutionStatistics.get().end("TypeReferenceService.getPackageUseFrequencies(projectModel,nameDepth,recursive)");
        return packageUseCount;
    }

    private void getPackageUseFrequencies(Map<String, Integer> data, ProjectModel projectModel, int nameDepth,
                boolean recursive)
    {
        ExecutionStatistics.get().begin("TypeReferenceService.getPackageUseFrequencies(data,projectModel,nameDepth,recursive)");
        // 1. Get all JavaHints for the given project
        GremlinPipeline<Vertex, Vertex> pipeline = new GremlinPipeline<>(projectModel.asVertex());
        pipeline.in(FileModel.FILE_TO_PROJECT_MODEL).in(InlineHintModel.FILE_MODEL);
        pipeline.has(WindupVertexFrame.TYPE_PROP, Text.CONTAINS, InlineHintModel.TYPE);

        pipeline.as("inlineHintVertex");
        pipeline.out(InlineHintModel.FILE_LOCATION_REFERENCE).has(WindupVertexFrame.TYPE_PROP, Text.CONTAINS,
                    JavaTypeReferenceModel.TYPE);
        pipeline.back("inlineHintVertex");

        // 2. Organize them by package name
        // summarize results.
        for (Vertex inlineHintVertex : pipeline)
        {
            InlineHintModel javaInlineHint = getGraphContext().getFramed().frame(inlineHintVertex,
                        InlineHintModel.class);

            int val = 1;
            FileLocationModel fileLocationModel = javaInlineHint.getFileLocationReference();
            if (fileLocationModel == null || !(fileLocationModel instanceof JavaTypeReferenceModel))
            {
                continue;
            }
            JavaTypeReferenceModel typeReferenceModel = (JavaTypeReferenceModel) fileLocationModel;

            String pattern = typeReferenceModel.getSourceSnippit();
            String[] keyArray = pattern.split("\\.");

            if (keyArray.length > 1 && nameDepth > 1)
            {
                StringBuilder patternSB = new StringBuilder();
                for (int i = 0; i < nameDepth; i++)
                {
                    String subElement = keyArray[i];
                    // FIXME/TODO - This shouldn't be necessary, but is at the moment due to some stuff emmitted by our
                    // AST
                    if (subElement.contains("(") || subElement.contains(")"))
                    {
                        continue;
                    }

                    if (patternSB.length() != 0)
                    {
                        patternSB.append(".");
                    }
                    patternSB.append(subElement);
                }
                if (patternSB.toString().contains("."))
                {
                    patternSB.append(".*");
                }
                pattern = patternSB.toString();
            }
            if (pattern.contains("("))
            {
                pattern = pattern.substring(0, pattern.indexOf('('));
            }

            if (data.containsKey(pattern))
            {
                val = data.get(pattern);
                val++;
            }
            data.put(pattern, val);
        }

        if (recursive)
        {
            for (ProjectModel childProject : projectModel.getChildProjects())
            {
                ExecutionStatistics.get().end("TypeReferenceService.getPackageUseFrequencies(data,projectModel,nameDepth,recursive)");
                getPackageUseFrequencies(data, childProject, nameDepth, recursive);
                ExecutionStatistics.get().begin("TypeReferenceService.getPackageUseFrequencies(data,projectModel,nameDepth,recursive)");
            }
        }
        ExecutionStatistics.get().end("TypeReferenceService.getPackageUseFrequencies(data,projectModel,nameDepth,recursive)");
    }

    public JavaTypeReferenceModel createTypeReference(FileModel fileModel, TypeReferenceLocation location,
                int lineNumber, int columnNumber, int length, String source)
    {
        ExecutionStatistics.get().begin("TypeReferenceService.createTypeReference(fileModel,location,lineNumber,columnNumber,length,source)");
        JavaTypeReferenceModel model = create();

        model.setFile(fileModel);
        model.setLineNumber(lineNumber);
        model.setColumnNumber(columnNumber);
        model.setLength(length);
        model.setSourceSnippit(source);
        model.setReferenceLocation(location);

        ExecutionStatistics.get().end("TypeReferenceService.createTypeReference(fileModel,location,lineNumber,columnNumber,length,source)");
        return model;
    }

}
