package com.ccp.implementations.email.sendgrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.email.CcpEmailProvider;
import com.ccp.especifications.email.CcpEmailSuppressionList;
import com.ccp.especifications.http.CcpHttpContentType;
import com.ccp.especifications.http.CcpHttpHandler;
import com.ccp.especifications.http.CcpHttpMethods;
import com.ccp.especifications.http.CcpHttpResponseType;
import com.ccp.implementations.email.sendgrid.SendGridEmailProviderSpecialWords.JsonFieldNames;
import java.util.stream.Stream;



import com.ccp.json.fields.validation.CcpJsonCommonsFields;
//TODO FIX THE SENDGRID ACCOUNT
/**
 * {@code CcpEmailProvider} implementation using the SendGrid API. Builds the JSON payload with
 * sender, recipients, subject and body, and sends it via {@code POST} authenticated with a Bearer token.
 * Validates the e-mail addresses before sending; throws {@code CcpErrorEmailInvalidAdresses} if
 * any address is invalid.
 * <p>
 * Reads the suppression lists with {@code GET <providerUrl>/<list>}, that is,
 * {@code https://api.sendgrid.com/v3/suppression/bounces} and its siblings, which return at most 500 records per request.
 * Only status 200 is accepted, so a rate limit (429) or an invalid key raises {@code CcpErrorHttp}. Going through
 * {@code CcpHttpResponseType.listRecord} a mapped 429 would not help: the list is parsed before the flow of the status.
 */
class SendGridEmailProvider implements CcpEmailProvider {
	/** The single instance: the class keeps no state. */
	public final static SendGridEmailProvider INSTANCE = new SendGridEmailProvider();

	/** The largest page of a suppression list that SendGrid accepts. */
	private static final int PAGE_SIZE = 500;

	private SendGridEmailProvider() {}

	/** Texts of the SendGrid requests. */
	enum Fields implements CcpJsonFieldName {
		/** The name of the reading of a suppression list in error messages. */
		getSuppressedEmails,
		/** Separates the base URL from the list. */
		PATH_SEPARATOR { public String getValue() { return "/"; } },
		/** The page size parameter, always the first one. */
		PARAMETER_LIMIT { public String getValue() { return "?limit="; } },
		/** The start of the time range parameter. */
		PARAMETER_START_TIME { public String getValue() { return "&start_time="; } },
		/** The parameter that skips the records of the previous pages. */
		PARAMETER_OFFSET { public String getValue() { return "&offset="; } },
		/** The authentication scheme, with its trailing space. */
		BEARER { public String getValue() { return "Bearer "; } }
	}

	/**
	 * Validates the recipients and posts the message to SendGrid with a Bearer token, expecting status 202. The
	 * {@code content.type} is the MIME type ({@code text/plain}/{@code text/html}); until 2026-10-06 it was the enum
	 * constant name, which SendGrid refuses. The template id is not sent on purpose: it is the key of the system's own
	 * message template, already resolved into {@code message}, not a SendGrid template.
	 * @param providerToken the SendGrid API key
	 * @param providerUrl the SendGrid send URL
	 * @param templateId not used (see above)
	 * @param sender the sender address
	 * @param subject the subject
	 * @param message the body
	 * @param contentType the content type
	 * @param emails the recipients
	 * @return the body sent to SendGrid
	 * @throws CcpErrorEmailInvalidAdresses when a recipient is not a valid e-mail
	 */
	public CcpJsonRepresentation sendSimpleTextEmailMessage(String providerToken, String providerUrl, String templateId, String sender, String subject, String message, CcpHttpContentType contentType, String... emails) {
		CcpJsonRepresentation body = this.sendSimpleTextEmailMessage(providerToken, providerUrl, templateId, sender, subject, message, contentType, CcpOtherConstants.EMPTY_JSON, emails);
		return body;
	}

	/**
	 * Posts the message with its custom headers, which go in the {@code headers} object of the body; with none, the
	 * object is left out.
	 * @param providerToken the SendGrid API key
	 * @param providerUrl the SendGrid send URL
	 * @param templateId not used (see {@link #sendSimpleTextEmailMessage(String, String, String, String, String, String, CcpHttpContentType, String...)})
	 * @param sender the sender address
	 * @param subject the subject
	 * @param message the body
	 * @param contentType the content type
	 * @param emailHeaders the value of each header, by its name
	 * @param emails the recipients
	 * @return the body sent to SendGrid
	 * @throws CcpErrorEmailInvalidAdresses when a recipient is not a valid e-mail
	 */
	public CcpJsonRepresentation sendSimpleTextEmailMessage(String providerToken, String providerUrl, String templateId, String sender, String subject, String message, CcpHttpContentType contentType, CcpJsonRepresentation emailHeaders, String... emails) {

		CcpHttpHandler ccpHttpHandler = new CcpHttpHandler(202, providerUrl);
		String bearerToken = this.getBearerToken(providerToken);
		CcpJsonRepresentation headersWithAuthorization = CcpOtherConstants.EMPTY_JSON
				.put(CcpJsonCommonsFields.Authorization, bearerToken);
				CcpJsonRepresentation headersWithUserAgent = headersWithAuthorization
				.put(SendGridEmailProviderSpecialWords.User_Agent, "sendgrid/3.0.0;java");

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
						
				.put(CcpJsonCommonsFields.type, contentType.mimeType);
				CcpJsonRepresentation contentWithTypeAndValue = contentWithType
				.put(CcpJsonCommonsFields.value, message);

				CcpJsonRepresentation bodyWithContent = bodyWithPersonalizations
				.addToList(JsonFieldNames.content, contentWithTypeAndValue)
				;
		boolean noEmailHeaders = emailHeaders.isEmpty();
		CcpJsonRepresentation body = noEmailHeaders ? bodyWithContent : bodyWithContent.put(JsonFieldNames.headers, emailHeaders);

		//		this.throwFakeServerErrorToTestingProcessFlow();
		ccpHttpHandler.executeHttpRequest("sendEmail", CcpHttpMethods.POST, headers, body, CcpHttpResponseType.singleRecord);
		return body;
	}

