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

// departmentId
def departmentIDAIB = new AttributeInfoBuilder("departmentID", String.class);
departmentIDAIB.setMultiValued(false);

// departmentIdType
def departmentIDTypeAIB = new AttributeInfoBuilder("departmentIDType", String.class);
departmentIDTypeAIB.setMultiValued(false);

// nationalIdentifier
def nationalIdentifierAIB = new AttributeInfoBuilder("nationalIdentifier", String.class);
nationalIdentifierAIB.setMultiValued(false);

// dob
def dateOfBirthAIB = new AttributeInfoBuilder("dateOfBirth", String.class);
dateOfBirthAIB.setMultiValued(false);

// password
def passwordAIB = new AttributeInfoBuilder("password", String.class);
passwordAIB.setMultiValued(false);

// gender
def genderAIB = new AttributeInfoBuilder("gender", String.class);
genderAIB.setMultiValued(false);

// marital status
def maritalStatusAIB = new AttributeInfoBuilder("maritalStatus", String.class);
maritalStatusAIB.setMultiValued(false);

// race
def raceAIB = new AttributeInfoBuilder("race", String.class);
raceAIB.setMultiValued(false);

//religion
def religionAIB = new AttributeInfoBuilder("religion", String.class);
religionAIB.setMultiValued(false);

// Name
def nameAIB = new AttributeInfoBuilder("name", String.class);
nameAIB.setMultiValued(false);

// telephone number
def telephoneNumberAIB = new AttributeInfoBuilder("telephoneNumber", String.class);
telephoneNumberAIB.setMultiValued(false);

// mobileNumber
def mobileNumberAIB = new AttributeInfoBuilder("mobileNumber", String.class);
mobileNumberAIB.setMultiValued(false);

// familyName
def familyNameAIB = new AttributeInfoBuilder("familyName", String.class);
familyNameAIB.setMultiValued(false);

// sn
def snAIB = new AttributeInfoBuilder("sn", String.class);
snAIB.setMultiValued(false);

// givenname
def givenNameAIB = new AttributeInfoBuilder("givenName", String.class);
givenNameAIB.setMultiValued(false);

// middleName
def middleNameAIB = new AttributeInfoBuilder("middleName", String.class);
middleNameAIB.setMultiValued(false);

// displayName
def displayNameAIB = new AttributeInfoBuilder("displayName", String.class);
displayNameAIB.setMultiValued(false);

// userName
def userNameAIB = new AttributeInfoBuilder("userName", String.class);
userNameAIB.setMultiValued(false);

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

// postal address
def postalAddressAIB = new AttributeInfoBuilder("postalAddress", String.class);
postalAddressAIB.setMultiValued(false);

// postal code
def postalCodeAIB = new AttributeInfoBuilder("postalCode", String.class);
postalCodeAIB.setMultiValued(false);

// state
def stateProvinceAIB = new AttributeInfoBuilder("stateProvince", String.class);
stateProvinceAIB.setMultiValued(false);

// stateAbbreviation
def stateAbbreviationAIB = new AttributeInfoBuilder("stateAbbreviation", String.class);
stateAbbreviationAIB.setMultiValued(false);

return builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attribute idAIB.build()
        attribute departmentIDAIB.build()
        attribute departmentIDTypeAIB.build()
        attribute nationalIdentifierAIB.build()
        attribute dateOfBirthAIB.build()
        attribute passwordAIB.build()
        attribute genderAIB.build()
        attribute maritalStatusAIB.build()
        attribute raceAIB.build()
        attribute religionAIB.build()
        attribute addressAIB.build()
        attribute nameAIB.build()
        attribute telephoneNumberAIB.build()
        attribute mobileNumberAIB.build()
        attribute givenNameAIB.build()
        attribute displayNameAIB.build()
        attribute userNameAIB.build()
        attribute familyNameAIB.build()
        attribute middleNameAIB.build()
        attribute snAIB.build()
        attribute zipAIB.build()
        attribute postalAddressAIB.build()
        attribute postalCodeAIB.build()
        attribute stateAbbreviationAIB.build()
        attribute stateProvinceAIB.build()
        attribute cityAIB.build()
        attribute countryAIB.build()
        attribute email.build()
    }
})

log.error(logPrefix + "Schema script dones");
