package com.ccp.implementations.email.sendgrid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.email.CcpEmailSender;
import com.ccp.especifications.http.CcpHttpContentType;
import com.ccp.especifications.http.CcpHttpHandler;
import com.ccp.especifications.http.CcpHttpMethods;
import com.ccp.especifications.http.CcpHttpResponseType;
import com.ccp.implementations.email.sendgrid.SendGridEmailSenderSpecialWords.JsonFieldNames;
import java.util.stream.Stream;



import com.ccp.json.fields.validation.CcpJsonCommonsFields;
//TODO FIX THE SENDGRID ACCOUNT
/**
 * {@code CcpEmailSender} implementation using the SendGrid API. Builds the JSON payload with
 * sender, recipients, subject and body, and sends it via {@code POST} authenticated with a Bearer token.
 * Validates the e-mail addresses before sending; throws {@code CcpErrorEmailInvalidAdresses} if
 * any address is invalid.
 */
class SendGridEmailSender implements CcpEmailSender {

	public CcpJsonRepresentation sendSimpleTextEmailMessage(String providerToken, String providerUrl, String templateId, String sender, String subject, String message, CcpHttpContentType contentType, String... emails) {

		CcpHttpHandler ccpHttpHandler = new CcpHttpHandler(202, providerUrl);
		String bearerToken = "Bearer " + providerToken;
		CcpJsonRepresentation headersWithAuthorization = CcpOtherConstants.EMPTY_JSON
				.put(CcpJsonCommonsFields.Authorization, bearerToken);
				CcpJsonRepresentation headersWithUserAgent = headersWithAuthorization
				.put(SendGridEmailSenderSpecialWords.User_Agent, "sendgrid/3.0.0;java");

				CcpJsonRepresentation headers = headersWithUserAgent
				.put(CcpJsonCommonsFields.Accept, "application/json")
		;
		

		List<CcpJsonRepresentation> personalizations = this.getPersonalizations(emails);
		CcpJsonRepresentation bodyWithSender = CcpOtherConstants.EMPTY_JSON
				.addToItem(JsonFieldNames.from, JsonFieldNames.email, sender);
				CcpJsonRepresentation bodyWithSubject = bodyWithSender
				.put(JsonFieldNames.subject, subject);
				CcpJsonRepresentation bodyWithPersonalizations = bodyWithSubject
				.put(JsonFieldNames.personalizations, personalizations);
				CcpJsonRepresentation contentWithType = CcpOtherConstants.EMPTY_JSON
						
				.put(CcpJsonCommonsFields.type, contentType);
				CcpJsonRepresentation contentWithTypeAndValue = contentWithType
				.put(CcpJsonCommonsFields.value, message);

				CcpJsonRepresentation body = bodyWithPersonalizations
				.addToList(JsonFieldNames.content, contentWithTypeAndValue)
				;
		
		//		this.throwFakeServerErrorToTestingProcessFlow();
		ccpHttpHandler.executeHttpRequest("sendEmail", CcpHttpMethods.POST, headers, body, CcpHttpResponseType.singleRecord);
		return body;
	}

	private List<CcpJsonRepresentation> getPersonalizations(String... emails) {

		List<String> list = Arrays.asList(emails);
		Stream<String> emailsStream = list.stream();
		var emailDecoratorsStream = emailsStream.map(email -> new CcpStringDecorator(email).email());
		var invalidEmailsStream = emailDecoratorsStream.filter(x -> false == x.isValid());
		List<CcpEmailDecorator> invalidEmails = invalidEmailsStream.collect(Collectors.toList());
		boolean invalidEmailsEmpty = invalidEmails.isEmpty();
		boolean hasInvalidEmails = false == invalidEmailsEmpty;

		if(hasInvalidEmails) {
			CcpErrorEmailInvalidAdresses ccpErrorEmailInvalidAdresses = new CcpErrorEmailInvalidAdresses(invalidEmails);
			throw ccpErrorEmailInvalidAdresses;
		}
		Stream<String> recipientEmailsStream = list.stream();
		var recipientsStream = recipientEmailsStream.map(email -> CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.email, email).content);

		List<Map<String, Object>> to = recipientsStream.collect(Collectors.toList());
		CcpJsonRepresentation personalization = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.to, to);
		List<CcpJsonRepresentation> personalizations = Arrays.asList( personalization);
		return personalizations;
	}

	@SuppressWarnings("serial")
	public static class CcpErrorEmailInvalidAdresses extends RuntimeException {
		private CcpErrorEmailInvalidAdresses(List<?> invalidEmails) {
			super("These following mail addresses are not valid: " + invalidEmails);
		}
	}
}
