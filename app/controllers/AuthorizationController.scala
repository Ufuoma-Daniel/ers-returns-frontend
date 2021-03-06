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

package controllers

import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import scala.concurrent.Future
import connectors.AuthenticationConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import utils.ExternalUrls
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
// $COVERAGE-OFF$
object AuthorizationController extends AuthorizationController {
}

trait AuthorizationController extends FrontendController
with Actions
with AuthenticationConnector
with Authenticator {

  def notAuthorised() = AuthorisedForAsync() {
    implicit user =>
      implicit request =>
      Future.successful(Ok(views.html.not_authorised.render(request, context)))
  }

  def timedOut() = UnauthorisedAction {
    implicit request =>
      val loginScreenUrl = ExternalUrls.portalDomain
      Ok(views.html.signedOut(loginScreenUrl))
  }

}
