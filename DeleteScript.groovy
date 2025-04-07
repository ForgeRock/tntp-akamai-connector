/*
 * Copyright 2014-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.Method.POST

import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.common.logging.Log

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid
def log = log as Log
def logPrefix = "[Akamai] [DeleteScript]: "
log.error(logPrefix + "Entering " + operation + " Delete Script")

// Build Basic Authentication header
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.error("[Akamai] [DeleteScript]: " + "Processing ObjectClass.ACCOUNT for " + operation + " Script")

        // Create a new map for the request body parameters
        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("type_name", "user");
        pairs.put("id", uid.uidValue);

        log.error(logPrefix + "Delete Pairs: {0}", new Object[]{pairs})

        // Make a POST request to the /entity.delete endpoint
        connection.request(POST, URLENC) { req ->
            uri.path = "/entity.delete"
            headers.'Authorization' = "Basic " + bauth
            body = pairs

            response.success = { resp ->
                log.error(logPrefix + "User profile deleted successfully")
                assert resp.status == 200
            }
        }
        break
        
    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}
