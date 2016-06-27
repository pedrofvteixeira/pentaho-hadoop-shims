package org.pentaho.hadoop.shim;

/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

import java.lang.reflect.Method;

import org.pentaho.hadoop.shim.HadoopConfiguration;

/**
 * A utility class for locating the Big Data Plugin's {@link HadoopConfigurationBootstrap}.
 */
public class HadoopConfigurationUtil {

    private static final String HADOOP_SPOON_PLUGIN = "HadoopSpoonPlugin";

    private static final String CLASS_PLUGIN_REGISTRY = "org.pentaho.di.core.plugins.PluginRegistry";

    private static final String CLASS_PLUGIN_INTERFACE = "org.pentaho.di.core.plugins.PluginInterface";

    private static final String CLASS_LIFECYCLE_PLUGIN_TYPE = "org.pentaho.di.core.plugins.LifecyclePluginType";

    private static final String CLASS_HADOOP_CONFIGURATION_BOOTSTRAP =
            "org.pentaho.di.core.hadoop.HadoopConfigurationBootstrap";

    private static final String CLASS_HADOOP_CONFIGURATION = "org.pentaho.hadoop.shim.HadoopConfiguration";

    private static final String METHOD_GET_INSTANCE = "getInstance";

    private static final String METHOD_FIND_PLUGIN_WITH_ID = "findPluginWithId";

    private static final String METHOD_GET_CLASS_LOADER = "getClassLoader";

    private static final String METHOD_GET_HADOOP_CONFIGURATION_PROVIDER = "getHadoopConfigurationProvider";

    private static final String METHOD_GET_ACTIVE_CONFIGURATION = "getActiveConfiguration";

    private static final String METHOD_GET_HADOOP_SHIM = "getHadoopShim";

    private static final String METHOD_NOTIFY_ON_CONFIGURATION_PREPARED = "notifyOnConfigurationPrepared";

    private ClassLoader bigDataPluginCL;

    /**
     * {@link org.pentaho.di.core.hadoop.HadoopConfigurationBootstrap}
     */
    private Class bootstrapClass;

    private Object provider;

    // public HadoopConfiguration HadoopConfigurationProvider#getActiveConfiguration()
    private Method getActiveConfiguration;

    // public HadoopShim HadoopConfiguration#getHadoopShim()
    private Method getHadoopShim;

    // public void HadoopConfigurationBootstrap#notifyOnConfigurationPrepared()
    private Method notifyOnConfigurationPrepared;

    ClassLoader findBigDataPluginClassLoader() throws Exception {
        Method findPluginById = null;
        Object pluginRegistry = null;
        Method getClassLoader = null;
        try {
            Class<?> pluginRegistryClass = Class.forName( CLASS_PLUGIN_REGISTRY );
            Class<?> pluginInterfaceClass = Class.forName( CLASS_PLUGIN_INTERFACE );
            Method getInstance = pluginRegistryClass.getMethod( METHOD_GET_INSTANCE );
            getClassLoader = pluginRegistryClass.getMethod( METHOD_GET_CLASS_LOADER, pluginInterfaceClass );
            findPluginById = pluginRegistryClass.getMethod( METHOD_FIND_PLUGIN_WITH_ID, Class.class, String.class );
            pluginRegistry = getInstance.invoke( pluginRegistryClass );
        } catch ( Exception ex ) {
            throw new Exception( "Unable to locate Kettle Plugin registry", ex );
        }

        try {
            Class<?> kettleLifecyclePluginTypeClass = Class.forName( CLASS_LIFECYCLE_PLUGIN_TYPE );
            Object hadoopConfigurationBootstrap = findPluginById.invoke( pluginRegistry, kettleLifecyclePluginTypeClass,
                    HADOOP_SPOON_PLUGIN );
            return (ClassLoader) getClassLoader.invoke( pluginRegistry, hadoopConfigurationBootstrap );
        } catch ( Exception ex ) {
            throw new Exception( "Unable to locate Big Data Plugin", ex );
        }
    }

    private Class findHadoopConfigurationBootstrap() throws Exception {
        try {
            if( bigDataPluginCL == null ) {
                bigDataPluginCL = findBigDataPluginClassLoader();
            }

            Class<?> registryClass = Class.forName( CLASS_HADOOP_CONFIGURATION_BOOTSTRAP, true, bigDataPluginCL );
            // Cache methods so we don't have to look them up every time
            notifyOnConfigurationPrepared =
                    registryClass.getMethod( METHOD_NOTIFY_ON_CONFIGURATION_PREPARED, HadoopConfiguration.class );
            return registryClass;
        } catch ( Exception ex ) {
            throw new Exception( "Unable to locate Hadoop Configuration Bootstrap", ex );
        }
    }

    private Object findHadoopConfigurationProvider() throws Exception {
        try {
            if( bootstrapClass == null ) {
                bootstrapClass = findHadoopConfigurationBootstrap();
            }

            Method getHadoopConfigurationProvider = bootstrapClass.getMethod( METHOD_GET_HADOOP_CONFIGURATION_PROVIDER );
            Object provider = getHadoopConfigurationProvider.invoke( null );
            // Cache methods so we don't have to look them up every time
            getActiveConfiguration = provider.getClass().getMethod( METHOD_GET_ACTIVE_CONFIGURATION );
            getHadoopShim = Class.forName( CLASS_HADOOP_CONFIGURATION, true, bigDataPluginCL ).getMethod( METHOD_GET_HADOOP_SHIM );
            return provider;
        } catch ( Exception ex ) {
            throw new Exception( "Unable to locate Hadoop Configuration Registry", ex );
        }
    }

    private synchronized Object getProvider() throws Exception {
        if ( provider == null ) {
            provider = findHadoopConfigurationProvider();
        }
        return provider;
    }

    /**
     * Look up the active Hadoop configuration ({@link org.pentaho.hadoop.shim.HadoopConfiguration}).
     *
     * @return The active Hadoop configuration exception
     * @throws Exception If the Hadoop configuration could not be retrieved. This will likely be a
     * {@link org.pentaho.hadoop.shim.ConfigurationException}.
     */
    public Object getActiveConfiguration() throws Exception {
        Object provider = getProvider();
        return getActiveConfiguration.invoke( provider );
    }

    /**
     * Look up the active Hadoop Configuration's {@link HadoopShim}.
     *
     * @return The {@link HadoopShim} for the active Hadoop Configuration
     * @throws Exception If the active Hadoop configuraiton could not be retrieved.
     */
    public Object getActiveHadoopShim() throws Exception {
        Object activeConfig = getActiveConfiguration();
        return getHadoopShim.invoke( activeConfig );
    }


    /**
     * Invokes HadoopConfigurationBootstrap's notifyOnConfigurationPrepared.
     *
     * @throws Exception If HadoopConfigurationBootstrap.notifyOnConfigurationPrepared() could not be invoked.
     */
    public void notifyOnConfigurationPrepared( HadoopConfiguration hadoopConfiguration ) throws Exception {

        if ( hadoopConfiguration == null ) {
            return;
        }

        if ( bootstrapClass == null ) {
            bootstrapClass = findHadoopConfigurationBootstrap();
        }

        notifyOnConfigurationPrepared.invoke( bootstrapClass, hadoopConfiguration );
    }
}
