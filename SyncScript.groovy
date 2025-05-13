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
import org.forgerock.openicf.connectors.scriptedrest.VisitorParameter
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
import static groovyx.net.http.ContentType.JSON
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
        log.error("TOKEN: " + token)

        def currentToken = System.currentTimeMillis()
        log.error("CURRENT TOKEN: " + currentToken)

        // Convert to Instant, then to ZonedDateTime in UTC
        def zdt = Instant.ofEpochMilli(token).atZone(ZoneOffset.UTC)

        // Define the desired formatter
        def formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS Z")

        // Format and print the result
        def formattedDate = zdt.format(formatter)

        // def token = 0 as Object

            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("type_name", "user");
            // pairs.put("max_results", maxResults)
            pairs.put("sort_on", '["id"]')
            pairs.put("filter", "lastUpdated > '" + formattedDate + "'")
            log.error("Pairs: {0}", new Object[]{pairs})

        log.error("Making request")
            connection.request(POST, URLENC) { req ->
                uri.path = '/entity.find'
                headers.'Authorization' = "Basic " + bauth
                headers.'Content-Type' = "application/x-www-form-urlencoded"
                body = pairs
                parseResponse = false

                response.success = { resp ->
                    assert resp.status == 200
                    log.error("Bulk Search Success")

                    def parsed = new JsonSlurper().parseText(resp.entity.content.text)
                    log.error("BULK SEARCH RESPONSE - JSON String: " + parsed)

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
                        log.error("MAP: " + map)

                        handler({
                            syncToken currentToken
                            CREATE_OR_UPDATE()
                            object {
                                uid item.id.toString()
                                id item.id.toString()
                                attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                            }
                        })
                    }
                    // if (parsed.results.size() < maxResults) {
                    //     continueLoop = false
                    // } else {
                    //     lastId = parsed.results.last().id
                    // }
                    // } else {
                    //     continueLoop = false
                    // }
                }
                return new SyncToken(currentToken)
            }

        response.failure = { resp, json ->
            log.error 'request failed'
            log.error(resp.status)
            assert resp.status >= 400
            throw new ConnectorException("List all Failed")
        }
    }
}
