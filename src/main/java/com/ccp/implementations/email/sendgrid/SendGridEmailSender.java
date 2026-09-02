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
		String valorMais = "Bearer " + providerToken;
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
				.put(JsonFieldNames.Authorization, valorMais);
				CcpJsonRepresentation put2 = put
				.put(SendGridEmailSenderSpecialWords.User_Agent, "sendgrid/3.0.0;java");

				CcpJsonRepresentation headers = put2
				.put(JsonFieldNames.Accept, "application/json")
		;
		

		List<CcpJsonRepresentation> personalizations = this.getPersonalizations(emails);
		CcpJsonRepresentation addToItem = CcpOtherConstants.EMPTY_JSON
				.addToItem(JsonFieldNames.from, JsonFieldNames.email, sender);
				CcpJsonRepresentation put3 = addToItem
				.put(JsonFieldNames.subject, subject);
				CcpJsonRepresentation put4 = put3
				.put(JsonFieldNames.personalizations, personalizations);
				CcpJsonRepresentation put5 = CcpOtherConstants.EMPTY_JSON
						
				.put(JsonFieldNames.type, contentType);
				CcpJsonRepresentation put6 = put5
				.put(JsonFieldNames.value, message);

				CcpJsonRepresentation body = put4
				.addToList(JsonFieldNames.content, put6)
				;
		
		//		this.throwFakeServerErrorToTestingProcessFlow();
		ccpHttpHandler.executeHttpRequest("sendEmail", CcpHttpMethods.POST, headers, body, CcpHttpResponseType.singleRecord);
		return body;
	}

	private List<CcpJsonRepresentation> getPersonalizations(String... emails) {

		List<String> list = Arrays.asList(emails);
		Stream<String> stream = list.stream();
		var streamMap = stream.map(email -> new CcpStringDecorator(email).email());
		var filter = streamMap.filter(x -> false == x.isValid());
		List<CcpEmailDecorator> invalidEmails = filter.collect(Collectors.toList());
		boolean invalidEmailsEmpty = invalidEmails.isEmpty();
		boolean hasInvalidEmails = false == invalidEmailsEmpty;

		if(hasInvalidEmails) {
			CcpErrorEmailInvalidAdresses ccpErrorEmailInvalidAdresses = new CcpErrorEmailInvalidAdresses(invalidEmails);
			throw ccpErrorEmailInvalidAdresses;
		}
		Stream<String> stream2 = list.stream();
		var stream2Map = stream2.map(email -> CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.email, email).content);

		List<Map<String, Object>> to = stream2Map.collect(Collectors.toList());
		CcpJsonRepresentation put7 = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.to, to);
		List<CcpJsonRepresentation> asList = Arrays.asList( put7);
		return asList;
	}

	@SuppressWarnings("serial")
	public static class CcpErrorEmailInvalidAdresses extends RuntimeException {
		private CcpErrorEmailInvalidAdresses(List<?> invalidEmails) {
			super("These following mail addresses are not valid: " + invalidEmails);
		}
	}
}