	/**
	 * Builds the single personalization with every recipient, after validating them.
	 * @param emails the recipients
	 * @return the personalizations
	 * @throws CcpErrorEmailInvalidAdresses when a recipient is not a valid e-mail
	 */
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

	/**
	 * Reads every page of the list.
	 * @param providerToken the SendGrid API key
	 * @param providerUrl the base URL of the suppression lists
	 * @param suppressionList the list to read
	 * @return every record of the list
	 * @throws CcpErrorHttp when a page does not answer 200
	 */
	public List<CcpJsonRepresentation> getSuppressedEmails(String providerToken, String providerUrl, CcpEmailSuppressionList suppressionList) {
		String listUrl = this.getListUrl(providerUrl, suppressionList);
		List<CcpJsonRepresentation> records = this.readAllPages(providerToken, listUrl);
		return records;
	}

	/**
	 * Reads every page of the records created from a moment on.
	 * @param providerToken the SendGrid API key
	 * @param providerUrl the base URL of the suppression lists
	 * @param suppressionList the list to read
	 * @param startTimeInSeconds the moment, inclusive, as a Unix timestamp in seconds
	 * @return the records created from that moment on
	 * @throws CcpErrorHttp when a page does not answer 200
	 */
	public List<CcpJsonRepresentation> getSuppressedEmailsCreatedSince(String providerToken, String providerUrl, CcpEmailSuppressionList suppressionList, long startTimeInSeconds) {
		String listUrl = this.getListUrl(providerUrl, suppressionList);
		String parameterStartTime = Fields.PARAMETER_START_TIME.getValue();
		String listUrlWithStartTime = listUrl + parameterStartTime + startTimeInSeconds;
		List<CcpJsonRepresentation> records = this.readAllPages(providerToken, listUrlWithStartTime);
		return records;
	}

	/**
	 * Builds the value of the {@code Authorization} header.
	 * @param providerToken the SendGrid API key
	 * @return the header value
	 */
	private String getBearerToken(String providerToken) {
		String bearer = Fields.BEARER.getValue();
		String bearerToken = bearer + providerToken;
		return bearerToken;
	}

	/**
	 * Builds the URL of the list, with the page size.
	 * @param providerUrl the base URL of the suppression lists
	 * @param suppressionList the list to read
	 * @return the URL, ready for more parameters
	 */
	private String getListUrl(String providerUrl, CcpEmailSuppressionList suppressionList) {
		String pathSeparator = Fields.PATH_SEPARATOR.getValue();
		String listName = suppressionList.name();
		String parameterLimit = Fields.PARAMETER_LIMIT.getValue();
		String listUrl = providerUrl + pathSeparator + listName + parameterLimit + PAGE_SIZE;
		return listUrl;
	}

	/**
	 * Requests one page after the other. A page shorter than {@link #PAGE_SIZE} is the last one; a list whose size is a
	 * multiple of the page size ends with an empty page.
	 * @param providerToken the SendGrid API key
	 * @param listUrl the URL of the list, with its filters
	 * @return every record found
	 * @throws CcpErrorHttp when a page does not answer 200
	 */
	private List<CcpJsonRepresentation> readAllPages(String providerToken, String listUrl) {
		String bearerToken = this.getBearerToken(providerToken);
		CcpJsonRepresentation headersWithAuthorization = CcpOtherConstants.EMPTY_JSON
				.put(CcpJsonCommonsFields.Authorization, bearerToken);
		CcpJsonRepresentation headers = headersWithAuthorization
				.put(CcpJsonCommonsFields.Accept, "application/json");
		String parameterOffset = Fields.PARAMETER_OFFSET.getValue();
		String trace = Fields.getSuppressedEmails.getValue();
		List<CcpJsonRepresentation> records = new ArrayList<>();
		int offset = 0;

		while(true) {
			String pageUrl = listUrl + parameterOffset + offset;
			CcpHttpHandler ccpHttpHandler = new CcpHttpHandler(200, pageUrl);
			List<CcpJsonRepresentation> page = ccpHttpHandler.executeHttpRequest(trace, CcpHttpMethods.GET, headers, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.listRecord);
			records.addAll(page);
			int recordsInThePage = page.size();
			boolean isTheLastPage = recordsInThePage < PAGE_SIZE;

			if(isTheLastPage) {
				return records;
			}
			offset += PAGE_SIZE;
		}
	}

	/** Raised when some recipient is not a valid e-mail address. */
	@SuppressWarnings("serial")
	public static class CcpErrorEmailInvalidAdresses extends RuntimeException {
		/**
		 * Builds the error listing the invalid addresses.
		 * @param invalidEmails the invalid addresses
		 */
		private CcpErrorEmailInvalidAdresses(List<?> invalidEmails) {
			super("These following mail addresses are not valid: " + invalidEmails);
		}
	}
}
