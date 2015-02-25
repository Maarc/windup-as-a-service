package org.jboss.windup.config.query;

import org.jboss.windup.config.GraphRewrite;

import com.thinkaurelius.titan.core.attribute.Text;
import com.thinkaurelius.titan.graphdb.query.TitanPredicate;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;

class QueryPropertyCriterion implements QueryGremlinCriterion
{

    private String propertyName;
    private QueryPropertyComparisonType searchType;
    private Object searchValue;

    public QueryPropertyCriterion(String propertyName, QueryPropertyComparisonType searchType,
                Object searchValue)
    {
        super();
        this.propertyName = propertyName;
        this.searchType = searchType;
        this.searchValue = searchValue;
    }

    @Override
    public void query(GraphRewrite event, GremlinPipeline<Vertex, Vertex> pipeline)
    {
        switch (searchType)
        {
        case EQUALS:
            pipeline.has(this.propertyName, this.searchValue);
            break;
        case CONTAINS_TOKEN:
            pipeline.has(this.propertyName, Text.CONTAINS, searchValue);
            break;
        case CONTAINS_ANY_TOKEN:
            pipeline.has(this.propertyName, new MultipleValueTitanPredicate(), searchValue);
            break;
        case REGEX:
            pipeline.has(this.propertyName, Text.REGEX, searchValue);
            break;
        default:
            throw new IllegalArgumentException("Unrecognized query type: " + searchType);
        }
    }

    private final class MultipleValueTitanPredicate implements TitanPredicate
    {
        @Override
        public boolean evaluate(Object first, Object second)
        {
            if (second instanceof Iterable<?>)
            {
                boolean found = false;
                ITERABLE: for (Object element : (Iterable<?>) second)
                {
                    if (element instanceof Enum && ((Enum<?>) element).name().equals(first))
                    {
                        found = true;
                        break ITERABLE;
                    }
                    if (first.equals(element))
                    {
                        found = true;
                        break ITERABLE;
                    }
                }
                return found;
            }
            return false;
        }

        @Override
        public boolean isValidCondition(Object condition)
        {
            return condition != null && condition instanceof Iterable<?>;
        }

        @Override
        public boolean isValidValueType(Class<?> clazz)
        {
            return Iterable.class.isAssignableFrom(clazz);
        }

        @Override
        public boolean hasNegation()
        {
            return false;
        }

        @Override
        public TitanPredicate negate()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isQNF()
        {
            return true;
        }
    }

    public String toString()
    {
        return ".has(" + propertyName + "," + searchType + "," + searchValue + ")";
    }
}