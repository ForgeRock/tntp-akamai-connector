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
import org.identityconnectors.common.security.SecurityUtil
import groovy.json.JsonSlurper

// Cast input parameters
def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def logPrefix = "[Akamai] [SearchScript]: "
log.error(logPrefix + "Entering " + operation + " Script")

// Build basic auth header
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

/**
 * ================================
 * VIEW USER PROFILE FROM AKAMAI IDENTITY CLOUD
 * ================================
 *
 * If filter is provided:
 *   - Extract the UID from the filter
 *   - Make a POST request to the /entity endpoint
 *   - Parse and map key attributes from the returned JSON profile
 */
if (filter != null) {
    def uuid = FrameworkUtil.getUidIfGetOperation(filter)
    log.error(logPrefix + "Filter Object: {0}", new Object[]{filter})
    log.error(logPrefix + "UID from FILTER: {0}", new Object[]{uuid})
    if (uuid != null) {
        // Clean up any cached data for this UID if present
        def special = configuration.getPropertyBag().get(uuid.uidValue)
        if (special != null) {
            configuration.getPropertyBag().remove(uuid.uidValue)
        }

        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("type_name", "user");
        pairs.put("id", Integer.parseInt(uuid.getUidValue()))

        connection.request(POST, URLENC) { req ->
            uri.path = '/entity'
            headers.'Authorization' = "Basic " + bauth
            headers.'Content-Type' = "application/x-www-form-urlencoded"
            headers.'Accept' = "application/json"
            body = pairs
            parseResponse = false

            response.success = { resp ->
                assert resp.status == 200
                log.error("Search Success")

                def parsed = new JsonSlurper().parseText(resp.entity.content.text)
                // log.error("SINGLE SEARCH RESPONSE - JSON String: " + parsed)

                def map = parsed.result
                if (parsed.result.password?.value != null) {
                    def originalPassword = parsed.result.password.value
                    def prefix = '''{BCRYPT}$2a$'''
                    def updatedPassword =  prefix + originalPassword.substring(4)
                    def guardedPassword = new GuardedString(updatedPassword.toCharArray())
                    map.password = guardedPassword
                }
                
                handler {
                    uid parsed.result.id.toString()
                    id parsed.result.id.toString()
                    attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                }
                return new org.identityconnectors.framework.common.objects.SearchResult()
            }

            response.failure = { resp, json ->
                log.error "Akamai API request failed with status: " + resp.status
                if (resp.status > 400 && resp.status != 404) {
                    throw new ConnectorException("View profile request failed")
                }
            }
        }
    }
/**
* ================================
* LIST ALL PATIENTS (No Filter Provided)
* ================================
*
* When no filter is provided,
*   - 1st:  The script performs a list operation by querying the /fhir/Patient endpoint. 
*   - 2nd:  It processes the initial response to capture a pagination URL (if available) 
*   - 3rd:  Iterates through pages (up to 10 pages) to retrieve and process all patient entries, passing each to the handler.
*
*/
} else {
    def lastId = 0
    def maxResults = 500
    def continueLoop = true
    
    while (continueLoop) {
        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("type_name", "user");
        pairs.put("max_results", maxResults)
        pairs.put("sort_on", '["id"]')
        pairs.put("filter", "id > " + lastId)
        log.error("Pairs: {0}", new Object[]{pairs})

        connection.request(POST, URLENC) { req ->
            uri.path = '/entity.find'
            headers.'Authorization' = "Basic " + bauth
            headers.'Content-Type' = "application/x-www-form-urlencoded"
            headers.'Accept' = "application/json"
            body = pairs
            parseResponse = false

            response.success = { resp ->
                assert resp.status == 200
                log.error("Bulk Search Success")

                def parsed = new JsonSlurper().parseText(resp.entity.content.text)
                // log.error("BULK SEARCH RESPONSE - JSON String: " + parsed)

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

                    handler {
                        uid item.id.toString()
                        id item.id.toString()
                        attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                    }
                }
                if (parsed.results.size() < maxResults) {
                    continueLoop = false
                } else {
                    lastId = parsed.results.last().id
                }
                } else {
                    continueLoop = false
                }
            }
            
            response.failure = { resp, json ->
                log.error "Akamai API request failed with status: " + resp.status
                if (resp.status > 400 && resp.status != 404) {
                    throw new ConnectorException("View profile request failed")
                }
            }
        }
    }
}