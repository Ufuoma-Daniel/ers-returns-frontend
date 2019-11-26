/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import play.Logger
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import play.api.i18n.Lang
import play.api.mvc.Call

import scala.util.Try
import controllers.routes
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import scala.concurrent.duration._
import scala.concurrent.duration.Duration

trait ApplicationConfig extends AppName {

  val assetsPrefix: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val validatorUrl: String

  val upscanInitiateHost: String
  val upscanRedirectBase: String

  val enableRetrieveSubmissionData: Boolean
  val sentViaSchedulerNoOfRowsLimit: Int
  val languageTranslationEnabled: Boolean
  val urBannerToggle:Boolean
  val urBannerLink: String

  val ggSignInUrl: String
  def languageMap: Map[String, Lang]
  def routeToSwitchLanguage: String => Call

  def reportAProblemPartialUrl: String

  val csvCacheInProgressRetryAmount: Int
  val csvCacheCompletedRetryAmount: Int
  val retryDelay: FiniteDuration
}

class ApplicationConfigImpl extends ApplicationConfig with ServicesConfig {

  val contactHost = baseUrl("contact-frontend")
  private lazy val _reportAProblemPartialUrl = s"$contactHost/contact/problem_reports?secure=false"

  override def reportAProblemPartialUrl: String = _reportAProblemPartialUrl

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  override def appNameConfiguration: Configuration = runModeConfiguration

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFormServiceIdentifier = "ERS"

  override lazy val assetsPrefix: String = loadConfig("assets.url") + loadConfig("assets.version")
  override lazy val analyticsToken: Option[String] = configuration.getString("govuk-tax.google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString("govuk-tax.google-analytics.host").getOrElse("service.gov.uk")

  override lazy val validatorUrl: String = baseUrl("ers-file-validator") + "/ers/:empRef/" + loadConfig("microservice.services.ers-file-validator.url")

  override val upscanInitiateHost: String = baseUrl("upscan")
  override val upscanRedirectBase: String = configuration.getString("microservice.services.upscan.redirect-base").get

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

  val csvCacheInProgressRetryAmount: Int = runModeConfiguration.getInt("retry.csv-callback-cache.in-progress.amount").getOrElse(1)
  val csvCacheCompletedRetryAmount: Int = runModeConfiguration.getInt("retry.csv-callback-cache.completed-upload.amount").getOrElse(1)
  override val retryDelay: FiniteDuration = (runModeConfiguration.getMilliseconds("retry.delay").get) milliseconds
}

object ApplicationConfig extends ApplicationConfigImpl

