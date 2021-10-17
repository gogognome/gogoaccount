package nl.gogognome.gogoaccount.component.email;

import org.junit.*;
import org.mockito.*;
import nl.gogognome.gogoaccount.component.settings.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.helpers.*;

public class EmailServiceTest {

	/**
	 * Execute this test to test sending emails manually.
	 * Check README.md for fixing issues with SSL and STARTTLS.
	 */
	@Ignore("This test will actually send an email.")
	@Test
	public void testSendingEmail() throws ServiceException {
		EmailService emailService = new EmailService(Mockito.mock(SettingsService.class), new TestTextResource());
		EmailConfiguration configuration = new EmailConfiguration();
		configuration.setSenderEmailAddress("foo@bar.nl");
		configuration.setSmtpHost("mail.server.nl");
		configuration.setSmtpPort(465);
		configuration.setSmtpEncryption(EmailConfiguration.SmtpEncryption.SSL);
		configuration.setSmtpUsername("foo@bar.nl");
		configuration.setSmtpPassword("***********");
		emailService.sendEmail("recipient@gmail.com", "Test message", "This is a test message.", "UTF-8", "text/plain", configuration);

		System.out.println("The email was sent successfully.");
	}
}