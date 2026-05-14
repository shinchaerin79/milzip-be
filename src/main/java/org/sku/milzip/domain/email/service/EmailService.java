package org.sku.milzip.domain.email.service;

import java.security.SecureRandom;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private static final String VERIFICATION_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final JavaMailSender mailSender;

  public String generateVerificationCode() {
    StringBuilder code = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      code.append(RANDOM.nextInt(10));
    }
    return code.toString();
  }

  public String generateTemporaryPassword() {
    String chars = VERIFICATION_CHARS + "!@#$%^&*";
    StringBuilder password = new StringBuilder(12);
    for (int i = 0; i < 12; i++) {
      password.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }
    return password.toString();
  }

  public void sendVerificationEmail(String to, String code) {
    log.info("[EmailService] 이메일 인증 코드 발송 요청 - to: {}", to);
    send(to, "[밀집] 이메일 인증 코드", buildVerificationBody(code));
    log.info("[EmailService] 이메일 인증 코드 발송 완료 - to: {}", to);
  }

  public void sendTemporaryPasswordEmail(String to, String tempPassword) {
    log.info("[EmailService] 임시 비밀번호 발송 요청 - to: {}", to);
    send(to, "[밀집] 임시 비밀번호 안내", buildTemporaryPasswordBody(tempPassword));
    log.info("[EmailService] 임시 비밀번호 발송 완료 - to: {}", to);
  }

  private void send(String to, String subject, String body) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(body, true);
      mailSender.send(message);
    } catch (MailAuthenticationException e) {
      log.error(
          "[EmailService] 메일 서버 인증 실패 - MAIL_USERNAME/MAIL_PASSWORD 환경변수 또는 Gmail 앱 비밀번호 확인 필요", e);
      throw new CustomException(AuthErrorCode.EMAIL_AUTH_FAILED);
    } catch (MailException e) {
      log.error("[EmailService] 이메일 전송 실패 - to: {}, subject: {}", to, subject, e);
      throw new CustomException(AuthErrorCode.EMAIL_SEND_FAILED);
    } catch (MessagingException e) {
      log.error("[EmailService] 이메일 메시지 구성 실패 - to: {}", to, e);
      throw new CustomException(AuthErrorCode.EMAIL_SEND_FAILED);
    }
  }

  private String buildVerificationBody(String code) {
    return "<p>아래 인증 코드를 입력해 주세요.</p>"
        + "<h2 style=\"letter-spacing:4px\">"
        + code
        + "</h2>"
        + "<p>코드는 5분간 유효합니다.</p>";
  }

  private String buildTemporaryPasswordBody(String tempPassword) {
    return "<p>임시 비밀번호가 발급되었습니다. 로그인 후 반드시 비밀번호를 변경해 주세요.</p>"
        + "<h2 style=\"letter-spacing:2px\">"
        + tempPassword
        + "</h2>";
  }
}
