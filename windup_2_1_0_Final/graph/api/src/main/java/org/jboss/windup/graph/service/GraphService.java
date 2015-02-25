package org.jboss.windup.graph.service;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.jboss.windup.graph.FramedElementInMemory;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.InMemoryVertexFrame;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.service.exception.NonUniqueResultException;
import org.jboss.windup.util.ExecutionStatistics;
import org.jboss.windup.util.Task;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.attribute.Text;
import com.thinkaurelius.titan.util.datastructures.IterablesUtil;
import com.tinkerpop.blueprints.GraphQuery;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraphQuery;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;
import com.tinkerpop.gremlin.java.GremlinPipeline;

public class GraphService<T extends WindupVertexFrame> implements Service<T>
{
    private Class<T> type;
    private GraphContext context;

    public GraphService(GraphContext context, Class<T> type)
    {
        this.context = context;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public static <T extends WindupVertexFrame> T refresh(GraphContext context, T frame)
    {
        return (T) context.getFramed().frame(frame.asVertex(), WindupVertexFrame.class);
    }

    @Override
    public void commit()
    {
        ExecutionStatistics.performBenchmarked("GraphService.commit", new Task<Void>()
        {
            @Override
            public Void execute()
            {
                getGraphContext().getGraph().getBaseGraph().commit();
                return null;
            }
        });
    }

    @Override
    public long count(final Iterable<?> obj)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.count", new Task<Long>()
        {
            @Override
            public Long execute() throws BuildException
            {
                GremlinPipeline<Iterable<?>, Object> pipe = new GremlinPipeline<Iterable<?>, Object>();
                long result = pipe.start(obj).count();
                return result;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public T createInMemory()
    {
        Class<?>[] resolvedTypes = new Class<?>[] { VertexFrame.class, InMemoryVertexFrame.class, type };
        return (T) Proxy.newProxyInstance(this.type.getClassLoader(),
                    resolvedTypes, new FramedElementInMemory<>(context, this.type));
    }

    /**
     * Create a new instance of the given {@link WindupVertexFrame} type. The ID is generated by the underlying graph database.
     */
    @Override
    public T create()
    {
        return ExecutionStatistics.performBenchmarked("GraphService.create", new Task<T>()
        {
            @Override
            public T execute() throws BuildException
            {
                return context.getFramed().addVertex(null, type);
            }
        });
    }

    @Override
    public T addTypeToModel(final WindupVertexFrame model)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.addTypeToModel", new Task<T>()
        {
            @Override
            public T execute() throws BuildException
            {
                return GraphService.addTypeToModel(getGraphContext(), model, type);
            }
        });
    }

    protected FramedGraphQuery findAllQuery()
    {
        return context.getQuery().type(type);
    }

    @Override
    public Iterable<T> findAll()
    {
        return (Iterable<T>) findAllQuery().vertices(type);
    }

    @Override
    public Iterable<T> findAllByProperties(final String[] keys, final String[] vals)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.findAllByProperties(" + Arrays.asList(keys) + ")", new Task<Iterable<T>>()
        {
            @Override
            public Iterable<T> execute() throws BuildException
            {
                FramedGraphQuery fgq = findAllQuery();

                for (int i = 0, j = keys.length; i < j; i++)
                {
                    String key = keys[i];
                    String val = vals[i];

                    fgq = fgq.has(key, val);
                }

                return fgq.vertices(type);
            }
        });
    }

    @Override
    public Iterable<T> findAllByProperty(final String key, final Object value)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.findAllByProperty(" + key + ")", new Task<Iterable<T>>()
        {
            @Override
            public Iterable<T> execute() throws BuildException
            {
                return context.getFramed().getVertices(key, value, type);
            }
        });
    }

    @Override
    public Iterable<T> findAllByPropertyMatchingRegex(final String key, final String... regex)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.findAllByPropertyMatchingRegex(" + key + ")", new Task<Iterable<T>>()
        {
            @Override
            public Iterable<T> execute() throws BuildException
            {
                if (regex.length == 0)
                    return IterablesUtil.emptyIterable();

                final String regexFinal;
                if (regex.length == 1)
                {
                    regexFinal = regex[0];
                }
                else
                {
                    StringBuilder builder = new StringBuilder();
                    builder.append("\\b(");
                    int i = 0;
                    for (String value : regex)
                    {
                        if (i > 0)
                            builder.append("|");
                        builder.append(value);
                        i++;
                    }
                    builder.append(")\\b");
                    regexFinal = builder.toString();
                }
                return findAllQuery().has(key, Text.REGEX, regexFinal).vertices(type);
            }
        });
    }

    /**
     * Returns the vertex with given ID framed into given interface.
     */
    @Override
    public T getById(Object id)
    {
        return context.getFramed().getVertex(id, this.type);
    }

    @Override
    public T frame(Vertex vertex)
    {
        return getGraphContext().getFramed().frame(vertex, this.getType());
    }

    @Override
    public Class<T> getType()
    {
        return this.type;
    }

    protected GraphQuery getTypedQuery()
    {
        return getGraphContext().getQuery().type(type);
    }

    /**
     * Returns what this' frame has in @TypeValue().
     */
    protected String getTypeValueForSearch()
    {
        TypeValue typeValue = this.type.getAnnotation(TypeValue.class);
        if (typeValue == null)
            throw new IllegalArgumentException("Must be annotated with '@TypeValue': " + this.type.getName());
        return typeValue.value();
    }

    @Override
    public T getUnique() throws NonUniqueResultException
    {
        Iterable<T> results = findAll();

        if (!results.iterator().hasNext())
        {
            return null;
        }

        Iterator<T> iter = results.iterator();
        T result = iter.next();

        if (iter.hasNext())
        {
            throw new NonUniqueResultException("Expected unique value, but returned non-unique.");
        }

        return result;
    }

    @Override
    public T getUniqueByProperty(String property, Object value) throws NonUniqueResultException
    {
        Iterable<T> results = findAllByProperty(property, value);

        if (!results.iterator().hasNext())
        {
            return null;
        }

        Iterator<T> iter = results.iterator();
        T result = iter.next();

        if (iter.hasNext())
        {
            throw new NonUniqueResultException("Expected unique value, but returned non-unique.");
        }

        return result;
    }

    protected T getUnique(GraphQuery framedQuery)
    {
        Iterable<Vertex> results = framedQuery.vertices();

        if (!results.iterator().hasNext())
        {
            return null;
        }

        Iterator<Vertex> iter = results.iterator();
        Vertex result = iter.next();

        if (iter.hasNext())
        {
            throw new NonUniqueResultException("Expected unique value, but returned non-unique.");
        }

        return frame(result);
    }

    protected GraphContext getGraphContext()
    {
        return context;
    }

    @Override
    public TitanTransaction newTransaction()
    {
        return context.getGraph().getBaseGraph().newTransaction();
    }

    /**
     * Adds the specified type to this frame, and returns a new object that implements this type.
     *
     * @see GraphTypeManagerTest
     */
    public static <T extends WindupVertexFrame> T addTypeToModel(GraphContext graphContext, WindupVertexFrame frame,
                Class<T> type)
    {
        Vertex vertex = frame.asVertex();
        graphContext.getGraphTypeRegistry().addTypeToElement(type, vertex);
        return graphContext.getFramed().frame(vertex, type);
    }

    /**
     * Removes the specified type from the frame.
     */
    public static <T extends WindupVertexFrame> T removeTypeFromModel(GraphContext graphContext, WindupVertexFrame frame,
                Class<T> type)
    {
        Vertex vertex = frame.asVertex();
        graphContext.getGraphTypeRegistry().removeTypeFromElement(type, vertex);
        return graphContext.getFramed().frame(vertex, type);
    }

    @Override
    public void remove(final T model)
    {
        ExecutionStatistics.performBenchmarked("GraphService.commit", new Task<Void>()
        {
            @Override
            public Void execute()
            {
                model.asVertex().remove();
                return null;
            }
        });
    }
}