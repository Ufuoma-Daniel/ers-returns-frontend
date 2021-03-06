/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig
import play.Logger
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import play.api.i18n.Lang
import play.api.mvc.Call

import scala.util.Try
import controllers.routes
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever

trait ApplicationConfig {

  val assetsPrefix: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val uploadCollection: String
  val validatorUrl: String

  val platformHostUrl: String
  val successPageUrl: String
  val failurePageUrl: String
  val callbackPageUrl: String

  val successCsvPageUrl: String
  val failureCsvPageUrl: String
  val callbackCsvPageUrl: String
  val enableRetrieveSubmissionData: Boolean
  val sentViaSchedulerNoOfRowsLimit: Int
  val languageTranslationEnabled: Boolean
  val urBannerToggle:Boolean
  val urBannerLink: String

  val ggSignInUrl: String
  def languageMap: Map[String, Lang]
  def routeToSwitchLanguage: String => Call

  def reportAProblemPartialUrl: String
}

class ApplicationConfigImpl extends ApplicationConfig with ServicesConfig {

  val contactHost = baseUrl("contact-frontend")
  private lazy val _reportAProblemPartialUrl = s"$contactHost/contact/problem_reports?secure=false"

  override def reportAProblemPartialUrl: String = _reportAProblemPartialUrl

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFormServiceIdentifier = "ERS"

  override lazy val assetsPrefix: String = loadConfig("assets.url") + loadConfig("assets.version")
  override lazy val analyticsToken: Option[String] = configuration.getString("govuk-tax.google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString("govuk-tax.google-analytics.host").getOrElse("service.gov.uk")
  override lazy val uploadCollection: String = loadConfig("settings.upload-collection")

  override lazy val validatorUrl: String = baseUrl("ers-file-validator") + "/ers/:empRef/" + loadConfig("microservice.services.ers-file-validator.url")

  private val frontendHost = loadConfig("platform.frontend.host")
  override lazy val platformHostUrl = Try{baseUrl("ers-returns-frontend")}.getOrElse("")
  override lazy val successPageUrl: String = frontendHost + loadConfig("microservice.services.ers-returns-frontend.success-page")
  override lazy val failurePageUrl: String = frontendHost + loadConfig("microservice.services.ers-returns-frontend.failure-page")
  override lazy val callbackPageUrl: String = platformHostUrl + loadConfig("microservice.services.ers-returns-frontend.callback-page")
  override lazy val successCsvPageUrl: String = frontendHost + loadConfig("microservice.services.ers-returns-frontend.csv-success-page")
  override lazy val failureCsvPageUrl: String = frontendHost + loadConfig("microservice.services.ers-returns-frontend.csv-failure-page")
  override lazy val callbackCsvPageUrl: String = platformHostUrl + loadConfig("microservice.services.ers-returns-frontend.csv-callback-page")

  override lazy val urBannerToggle:Boolean = loadConfig("urBanner.toggle").toBoolean
  override lazy val urBannerLink: String = loadConfig("urBanner.link")

  override val ggSignInUrl: String = configuration.getString("government-gateway-sign-in.host").getOrElse("")

  override lazy val enableRetrieveSubmissionData: Boolean = Try(loadConfig("settings.enable-retrieve-submission-data").toBoolean).getOrElse(false)
  override lazy val sentViaSchedulerNoOfRowsLimit: Int = {
    Logger.info("sent-via-scheduler-noofrows vakue is " + Try(loadConfig("sent-via-scheduler-noofrows").toInt).getOrElse(10000))
    Try(loadConfig("sent-via-scheduler-noofrows").toInt).getOrElse(10000)
  }

  override lazy val languageTranslationEnabled = runModeConfiguration.getBoolean("microservice.services.features.welsh-translation").getOrElse(true)

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))
  def routeToSwitchLanguage = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)
}

object ApplicationConfig extends ApplicationConfigImpl

