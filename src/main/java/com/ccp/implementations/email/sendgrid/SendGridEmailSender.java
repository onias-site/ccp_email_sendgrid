package com.ccp.implementations.email.sendgrid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.email.CcpEmailSender;
import com.ccp.especifications.http.CcpHttpHandler;
import com.ccp.especifications.http.CcpHttpResponseType;
import com.ccp.exceptions.email.CcpErrorEmailInvalidAdresses;
import com.ccp.http.CcpHttpMethods;

enum SendGridEmailSenderConstants implements CcpJsonFieldName{
	token, url, message, subject, sender, format, method, emails, Authorization, Accept, from, personalizations, content, type, value, to, email
}

enum SendGridEmailSenderSpecialWords implements CcpJsonFieldName{
	User_Agent("User-agent")
	;
	private final String value;
	
	private SendGridEmailSenderSpecialWords(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}

class SendGridEmailSender implements CcpEmailSender {

	public CcpJsonRepresentation send(CcpJsonRepresentation emailApiParameters) {
		String apiTokenKeyName = emailApiParameters.getAsString(SendGridEmailSenderConstants.token);

		String apiUrlKeyName = emailApiParameters.getAsString(SendGridEmailSenderConstants.url);

		String message = emailApiParameters.getAsString(SendGridEmailSenderConstants.message);

		String subject = emailApiParameters.getAsString(SendGridEmailSenderConstants.subject);

		String sender = emailApiParameters.getAsString(SendGridEmailSenderConstants.sender);
		
		String format = emailApiParameters.getAsString(SendGridEmailSenderConstants.format);

		CcpHttpMethods method = CcpHttpMethods.valueOf(emailApiParameters.getAsString(SendGridEmailSenderConstants.method));

		List<String> recipients = emailApiParameters.getAsStringList(SendGridEmailSenderConstants.emails, SendGridEmailSenderConstants.email);

		if(format.trim().isEmpty()) {
			format = "text/html";
		}
		
		CcpJsonRepresentation systemProperties = new CcpStringDecorator("application_properties").propertiesFrom().environmentVariablesOrClassLoaderOrFile();
		
		String sendgridApiKey =  systemProperties.getDynamicVersion().getAsString(apiTokenKeyName);
		String sendgridApiUrl =  systemProperties.getDynamicVersion().getAsString(apiUrlKeyName);

		CcpHttpHandler ccpHttpHandler = new CcpHttpHandler(202);
		
		CcpJsonRepresentation headers = CcpOtherConstants.EMPTY_JSON
				.put(SendGridEmailSenderConstants.Authorization, "Bearer " + sendgridApiKey)
				.put(SendGridEmailSenderSpecialWords.User_Agent, "sendgrid/3.0.0;java")
				.put(SendGridEmailSenderConstants.Accept, "application/json")
		;
		
		String[] emails = recipients.toArray(new String[recipients.size()]);

		List<CcpJsonRepresentation> personalizations = this.getPersonalizations(emails);
		
		CcpJsonRepresentation body = CcpOtherConstants.EMPTY_JSON
				.addToItem(SendGridEmailSenderConstants.from, SendGridEmailSenderConstants.email, sender)
				.put(SendGridEmailSenderConstants.subject, subject)
				.put(SendGridEmailSenderConstants.personalizations, personalizations)
				.addToList(SendGridEmailSenderConstants.content, CcpOtherConstants.EMPTY_JSON
						
				.put(SendGridEmailSenderConstants.type, format)
				.put(SendGridEmailSenderConstants.value, message))
				;
		
//		this.throwFakeServerErrorToTestingProcessFlow();
		ccpHttpHandler.executeHttpRequest("sendEmail", sendgridApiUrl, method, headers, body, CcpHttpResponseType.singleRecord);
		return CcpOtherConstants.EMPTY_JSON;
	}

	private List<CcpJsonRepresentation> getPersonalizations(String... emails) {
		
		List<String> list = Arrays.asList(emails);
		List<CcpEmailDecorator> invalidEmails = list.stream().map(email -> new CcpStringDecorator(email).email()).filter(x -> x.isValid() == false).collect(Collectors.toList());
		boolean hasInvalidEmails = invalidEmails.isEmpty() == false;
		
		if(hasInvalidEmails) {
			throw new CcpErrorEmailInvalidAdresses(invalidEmails);
		}
		
		List<Map<String, Object>> to = list.stream().map(email -> CcpOtherConstants.EMPTY_JSON.put(SendGridEmailSenderConstants.email, email).content).collect(Collectors.toList());
		List<CcpJsonRepresentation> asList = Arrays.asList( CcpOtherConstants.EMPTY_JSON.put(SendGridEmailSenderConstants.to, to));
		return asList;
	}
	
}


