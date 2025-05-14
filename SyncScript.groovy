/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.*

import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.connectors.scriptedrest.SimpleCRESTFilterVisitor
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.Filter
import org.identityconnectors.framework.common.FrameworkUtil
import org.identityconnectors.common.security.GuardedString

import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTUtils
import org.identityconnectors.common.security.SecurityUtil
import groovy.json.JsonSlurper

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Cast input parameters
def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def objectClass = objectClass as ObjectClass
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def logPrefix = "[Akamai] [SyncScript]: "
log.error(logPrefix + "Entering " + operation + " Script")

// Build basic auth header
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()


if (OperationType.GET_LATEST_SYNC_TOKEN.equals(operation)) {
    
    def unixTime = System.currentTimeMillis()
    return new SyncToken(unixTime)

} else if (OperationType.SYNC.equals(operation)) {
    
    def lastToken = token as Object

    log.error("Initial run - TOKEN: {0}", token)

    def currentToken = System.currentTimeMillis()
    log.error("Initial run - CURRENT TOKEN: {0}", currentToken)

    // Converts the token (epoch time) to a UTC ZonedDateTime and formats it as a string.
    def zdt = Instant.ofEpochMilli(token).atZone(ZoneOffset.UTC)
    def formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS Z")
    def formattedDate = zdt.format(formatter)

    log.error("Formatted Date: {0}", formattedDate)

    Map<String, String> pairs = new HashMap<String, String>();
    pairs.put("type_name", "user");
    pairs.put("sort_on", '["id"]')
    pairs.put("filter", "lastUpdated > '" + formattedDate + "'")
    log.error("Pairs: {0}", new Object[]{pairs})

    log.error("Making request")
    return connection.request(POST, URLENC) { req ->
        uri.path = '/entity.find'
        headers.'Authorization' = "Basic " + bauth
        headers.'Content-Type' = "application/x-www-form-urlencoded"
        body = pairs
        parseResponse = false

        //** RESPONSE SUCCESS **//
        response.success = { resp ->
            assert resp.status == 200
            log.error("Sync Success")

            def parsed = new JsonSlurper().parseText(resp.entity.content.text)
            // def parsed = []
            log.error("SYNC RESPONSE - JSON String: {0}", parsed)
            
            if (parsed.results && parsed.results.size() > 0) {
                parsed.results.each { item ->
                    def map = new LinkedHashMap<>(item);
                    if (item.password?.value != null) {
                        def originalPassword = item.password.value
                        def prefix = '''{BCRYPT}$2a$'''
                        def updatedPassword =  prefix + originalPassword.substring(4)
                        def guardedPassword = new GuardedString(updatedPassword.toCharArray())
                        map.password = guardedPassword
                    }
                    
                    log.error("MAP: {0}", map)
                    log.error("SYNC TOKEN: {0}", currentToken)
                    log.error("LAST TOKEN: {0}", lastToken)
                    log.error("ID: {0}", item.id.toString())
                    log.error("UID: {0}", item.id.toString())

                    return handler({
                        syncToken currentToken
                        lastToken = currentToken
                        CREATE_OR_UPDATE()
                        object {
                            uid item.id.toString()
                            id item.id.toString()
                            attributes ScriptedRESTUtils.MapToAttributes(map, ["_id", "name"], false, false)
                        }
                    })  
                }
            }
            log.error("Before return - Current Token: {0}", currentToken)
            return new SyncToken(currentToken)
        }

        //** RESPONSE FAILURE **//
        response.failure = { resp, json ->
            log.error 'request failed'
            log.error(resp.status)
            assert resp.status >= 400
            throw new ConnectorException("List all Failed")
        }
    }
}