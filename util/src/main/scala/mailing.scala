package schola
package oadmin
package utils

import org.apache.commons.mail.{ HtmlEmail, DefaultAuthenticator, MultiPartEmail }

trait MailerAPI {

  def setSubject(subject: String, args: java.lang.Object*): MailerAPI

  def setCc(ccRecipients: String*): MailerAPI

  def setBcc(bccRecipients: String*): MailerAPI

  def setRecipient(recipients: String*): MailerAPI

  def setSubject(subject: String): MailerAPI

  def setFrom(from: String): MailerAPI

  def setReplyTo(replyTo: String): MailerAPI

  def setCharset(charset: String): MailerAPI

  def addHeader(key: String, value: String): MailerAPI

  def send(bodyText: String): Unit

  def send(bodyText: String, bodyHtml: String): Unit

  def sendHtml(bodyHtml: String): Unit
}

trait MailerBuilder extends MailerAPI {

  protected val context = new ThreadLocal[collection.mutable.Map[String, List[String]]] {
    protected override def initialValue(): collection.mutable.Map[String, List[String]] = {
      collection.mutable.Map[String, List[String]]()
    }
  }

  protected def e(key: String): List[String] = {
    val splitIndex = key.indexOf("-")
    if (splitIndex >= 0)
      context.get.toList
        .filter(_._1 startsWith key.substring(0, splitIndex)) //get the keys that have the parameter key
        .map(e => e._1.substring(splitIndex + 1) + ":" + e._2.head) //column cannot be part of a header's name, so we can use this for splitting.
    else
      context.get.get(key).getOrElse(List[String]())
  }

  def setSubject(subject: String, args: AnyRef*): MailerAPI = {
    context.get += ("subject" -> List(String.format(subject, args: _*)))
    this
  }

  def setSubject(subject: String): MailerAPI = {
    context.get += ("subject" -> List(subject))
    this
  }

  def setFrom(from: String): MailerAPI = {
    context.get += ("from" -> List(from))
    this
  }

  def setCc(ccRecipients: String*): MailerAPI = {
    context.get += ("ccRecipients" -> ccRecipients.toList)
    this
  }

  def setBcc(bccRecipients: String*): MailerAPI = {
    context.get += ("bccRecipients" -> bccRecipients.toList)
    this
  }

  def setRecipient(recipients: String*): MailerAPI = {
    context.get += ("recipients" -> recipients.toList)
    this
  }

  def setReplyTo(replyTo: String): MailerAPI = {
    context.get += ("replyTo" -> List(replyTo))
    this
  }

  def setCharset(charset: String): MailerAPI = {
    context.get += ("charset" -> List(charset))
    this
  }

  def addHeader(key: String, value: String): MailerAPI = {
    context.get += ("header-" + key -> List(value))
    this
  }

  def send(bodyText: String): Unit = send(bodyText, "")

  def sendHtml(bodyHtml: String): Unit = send("", bodyHtml)

}

/**
 * providers an Emailer using apache commons-email
 * (the implementation si based on
 * the EmailNotifier trait by Aishwarya Singhal
 * and also Justin Long's gist)
 */

class CommonsMailer(smtpHost: String, smtpPort: Int, smtpSsl: Boolean, smtpTls: Boolean, smtpUser: Option[String], smtpPass: Option[String]) extends MailerBuilder {

  def send(bodyText: String, bodyHtml: String): Unit = {
    val email = createEmailer(bodyText, bodyHtml)

    email.setCharset(e("charset").headOption.getOrElse("utf-8"))
    email.setSubject(e("subject").headOption.getOrElse(""))

    e("from").foreach(setAddress(_) { (address, name) => email.setFrom(address, name) })
    e("replyTo").foreach(setAddress(_) { (address, name) => email.addReplyTo(address, name) })
    e("recipients").foreach(setAddress(_) { (address, name) => email.addTo(address, name) })
    e("ccRecipients").foreach(setAddress(_) { (address, name) => email.addCc(address, name) })
    e("bccRecipients").foreach(setAddress(_) { (address, name) => email.addBcc(address, name) })
    e("header-") foreach (e => { val split = e.indexOf(":"); email.addHeader(e.substring(0, split), e.substring(split + 1)) })

    email.setHostName(smtpHost)
    email.setSmtpPort(smtpPort)
    email.setSSLOnConnect(smtpSsl)
    email.setStartTLSEnabled(smtpTls)

    for (u <- smtpUser; p <- smtpPass) yield email.setAuthenticator(new DefaultAuthenticator(u, p))

    email.setDebug(false)
    email.send

    context.get.clear()
  }

  private def setAddress(emailAddress: String)(setter: (String, String) => Unit) = {

    if (emailAddress ne null) {
      try {
        val iAddress = new javax.mail.internet.InternetAddress(emailAddress)
        val address = iAddress.getAddress
        val name = iAddress.getPersonal

        setter(address, name)
      } catch {
        case e: Exception =>
          setter(emailAddress, null)
      }
    }
  }

  private def createEmailer(bodyText: String, bodyHtml: String): MultiPartEmail = {
    if (bodyHtml == null || bodyHtml == "") {
      val e = new MultiPartEmail
      e.setMsg(bodyText)
      e
    } else if (bodyText == null || bodyText == "")
      new HtmlEmail().setHtmlMsg(bodyHtml)
    else
      new HtmlEmail().setHtmlMsg(bodyHtml).setTextMsg(bodyText)
  }
}

/**
 * Emailer that just prints out the content to the console
 */

case object MockMailer extends MailerBuilder {

  // private val log = Logger("oadmin.mockmailer")

  def send(bodyText: String, bodyHtml: String): Unit = {
    /*log.info("MOCK MAILER: send email")
    e("from").foreach(from => log.info("FROM:" + from))
    e("replyTo").foreach(replyTo => log.info("REPLYTO:" + replyTo))
    e("recipients").foreach(to => log.info("TO:" + to))
    e("ccRecipients").foreach(cc => log.info("CC:" + cc))
    e("bccRecipients").foreach(bcc => log.info("BCC:" + bcc))
    e("header-") foreach (header => log.info("HEADER:" + header))
    if (bodyText != null && bodyText != "") {

      log.info("TEXT: " + bodyText)
    }
    if (bodyHtml != null && bodyHtml != "") {
      log.info("HTML: " + bodyHtml)
    }*/
    context.get.clear()
  }
}