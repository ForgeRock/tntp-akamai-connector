/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_READABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_UPDATEABLE

import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.ObjectClass

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def logPrefix = "[Akamai] [SchemaScript]: "

log.info(logPrefix + "Entering " + operation + " Script!");

// Declare the __ACCOUNT__ attributes
// _id
def idAIB = new AttributeInfoBuilder("__NAME__", String.class);
idAIB.setCreateable(true);
idAIB.setMultiValued(false);
idAIB.setUpdateable(false);

// password
def passwordAIB = new AttributeInfoBuilder("password", String.class);
passwordAIB.setMultiValued(false);

// gender
def genderAIB = new AttributeInfoBuilder("gender", String.class);
genderAIB.setMultiValued(false);

// mobileNumber
def mobileNumberAIB = new AttributeInfoBuilder("mobileNumber", String.class);
mobileNumberAIB.setMultiValued(false);

// familyName
def familyNameAIB = new AttributeInfoBuilder("familyName", String.class);
familyNameAIB.setMultiValued(false);

// givenname
def givenNameAIB = new AttributeInfoBuilder("givenName", String.class);
givenNameAIB.setMultiValued(false);

// middleName
def middleNameAIB = new AttributeInfoBuilder("middleName", String.class);
middleNameAIB.setMultiValued(false);

// displayName
def displayNameAIB = new AttributeInfoBuilder("displayName", String.class);
displayNameAIB.setMultiValued(false);

//email
def email = new AttributeInfoBuilder("email", String.class);
email.setMultiValued(false);

// address1
def addressAIB = new AttributeInfoBuilder("address1", String.class);
addressAIB.setMultiValued(false);

// city
def cityAIB = new AttributeInfoBuilder("city", String.class);
cityAIB.setMultiValued(false);

// country
def countryAIB = new AttributeInfoBuilder("country", String.class);
countryAIB.setMultiValued(false);

// zip code
def zipAIB = new AttributeInfoBuilder("zip", String.class);
zipAIB.setMultiValued(false);

// stateAbbreviation
def stateAbbreviationAIB = new AttributeInfoBuilder("stateAbbreviation", String.class);
stateAbbreviationAIB.setMultiValued(false);

return builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attribute idAIB.build()
        attribute passwordAIB.build()
        attribute genderAIB.build()
        attribute addressAIB.build()
        attribute email.build()
        attribute mobileNumberAIB.build()
        attribute givenNameAIB.build()
        attribute familyNameAIB.build()
        attribute displayNameAIB.build()
        attribute middleNameAIB.build()
        attribute zipAIB.build()
        attribute stateAbbreviationAIB.build()
        attribute cityAIB.build()
        attribute countryAIB.build()
    }
})

log.error(logPrefix + "Schema script done");
