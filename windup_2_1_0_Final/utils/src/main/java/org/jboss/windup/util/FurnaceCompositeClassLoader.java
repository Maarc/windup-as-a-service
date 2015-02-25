package org.jboss.windup.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class FurnaceCompositeClassLoader extends ClassLoader
{
    private final List<ClassLoader> loaders = Collections.synchronizedList(new ArrayList<ClassLoader>());

    public FurnaceCompositeClassLoader(List<ClassLoader> loaders)
    {
        this.loaders.addAll(loaders);
    }

    public FurnaceCompositeClassLoader(ClassLoader classLoader, List<ClassLoader> loaders)
    {
        super(classLoader);
        this.loaders.addAll(loaders);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException
    {
        for (ClassLoader classLoader : loaders)
        {
            try
            {
                return classLoader.loadClass(name);
            }
            catch (ClassNotFoundException notFound)
            {
                // oh well
            }
        }

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null)
        {
            return contextLoader.loadClass(name);
        }
        else
        {
            return super.findClass(name);
        }
    }

    @Override
    public URL getResource(String name)
    {
        for (ClassLoader classLoader : loaders)
        {
            URL resource = classLoader.getResource(name);
            if (resource != null)
            {
                return resource;
            }
        }

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null)
        {
            return contextLoader.getResource(name);
        }
        else
        {
            return super.getResource(name);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        List<URL> result = new ArrayList<>();
        for (ClassLoader classLoader : loaders)
        {
            result.addAll(Collections.list(classLoader.getResources(name)));
        }

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null)
        {
            return contextLoader.getResources(name);
        }
        else
        {
            return super.getResources(name);
        }
    }
}