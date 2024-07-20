package com.projects.MovieTicketBookingSystem.util;

import com.projects.MovieTicketBookingSystem.constants.TicketBookingConstants;
import com.projects.MovieTicketBookingSystem.entity.Show;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Set;

@Component
public class NotificationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationUtil.class);

    @Value("${mail.config.filepath}")
    private String mailConfigFilePath;

    private static String staticMailConfigFilePath;

    @PostConstruct
    public void init(){
        staticMailConfigFilePath = mailConfigFilePath;
    }

    public static void sendConfirmationEmail(String orderId, Show show, String recipientEmailId, Set<String> seatsBooked) throws IOException, MessagingException {
        LOGGER.debug("In method sendConfirmationEmail");
//        Properties mailConfigs = FetchCredentials.loadMailConfigs();
        Properties mailProperties = new Properties();
//        Properties cp = FetchCredentials.loadCredentialsProperties();
        FileReader reader = new FileReader(staticMailConfigFilePath);
        mailProperties.load(reader);
        Session mailSession = Session.getInstance(mailProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailProperties.getProperty("mail.username"), mailProperties.getProperty("mail.password"));
            }
        });
        String header = "Movie ticket booking confirmation for {0}";
        header = MessageFormat.format(header, show.getMovie().getName());

        String body = getMailBody(orderId, show, seatsBooked);
        Message message = new MimeMessage(mailSession);
        message.setSubject(header);
        message.setText(body);
        message.setFrom(new InternetAddress(TicketBookingConstants.EMAIL_FROM));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmailId));
        Transport.send(message);
    }

    private static String getMailBody(String orderId, Show show, Set<String> seatsBooked) {
        LOGGER.debug("In method getMailBody");
        StringBuffer body = new StringBuffer();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        body.append("Movie : ")
                .append(show.getMovie().getName())
                .append("\n\n")
                .append("Venue : ")
                .append(show.getMultiplex().getName())
                .append(show.getMultiplex().getAddress())
                .append("\n\n")
                .append("Show time : ")
                .append(formatter.format(show.getStartTime()))
                .append("\n\n")
                .append("Seats chosen : ")
                .append(String.join(", ",seatsBooked));
        return body.toString();
    }
}
