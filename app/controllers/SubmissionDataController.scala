/*
 * Copyright 2017 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.ErsConnector
import models.SchemeInfo
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.Future

object SubmissionDataController extends SubmissionDataController {
  override val ersConnector: ErsConnector = ErsConnector
}

trait SubmissionDataController extends ERSReturnBaseController with Authenticator {

  val ersConnector: ErsConnector

  def createSchemeInfoFromURL(request: Request[Any]): Option[JsObject] = {

    val schemeRef: Option[String] = request.getQueryString("schemeRef")
    val timestamp: Option[String] = request.getQueryString("confTime")

    if(schemeRef.isDefined && timestamp.isDefined) {
      Some(
        Json.obj(
          "schemeRef" -> schemeRef.get,
          "confTime" -> timestamp.get
        )
      )
    }
    else {
      None
    }

  }

  def retrieveSubmissionData() = AuthorisedForAsync() {
    implicit user =>
      implicit request =>
        getRetrieveSubmissionData()(user, request, hc)
  }

  def getRetrieveSubmissionData()(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {

    Logger.debug("Retrieve Submission Data Request")

    if(ApplicationConfig.enableRetrieveSubmissionData) {

      Logger.debug("Retrieve SubmissionData Enabled")

      val data: Option[JsObject] = createSchemeInfoFromURL(request)
      if (data.isDefined) {

        ersConnector.retrieveSubmissionData(data.get).map { res =>
          res.status match {
            case 200 => Ok(res.body)
            case _ => {
              Logger.error(s"RetrieveSubmissionData status: ${res.status}")
              getGlobalErrorPage
            }
          }
        }.recover {
          case ex: Exception => {
            Logger.error(s"RetrieveSubmissionData Exception: ${ex.getMessage}")
            getGlobalErrorPage
          }
        }

      }
      else {
        Future.successful(NotFound(views.html.global_error(Messages("ers_not_found.title"), Messages("ers_not_found.heading"), Messages("ers_not_found.message"))))
      }
    }
    else {
      Logger.debug("Retrieve SubmissionData Disabled")
      Future.successful(NotFound(views.html.global_error(Messages("ers_not_found.title"), Messages("ers_not_found.heading"), Messages("ers_not_found.message"))))
    }
  }

  def getGlobalErrorPage = Ok(views.html.global_error(Messages("ers.global_errors.title"), Messages("ers.global_errors.heading"), Messages("ers.global_errors.message")))

}
