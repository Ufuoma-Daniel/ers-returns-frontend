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

package controllers

import _root_.models._
import connectors.ErsConnector
import metrics.Metrics
import org.joda.time.DateTime
import play.Logger
import play.api.Play
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.{AllowAll, AuthContext}
import utils._
import config._
import scala.concurrent.Future
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.http.HeaderCarrier

object ReturnServiceController extends ReturnServiceController {
  override val cacheUtil: CacheUtil = CacheUtil

  override val accessThreshold = Play.current.configuration.getInt("accessThreshold").getOrElse(100)
  override val accessDeniedUrl = "/outage-ers-frontend/index.html"
  override val metrics = Metrics
}

trait ErsConstants {
  val screenSchemeInfo = "screenSchemeInfo"
  val schemeId = "schemeId"
  val schemeRef = "schemeRef"
  implicit val context: ErsContext = ErsContextImpl
}

trait ReturnServiceController extends ERSReturnBaseController with Authenticator with ErsConstants {

  val accessDeniedUrl: String

  val cacheUtil: CacheUtil

  val accessThreshold: Int
  val metrics: Metrics

  def cacheParams(ersRequestObject: RequestObject)(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {

    implicit val formatRSParams = Json.format[ErsMetaData]

    Logger.debug("Request Object created --> " + ersRequestObject)
    cacheUtil.cache(CacheUtil.ersRequestObject, ersRequestObject).map {
      cacheResult => {
        Logger.debug("Request Object Cached --> " + ersRequestObject); showInitialStartPage(ersRequestObject)(authContext, request, hc)
      }
    } recover { case e: Exception =>
      Logger.warn(s"Caught exception ${e.getMessage}", e)
      getGlobalErrorPage
    }
  }

  def getRequestParameters(request: Request[AnyContent]): RequestObject = {
    val aoRef: Option[String] = request.getQueryString("aoRef")
    val taxYear: Option[String] = request.getQueryString("taxYear")
    val ersSchemeRef: Option[String] = request.getQueryString("ersSchemeRef")
    val schemeType: Option[String] = request.getQueryString("schemeType")
    val schemeName: Option[String] = request.getQueryString("schemeName")
    val agentRef: Option[String] = request.getQueryString("agentRef")
    val empRef: Option[String] = request.getQueryString("empRef")
    val ts: Option[String] = request.getQueryString("ts")
    val hmac: Option[String] = request.getQueryString("hmac")
    val reqObj = RequestObject(aoRef, taxYear, ersSchemeRef, schemeName, schemeType, agentRef, empRef, ts, hmac)
    Logger.warn(s"Request Parameters:  ${reqObj.toString}")
    reqObj
  }

  def hmacCheck() = AuthenticatedBy(ERSGovernmentGateway, pageVisibility = AllowAll).async {
    implicit user =>
      implicit request =>
        Logger.warn("HMAC Check Authenticated")
        if (request.getQueryString("ersSchemeRef").getOrElse("") == "") {
          Logger.warn("Missing SchemeRef in URL")
          Future(getGlobalErrorPage)
        } else {
          if (HMACUtil.isHmacAndTimestampValid(getRequestParameters(request))) {
            Logger.warn("HMAC Check Valid")
            try {
              cacheParams(getRequestParameters(request))
            } catch {
              case e: Throwable => Logger.warn(s"Caught exception ${e.getMessage}", e)
                Future(getGlobalErrorPage)
            }
          } else {
            Logger.warn("HMAC Check Invalid")
            showUnauthorisedPage(request)
          }
        }
  }

  def showInitialStartPage(requestObject: RequestObject)(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Result = {

    val sessionData = s"${requestObject.getSchemeId} - ${requestObject.getPageTitle}"
    Ok(views.html.start(requestObject)).
      withSession(request.session + (screenSchemeInfo -> sessionData) - "bundelRef" - "dateTimeSubmitted")
  }

  def startPage(): Action[AnyContent] = AuthenticatedBy(ERSGovernmentGateway, pageVisibility = AllowAll).async {
    implicit user =>
      implicit request =>
        cacheUtil.fetch[RequestObject](CacheUtil.ersRequestObject).map{
          result =>
            Ok(views.html.start(result)).withSession(request.session - "bundelRef" - "dateTimeSubmitted")
        }
  }

  def showUnauthorisedPage(request: Request[AnyRef]): Future[Result] = {
    Future.successful(Ok(views.html.unauthorised()(request, context)))
  }

  def getGlobalErrorPage(implicit messages: Messages) = Ok(views.html.global_error(
    messages("ers.global_errors.title"),
    messages("ers.global_errors.heading"),
    messages("ers.global_errors.message"))(messages))
}
