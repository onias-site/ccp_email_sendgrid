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
import com.ccp.especifications.email.CcpErrorEmailInvalidAdresses;
import com.ccp.especifications.http.CcpHttpContentType;
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
/**
 * Implementação de {@code CcpEmailSender} via API SendGrid. Monta o payload JSON com
 * remetente, destinatários, assunto e corpo, e envia via {@code POST} autenticado por Bearer token.
 * Valida os endereços de e-mail antes do envio; lança {@code CcpErrorEmailInvalidAdresses} se
 * algum endereço for inválido.
 */
class SendGridEmailSender implements CcpEmailSender {

	public CcpJsonRepresentation sendSimpleTextEmailMessage(String providerToken, String providerUrl, String templateId, String sender, String subject, String message, CcpHttpContentType contentType, String... emails) {


		CcpHttpHandler ccpHttpHandler = new CcpHttpHandler(202, providerUrl);
		
		CcpJsonRepresentation headers = CcpOtherConstants.EMPTY_JSON
				.put(JsonFieldNames.Authorization, "Bearer " + providerToken)
				.put(SendGridEmailSenderSpecialWords.User_Agent, "sendgrid/3.0.0;java")
				.put(JsonFieldNames.Accept, "application/json")
		;
		

		List<CcpJsonRepresentation> personalizations = this.getPersonalizations(emails);
		
		CcpJsonRepresentation body = CcpOtherConstants.EMPTY_JSON
				.addToItem(JsonFieldNames.from, JsonFieldNames.email, sender)
				.put(JsonFieldNames.subject, subject)
				.put(JsonFieldNames.personalizations, personalizations)
				.addToList(JsonFieldNames.content, CcpOtherConstants.EMPTY_JSON
						
				.put(JsonFieldNames.type, contentType)
				.put(JsonFieldNames.value, message))
				;
		
		//		this.throwFakeServerErrorToTestingProcessFlow();
		ccpHttpHandler.executeHttpRequest("sendEmail", CcpHttpMethods.POST, headers, body, CcpHttpResponseType.singleRecord);
		return body;
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


