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

import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L50
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, IdentityConfidencePredicate}

import scala.concurrent.Future
import scala.concurrent.duration._
import config.ERSAuthConnector
import connectors.AuditServiceConnector

trait ERSReturnBaseController extends FrontendController with Actions {
  val auditConnector = AuditServiceConnector
  override lazy val authConnector = ERSAuthConnector
  val maxTimeOut = 5 seconds
  val pageVisibilityPredicate = new IdentityConfidencePredicate(L50, Future.successful(Forbidden))
}
