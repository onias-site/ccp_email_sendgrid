package com.ccp.implementations.email.sendgrid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.email.CcpEmailSender;
import com.ccp.especifications.email.CcpErrorEmailInvalidAdresses;
import com.ccp.especifications.http.CcpHttpHandler;
import com.ccp.especifications.http.CcpHttpMethods;
import com.ccp.especifications.http.CcpHttpResponseType;
import com.ccp.implementations.email.sendgrid.SendGridEmailSenderSpecialWords.JsonFieldNames;


enum SendGridEmailSenderSpecialWords implements CcpJsonFieldName{
	User_Agent("User-agent")
	;
	enum JsonFieldNames implements CcpJsonFieldName{
		token, url, message, subject, sender, format, method, emails, Authorization, Accept, from, personalizations, content, type, value, to, email
	}
	private final String value;
	
	private SendGridEmailSenderSpecialWords(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
//FIXME CORRIGIR CONTA DO SENDGRID
class SendGridEmailSender implements CcpEmailSender {

	public CcpJsonRepresentation send(CcpJsonRepresentation emailApiParameters) {
		String apiTokenKeyName = emailApiParameters.getAsString(JsonFieldNames.token);

		String apiUrlKeyName = emailApiParameters.getAsString(JsonFieldNames.url);

		String message = emailApiParameters.getAsString(JsonFieldNames.message);

		String subject = emailApiParameters.getAsString(JsonFieldNames.subject);

		String sender = emailApiParameters.getAsString(JsonFieldNames.sender);
		
		String format = emailApiParameters.getAsString(JsonFieldNames.format);

		CcpHttpMethods method = CcpHttpMethods.valueOf(emailApiParameters.getAsString(JsonFieldNames.method));

		List<String> recipients = emailApiParameters.getAsStringList(JsonFieldNames.emails, JsonFieldNames.email);

		if(format.trim().isEmpty()) {
			format = "text/html";
		}
		
		CcpJsonRepresentation systemProperties = new CcpStringDecorator("application_properties").propertiesFrom().environmentVariablesOrClassLoaderOrFile();
		
		String sendgridApiKey =  systemProperties.getDynamicVersion().getAsString(apiTokenKeyName);
		String sendgridApiUrl =  systemProperties.getDynamicVersion().getAsString(apiUrlKeyName);

		CcpHttpHandler ccpHttpHandler = new CcpHttpHandler(202);
		
		CcpJsonRepresentation headers = CcpOtherConstants.EMPTY_JSON
				.put(JsonFieldNames.Authorization, "Bearer " + sendgridApiKey)
				.put(SendGridEmailSenderSpecialWords.User_Agent, "sendgrid/3.0.0;java")
				.put(JsonFieldNames.Accept, "application/json")
		;
		
		String[] emails = recipients.toArray(new String[recipients.size()]);

		List<CcpJsonRepresentation> personalizations = this.getPersonalizations(emails);
		
		CcpJsonRepresentation body = CcpOtherConstants.EMPTY_JSON
				.addToItem(JsonFieldNames.from, JsonFieldNames.email, sender)
				.put(JsonFieldNames.subject, subject)
				.put(JsonFieldNames.personalizations, personalizations)
				.addToList(JsonFieldNames.content, CcpOtherConstants.EMPTY_JSON
						
				.put(JsonFieldNames.type, format)
				.put(JsonFieldNames.value, message))
				;
		
//		this.throwFakeServerErrorToTestingProcessFlow();
		ccpHttpHandler.executeHttpRequest("sendEmail", sendgridApiUrl, method, headers, body, CcpHttpResponseType.singleRecord);
		return CcpOtherConstants.EMPTY_JSON;
	}

	private List<CcpJsonRepresentation> getPersonalizations(String... emails) {
		
		List<String> list = Arrays.asList(emails);
		List<CcpEmailDecorator> invalidEmails = list.stream().map(email -> new CcpStringDecorator(email).email()).filter(x -> false == x.isValid()).collect(Collectors.toList());
		boolean hasInvalidEmails = false == invalidEmails.isEmpty();
		
		if(hasInvalidEmails) {
			throw new CcpErrorEmailInvalidAdresses(invalidEmails);
		}
		
		List<Map<String, Object>> to = list.stream().map(email -> CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.email, email).content).collect(Collectors.toList());
		List<CcpJsonRepresentation> asList = Arrays.asList( CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.to, to));
		return asList;
	}
	
}


