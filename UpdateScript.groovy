/*
 * Copyright 2014-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.POST

import org.identityconnectors.framework.common.exceptions.InvalidCredentialException
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException
import org.identityconnectors.framework.common.exceptions.UnknownUidException

import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.FrameworkUtil

// Cast input parameters
def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def name = id as String
def log = log as Log
def uid = uid as Uid
def logPrefix = "[Akamai] [UpdateScript]: "
log.error(logPrefix + "Entering " + operation + " Script");

def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject

// Build basic auth header
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

switch (operation) {
    case OperationType.UPDATE:
        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                log.error("[Akamai] [UpdateScript]: " + "Processing ObjectClass.ACCOUNT for " + operation + " Script")

                // Collect all user attributes into a map
                HashMap hm = new HashMap()
                def uuid = ""
                for (Attribute attr : attributes) {
                    // Skip certain attributes
                    if (attr.getName() == "password" || attr.getName() == "uuid" || attr.getName() == "created" || attr.getName() == "lastUpdated") {
                        if (attr.getName() == "uuid") {
                            uuid = attr.getValue().get(0).toString()
                            log.error("[Akamai] [UpdateScript]: " + "UUID: " + uuid)
                        }
                        log.error("Skipping attribute")
                        continue
                    }
                    def value = attr.getValue()
                    if (value instanceof List && !value.isEmpty()) {
                        value = value.get(0)
                    }
                    log.error("[Akamai] [UpdateScript]: " + "Attribute " + attr.getName() + ": " + value)
                    hm.put(attr.getName(), value)
                }

                // Convert the HashMap to a JSON string
                def hmAttributes = new JsonBuilder(hm).toString()
                log.error("[Akamai] [UpdateScript]: " + "STRING HashMap Attributes: {0}", new Object[]{hmAttributes})

                // Create a new map for the request body parameters
                Map<String, String> pairs = new HashMap<String, String>();
                pairs.put("type_name", "user");
                pairs.put("uuid", uuid);
                pairs.put("attributes", hmAttributes);
                log.error("[Akamai] [UpdateScript]: " + "Pairs: {0}", new Object[]{pairs})

                // Make the POST request to the /entity.update endpoint
                return connection.request(POST, URLENC) { req ->
                    uri.path = "/entity.update"
                    headers.'Authorization' = "Basic " + bauth
                    body = pairs

                    response.success = { resp, json ->
                        log.error("[Akamai] [UpdateScript]: " + "Update Success")
                        log.error("[Akamai] [UpdateScript]: " + "RAW JSON Response: {0}", new Object[]{json})
                        return uid
                    }
                }
            default:
                throw new ConnectorException("UpdateScript can not handle object type: " + objectClass.objectClassValue)
        }
    default:
        throw new ConnectorException("UpdateScript can not handle operation:" + operation.name())
}
