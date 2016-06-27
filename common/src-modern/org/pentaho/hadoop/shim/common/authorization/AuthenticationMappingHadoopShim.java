package org.pentaho.hadoop.shim.common.authorization;

import org.pentaho.di.core.auth.NoAuthenticationAuthenticationProvider;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.hadoop.shim.HadoopConfiguration;
import org.pentaho.hadoop.shim.HadoopConfigurationFileSystemManager;
import org.pentaho.hadoop.shim.common.delegating.DelegatingHadoopShim;

/**
 * This HadoopShim tries to check if we have a authenticationMapping.json in play and if so uses that one,
 * thus enabling the new mapped authentication logic.
 *
 * authenticationMapping.json is located at pentaho-solutions/system/karaf/etc;
 *
 * If no authenticationMapping.json file exists ( or if it is not properly configured ), then we'll fallback
 * onto the legacy behaviour, which is to look for a config.properties file within a shim folder.
 *
 */
public class AuthenticationMappingHadoopShim extends AuthenticatingHadoopShim {

    private final String AUTH_MAPPING_AVAILABLE = "authentication.mapping.available";

    @Override
    public void onLoad( HadoopConfiguration config, HadoopConfigurationFileSystemManager fsm ) throws Exception {

        String autenticationMappingAvailable = config.getConfigProperties().getProperty( AUTH_MAPPING_AVAILABLE );

        // Boolean.parseBoolean is NPE-safe
        if( Boolean.parseBoolean( autenticationMappingAvailable ) ) {

            // we will now switch the configured provider to NoAuthenticationAuthenticationProvider.NO_AUTH_ID
            // to trigger the reading of the new mapped authentication logic ( i.e. authenticationMapping.json )
            config.getConfigProperties().put( SUPER_USER , NoAuthenticationAuthenticationProvider.NO_AUTH_ID );

        } else {

            // falling back onto the legacy behaviour, which is to read a static impersonation from config.properties
            LogChannel.GENERAL.logBasic( "authenticationMapping.json not found (or improperly configured). Falling back " +
                    "to the legacy behaviour of reading authentication provider set in config.properties file. " +
                    "This is a legacy configuration and will be deprecated in future releases." );

        }

        // carry on
        super.onLoad( config, fsm );
    }
}
