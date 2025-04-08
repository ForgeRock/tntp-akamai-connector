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

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.exceptions.ConnectorException

// Cast input parameters
def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def logPrefix = "[Akamai] [CreateScript]: "
log.error(logPrefix + "Entering " + operation + " Script")

def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject

// Build basic auth header
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.error(logPrefix + "Processing ObjectClass.ACCOUNT for " + operation + " Script")

        // Collect attributes into a map
        HashMap hm = new HashMap()
        for (Attribute attr : attributes) {
            def value = attr.getValue()
            if (value instanceof List && !value.isEmpty()) {
                value = value.get(0)
            }
            log.error(logPrefix + "Attribute " + attr.getName() + ": " + value)
            hm.put(attr.getName(), value)
        }

        def hmAttributes = new JsonBuilder(hm).toString()
        log.error(logPrefix + "JSON Attributes: {0}", new Object[]{hmAttributes})

        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("type_name", "user");
        pairs.put("attributes", hmAttributes);
        log.error(logPrefix + "Pairs: {0}", new Object[]{pairs})

        try {
            return connection.request(POST, URLENC) { req ->
                uri.path = "/entity.create"
                headers.'Authorization' = "Basic " + bauth
                headers.'Content-Type' = "application/x-www-form-urlencoded"
                body = pairs

                response.success = { resp, json ->
                    log.error(logPrefix + "User profile created successfully")
                    log.error(logPrefix + "RAW JSON Response: {0}", new Object[]{json})

                    def parsed = new JsonSlurper().parseText(json.keySet().toArray()[0])
                    log.error(logPrefix + "JSON String: " + parsed)
                    def json_id = parsed.id

                    return json_id.toString()
                }
            }
        } catch (Exception ex) {
            log.error(logPrefix + "Exception occurred during create: " + ex)
        }
        break
}
return name
