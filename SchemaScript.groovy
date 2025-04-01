// /*
//  * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
//  *
//  * Use of this code requires a commercial software license with ForgeRock AS.
//  * or with one of its affiliates. All use shall be exclusively subject
//  * to such license between the licensee and ForgeRock AS.
//  */

import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.URLENC

import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import static groovyx.net.http.ContentType.JSON
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.security.SecurityUtil
import org.forgerock.openicf.connectors.groovy.OperationType
import org.identityconnectors.framework.common.objects.ObjectClass
import org.forgerock.openicf.connectors.scriptedrest.SimpleCRESTFilterVisitor
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_READABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_UPDATEABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.REQUIRED
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_CREATABLE

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def logPrefix = "[Akamai] [SchemaScript]: "
log.info(logPrefix + "Entering " + operation + " Script!");

// Build basic auth header
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

/**
* =============================
* GET ACCESS SCHEMA FROM AKAMAI IDENTITY CLOUD
* =============================
*/
List <AttributeInfoBuilder> attributes = []

Map<String, String> pairs = new HashMap<String, String>();
pairs.put("type_name", "user");
pairs.put("access_type", "read");
pairs.put("for_client_id", configuration.getUsername());

connection.request(POST, URLENC) { req ->
    uri.path = '/entityType.getAccessSchema'
    headers.'Authorization' = "Basic " + bauth
    headers.'Content-Type' = "application/x-www-form-urlencoded"
    body = pairs

    response.success = { resp, json ->
        assert resp.status == 200
        log.error("getAccessSchema Success")

        def parsed = new JsonSlurper().parseText(json.keySet().toArray()[0])
        def attrDefs = parsed.schema.attr_defs

        attrDefs.each { attr ->
            def name = attr.name
            if (attr.type == 'string' || attr.type == 'id' || attr.type == 'uuid' || attr.type == 'dateTime' || attr.type == 'date' || attr.type == 'password-bcrypt') {
                attributes.add(new AttributeInfoBuilder(attr.name, String.class))
            } else if (attr.type == 'boolean') {
                attributes.add(new AttributeInfoBuilder(attr.name, Boolean.class))
            } else if (attr.type == 'object') {
                attributes.add(new AttributeInfoBuilder(attr.name, Map.class))
            } else if (attr.type == 'plural') {
                attributes.add(new AttributeInfoBuilder(attr.name, Map.class).setMultiValued(true))
            }
        }
    }

    response.failure = { resp, json ->
        log.error "Akamai API request failed with status: " + resp.status
        if (resp.status > 400 && resp.status != 404) {
            throw new ConnectorException("getAccessSchema request failed")
        }
    }
}

// _id
def idAIB = new AttributeInfoBuilder("__NAME__", String.class);
idAIB.setCreateable(true);
idAIB.setMultiValued(false);
idAIB.setUpdateable(false);

/** 
* =============================
* BUILD SCHEMA
* =============================
*/
return builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attribute idAIB.build()
        attributes.each { attr ->
            attribute attr.build()
        }
    }
})

log.error(logPrefix + "Schema script done");